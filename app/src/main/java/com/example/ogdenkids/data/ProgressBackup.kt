package com.example.ogdenkids.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 学习进度 JSON 备份（不含 API Key / 家长 PIN / 发音等偏好）。
 * 纯数据变换，便于单测；读写文件由调用方负责。
 *
 * v1：无 units / unitLevels
 * v2：含 unit_progress 与 unit_level_progress
 */
data class ProgressSnapshot(
    val words: List<WordProgressEntity>,
    val levels: List<LevelProgressEntity>,
    val daily: List<DailyActivityEntity>,
    val earned: List<EarnedRewardEntity>,
    val streak: Int,
    val lastStudyDay: Long,
    val lastCategory: String,
    val lastLevel: Int,
    val units: List<UnitProgressEntity> = emptyList(),
    val unitLevels: List<UnitLevelProgressEntity> = emptyList()
)

const val PROGRESS_BACKUP_VERSION = 2
private val SUPPORTED_BACKUP_VERSIONS = setOf(1, 2)

fun encodeProgressSnapshot(snapshot: ProgressSnapshot, exportedAt: Long = System.currentTimeMillis()): String {
    val root = JSONObject()
    root.put("version", PROGRESS_BACKUP_VERSION)
    root.put("exportedAt", exportedAt)
    root.put("streak", snapshot.streak)
    root.put("lastStudyDay", snapshot.lastStudyDay)
    root.put("lastCategory", snapshot.lastCategory)
    root.put("lastLevel", snapshot.lastLevel)
    root.put("words", JSONArray().apply {
        snapshot.words.forEach { w ->
            put(
                JSONObject()
                    .put("w", w.word)
                    .put("favorite", w.favorite)
                    .put("mistake", w.mistake)
                    .put("mastery", w.mastery)
                    .put("attempts", w.attempts)
                    .put("correct", w.correct)
                    .put("last", w.lastAnsweredAt)
                    .put("repetitions", w.repetitions)
                    .put("intervalDays", w.intervalDays)
                    .put("easeFactor", w.easeFactor)
                    .put("dueAt", w.dueAt)
            )
        }
    })
    root.put("levels", JSONArray().apply {
        snapshot.levels.forEach { l ->
            put(
                JSONObject()
                    .put("c", l.category)
                    .put("l", l.level)
                    .put("at", l.completedAt)
            )
        }
    })
    root.put("daily", JSONArray().apply {
        snapshot.daily.forEach { d ->
            put(
                JSONObject()
                    .put("day", d.day)
                    .put("answered", d.answered)
                    .put("correct", d.correct)
            )
        }
    })
    root.put("earned", JSONArray().apply {
        snapshot.earned.forEach { e ->
            put(
                JSONObject()
                    .put("c", e.category)
                    .put("d", e.difficulty)
                    .put("text", e.text)
                    .put("at", e.earnedAt)
            )
        }
    })
    root.put("units", JSONArray().apply {
        snapshot.units.forEach { u ->
            put(
                JSONObject()
                    .put("id", u.unitId)
                    .put("at", u.completedAt)
            )
        }
    })
    root.put("unitLevels", JSONArray().apply {
        snapshot.unitLevels.forEach { ul ->
            put(
                JSONObject()
                    .put("id", ul.unitId)
                    .put("l", ul.level)
                    .put("at", ul.completedAt)
            )
        }
    })
    return root.toString()
}

