# 学习进度存储：Room 数据库

记录日期：2026-09-17

把学习进度从 SharedPreferences 换成本地 Room 数据库，目的是给后续的**间隔重复**与**学习报表**留出可查询的结构（例如「按最后答题时间排复习队列」「按正确率排序」「按时间看历史」）。本次只做存储层替换与表结构设计，界面与交互行为不变。

## 表结构

`ogden-progress.db`，`ProgressDatabase` 版本 1，`exportSchema = true`，架构 json 导出到 `app/schemas/`（`kapt` 的 `room.schemaLocation`），日后迁移可直接 diff。

`word_progress`（`WordProgressEntity`，一词一行）：
- `word` TEXT 主键
- `favorite` / `mistake` BOOLEAN：收藏、错词本
- `mastery` INT：0-3 的掌握星星，建索引（报表要按掌握度筛选/排序）
- `attempts` / `correct` INT：练习次数与答对次数，正确率 = correct / attempts，暂不落冗余列
- `lastAnsweredAt` INT(Long)：最后答题时间戳，**建索引**，是间隔重复排序的主键字段

`level_progress`（`LevelProgressEntity`，复合主键 `category` + `level`）：
- `category` TEXT：`Category.code`（op / gt / pt / qg / qo）
- `level` INT
- `completedAt` INT(Long)：通关时间，留给学习报表的时间线

旧存储只有 `level.<code>.<n>.complete = true` 一个布尔键，没有时间；新表用「存在即通关」，`isLevelComplete` 查行是否存在，语义与旧版一致。

## 标量为什么留在 SharedPreferences

`streak`、`lastStudyDay`、`lastCategory`、`lastLevel`、`accent` 仍在名为 `ogden-progress` 的 SharedPreferences 里。理由：它们都是单值标量，读取发生在组合期间（`dailyStreak()`、`lastCategory()`、`savedAccent()` 都是同步返回值），SharedPreferences 本身就是内存里的一份 map，读它不涉及磁盘 I/O；为四个数字单开一张单行表，只会把同步读变成「异步读 + 又一层快照缓存」，代码更多、收益为零。等真要做「历史趋势」时，学习日历应该是一张**每日一行**的新表（`study_day(day, answered, correct)`），而不是把现在这个单值 `streak` 塞进数据库，所以现在搬它没有前瞻价值。

`accent` 是发音偏好，属于界面设置而非学习进度，本来就不该进进度库，继续留在 SharedPreferences。

## 从 SharedPreferences 迁移

老用户的进度全在偏好里，不能丢。`ProgressStore` 初始化时在 IO 线程执行 `importLegacyPrefsIfNeeded()`：
1. 若 `roomImported = true` 直接跳过（幂等开关）。
2. 否则取 `prefs.all` 快照，用纯函数 `legacyWordProgress(map)` / `legacyLevelProgress(map)` 映射成实体：`favorites`/`mistakes` 两个 string set、`mastery.*`/`attempts.*`/`correct.*`/`last.*` 四组键合并成逐词行；`level.<code>.<n>.complete == true` 的键变成关卡行。
3. `REPLACE` 批量写库，**写成功后**才 `putBoolean("roomImported", true).commit()`。写库抛异常则标志不落地，下次启动重新导入。
4. **不删除任何旧键**。旧键留在偏好里既是回滚余地，也避免「删了旧键但库写失败」的丢数据窗口；它们对新代码是死数据，只占几十 KB。

重置进度：内存快照立即清零 → 用纯函数 `resettableProgressKeys` 选键删偏好（**保留 `accent`**，也**保留 `roomImported`**：删掉它会让残留的旧键在下次启动被重新导入，等于「重置无效」）→ 协程里 `clearWordProgress()` + `clearLevelProgress()`。

## 线程模型与可观测性

Room 不能在主线程碰，同时上一轮性能优化明确要求组合期间不做 I/O，所以沿用既有的「内存快照缓存 + 异步落库」模式，`ProgressStore` 的同步读 API（`progress(word)` 返回值而不是 Flow）保持原样，绝大多数调用点一行没改：
- 读：`entries` / `levels` 仍是 `mutableStateMapOf`，`streak` 仍是 `mutableStateOf`；`ChallengeScreen` / `ReviewScreen` 的 `derivedStateOf`、`LibraryScreen` 行内读取、`WordDetailScreen` 直接读，全部照旧生效。
- 写：先改快照（界面立刻刷新），再 `scope.launch(Dispatchers.IO)` 整行覆盖写库。落库失败只会丢最后一次写，不会让界面与库长期不一致（下次启动以库为准）。
- 构造：`ProgressStore(context, scope)` 多了一个 `CoroutineScope` 参数，由 `OgdenKidsApp` 的 `rememberCoroutineScope()` 提供；没有引入 ViewModel、Hilt、DataStore。
- 首帧：数据库首读是异步的，`ProgressStore.ready` 为 false 时与词库未加载走同一个分支显示 `LoadingScreen`，宁可多等一瞬，也不让首页统计先闪一次 0 再跳成真实值。
- `lastAnsweredAt` 不参与界面显示，用普通 `mutableMapOf` 缓存，只在整行覆盖写时带上，不触发重组。

## 有意留给后续的部分

- `ProgressDao.wordsDueForReview(mastery, limit)`：未掌握的词按 `lastAnsweredAt ASC, mastery ASC` 排序（从未答过的 `0` 自然排最前）。这是本次换数据库的直接理由，先建好查询与索引，**目前还没有界面调用**。
- 没有实现 SM-2 之类的调度算法，也没有 `interval` / `easeFactor` / `dueAt` 列；真做的时候是版本 2 的一次加列迁移。
- 没有答题历史明细表（每次答题一行）与每日汇总表，「历史趋势」报表要用时再加，不影响现有表。
- 纯逻辑已抽成可单测的函数并补了测试：`nextProgress`（星星上限 3 / 下限 0 / 掌握度归零也进错词本）、`nextStreak`（同日不变 / 隔日 +1 / 断档归 1）、`legacyWordProgress` 与 `legacyLevelProgress`（迁移映射）、`resettableProgressKeys`。Room 本身的行为不做 Robolectric / 仪器测试。
