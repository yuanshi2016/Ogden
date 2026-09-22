# Ogden Basic

一款面向中文英语学习者的原生 Android 单词 App，基于 Kotlin + Jetpack Compose 构建。它围绕 Ogden Basic English 的 850 个基础词展开，把词库、发音、闯关和复习都放在离线可用的移动端体验里。

> 本项目是一次二次创作。我在 X 上看到 [Ogden's Basic English](https://ogden.munch.love/) 的作品后很受触动，并在征得原网站作者同意的情况下，将其内容与气质重新设计为 Android 学习应用。原作地址：[https://ogden.munch.love/](https://ogden.munch.love/)

欢迎大家下载、使用并反馈。作者 X 账号：[@Skivein](https://x.com/Skivein)

## Screenshots

| Home | Challenge |
| --- | --- |
| ![Home](docs/screenshots/home.jpg) | ![Challenge](docs/screenshots/challenge.jpg) |

| Library | Review |
| --- | --- |
| ![Library](docs/screenshots/library.jpg) | ![Review](docs/screenshots/review.jpg) |

## Features

- 内置 Ogden Basic English 850 词，支持离线学习。
- 词库分为 Operations、General Things、Picturable、Qualities、Opposites。
- 支持搜索、分类筛选、US/UK 发音切换，界面固定简体中文。
- 首页展示英文谚语卡片，点击翻转查看中文翻译。
- 每 10 个词为一关，分类逐步解锁。
- 练习包含听音选词、看中文选英文、例句填空、拼写挑战、近义词配对。
- 本地保存学习进度、收藏、错词、熟练度和连续学习天数。
- 复习中心：智能复习队列（未满星、最久未答优先）、错词本、收藏夹；均可一键开练。
- 设置页支持学习进度 JSON 导出 / 导入（不含 API Key 与家长密码）。
- 学习图表含近 30 天柱状图与薄弱词 Top10。
- 可选 AI 口语对练（绑定待复习词）与拍照答疑（需自备 DeepSeek Key）。
- 内置 US / UK 两套单词音频，优先离线播放；跟读使用本地 whisper。

## Word Data

词库总数为 1203：Ogden Basic English 850 核心词，外加 353 个日常补充词（见 `docs/daily-vocabulary.md`）。

- Operations: 100
- General Things: 400
- Picturable: 200
- Qualities: 100
- Opposites: 50
- Function（功能词）: 61
- Verbs（动词）: 74
- Nouns（名词形容词）: 73
- Topics（主题词）: 145

主要数据文件：

- `app/src/main/assets/ogden_words.json`
- `app/src/main/assets/ogden_ipa.json`
- `app/src/main/assets/audio/us`
- `app/src/main/assets/audio/uk`

补充词的生成工具：

- `scripts/new_words.json`：353 个补充词内容（中文、释义、例句、近义词）
- `scripts/merge_new_words.py`：合并进 `ogden_words.json`
- `scripts/word_audio_manifest.json` / `scripts/generate_word_audio.py`：调用本地 Qwen TTS WebUI 生成 US/UK 发音

## Build

使用 JDK 17。用 Android Studio 打开本目录，等待 Gradle 同步后运行 `app`。

命令行构建：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-parallel
```

APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Notes

- 上架包名（applicationId）已是 `com.skivein.ogdenbasic`（作者 Skivein）；源码 namespace 仍为 `com.example.ogdenkids`。旧包 `com.example.ogdenkids` 需卸载后重装。
- App 首版以离线学习为主，不接入账号、云同步或排行榜。
- 发音优先使用内置音频；例句或缺失音频场景可回退到系统 TTS / 在线服务。
- 跟读模型首次使用时从网络下载（约 57MB），之后完全离线识别。
- 可选：每日复习通知、DeepSeek AI 口语/拍照（自备 Key，本机加密存储）。
- 一点颜体字体来自 [wordshub/free-font](https://github.com/wordshub/free-font)，用于首页底部短句。

## License

本项目以 [MIT License](LICENSE) 开源。
