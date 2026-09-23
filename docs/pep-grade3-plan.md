# 人教版英语学习路径方案（三年级先行）

记录日期：2026-09-23  
状态：P0 + P1 + P2 + P3（三～六年级）已落地（2026-09-23；读写任务/语法引擎仍暂缓；TTS → 127.0.0.1:7860）  
依据：`docs/人教版英语.md` + 现有 Ogden Basic 能力评估

---

## 1. 目标与边界

### 1.1 产品目标

在现有 Ogden App 内增加第二条学习轨道：**课本同步（人教版）**，与现有 **基础词（Ogden 850+）** 双轨并存。

- 面向小学生 + 家长陪学
- 按 **年级 → 册 → 单元** 推进，而不是按 Ogden 词类闯关
- 一期做成「完整课本同步体验」：词、句型、听读、歌谣/活动提示、单元闯关、复习

### 1.2 一期范围（P0–P1）

| 项 | 决定 |
|---|---|
| 年级 | **三～六年级上/下册**（P3/P3b 已加齐；一期曾仅三年级） |
| 粒度 | 完整单元体验（词 + 句型对话 + 听读任务 + 活动提示 + 闯关） |
| 与 Ogden | **双轨独立进度**：词条可共享，熟练度/关卡分开 |
| 二年级 | **不做**（文档已说明人教版通常从三年级起） |
| 读写/语法引擎 | **暂缓**（见 P3b） |

### 1.3 明确不做（一期）

- 不做成「电子课本」全文 OCR / 课文逐页翻页
- 不接入账号云同步、班级排行
- 不讲解复杂语法概念（三年级以模仿听说为主）
- 不把 Ogden 分类导航改成课本导航（保留原闯关）

---

## 2. 对现有能力的评估

### 2.1 可直接复用

| 能力 | 现状 | 课本路径用法 |
|---|---|---|
| 练习引擎 | 听音选词 / 中英互选 / 例句填空 / 拼写 / 近义词 / 跟读 | 单元闯关题型池 |
| 难度档 | Easy / Medium / Hard / Speak | 按年级映射默认档（三上偏 Easy+Speak） |
| 进度库 | Room：`word_progress`、SM-2、错词/收藏 | **词级进度可共享**（同一 `word` 主键） |
| 关卡进度 | `level_progress(category, level)` | **不能直接用**：需新增课本关卡表 |
| 音频 | assets US/UK + TTS 回退 | 单词复用；句型/歌谣可后补或 TTS |
| 儿童主题 | `ThemeMode.Child` | 课本路径默认走儿童 UI 气质 |
| 复习中心 | 到期队列 / 错词 / 收藏 | 增加「本单元复习」入口即可 |

### 2.2 缺口（必须新建）

1. **课程数据结构**：Grade / Book / Unit / LessonItem（词、句型、活动）
2. **课本关卡与解锁**：按单元顺序解锁，与 Ogden `category` 解耦
3. **内容资源**：三年级词表、句型、活动文案（文档目前只有主题名与周次，缺词句明细）
4. **导航入口**：首页/底部「学习模式」切换或独立 Tab/入口
5. **周计划（可选二期）**：文档中的 18 周节奏，可作为「本周建议」轻提示

### 2.3 文档内容风险

`docs/人教版英语.md` 当前是 **单元主题 + 周次安排 + 学法建议**，不是可练习的词句数据：

- 三上/三下单元名可用作骨架
- **核心词、句型、对话需另行整理**（以正式课本/教师用书为准，避免侵权全文录入）
- 策略：**只收录教学所需的词汇表、关键句型、活动引导**，不收录课文全文与教辅答案

---

## 3. 产品形态

### 3.1 双轨导航

```
首页 / 模式选择
├── 基础词（现有）
│   └── Operations / General Things / … → 每 10 词一关
└── 课本同步（新建）
    └── 三年级 → 上册 / 下册 → Unit 1..6 + Revision → 单元学习页
```

