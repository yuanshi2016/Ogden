# 学习进度存储：Room 数据库

记录日期：2026-09-17（SM-2 接线更新：2026-09-23）

把学习进度从 SharedPreferences 换成本地 Room 数据库，目的是给**间隔重复（SM-2）**与**学习报表**留出可查询的结构。界面与交互行为尽量不变，读写仍走 `ProgressStore` 内存快照。

## 表结构

`ogden-progress.db`，`ProgressDatabase` **版本 5**，`exportSchema = true`，架构 json 导出到 `app/schemas/`（`kapt` 的 `room.schemaLocation`），日后迁移可直接 diff。

版本演进：

| 版本 | 变更 |
|------|------|
| 1 | `word_progress` + `level_progress` |
| 2 | `daily_activity` |
| 3 | `category_reward` |
| 4 | `earned_reward` |
| 5 | `word_progress` 增加 SM-2 三列 + `dueAt` 索引（`MIGRATION_4_5`：`ALTER TABLE`） |

`word_progress`（`WordProgressEntity`，一词一行）：

- `word` TEXT 主键
- `favorite` / `mistake` BOOLEAN：收藏、错词本
- `mastery` INT：0–3 的掌握星星，建索引
- `attempts` / `correct` INT：练习次数与答对次数，正确率 = correct / attempts
- `lastAnsweredAt` INT(Long)：最后答题时间戳，建索引
- **`intervalDays` REAL**：SM-2 当前间隔（天），默认 `0.0`（尚未进入间隔重复）
- **`easeFactor` REAL**：SM-2 易度因子，默认 `2.5`（`Sm2State.DEFAULT_EASE`）
- **`dueAt` INT(Long)**：下次应复习的 epoch millis，默认 `0`；**建索引**。`0` 表示旧数据或「已到期」兼容值

`level_progress` / `daily_activity` / `category_reward` / `earned_reward` 见历史版本说明，本轮未改。

### MIGRATION_4_5

```sql
ALTER TABLE `word_progress` ADD COLUMN `intervalDays` REAL NOT NULL DEFAULT 0;
ALTER TABLE `word_progress` ADD COLUMN `easeFactor` REAL NOT NULL DEFAULT 2.5;
ALTER TABLE `word_progress` ADD COLUMN `dueAt` INTEGER NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS `index_word_progress_dueAt` ON `word_progress` (`dueAt`);
```

旧行三列保持默认，复习队列里 `dueAt == 0` 视为已到期。

## SM-2 与答题落库

纯函数在 `data/Sm2.kt`：`nextSm2(Sm2State, correct, nowMillis)`。

- 答错：间隔约 10 分钟（`FAIL_INTERVAL_DAYS`），易度 −0.2（下限 1.3），`dueAt = now + interval`
- 答对：经典序列 0→1→6→×EF，易度 +0.1（上限 3.0），`dueAt = now + intervalDays`

`ProgressStore.record`：先 `nextProgress` 更新 mastery/attempts，再 `nextSm2` 写入 `WordProgress.intervalDays/easeFactor/dueAt`，内存 `entries` 与 Room 整行覆盖一并持久化。

## 智能复习排序

`selectDueForReview`（`ProgressBackup.kt`，内存快照）与 `ProgressDao.wordsDueForReview` 对齐：

1. `mastery < 3` 且 `attempts > 0`
2. `dueAt == 0` **或** `dueAt <= now`（未到期不进队列）
3. 排序：`dueAt ASC`，`mastery ASC`，词序稳定

## 标量为什么留在 SharedPreferences

`streak`、`lastStudyDay`、`lastCategory`、`lastLevel`、`accent` 以及主题/跟读/提醒等偏好仍在 `ogden-progress` SharedPreferences。它们是单值标量或 UI 设置，组合期同步读；不把偏好塞进进度库。

相关偏好键（重置进度时保留）：`speakLevel`、`speakCalibrationSamples` / `speakThresholds`（U1）、`reviewReminderEnabled` / `reviewReminderHour` / `reminderDueCount`（U2，默认关、本地 19:00）。

课本周次（进度侧，**重置时清掉**）：`pep.currentWeek`（手动第 N 周，0=未设）、`pep.termStartDay`（开学日本地纪元日，0=未设）。与 `pep.lastUnitId` / `pep.lastUnitId.g{N}` / `pep.daycheck.*` / `pep.phrasepass.*` 同类。

课本句型关跟读（**跨日保留**，重置时清掉）：`pep.phrasepass.{unitId}` → `StringSet`，元素为 `phrase.0` / `phrase.1` …（见 `phrasePassId`）。与按日重置的 `pep.daycheck.{unitId}.{epochDay}`（含 `speak_phrase.N`、今日听读勾选）分开：句型关完成依赖前者；「今日跟读」勾选仍用后者。跟读通过时双写两套，便于同一天既推进句型关又勾选听读清单。

上次单元（复习入口）：优先 `pep.lastUnitId.g{grade}`；兼容旧全局 `pep.lastUnitId`。复习 Tab 仅当该 id 属于**当前年级**课本 bundle 时才展示入口，并打开单元页而非直接练习。

## 从 SharedPreferences 迁移

老用户的进度全在偏好里，不能丢。`ProgressStore` 初始化时在 IO 线程执行 `importLegacyPrefsIfNeeded()`：

1. 若 `roomImported = true` 直接跳过（幂等开关）。
2. 否则取 `prefs.all` 快照，用纯函数 `legacyWordProgress(map)` / `legacyLevelProgress(map)` 映射成实体（SM-2 列用默认值）。
3. `REPLACE` 批量写库，**写成功后**才 `putBoolean("roomImported", true).commit()`。
4. **不删除任何旧键**。

重置进度：内存快照立即清零 → `resettableProgressKeys` 选键删偏好（保留 accent / theme / speak / aiKey / parentPin / 跟读标定 / 复习提醒 / `roomImported`）→ 协程清 Room 各表。

## 线程模型与可观测性

Room 不能在主线程碰；组合期间不做 I/O。模式仍是「内存快照 + 异步落库」：

- 读：`entries` / `levels` 为 `mutableStateMapOf`；`progress(word)` 同步返回。
- 写：先改快照，再 `scope.launch(Dispatchers.IO)` 整行覆盖写库。
- `lastAnsweredAt` 用普通 `mutableMapOf` 缓存；SM-2 三列挂在 `WordProgress` 上随 `entries` 走。
- 首帧：`ProgressStore.ready == false` 时与词库未加载同显示 `LoadingScreen`。

## 备份

`ProgressBackup.kt`：`encodeProgressSnapshot` / `decodeProgressSnapshot`（`PROGRESS_BACKUP_VERSION = 2`；兼容读 v1，含 `units` / `unitLevels`）。

- 每个词 JSON 含 `intervalDays` / `easeFactor` / `dueAt`
- 旧备份缺字段：`optDouble` / `optLong` 回落到默认（0.0 / 2.5 / 0）
- 不含 API Key / 家长 PIN / 发音等偏好

## 单测

纯逻辑单测（`OgdenDataTest`）：`nextSm2`、`selectDueForReview`（含 dueAt=0 与未到期过滤）、`nextProgress`、`nextStreak`、legacy 映射、`resettableProgressKeys`、备份编解码（含缺 SM-2 字段默认）。Room 本身不做 Robolectric / 仪器测试；schema v5 由 kapt 在构建时生成到 `app/schemas/`。
