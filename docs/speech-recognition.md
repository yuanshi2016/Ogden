# 闯关跟读：本地语音识别（whisper.cpp）

记录日期：2026-09-19

给闯关模式新增一种**独立难度「跟读」**（`Difficulty.Speak`，只含 `PracticeType.Speak` 一种题型）：显示单词，让学习者大声读出来，手机**完全离线**把语音转成文本，再和目标单词比对判定对错。选型上没走 Vosk / 系统 `SpeechRecognizer`，而是直接内置 whisper.cpp，理由见下。

## 为什么是 whisper.cpp（而不是别的）

- 系统 `SpeechRecognizer`：`EXTRA_PREFER_OFFLINE` 依赖设备是否装了英文离线包，`createOnDeviceSpeechRecognizer()` 仅 Android 13+，本项目 `minSdk 24` 下大部分设备会回落到联网，不满足「本地」。
- Vosk：纯离线、体积小、`minSdk` 友好，但孤立单词/儿童发音的错读更容易被「脑补成正确词」，正好掩盖要抓的错误。
- whisper.cpp：准确率更高、纯本地，代价是 APK 体积大（模型约 60MB）。这是用体积换跟读判定的可靠性，符合本项目目标。

## 模型

- 选用 `base.en`，量化版 **`ggml-base.en-q5_1.bin`**（约 59.7MB / 57MiB）。`base` 在单词跟读的「抓错读」上比 `tiny` 稳，又比 `small` 快、小，两只目标机（S21 Ultra / 小米 15 Ultra）CPU 跑近实时。
- 只用了 `.en` 英文专用模型，不用多语言版：单词场景更准、更小。
- 来源：`https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en-q5_1.bin`。
- **按需获取（E7）**：模型**不随 APK 打包**，首次使用时按优先级落到 `filesDir/whisper/`：
  1. `filesDir` 已有完整文件 → 直接用；
  2. 若 `assets/whisper/` 里有同名文件（兼容旧包或本地开发再放回）→ 拷贝到 `filesDir`；
  3. 否则用 `HttpURLConnection` 从 HuggingFace 下载到 `filesDir`（先写 `.tmp` 再 rename）。
- 文件就绪后 `whisper_init_from_file_with_params` 以 mmap 方式加载，不常驻额外内存。识别本身仍完全离线；仅首次（或清数据后）需要联网下载模型。`INTERNET` 权限已声明。

**换模型 / 升级**：
1. 改 `WhisperEngine` 里的 `MODEL_FILE` / `MODEL_URL` / `MODEL_ASSET` 常量保持同步；
2. 模型格式必须与 whisper.cpp 版本匹配（GGML 格式有版本校验，太新/太旧会加载失败）。想上 `small.en` 就换 URL 与文件名，`WhisperEngine.threadCount()` 的线程上限可按需调高；
3. 可选：把 `.bin` 放回 `assets/whisper/` 可走拷贝路径（离线首启），默认不打包以减小 APK。

## 结构

```
app/src/main/cpp/
  CMakeLists.txt          顶层：关掉 whisper 的示例/测试，静态链接后 add_library(whisper-jni)
  whisper-jni.cpp         JNI 桥：init/free/transcribe 三个方法
  whisper.cpp/            整仓 vendored（ggml + src + include + cmake）
app/src/main/java/com/example/ogdenkids/speech/
  WhisperEngine.kt        System.loadLibrary + 模型按需下载/拷贝 + 串行识别
  SpeechRecorder.kt       AudioRecord 16kHz 单声道 PCM + 按住说话(start/stop)
```

- `CMakeLists.txt` 里 `BUILD_SHARED_LIBS=OFF`，`whisper`/`ggml` 全静态链进单个 `libwhisper-jni.so`，避免分发包里拖一堆库。`GGML_OPENMP=OFF`：ggml 的 OpenMP 后端在 ARM Android 上会让 GELU 算子算出 NaN，触发 `assert("!isnan(x)")` 崩溃（已踩坑，崩溃栈里是 `__kmp_invoke_microtask`）；改用 ggml 自带 std::thread 线程池后稳定，且不再随包带 `libomp.so`。
- 顶层 `CMakeLists.txt` 强制 `CMAKE_BUILD_TYPE=Release`：AGP 的 debug 变体默认给 native 传 `Debug`（无 `-O`，即 `-O0`），会让 whisper 慢两个数量级（1.3s 音频要 77s）；覆盖成 `-O3 -DNDEBUG` 后回到亚秒级。这是「慢」而非「CPU vs GPU」——本方案纯 CPU，没有接 GPU 后端。
- `abiFilters` 只编 `arm64-v8a`（目标机）与 `x86_64`（模拟器），不编 32 位。
- JNI 三个函数对应 Kotlin 的 `external fun`：`nativeInit(path, nThreads)`、`nativeFree(ctx)`、`nativeTranscribe(ctx, samples, nSamples, nThreads)`。

