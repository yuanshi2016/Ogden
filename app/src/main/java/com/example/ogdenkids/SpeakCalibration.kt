package com.example.ogdenkids

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * 跟读判定阈值标定：收集「机器判定 vs 用户反馈」样本，累计后微调 SpeakLevel 阈值。
 * 纯逻辑可单测；prefs 读写由 [SpeakCalibrationStore] 负责。
 */

data class SpeakThresholds(
    val minSimilarity: Float,
    val minConfidence: Float
)

data class SpeakFeedbackSample(
    val similarity: Float,
    val confidence: Float,
    val userSaysShouldPass: Boolean,
    val level: SpeakLevel,
    val recordedAt: Long = 0L
)

/** 三档默认阈值（与历史硬编码一致） */
fun defaultSpeakThresholds(level: SpeakLevel): SpeakThresholds = when (level) {
    SpeakLevel.Lenient -> SpeakThresholds(minSimilarity = 0.6f, minConfidence = 0f)
    SpeakLevel.Normal -> SpeakThresholds(minSimilarity = 0.85f, minConfidence = 0.3f)
    SpeakLevel.Strict -> SpeakThresholds(minSimilarity = 0.95f, minConfidence = 0.55f)
}

/**
 * 跟读判定：相似度 + whisper 置信度按严格度阈值。
 * [thresholds] 可来自 [SpeakCalibrationStore]；null 用 [defaultSpeakThresholds]。
 * Lenient 仍忽略置信度下限。
 */
fun assessPronunciation(
    target: String,
    spoken: String,
    confidence: Float,
    level: SpeakLevel,
    thresholds: SpeakThresholds? = null
): SpeakResult {
    val sim = spokenSimilarity(target, spoken)
    val t = thresholds ?: defaultSpeakThresholds(level)
    val confOk = level == SpeakLevel.Lenient || confidence >= t.minConfidence
    val passed = sim >= t.minSimilarity && confOk
    return SpeakResult(passed, sim)
}

/**
 * 根据样本微调阈值。
 * - 用户说「应过」但未过 → 略降门槛（判定偏严）
 * - 用户说「应不过」但过了 → 略升门槛（判定偏松）
 * 多条样本取平均步长，夹在合理范围内。
 */
fun adjustSpeakThresholds(
    current: SpeakThresholds,
    samples: List<SpeakFeedbackSample>,
    level: SpeakLevel
): SpeakThresholds {
    val relevant = samples.filter { it.level == level }
    if (relevant.isEmpty()) return current

    var simDelta = 0f
    var confDelta = 0f
    relevant.forEach { s ->
        val machinePass = if (level == SpeakLevel.Lenient) {
            s.similarity >= current.minSimilarity
        } else {
            s.similarity >= current.minSimilarity && s.confidence >= current.minConfidence
        }
        when {
            s.userSaysShouldPass && !machinePass -> {
                simDelta -= 0.02f
                confDelta -= 0.02f
            }
            !s.userSaysShouldPass && machinePass -> {
                simDelta += 0.02f
                confDelta += 0.02f
            }
        }
    }
    val n = relevant.size.coerceAtLeast(1)
    simDelta /= n
    confDelta /= n

    val (minSim, maxSim) = when (level) {
        SpeakLevel.Lenient -> 0.45f to 0.80f
        SpeakLevel.Normal -> 0.70f to 0.95f
        SpeakLevel.Strict -> 0.85f to 0.99f
    }
    val (minConf, maxConf) = when (level) {
        SpeakLevel.Lenient -> 0f to 0f
        SpeakLevel.Normal -> 0.15f to 0.55f
        SpeakLevel.Strict -> 0.35f to 0.75f
    }
    return SpeakThresholds(
        minSimilarity = (current.minSimilarity + simDelta).coerceIn(minSim, maxSim),
        minConfidence = if (level == SpeakLevel.Lenient) {
            0f
        } else {
            (current.minConfidence + confDelta).coerceIn(minConf, maxConf)
        }
    )
}

/** 累计至少这么多样本才写回微调后的阈值 */
const val SPEAK_CALIBRATION_BATCH = 8

