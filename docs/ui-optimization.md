# UI 与交互优化清单

记录日期：2026-09-16

代码全部集中在 `app/src/main/java/com/example/ogdenkids/MainActivity.kt`（1859 行）。行号为记录时的位置，实施后会漂移。

## 阶段一：功能缺陷

- [x] D1 练习页选项每次重组重新洗牌。`buildQuestion` 调用处（约 1605 行）没有 `remember`，且内部用无种子 `.shuffled()`。点选后 `answerShown` 变化触发重组，四个选项当场换位，绿色对勾出现在用户没点的那一行。判分基于文本比较，不影响正确性，只是视觉错乱。已用 `remember(word, level, index)` 包裹 `buildQuestion`，同一题选项顺序稳定（`buildQuestion` 本身的种子策略留给 P4）。
- [x] D2 系统返回键无效。导航为 `var screen by remember`，全局没有 `BackHandler`。详情、关卡、练习、设置、隐私页按返回键直接退出应用。同时没有 `rememberSaveable`，转屏或进程重启后回到首页。已在 `OgdenKidsApp` 加一个 `BackHandler`（二级页回主页、非首页 tab 回首页、首页 tab 不拦截）并把 `selectedTab` 改为 `rememberSaveable`；`screen` 仍为 `remember`，因 `Screen.Detail` 持有 `OgdenWord`，序列化不在本阶段范围。

## 阶段二：练习流程

- [x] P1 「拼写挑战」不拼写。`PracticeType.Spelling` 与 `Meaning` 逻辑相同，都是四选一，界面却写着「拼出这个单词」。需要真实输入框。已改为中文+英文释义作题干，`OutlinedTextField` 输入英文并用「提交答案」判分（trim + 忽略大小写），答错在原有反馈卡里显示正确拼写；沿用同一张反馈卡与下一题/完成按钮，`onRecord` 仍每题一次。
- [x] P2 近义词题选项可能不足。干扰项取其他词的首个近义词，`distinct()` 后可能只剩 2-3 项，且可能与答案撞车。已改为用 `linkedSetOf(answer)` 依次从其他词的完整近义词列表、再退回其他词本身补足，保证恰好 4 个互异选项且答案只出现一次。
- [x] P3 通关判定不看正确率。最后一题点「完成并返回」时无条件 `onComplete()`，0/10 也解锁下一关，`isLevelUnlocked` 形同虚设。加正确率门槛（建议 60%）。已要求答对数达到 60%（10 题需 6 题）才调用 `onComplete()`；不足时最后一张反馈卡说明「答对 x / 10，答对 6 个就能解锁下一关」，并给「再练一次」「先返回」两个按钮。
- [x] P4 重玩内容完全一致。题型固定为 `index % 5`，干扰项用 `Random(word.hashCode() + type.ordinal)` 固定种子。每次进入关卡应换题型顺序与干扰项。已在 `PracticeScreen` 用 `remember(source) { Random.nextInt() }` 生成每次尝试的种子：题型顺序按该种子洗牌后轮转（10 词仍各出一题），种子也传入 `buildQuestion` 参与干扰项洗牌；`question` 的 `remember` key 含种子，单次尝试内仍稳定。

## 阶段三：性能

- [x] R1 组合期间读 SharedPreferences。已让 `ProgressStore` 在构造时用 `prefs.all` 一次性把已有进度载入 `mutableStateMapOf`（另有关卡完成状态与连续天数的快照状态），组合期间只读内存，写入时同时更新状态并落盘；`ChallengeScreen` / `ReviewScreen` 的已掌握数、错词、收藏、分类进度改用 `remember + derivedStateOf`，仅在进度变化时重算。进度成为真正的 `State`，收藏与答题结果自动传播，不再依赖导航碰巧重组。
- [x] R2 `filtered` 未记忆化。已改为 `remember(words, debouncedQuery, category)`，过滤条件抽成纯函数 `matchesLibraryFilter`（附单测）；查询用 `LaunchedEffect + delay(250)` 防抖，输入框本身仍即时响应。
- [x] R3 首帧同步解析 JSON。已改为 `produceState` + `Dispatchers.IO` 加载词库，未就绪时显示纸感加载态 `LoadingScreen`（沿用 `Paper` / `EmptyCard`，文案「正在准备词库……」）；`kotlinx-coroutines` 由 Compose 传递引入，`app/build.gradle` 无需改动。

## 阶段四：信息架构与视觉（需先确认方向）