## 识别流程

1. `SpeakPracticeCard` 用「按住说话」交互（`detectTapGestures` 的 `onPress`）：手指按下 `SpeechRecorder.start()` 开始录音，松开 `stop()` 结束并立即识别判定。首次按下若无 `RECORD_AUDIO` 授权会先弹系统授权，再按一次即生效。
2. `SpeechRecorder`：16kHz 单声道，独立后台线程持续采样进内存，单次最长 8s 封顶（按住不放也不会撑爆内存）；松开后把采样归一化到 `[-1,1]`。少于 250ms 判为误触，提示「按住说话，读完再松开」，不拿去识别。
3. `WhisperEngine.transcribe(samples, prompt)`：`Dispatchers.Default` 后台 + `synchronized` 串行（whisper 上下文非线程安全），`whisper_full` 用 `WHISPER_SAMPLING_GREEDY`，`language="en"`、`single_segment`、`no_context`；**`prompt` 用目标词做 `initial_prompt`**，让 decoder 朝目标词靠，减少初学口音的近音误判。返回 `Transcription(text, confidence)`，`confidence` 是 whisper 文本 token 的平均概率(0..1)。
4. 判定 `assessPronunciation(target, text, confidence, level, thresholds?)`：先算相似度 `spokenSimilarity`（精确 > 子串 > 音近 > 编辑距离），再按严格度阈值 + 置信度定过不过。`thresholds` 可来自本地标定（见下）；`null` 用默认分档。

```kotlin
// 相似度 0..1：精确(1) > 子串(0.95) > 音近(0.85) > 编辑距离(≤0.84)
fun spokenSimilarity(target: String, spoken: String): Float

// 默认阈值（SpeakCalibration.defaultSpeakThresholds）
// Lenient: sim ≥ 0.6
// Normal:  sim ≥ 0.85 && confidence ≥ 0.3
// Strict:  sim ≥ 0.95 && confidence ≥ 0.55
fun assessPronunciation(
    target: String, spoken: String, confidence: Float, level: SpeakLevel,
    thresholds: SpeakThresholds? = null
): SpeakResult
```

音近用**单 Metaphone**（`metaphone()`）：把 c/k、ph/f 等归一到同一发音键，兜底"kat"≈"cat"、同音异拼（如 hear/here）这类 whisper 转成了别的拼写的情况。元音差异（cat/cut）Metaphone 分不出，交给 whisper 转写 + 置信度把关。

严格度在设置页「跟读判定」切换（`SpeakLevel`：宽松/标准/严格），存 SharedPreferences（`speakLevel`，重置进度时保留）。答案卡会显示「相似度 xx% · 置信度 xx%」，方便学习者对照自己的发音。

### 阈值标定（U1）

跟读答完后，反馈区可点「判定偏严 / 判定偏松」或「这次判对了」：

- 样本写入 prefs（`speakCalibrationSamples`）：similarity、confidence、userSaysShouldPass、level
- 同档累计 `SPEAK_CALIBRATION_BATCH`（8）条后，`adjustSpeakThresholds` 微调该档门槛，写入 `speakThresholds`
- 重置学习进度**不删**这两类键（与 speakLevel 同属发音偏好）
- 纯逻辑单测见 `OgdenDataTest`（`adjustSpeakThresholds*`、`assessPronunciationUsesCustomThresholds`）

## 线程模型与权限

- 录音、转写都在后台（`Dispatchers.Default`），回主线程后才更新 Compose 状态；`onResult` 回调在主线程做计分（`correctCount`/`combo`/音效/落库），与拼写题走同一套 `onRecord` 逻辑。
- 权限声明 `RECORD_AUDIO`（跟读时请求）与 `INTERNET`（首次按需下载模型）；识别转写全程离线，不依赖网络。
- 跟读结果额外用 `speakDetail` 字段存「相似度/置信度」文案，错题时 `selected` 仍是识别文本，不新增多余状态。