建议入口（实现时二选一，优先 A）：

- **A. 闯关 Tab 顶部 Segmented：「基础词 | 课本」**（改动小、发现成本低）
- B. 设置里选默认轨道 + 首页卡片切换

### 3.2 单元学习页（完整体验）

每个 Unit 一页，区块：

1. **本单元目标**（1–2 句中文，来自主题）
2. **核心词汇**（列表 → 点开复用词详情 / 跟读）
3. **关键句型**（卡片：英文 + 中文；可听、可跟读）
4. **听读任务**（每日建议：跟读录音 10–15 分钟的勾选清单）
5. **课堂/亲子活动**（歌谣名、角色扮演提示、亲子对话——文案级，非多媒体必需）
6. **开始闯关**（本组词 + 句型抽题）
7. **单元复习**（错词 + 未满星 + 句型再练）

### 3.3 三年级默认学习节奏（产品侧）

对齐文档，但不强制日历锁死：

| 环节 | 建议 | App 落地 |
|---|---|---|
| 听读输入 | 每天 10–15 分钟 | 单元页「今日听读」清单 + 跟读 |
| 字母书写 | 每周 2–3 次 | 一期仅提示文案；书写描红二期 |
| 亲子互动 | 每周 1–2 次 | 活动卡片「亲子任务」 |
| 闯关 | 学完词句后 | 解锁单元练习 |

避坑（写进单元引导）：

- 三年级不要求大量抄写
- 不讲语法概念，鼓励开口

---

## 4. 数据模型

### 4.1 静态内容（assets JSON）

建议新增：

```
app/src/main/assets/curriculum/
  pep_grade3_index.json          # 册列表、单元索引
  pep_grade3_vol1.json           # 上册各单元详情
  pep_grade3_vol2.json           # 下册各单元详情
```

**单元结构（草案）：**

```json
{
  "id": "pep.g3.vol1.u1",
  "grade": 3,
  "volume": 1,
  "unit": 1,
  "titleEn": "Making friends",
  "titleZh": "交朋友",
  "goalsZh": ["会问候", "会自我介绍"],
  "words": ["hello", "hi", "name", "I", "am", "..."],
  "phrases": [
    {
      "en": "Hello! What's your name?",
      "zh": "你好！你叫什么名字？",
      "audio": optional
    }
  ],
  "activities": [
    {
      "type": "chant|roleplay|parent|listen",
      "titleZh": "歌谣：Hello song",
      "bodyZh": "跟唱两遍，对镜子打招呼"
    }
  ],
  "weeksHint": [1, 2]
}
```

**词条策略：**

- `words[]` 存英文 key，优先解析到现有 `ogden_words.json`
- 课本专有、词库没有的词：写入 `curriculum/pep_extra_words.json`（同 `OgdenWord` 字段子集），加载时合并进内存词典，分类可用新枚举值或标记 `source=pep`
- **不**把人教版词强行塞进 Operations 等 Ogden 分类闯关顺序

### 4.2 进度（Room 增量）

词级进度继续用现有 `word_progress`（双轨共享掌握度——同一词学会了两边都受益）。

新增课本轨道进度（建议）：

```text
unit_progress
  unitId TEXT PK          -- pep.g3.vol1.u1
  unlocked INTEGER
  wordsDone INTEGER       -- 已点学/跟读过的词数（可选）
  practiceComplete INTEGER
  completedAt INTEGER

unit_level_progress       -- 若一单元多关（如词关 / 句型关 / 综合关）
  unitId TEXT
  level INT
  completedAt INTEGER
  PRIMARY KEY (unitId, level)
```

SharedPreferences 可增：

- `learningTrack` = `ogden` | `pep`
- `pep.lastUnitId`

备份 JSON：版本 +1，附带 `unit_progress`；旧备份缺字段则空。

### 4.3 关卡如何映射练习引擎

现有 `Screen.Practice(categoryCode, level, wordKeys, title)` 已支持 `wordKeys` 自定义词表。

