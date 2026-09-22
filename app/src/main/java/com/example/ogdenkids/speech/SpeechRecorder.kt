package com.example.ogdenkids.speech

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log

// 按住说话式录音：start() 开始把 16kHz 单声道 PCM 采进内存，stop() 停止并返回归一化到
// [-1,1] 的 float 采样。单次最长 maxDurationMs（默认 8s），超长不再追加，避免按住不放撑爆内存。
class SpeechRecorder(
    private val sampleRate: Int = 16000,
    private val maxDurationMs: Int = 8000
) {
    private companion object { const val TAG = "OgdenSpeak" }

    private var record: AudioRecord? = null
    private var reader: Thread? = null

    @Volatile
    private var running = false
    private val pcm = ArrayList<Float>()

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (running) {
            Log.w(TAG, "recorder.start: 已在录音")
            return false
        }
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val bufSize = maxOf(minBuf * 2, sampleRate / 5)
        Log.d(TAG, "AudioRecord 准备: rate=$sampleRate minBuf=$minBuf bufSize=$bufSize")
        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize
            )
        } catch (e: Throwable) {
            Log.e(TAG, "AudioRecord 构造失败: ${e.javaClass.name}: ${e.message}", e)
            return false
        }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord 未初始化: state=${recorder.state}")
            recorder.release()
            return false
        }
        record = recorder
        synchronized(pcm) { pcm.clear() }
        try {
            recorder.startRecording()
        } catch (e: Throwable) {
            Log.e(TAG, "startRecording 失败: ${e.javaClass.name}: ${e.message}", e)
            recorder.release()
            record = null
            return false
        }
        running = true
        val maxSamples = sampleRate * maxDurationMs / 1000
        reader = Thread({
            val buf = ShortArray(sampleRate / 10) // 100ms 一帧
            while (running) {
                val n = recorder.read(buf, 0, buf.size)
                if (n <= 0) break
                synchronized(pcm) {
                    for (i in 0 until n) {
                        if (pcm.size >= maxSamples) break
                        pcm.add(buf[i] / 32768f)
                    }
                }
            }
        }, "OgdenSpeakRecorder").apply { isDaemon = true; start() }
        Log.d(TAG, "录音开始，后台线程 ${reader?.name} 已启动")
        return true
    }

    fun stop(): FloatArray? {
        Log.d(TAG, "recorder.stop 开始")
        running = false
        reader?.join(500)
        runCatching { record?.stop() }
        runCatching { record?.release() }
        record = null
        reader = null
        val result = synchronized(pcm) { if (pcm.isEmpty()) null else pcm.toFloatArray() }
        val durMs = if (result == null) 0 else result.size * 1000 / sampleRate
        Log.d(TAG, "recorder.stop 完成: 样本=${result?.size ?: 0}, 约 ${durMs}ms")
        return result
    }
}