## 模型加载进度

模型是懒加载的：跟读卡片一进入就调 `WhisperEngine.ensureLoaded()`，加载进度经 `WhisperEngine.status`（`WhisperStatus`：`Idle / Copying(progress) / Downloading(progress) / Initializing / Ready / Failed`）暴露给 Compose，卡片据此切换界面，避免首次下载/拷贝 + 加载约 57MB 时的「假死」观感：

- `Copying`：从 assets 拷贝时，按已拷贝字节显示「正在准备语音模型 xx%」。
- `Downloading`：从网络下载时，按 `Content-Length`（若无则按约 57MiB 粗略估算）显示「正在下载语音模型 xx%」。
- `Initializing`：`nativeInit`（mmap + 建图）没有回调进度，显示「正在加载模型…」占位，这段通常 1~2s。
- `Failed`：显示错误原因 + 「重试」按钮，重新走 `ensureLoaded()`。
- `Ready` 才启用麦克风按钮，防止模型没就绪就录音。

写入 `filesDir` 时先写 `.tmp` 再 rename，中断过的半成品不会被当成已就绪，下次还能从头拷/下。`ensureLoaded` 在 `Copying` / `Downloading` / `Initializing` 期间直接跳过，配合 `synchronized` 避免并发重复下载。`status` 是 `mutableStateOf` 单例状态，与 `ProgressStore` 的内存快照同一种可观测模式，不引入 Flow/ViewModel。

## 有意留给后续的部分

- **音素级对齐 / 发音评分**：当前相似度是「编辑距离 + 单 Metaphone」，分不清元音差异（cat/cut）、也不给音位级反馈。要更准，换音素对齐（CMUdict / MFA）或发音评分（Azure/Google，但那是联网方案）。
- **阈值标定深化**：U1 已接本地反馈样本与轻量微调；仍缺大规模儿童发音数据与更稳的标定策略（分位数 / 分层）。
- **流式识别 / 实时显示**：现在是「录完再识别」，`single_segment` 一次性出结果；要做边说边出字，需接 whisper 的流式路径或换 streaming 模型。
- **更快的模型**：`tiny.en` 换进去能更快，但会牺牲抓错读能力；`small.en` 反之。阈值 `threadCount()` 上限 4 是按 base 模型的取舍。
- **录音时长阈值**：`SpeechRecorder` 的 `maxDurationMs`（8s）与误触下限（250ms）是经验值，没做过儿童音量/语速标定，现场试错后可能要调。
- **首次加载的预热**：`OgdenKidsApp` 在词库就绪后延迟约 2.5s 调用 `WhisperEngine.ensureLoaded`，把下载/拷贝/加载挪到空闲时段；跟读/口语页仍会再调一次（已就绪或进行中则立刻返回）。模型按需下载已实现（E7）。

## 升级 whisper.cpp

源码以**整仓 vendored** 方式放在 `app/src/main/cpp/whisper.cpp/`（剔除了 `.git`/`examples`/`tests`/`models`/`bindings` 等无关目录），当前基线是上游 commit `5670d5c0bbcb148feabef84400a07cfca9aa3b30`（版本 `1.9.4-dev`）。升级步骤：

1. `git clone https://github.com/ggml-org/whisper.cpp.git`（或 checkout 目标 tag/commit）。
2. 覆盖 `app/src/main/cpp/whisper.cpp/` 下同名目录（保留顶层 `CMakeLists.txt` 与 `whisper-jni.cpp` 不动）。
3. 若上游改了 `whisper.h` 的 API 签名或 ggml 结构，同步改 `whisper-jni.cpp`。
4. 更新 `WhisperEngine` 的 `MODEL_URL` / `MODEL_FILE` 指向与新版匹配的模型；用户侧会按需重新下载（或清 `filesDir/whisper/` 后触发）。可选把模型放回 `assets/whisper/` 做离线首启。
5. 保留顶层 `CMakeLists.txt` 里的 `GGML_OPENMP=OFF`（不关会复现 GELU NaN 崩溃），再 `./gradlew assembleDebug` 验证，重点看 `configureCMakeDebug` 是否因 ggml 后端拆分报错（后端都在 `ggml/src/ggml-*` 下，非 CPU 后端不会参与 Android 编译）。