fun decodeProgressSnapshot(json: String): ProgressSnapshot {
    val root = JSONObject(json.trimStart('\uFEFF'))
    val version = root.optInt("version", 0)
    require(version in SUPPORTED_BACKUP_VERSIONS) { "不支持的备份版本：$version" }

    val words = root.optJSONArray("words").orEmpty().mapObjects { obj ->
        WordProgressEntity(
            word = obj.getString("w"),
            favorite = obj.optBoolean("favorite", false),
            mistake = obj.optBoolean("mistake", false),
            mastery = obj.optInt("mastery", 0).coerceIn(0, 3),
            attempts = obj.optInt("attempts", 0).coerceAtLeast(0),
            correct = obj.optInt("correct", 0).coerceAtLeast(0),
            lastAnsweredAt = obj.optLong("last", 0L),
            repetitions = obj.optInt("repetitions", 0).coerceAtLeast(0),
            intervalDays = obj.optDouble("intervalDays", 0.0).coerceAtLeast(0.0),
            easeFactor = obj.optDouble("easeFactor", Sm2State.DEFAULT_EASE).coerceAtLeast(Sm2State.MIN_EASE),
            dueAt = obj.optLong("dueAt", 0L).coerceAtLeast(0L)
        )
    }.filter { it.word.isNotBlank() }

    val levels = root.optJSONArray("levels").orEmpty().mapObjects { obj ->
        LevelProgressEntity(
            category = obj.getString("c"),
            level = obj.getInt("l"),
            completedAt = obj.optLong("at", 0L)
        )
    }.filter { it.category.isNotBlank() && it.level >= 1 }

    val daily = root.optJSONArray("daily").orEmpty().mapObjects { obj ->
        DailyActivityEntity(
            day = obj.getLong("day"),
            answered = obj.optInt("answered", 0).coerceAtLeast(0),
            correct = obj.optInt("correct", 0).coerceAtLeast(0)
        )
    }

    val earned = root.optJSONArray("earned").orEmpty().mapObjects { obj ->
        EarnedRewardEntity(
            category = obj.getString("c"),
            difficulty = obj.getString("d"),
            text = obj.optString("text", ""),
            earnedAt = obj.optLong("at", 0L)
        )
    }.filter { it.category.isNotBlank() && it.difficulty.isNotBlank() }

    // v1 无 units / unitLevels → 空列表
    val units = root.optJSONArray("units").orEmpty().mapObjects { obj ->
        UnitProgressEntity(
            unitId = obj.optString("id", "").ifBlank { obj.optString("unitId", "") },
            completedAt = obj.optLong("at", 0L)
        )
    }.filter { it.unitId.isNotBlank() }

    val unitLevels = root.optJSONArray("unitLevels").orEmpty().mapObjects { obj ->
        UnitLevelProgressEntity(
            unitId = obj.optString("id", "").ifBlank { obj.optString("unitId", "") },
            level = obj.optInt("l", 0),
            completedAt = obj.optLong("at", 0L)
        )
    }.filter { it.unitId.isNotBlank() && it.level >= 1 }

    return ProgressSnapshot(
        words = words,
        levels = levels,
        daily = daily,
        earned = earned,
        streak = root.optInt("streak", 0).coerceAtLeast(0),
        lastStudyDay = root.optLong("lastStudyDay", 0L),
        lastCategory = root.optString("lastCategory", "op").ifBlank { "op" },
        lastLevel = root.optInt("lastLevel", 1).coerceAtLeast(1),
        units = units,
        unitLevels = unitLevels
    )
}

/**
 * 智能复习排序（SM-2）：
 * - mastery < 3 且 attempts > 0
 * - dueAt <= now 才进队列；dueAt == 0 视为已到期（旧数据兼容）
 * - 按 dueAt ASC、mastery ASC（同 due 再按词序稳定）
 * 与 [ProgressDao.wordsDueForReview] 语义对齐，在内存快照上跑，避免组合期查库。
 */
fun selectDueForReview(
    candidates: List<Pair<String, WordProgressEntity>>,
    limit: Int = 20,
    nowMillis: Long = System.currentTimeMillis()
): List<String> {
    if (limit <= 0) return emptyList()
    return candidates
        .asSequence()
        .filter { (_, p) -> p.mastery < 3 && p.attempts > 0 }
        .filter { (_, p) -> p.dueAt == 0L || p.dueAt <= nowMillis }
        .sortedWith(
            compareBy<Pair<String, WordProgressEntity>> { (_, p) -> p.dueAt }
                .thenBy { it.second.mastery }
                .thenBy { it.first }
        )
        .take(limit)
        .map { it.first }
        .toList()
}

/** 正确率升序的薄弱词（至少答过 1 次），用于学习图表。 */
fun selectWeakWords(
    candidates: List<Pair<String, WordProgressEntity>>,
    limit: Int = 10
): List<Pair<String, WordProgressEntity>> {
    if (limit <= 0) return emptyList()
    return candidates
        .asSequence()
        .filter { (_, p) -> p.attempts > 0 }
        .sortedWith(
            compareBy<Pair<String, WordProgressEntity>> { (_, p) ->
                if (p.attempts == 0) 1.0 else p.correct.toDouble() / p.attempts
            }.thenByDescending { it.second.attempts }
                .thenBy { it.first }
        )
        .take(limit)
        .toList()
}

/** AI 口语陪练：把焦点词拼进 system 附加说明（空列表则返回空串）。 */
fun tutorFocusAddon(focusWords: List<String>, maxWords: Int = 20): String {
    val cleaned = focusWords.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(maxWords)
    if (cleaned.isEmpty()) return ""
    return buildString {
        append("The learner is currently reviewing these Ogden words: ")
        append(cleaned.joinToString(", "))
        append(". Prefer these words in your questions and short replies when natural.")
    }
}

private fun JSONArray?.orEmpty(): JSONArray = this ?: JSONArray()

private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
    val out = ArrayList<T>(length())
    for (i in 0 until length()) {
        val value = opt(i)
        if (value is JSONObject) out += transform(value)
    }
    return out
}
