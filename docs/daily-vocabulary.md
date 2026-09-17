# 日常交流词汇补充计划

记录日期：2026-09-17

目标：现有 850 词面向「用最少词解释一切概念」，不面向日常对话。本计划补一层日常层，让 app 能支撑真实交流。

## 为什么要补

拿 263 个日常高频词比对现有词库，128 个不在其中。最说明问题的是代词：`op` 分类 100 个词里只有 `I` / `he` / `you` / `who`，`she`、`it`、`we`、`they`、`me`、`my`、`your`、`them` 全缺。

这些词在 app 自己的例句里天天出现：`is` 156 次、`your` 40 次、`my` 36 次、`she` 31 次、`me` 27 次、`it` 27 次。例句平均 4.1 词，其中用到 433 个词库外的词形。学习者撞见了却查不到，没有词条、没有音标、没有发音。

根因是 Ogden 的设计意图：850 是**释义**词汇，只保留 17 个动词，`eat` 要用 `take food` 表达，`buy` 要用 `give money for` 表达。造词典精妙，日常对话不可用。同理缺 `hello`、`sorry`、`thanks`、`want`、`know`、`think`、`understand`——这些都能被绕开。

以下三层词表已逐个与现有 850 词去重，共 188 词，均为净新增。

## 第一层：功能词与词形（41 词）

最便宜、收益最大：让现有 850 词能真正组成句子。

```
already also always am an are can could during her him his
into is it its me might must my never often our really
she should since sometimes their them they until us usually was we
were without would your yours
```

## 第二层：日常动词（74 词）

Ogden 有意绕开的那批。

```
agree apologize arrive ask borrow bother break bring build buy call carry
catch choose climb close continue cost decide drive eat enjoy explain feel
find finish follow forget happen hear hurry introduce invite know learn leave
lend listen live lose mean meet pay plan prefer promise read remember
repair return ride save sell show sit speak spend stand study teach
tell thank think throw try understand visit wait wake want wear win
worry write
```

## 第三层：社交用语、现代名词与形容词（73 词）

寒暄语现有词库一个都没有。形容词侧注意 850 有 `great` 无 `big`、有 `warm` 无 `hot`，这是用反义对压缩词表的结果。

```
afternoon airport app bank bathroom bedroom big bill birthday boring breakfast bus
busy bye cafe careful cash coffee computer crowded dangerous delicious dinner easy
email evening expensive famous fast fine goodbye hello hi holiday hot hotel
hungry interesting internet juice kitchen lucky lunch mall menu movie noisy noodle
okay party pharmacy phone photo polite popular restaurant rude shop sick sorry
subway supermarket tea thanks thirsty toilet tonight ugly vegetable video website weekend
welcome
```

## 第四层：固定说法（约 120 条，待定稿）

日常交流真正的最小可用单位，比单词更直接管用。示例：

```
How are you        What's your name       How much is it
Can you help me    I don't understand     See you tomorrow
Excuse me          Nice to meet you       What time is it
```

这批与 `OgdenWord` 的适配有真问题，见下节。

## 补充词表（两轮评审，已核对 app 实际 ogden_words.json）

以下全部与 850 + 上文 188 去重，均为净新增。

### 更正

初评时曾认为 `need` `sad` `look` `swim` 缺失，实际均在 850 中（`need`/`look`/`swim` 以名词词条收录）。建议在这几词条的中英文释义里补动词义，零成本覆盖动词用法，不开新词条。

### 功能词补完（20 词，并入第一层）

```
more most next too yet just soon ago which each few both another
something anything nothing everything someone everyone nobody
```

`next week`、`three days ago`、`me too`、`more water` 全靠这批，比很多名词更致命。

### 基础词表：数词、星期、月份（31 词，单开分类）

Basic English 传统上把这些当附属表，app 完全没接。三年级校内起点正好从这里教，优先级与第一层并列。

```
one two three four five six seven eight nine ten hundred thousand
Monday Tuesday Wednesday Thursday Friday Saturday Sunday
January February March April May June July August September October November December
```

### 共享底座补充（32 词）

```
today city car bike taxi doctor medicine pet chair desk sofa fridge wallet
sing dance draw stay game maybe sure nice fun TV
grandmother grandfather uncle aunt scared excited chicken pizza banana
```

850 有 tomorrow/yesterday 却无 today，有 carriage/cart 却无 car；家庭称呼词一个都没有。

### 儿童包（24 词）

```
teacher classroom homework playground classmate toy doll robot dinosaur cartoon
zoo lion tiger elephant bear rabbit panda
ice cream candy cookie chocolate hamburger cool hide
```