- [x] A1 默认落地页 `Tab.Home` 只有两张谚语卡，没有开始学习入口；「继续之前」在第二个 tab。静态的 `Tab.Software` 占据一个主导航位。已删除 `Tab.Home` 与 `ProverbHomeScreen`，谚语标题、单张谚语卡（`remember` 内 `proverbs().shuffled().first()`，保留翻面看中文）与底部一行「从850个词开始……」移到闯关页顶部/末尾；底部导航精简为闯关、词库、复习三项，`selectedTab` 与 `BackHandler` 默认目标改为 `Tab.Challenge`；软件页改为二级页 `Screen.Software`，入口是闯关页顶部标题右侧的信息图标（一次点击可达，含 `contentDescription`）。
- [x] A2 UK/US 切换只在词库 tab 浮于底部导航上方，真正需要它的详情页和练习页切不了，设置页又有重复一份。已移除 `MainScaffold` 里按 tab 条件显示的浮动 `SettingsToggleRow`，权威开关保留在设置页；详情页音标行右侧新增同一组 `TogglePill`，共享同一全局状态并仍走 `progressStore.saveAccent`；练习页不加，避免答题中分心。`TogglePill` 触控高度顺手提到 48dp（与 A6 重叠，A6 不必重做）。
- [x] A3 词库可用性（A-Z 索引除外，见末句）。搜索框、筛选 chip 与结果数从 `LazyColumn` 的 item 移到列表上方的固定 `Column`，滚动时常驻；搜索框在非空时显示带 `contentDescription` 的清空按钮。列表默认行改用扩展后的 `CompactWordRow`（单词 + 音标 + 中文 + 星星 + 朗读 + 收藏，48dp 触控），不再用约 200dp 的 `WordListCard`，该组件已删除；行内收藏走 `store.toggleFavorite`，靠 R1 的快照状态即时刷新。滚动超过 4 项后右下角出现「回到顶部」`FloatingActionButton`（`rememberLazyListState().firstVisibleItemIndex` + `animateScrollToItem(0)`），列表底部留 80dp 不被按钮遮挡。**未做 A-Z 索引**：850 词已可搜索 + 分类筛选，跳字母 UI 的界面成本大于收益，故有意跳过。
- [x] A4 详情页可切换相邻词。`Screen.Detail` 增加 `neighbors: List<OgdenWord>` 字段，词库/收藏夹/错词本在打开详情时把「当前显示的那份列表」一起传入，详情页底部加「上一个 · xxx」「下一个 · xxx」两个按钮（顺序即列表顺序，首尾自动 `enabled = false`）。未引入 pager 或导航库。
- [x] A5 字体放大与小屏。`ProverbCard` 从 `height(176.dp)` 改 `heightIn(min = 176.dp)`（内部 `fillMaxSize` 同步改 `fillMaxWidth`），可随文字长高；`HeroCard` 标题去掉 `maxLines = 1`，`StatCard` 数值去掉 `maxLines = 1` 并居中换行，`WordCollectionScreen` 顶栏标题允许两行；`CompactWordRow` 的音标加 `maxLines = 1 + Ellipsis` 只截音标不截中文。其余固定尺寸（进度条高度、圆点、关卡序号圆）不含可变长文本，未改动。
- [x] A6 深色模式与 edge-to-edge。新增暖色深色调色板（纸面 `#17130F` / `#221C16`，墨色 `#F3EADA`），原来的 `Paper`/`Ink` 等顶层常量改为读取 `LocalPalette` 的 `@Composable @ReadOnlyComposable` 属性，调用点写法不变、不需要逐层传色；`isSystemInDarkTheme()` 决定调色板与 `light/darkColorScheme`。`Category.tint` / `soft` 改为基于 `baseTint`/`baseSoft` 的组合式属性，深色下向白色插值 52%（`soft` 改为 20% alpha 覆盖），保证在深底上可读。`styles.xml` 状态栏/导航栏改透明并用 `@color/paper`（含 `values-night`）作窗口底色，`MainActivity` 调 `WindowCompat.setDecorFitsSystemWindows(window, false)` 走 edge-to-edge，系统栏图标明暗用 `WindowCompat.getInsetsController` 按主题切换；主导航用 `statusBarsPadding + navigationBarsPadding`，七个二级页的普通 `Row` 顶栏抽成 `SecondaryTopBar` 统一加 `statusBarsPadding()`。未新增依赖（`androidx.core` 由 activity-compose 1.7.2 传递提供）。`TogglePill` 的 48dp 触控在 A2 已完成。

## 约束

- 构建与测试用 JDK 17（Gradle 7.6.4 / AGP 7.4.2 不支持 JDK 21）。
- 验证命令：`gradlew :app:assembleDebug :app:testDebugUnitTest --offline`。
- 不为缩短文件而拆分 `MainActivity.kt`（沿用 `optimization.md` 的既有原则）。

## 后续补充

记录日期：2026-09-17

- 合并二级页。原「关于软件」（`Screen.Software` / `SoftwareScreen`）与「软件设置」两页合成一页 `Screen.Settings`，标题「设置与关于软件」，闯关页信息图标一次点击直达（`contentDescription` 同步改为「设置与关于软件」，回调改名 `onOpenSettings`）。页面按 `SectionTitle` + `Card` 分为四组：应用信息（Ogden Basic / 版本 1.0）、发音设置（UK/US 开关）、学习数据（重置进度）、关于（隐私声明、关于作者）。`Screen.Software` 与 `SoftwareScreen` 已删除。
- 重置学习进度。`ProgressStore.resetProgress()` 同时清空内存快照（`entries` / `levels` / `streak`）与 SharedPreferences，落盘部分用纯函数 `resettableProgressKeys` 选键：删除 `favorites`、`mistakes`、`mastery.*`、`attempts.*`、`correct.*`、`last.*`、`level.*.complete`、`streak`、`lastStudyDay`、`lastCategory`、`lastLevel`，**保留 `accent`**（英美发音是偏好设置，不属于学习进度，不能被重置顺带清掉）；`lastCategory` / `lastLevel` 代表「继续之前」的位置，属于进度，故一起清除。不用 `prefs.edit().clear()`，避免误删 accent。该选键函数附单测（accent 存活、其余键全删）。
- 破坏性操作有确认。入口在「学习数据」组，`OutlinedButton` 用 `Error` 描边与文字，点击弹 material3 `AlertDialog`，中文说明将清空掌握星星、错词本、收藏、关卡解锁、连续天数且无法撤销、发音设置保留；确认按钮为 `Error` 填充的 `Button`「确认重置」，取消为 `TextButton`「取消」，触控高度 48dp。重置成功后卡片文案换成绿色提示，首页统计因 `derivedStateOf` 读的是快照状态会自动归零，未加 Snackbar。
- 学习进度改用 Room 本地数据库（为后续间隔重复与学习报表铺路）。表结构、标量为何留在 SharedPreferences、迁移与幂等、线程模型见 `docs/storage.md`。
