package com.example.ogdenkids.speech

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

// 模型加载状态，暴露给 UI 显示进度，避免首次加载时的「假死」观感。
sealed interface WhisperStatus {
    object Idle : WhisperStatus
    data class Copying(val progress: Float) : WhisperStatus
    data class Downloading(val progress: Float) : WhisperStatus
    object Initializing : WhisperStatus
    object Ready : WhisperStatus
    data class Failed(val message: String) : WhisperStatus
}

// 本地语音识别（whisper.cpp base.en-q5_1）。模型优先用 filesDir 已有文件；
// 其次兼容旧包从 assets 拷贝；都没有则从 HuggingFace 按需下载到 filesDir。
// 识别必须在后台线程、串行执行（whisper 上下文非线程安全）。
// 日志统一打 tag=OgdenSpeak，native 层用 WhisperJNI，便于 logcat 筛选。
object WhisperEngine {
    private const val TAG = "OgdenSpeak"
    private const val MODEL_ASSET = "whisper/ggml-base.en-q5_1.bin"
    private const val MODEL_FILE = "ggml-base.en-q5_1.bin"
    private const val MODEL_URL =
        "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en-q5_1.bin"
    // 已知模型大小约 57MiB，无 Content-Length 时作粗略分母
    private const val MODEL_SIZE_HINT = 57L * 1024L * 1024L

    var status: WhisperStatus by mutableStateOf<WhisperStatus>(WhisperStatus.Idle)
        private set

    @Volatile
    private var ctx: Long = 0

    @Volatile
    private var libReady = false

    init {
        try {
            // OpenMP 依赖先显式加载，避免 libwhisper-jni 的 DT_NEEDED 解析不到 libomp.so
            runCatching { System.loadLibrary("omp") }
            System.loadLibrary("whisper-jni")
            libReady = true
            Log.d(TAG, "libwhisper-jni 加载成功")
        } catch (t: Throwable) {
            Log.e(TAG, "libwhisper-jni 加载失败: ${t.javaClass.name}: ${t.message}", t)
            status = WhisperStatus.Failed("本地库加载失败: ${t.message}")
        }
    }

    private external fun nativeInit(modelPath: String, nThreads: Int): Long
    private external fun nativeFree(ctx: Long)
    private external fun nativeTranscribe(ctx: Long, samples: FloatArray, nSamples: Int, nThreads: Int, prompt: String, confidenceOut: FloatArray): String

    data class Transcription(val text: String, val confidence: Float)

    private fun isBusy(): Boolean = when (status) {
        is WhisperStatus.Copying,
        is WhisperStatus.Downloading,
        WhisperStatus.Initializing -> true
        else -> false
    }

    // 预加载：跟读卡片进入时调用，把拷贝/下载/加载进度推到 status，让 UI 有反馈
    suspend fun ensureLoaded(context: Context) {
        if (!libReady) {
            Log.w(TAG, "ensureLoaded: 本地库未就绪，跳过")
            return
        }
        if (ctx != 0L || status == WhisperStatus.Ready || isBusy()) {
            Log.d(TAG, "ensureLoaded: 跳过 (ctx=$ctx status=$status)")
            return
        }
        withContext(Dispatchers.Default) {
            synchronized(this@WhisperEngine) {
                if (ctx != 0L || status == WhisperStatus.Ready || isBusy()) return@synchronized
                load(context)
            }
        }
    }

    // prompt 用目标词做 decoder 初始提示，让识别朝目标词靠；confidence 是 whisper 的 token 平均概率(0..1)
    suspend fun transcribe(context: Context, samples: FloatArray, prompt: String): Transcription =
        withContext(Dispatchers.Default) {
            if (!libReady) {
                Log.e(TAG, "transcribe: 本地库未就绪")
                return@withContext Transcription("", 0f)
            }
            synchronized(this@WhisperEngine) {
                if (ctx == 0L) load(context)
                if (ctx == 0L) {
                    Log.e(TAG, "transcribe: 模型上下文为 0，无法识别")
                    Transcription("", 0f)
                } else {
                    Log.d(TAG, "调用 nativeTranscribe: 样本=${samples.size} 线程=${threadCount()} prompt=$prompt")
                    val conf = FloatArray(1)
                    val text = runCatching { nativeTranscribe(ctx, samples, samples.size, threadCount(), prompt, conf) }
                        .getOrElse { e ->
                            Log.e(TAG, "nativeTranscribe 异常: ${e.javaClass.name}: ${e.message}", e)
                            ""
                        }
                    Log.d(TAG, "识别结果: \"$text\" 置信度=${conf[0]}")
                    Transcription(text, conf[0])
                }
            }
        }