课本闯关：**不走 category 关卡**，直接：

```text
Practice(
  categoryCode = "pep",   // 占位或新增 Curriculum 伪分类
  level = 0,
  wordKeys = unit.words,
  title = "三上 U1 · Making friends"
)
```

三年级默认难度：

- 第一关：Easy + Speak
- 第二关：Medium（加例句填空）
- Revision 单元：Hard 可选

近义词题：若词无 synonyms，练习生成时自动跳过该题型（现有逻辑需确认并补强）。

---

## 5. 三年级内容骨架（来自总结文档）

### 5.1 三年级上册

| Unit | 主题 | 学习侧重（产品） |
|---|---|---|
| 1 | Making friends | 问候、自我介绍 |
| 2 | Different families | 家庭成员 |
| 3 | Our animal friends | 动物、What's this? |
| 4 | Plants around us | 植物词汇 |
| 5 | The colourful world | 颜色 |
| 6 | Useful numbers | 数字 1–10 |
| Revision | Being a good guest | 综合 |

### 5.2 三年级下册

| Unit | 主题 | 学习侧重（产品） |
|---|---|---|
| 1 | Meeting new people | I'm from... |
| 2 | My words and actions | 动作与表达 |
| 3 | Tools and senses for learning | 学习工具、感官 |
| 4 | Healthy food | Do you like...? |
| 5 | Old things | 旧物描述 |
| 6 | Numbers in Life | 数字 11–100 |
| Revision | Going to a school fair | 综合 |

> 实际词句以课本为准整理进 JSON；文档中的 18 周表可作为「本周建议」元数据，不强制锁进度。

---

## 6. 分期计划

### P0 — 骨架可跑（约 3–5 天开发量级）✅

- [x] assets：`pep_grade3_vol1/vol2` 骨架（每单元先填 8–15 词 + 3–5 句型，允许迭代加厚）
- [x] 加载器 + 与 `OgdenWord` 合并解析（`pep_extra_words.json` 仅 ruler/tomato）
- [x] 闯关 Tab 增加「基础词 | 课本」切换
- [x] 册选 → 单元列表 → 单元页（词列表 + 开始练习）
- [x] `unit_progress` 解锁：U1 默认开，通关（≥60%）解锁下一单元
- [x] 练习复用 `wordKeys` 路径；进度写入现有 `word_progress`
- [x] 单测：课程 JSON 解析、解锁规则、缺词回退（`CurriculumTest`）

### P1 — 完整单元体验 ✅

- [x] 句型卡片：播放（TTS/音频）+ 跟读（`PhraseSpeak`）
- [x] 活动区：chant / roleplay / parent 三类文案 + 「今日已做」勾选
- [x] 今日听读清单（本地勾选，按 unitId+day，`pep.daycheck.*`）
- [x] 单元内多关：词汇关 → 句型关 → 综合关（仅综合关 `markUnitComplete`；Room v8）
- [x] 复习中心增加「当前单元」快捷练（`pep.lastUnitId`）
- [x] 备份导入导出兼容新表（备份 v2：`units` + `unitLevels`；v1 仍可读）

### P2 — 体验打磨 ✅

- [x] 周次建议条（`pep.currentWeek` 手动优先 / `pep.termStartDay` 开学日推算；列表高亮+置顶；`WeekHint.kt`）
- [x] 家长报告：本单元三关、今日听读勾选、跟读通过句数（单元页「给家长看」；`ParentReport.kt`）
- [x] 课本专有词/句型音频：`scripts/generate_pep_audio.py` + `pep_audio_manifest.json`；播放路径 `audio/phrases/{us,uk}` 优先于 examples；`ruler`/`tomato` + 60×2 句型已生成
- [x] Revision 综合测评卷（`UNIT_LEVEL_REVISION_EXAM=10`，固定 `REVISION_EXAM_COUNT=20`；`examCount` 对 wordKeys 生效；不 `markUnitComplete`）