fun encodeSpeakSamples(samples: List<SpeakFeedbackSample>): String {
    val arr = JSONArray()
    samples.forEach { s ->
        arr.put(
            JSONObject()
                .put("sim", s.similarity.toDouble())
                .put("conf", s.confidence.toDouble())
                .put("pass", s.userSaysShouldPass)
                .put("level", s.level.name)
                .put("at", s.recordedAt)
        )
    }
    return arr.toString()
}

fun decodeSpeakSamples(json: String): List<SpeakFeedbackSample> {
    if (json.isBlank()) return emptyList()
    val arr = JSONArray(json)
    val out = ArrayList<SpeakFeedbackSample>(arr.length())
    for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        val level = runCatching { SpeakLevel.valueOf(obj.optString("level", SpeakLevel.Normal.name)) }
            .getOrDefault(SpeakLevel.Normal)
        out += SpeakFeedbackSample(
            similarity = obj.optDouble("sim", 0.0).toFloat(),
            confidence = obj.optDouble("conf", 0.0).toFloat(),
            userSaysShouldPass = obj.optBoolean("pass", false),
            level = level,
            recordedAt = obj.optLong("at", 0L)
        )
    }
    return out
}

fun encodeSpeakThresholds(map: Map<SpeakLevel, SpeakThresholds>): String {
    val root = JSONObject()
    map.forEach { (level, t) ->
        root.put(
            level.name,
            JSONObject()
                .put("sim", t.minSimilarity.toDouble())
                .put("conf", t.minConfidence.toDouble())
        )
    }
    return root.toString()
}

fun decodeSpeakThresholds(json: String): Map<SpeakLevel, SpeakThresholds> {
    if (json.isBlank()) return emptyMap()
    val root = JSONObject(json)
    val out = mutableMapOf<SpeakLevel, SpeakThresholds>()
    SpeakLevel.values().forEach { level ->
        val obj = root.optJSONObject(level.name) ?: return@forEach
        val defaults = defaultSpeakThresholds(level)
        out[level] = SpeakThresholds(
            minSimilarity = obj.optDouble("sim", defaults.minSimilarity.toDouble()).toFloat(),
            minConfidence = obj.optDouble("conf", defaults.minConfidence.toDouble()).toFloat()
        )
    }
    return out
}

/**
 * SharedPreferences 封装：样本队列 + 可调阈值。
 * 键与 speakLevel 同属发音偏好，重置进度时保留。
 */
class SpeakCalibrationStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("ogden-progress", Context.MODE_PRIVATE)

    fun thresholdsFor(level: SpeakLevel): SpeakThresholds {
        val custom = decodeSpeakThresholds(prefs.getString(THRESHOLDS_KEY, "").orEmpty())
        return custom[level] ?: defaultSpeakThresholds(level)
    }

    fun allThresholds(): Map<SpeakLevel, SpeakThresholds> =
        SpeakLevel.values().associateWith { thresholdsFor(it) }

    fun samples(): List<SpeakFeedbackSample> =
        decodeSpeakSamples(prefs.getString(SAMPLES_KEY, "").orEmpty())

    /**
     * 记录一条用户反馈；满 [SPEAK_CALIBRATION_BATCH] 条同 level 样本后微调并清空该档队列。
     * @return 是否刚完成一次阈值微调
     */
    fun recordFeedback(sample: SpeakFeedbackSample): Boolean {
        val all = samples().toMutableList()
        all += sample.copy(recordedAt = if (sample.recordedAt == 0L) System.currentTimeMillis() else sample.recordedAt)
        while (all.size > 64) all.removeAt(0)

        val forLevel = all.filter { it.level == sample.level }
        if (forLevel.size < SPEAK_CALIBRATION_BATCH) {
            prefs.edit().putString(SAMPLES_KEY, encodeSpeakSamples(all)).commit()
            return false
        }

        val current = thresholdsFor(sample.level)
        val adjusted = adjustSpeakThresholds(current, forLevel.takeLast(SPEAK_CALIBRATION_BATCH), sample.level)
        val remaining = all.filter { it.level != sample.level }
        val map = allThresholds().toMutableMap()
        map[sample.level] = adjusted
        prefs.edit()
            .putString(SAMPLES_KEY, encodeSpeakSamples(remaining))
            .putString(THRESHOLDS_KEY, encodeSpeakThresholds(map))
            .commit()
        return true
    }

    companion object {
        const val SAMPLES_KEY = "speakCalibrationSamples"
        const val THRESHOLDS_KEY = "speakThresholds"
    }
}