    private fun load(context: Context) {
        Log.d(TAG, "开始加载模型")
        val model = ensureModelFile(context)
        if (model == null) {
            Log.e(TAG, "模型文件不可用")
            if (status !is WhisperStatus.Failed) {
                status = WhisperStatus.Failed("模型获取失败")
            }
            return
        }
        Log.d(TAG, "模型文件就绪: ${model.absolutePath} (${model.length()} bytes)")
        status = WhisperStatus.Initializing
        Log.d(TAG, "调用 nativeInit: path=${model.absolutePath} 线程=${threadCount()}")
        val handle = runCatching { nativeInit(model.absolutePath, threadCount()) }
            .getOrElse { e ->
                Log.e(TAG, "nativeInit 异常: ${e.javaClass.name}: ${e.message}", e)
                0L
            }
        ctx = handle
        Log.d(TAG, "nativeInit 返回 ctx=$handle")
        status = if (ctx == 0L) WhisperStatus.Failed("模型加载失败") else WhisperStatus.Ready
        Log.d(TAG, "模型加载完成: $status")
    }

    // 优先级：filesDir 已有完整文件 → assets 拷贝（兼容旧包）→ 网络下载到 filesDir
    private fun ensureModelFile(context: Context): File? {
        val out = File(context.filesDir, "whisper/$MODEL_FILE")
        if (out.exists() && out.length() > 0L) {
            Log.d(TAG, "模型已存在: ${out.absolutePath} (${out.length()} bytes)")
            return out
        }
        out.parentFile?.mkdirs()

        val fromAssets = copyFromAssets(context, out)
        if (fromAssets != null) return fromAssets

        return downloadModel(out)
    }

    // 兼容旧包或本地开发再放回 assets 的情况；assets 没有则返回 null，不记 Failed
    private fun copyFromAssets(context: Context, out: File): File? {
        val input = runCatching { context.assets.open(MODEL_ASSET) }.getOrNull() ?: run {
            Log.d(TAG, "assets 无模型，将尝试网络下载")
            return null
        }
        return runCatching {
            status = WhisperStatus.Copying(0f)
            input.use { stream ->
                val total = stream.available().toLong().coerceAtLeast(1L)
                Log.d(TAG, "开始从 assets 拷贝模型，总大小 $total bytes")
                val tmp = File(out.parentFile, "$MODEL_FILE.tmp")
                tmp.outputStream().use { output ->
                    val buf = ByteArray(1 shl 16)
                    var copied = 0L
                    while (true) {
                        val n = stream.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                        copied += n
                        status = WhisperStatus.Copying((copied.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
                if (!tmp.renameTo(out)) {
                    tmp.copyTo(out, overwrite = true)
                    tmp.delete()
                }
            }
            status = WhisperStatus.Copying(1f)
            Log.d(TAG, "模型拷贝完成: ${out.absolutePath} (${out.length()} bytes)")
            out
        }.getOrElse { e ->
            Log.e(TAG, "模型拷贝异常: ${e.javaClass.name}: ${e.message}", e)
            status = WhisperStatus.Failed("模型拷贝失败: ${e.message}")
            null
        }
    }

    // HttpURLConnection 下载；先写 .tmp 再 rename，中断半成品下次可重下
    private fun downloadModel(out: File): File? = runCatching {
        status = WhisperStatus.Downloading(0f)
        Log.d(TAG, "开始下载模型: $MODEL_URL")
        val tmp = File(out.parentFile, "$MODEL_FILE.tmp")
        if (tmp.exists()) tmp.delete()

        val conn = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                error("HTTP $code")
            }
            val contentLength = conn.contentLengthLong.takeIf { it > 0 }
                ?: conn.contentLength.toLong().takeIf { it > 0 }
            val totalHint = contentLength ?: MODEL_SIZE_HINT
            Log.d(TAG, "下载 Content-Length=${contentLength ?: "unknown"} hint=$totalHint")

            conn.inputStream.use { input ->
                FileOutputStream(tmp).use { output ->
                    val buf = ByteArray(1 shl 16)
                    var downloaded = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                        downloaded += n
                        val progress = if (contentLength != null) {
                            (downloaded.toFloat() / contentLength).coerceIn(0f, 1f)
                        } else {
                            // 无长度：按 hint 粗略显示，超过 hint 后缓慢逼近 0.95
                            val raw = downloaded.toFloat() / totalHint
                            if (raw < 0.95f) raw.coerceIn(0f, 0.95f) else 0.95f
                        }
                        status = WhisperStatus.Downloading(progress)
                    }
                }
            }
            if (tmp.length() <= 0L) error("下载文件为空")
            if (!tmp.renameTo(out)) {
                tmp.copyTo(out, overwrite = true)
                tmp.delete()
            }
            status = WhisperStatus.Downloading(1f)
            Log.d(TAG, "模型下载完成: ${out.absolutePath} (${out.length()} bytes)")
            out
        } finally {
            conn.disconnect()
        }
    }.getOrElse { e ->
        Log.e(TAG, "模型下载异常: ${e.javaClass.name}: ${e.message}", e)
        runCatching { File(out.parentFile, "$MODEL_FILE.tmp").delete() }
        status = WhisperStatus.Failed("模型下载失败: ${e.message}")
        null
    }

    private fun threadCount(): Int = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4)
}