### P3 — 扩年级（四年级）✅ 2026-09-23

- [x] 四年级上/下册按同一 JSON schema 加册（`pep_grade4_index/vol1/vol2`；厚度同三年级）
- [x] Loader/UI：年级切换 3|4 → 册 → 单元；`pep.lastGrade`；不强制三→四通关；解锁用当前年级 `orderedUnitIds`
- [x] 音频：manifest 纳入 grade4；本地 TTS 生成缺词/句型 us+uk

### P3b — 扩年级（五、六年级）✅ 2026-09-23

- [x] 五年级上/下 + 六年级上/下加册（`pep_grade5_*` / `pep_grade6_*`；厚度同三/四年级：约 12 词 + 3–5 句型 + activities）
- [x] `PEP_SUPPORTED_GRADES = [3,4,5,6]`；UI 年级 pill 自动跟列表；`pep.lastGrade`；年级间解锁隔离
- [x] **五下已对齐 `docs/人教版英语.md` 版本一**（Following the rules 等；版本二 My day 等未采用）。Revision 文档为 —，App 自拟为 Our travel show / 我们的旅行秀：
  | 单元 | titleEn | titleZh | weeksHint |
  |---|---|---|---|
  | U1 | Following the rules | 遵守规则 | 1–3 |
  | U2 | Our community | 我们的社区 | 4–6 |
  | U3 | Life in different seasons | 不同季节的生活 | 7–8 |
  | U4 | My hometown | 我的家乡 | 10–12 |
  | U5 | Travelling around | 四处旅行 | 13–15 |
  | U6 | Making a travel plan | 制定旅行计划 | 16–17 |
  | Revision | Our travel show | 我们的旅行秀 | 18 |
- [x] 六下结构特殊：仅 4 Unit + Recycle（`pep.g6.vol2.rev`，id 仍以 `.rev` 结尾以走 Revision 测评；UI 显示 Recycle）
- [x] 音频：manifest 纳入 grade5/6；缺词进 `pep_extra_words.json`；TTS us+uk
- [ ] ~~五六年级增加「读写任务」类型~~ **暂缓**（短写提示、语篇阅读——新 PracticeType 再议）
- [ ] ~~六年级下语法专项（过去时）~~ **暂缓**（六下用词句 + 活动提示表达过去经历即可，无独立语法引擎）

---

## 7. 技术注意点

1. **包名/命名空间**：继续 `com.example.ogdenkids` 源码结构；新文件建议 `curriculum/` 包。
2. **Category 枚举**：避免把 `pep` 硬塞进 Ogden 九类；练习占位用独立 `track` 字段更干净。若短期图快，可用伪 `categoryCode = "pep"` 并保证 `Category.from` 不崩溃（需扩展或 Practice 不再依赖真实 Category）。
3. **版权**：只整理词表与教学句型，不复制课文、教参、录音版权资源；音频优先 TTS 或自产。
4. **儿童模式**：课本轨道文案短、按钮大、少设置项。
5. **数据真实性**：上线前用人教版 2026/当地实际目录核对单元名与核心词；文档已声明部分为综合整理。

---

## 8. 成功标准（三年级一期）

- 用户能在 App 内完成：**选三上 → Unit1 → 学词/句 → 闯关 → 解锁 Unit2**
- 同一单词在课本中练过，基础词词库里能看到熟练度变化（共享词进度）
- 课本关卡完成情况**不影响** Ogden 分类关卡解锁
- 无网可学（跟读模型仍按现有策略首次下载）
- 核心路径有单元测试覆盖解析与解锁

---

## 9. 下一步行动

1. 整理三上 Unit 1–2 的最小词表 + 句型（验证 schema）
2. 实现 P0 导航与练习接通
3. 补全三上/三下其余单元内容
4. 再做 P1 完整体验

内容整理与代码可并行：先 schema + 空壳单元，再逐单元填词。