### 成人包（18 词）

```
job boss colleague salary address rent corner
password download online message chat cancel exercise nurse luggage appointment beer
```

### 第二批日常词（42 词）

```
动词:   share hug hit hold count dream celebrate pick
形容词: funny cute shy brave proud silly naughty wonderful amazing pretty empty heavy
地点:   park pool beach police museum cinema
动物:   duck mouse frog butterfly
人物:   farmer driver worker husband wife
学校:   lesson math grade
其他:   clothes mirror screen age
```

### 总量与优先级

850 + 188 + 20 + 31 + 32 + 24 + 18 + 42 ≈ 1205 词，日常口语覆盖约 85%，是「起步段」的上限。到此收手，再往上堆词的边际收益不如短语层。

优先顺序：

1. 基础词表（数词星期月份）与第一层 41 功能词 + 补完 20 词同批上线——与校内同步。
2. 共享底座补充（优先 today / car / nice / doctor / 家庭称呼）。
3. 儿童包 + 第二批日常词。
4. 成人包。
5. 短语层前置到与第 2 批并列——对三年级，句型比单词更直接管用。

## 接进现有 app 要动的地方

- `Category` 枚举硬编码 5 项，`count` 写死在构造参数里（100/400/200/100/50），`Category.from(code)` 遇未知 code 抛异常。加分类要同时改枚举。
- 词库页筛选 chip 有写死的 `All · 850` 标签。
- 单测 `OgdenDataTest` 有 `assertEquals(850, words.length())` 与各分类计数断言，需同步更新。
- 关卡按列表顺序 `drop((level-1)*10).take(10)` 切，新分类自动获得关卡，无需额外改动。
- 发音：现有 1700 个 mp3 是 850 词 × uk/us。新词无本地音频时 `localAudioPath` 返回 null，走 `ogden.munch.love/api/tts` 在线合成，再退到系统 TTS。即开箱可用但依赖网络。
- `ogden_ipa.json` 缺条目时音标渲染为空字符串，不崩但留白。
- 固定说法与题型冲突：`Example` 题型把目标词从例句挖空，`Spelling` 题型让人拼写整条内容——让人拼 "How are you?" 是荒谬的。短语要么单独一个类型并排除这两种题型，要么只进词库不进闯关。

## 分期

1. 第一层 41 个功能词，作为新分类接进去。价值是验证整条链路：枚举、计数、单测、缺音标与缺音频的降级表现。改动最小，跑通后面就是灌数据。
2. 第二层 74 个动词。开始面对内容工作量：每词需中文、英文释义、例句、例句翻译、3 个近义词，与现有 JSON 字段对齐（`w` `c` `zh` `en` `ex` `exz` `s`）。
3. 第三层 73 个名词形容词与寒暄语。
4. 固定说法，需先决定题型适配方案。
5. 批量生成新词音频接进 assets，去掉对网络的依赖。

工作量的真实分布：代码改动很轻，内容制作是主体。188 词 × 7 字段是唯一值得认真估时的部分。

## 不做的

- 不把新词混进现有五个分类。850 是有出处的封闭集合，混进去「Ogden 基础英语」这个招牌就不成立，也无法再与原作对照。单开分类既保住原始集合，也让用户看得见哪些是补充的。
- 不一次做完 188 词再上线。第一层 41 词就能立刻改善现有例句的可理解度，先放出去。

## 三年级学习者使用建议

三年级是义务教育英语起点（人教 PEP 三上），本表补完后与教材话题基本全覆盖：打招呼、自我介绍、数字、颜色、身体、动物、食物、文具、家人。几条针对性建议：

1. **加「按教材主题」的筛选维度**。内容已够，只缺分组：孩子课上学的当天能在 app 找到，正反馈最强。成本是一个标签，不是新内容。
2. **单次量减半**。关卡现在一次 10 词，对三年级偏多。儿童画像下每关 5 词，每天 3–5 新词 + 复习。
3. **发音优先于拼写**。此年龄段靠模仿和自然拼读，儿童画像默认隐藏 IPA（学校高年级才教音标），Spelling 题型后置；固定说法若进闯关，口语/听力题型优先。
4. **短语层前置**。三年级课堂就是句型教学（I like... / Can I have...），短语层和单词层并行上线，别等到第 4 期。
5. **兴趣词当奖励**。玩具、恐龙、冰淇淋这批是「兴趣驱动」内容，放在解锁奖励位，孩子主动学的动力大于高频词。
6. **家长伴读**。app 有发音，每天复习时家长跟读一遍。8–9 岁模仿能力最强，纠正发音的窗口期就是现在。
