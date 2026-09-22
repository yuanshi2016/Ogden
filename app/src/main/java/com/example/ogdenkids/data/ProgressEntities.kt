package com.example.ogdenkids.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// 每个词一行：dueAt / mastery 供智能复习；lastAnsweredAt 供报表
@Entity(
    tableName = "word_progress",
    indices = [Index("lastAnsweredAt"), Index("mastery"), Index("dueAt")]
)
data class WordProgressEntity(
    @PrimaryKey val word: String,
    val favorite: Boolean = false,
    val mistake: Boolean = false,
    val mastery: Int = 0,
    val attempts: Int = 0,
    val correct: Int = 0,
    val lastAnsweredAt: Long = 0L,
    /** SM-2 连续质量≥3 的次数 n */
    val repetitions: Int = 0,
    /** SM-2 当前间隔（天），0 表示尚未进入间隔重复 */
    val intervalDays: Double = 0.0,
    /** SM-2 易度因子，默认 2.5 */
    val easeFactor: Double = Sm2State.DEFAULT_EASE,
    /** 下次应复习的 epoch millis；0 表示旧数据，按 lastAnsweredAt 兼容排序 */
    val dueAt: Long = 0L
)

/**
 * 每次答题一行，供学习曲线 / 正确率趋势 / 质量分分布。
 * 不进备份 JSON 的热路径亦可（体积大）；导出可选附带最近 N 条。
 */
@Entity(
    tableName = "answer_event",
    indices = [Index("word"), Index("answeredAt"), Index("day")]
)
data class AnswerEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val word: String,
    /** 是否计入「答对」（mastery 用的 dichotomous 结果） */
    val correct: Boolean,
    /** SM-2 质量 0..5 */
    val quality: Int,
    val answeredAt: Long,
    /** 本地纪元日，与 daily_activity.day 对齐 */
    val day: Long,
    /** 可选：题型名 PracticeType.name，空表示未知 */
    val practiceType: String = ""
)

// 关卡完成记录：category 是 Category.code，completedAt 留给日后的学习报表
@Entity(tableName = "level_progress", primaryKeys = ["category", "level"])
data class LevelProgressEntity(
    val category: String,
    val level: Int,
    val completedAt: Long = 0L
)

// 每日学习统计：day 是本地时区的纪元日（0 点为界），供日图表使用
@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey val day: Long,
    val answered: Int = 0,
    val correct: Int = 0
)

// 分类 × 难度的闯关奖励文案，家长在选关页填写
@Entity(tableName = "category_reward", primaryKeys = ["category", "difficulty"])
data class RewardEntity(
    val category: String,
    val difficulty: String,
    val text: String
)

// 已领取：同一分类同一难度只记一次，文案按领取当时冻结，方便碎片按同名汇总
@Entity(tableName = "earned_reward", primaryKeys = ["category", "difficulty"])
data class EarnedRewardEntity(
    val category: String,
    val difficulty: String,
    val text: String,
    val earnedAt: Long = 0L
)

// 旧 SharedPreferences -> Room 的映射抽成纯函数，便于单测；只认识旧版写过的键
fun legacyWordProgress(prefs: Map<String, Any?>): List<WordProgressEntity> {
    val favorites = legacySet(prefs["favorites"])
    val mistakes = legacySet(prefs["mistakes"])
    val words = sortedSetOf<String>()
    words += favorites
    words += mistakes
    prefs.keys.forEach { key ->
        if (key.startsWith("mastery.") || key.startsWith("attempts.") ||
            key.startsWith("correct.") || key.startsWith("last.")
        ) {
            words += key.substringAfter('.')
        }
    }
    return words.filter { it.isNotEmpty() }.map { word ->
        WordProgressEntity(
            word = word,
            favorite = favorites.contains(word),
            mistake = mistakes.contains(word),
            mastery = prefs["mastery.$word"] as? Int ?: 0,
            attempts = prefs["attempts.$word"] as? Int ?: 0,
            correct = prefs["correct.$word"] as? Int ?: 0,
            lastAnsweredAt = prefs["last.$word"] as? Long ?: 0L
        )
    }
}

fun legacyLevelProgress(prefs: Map<String, Any?>): List<LevelProgressEntity> =
    prefs.entries.mapNotNull { (key, value) ->
        // 旧键形如 level.op.3.complete
        if (value != true || !key.startsWith("level.") || !key.endsWith(".complete")) return@mapNotNull null
        val parts = key.split('.')
        val level = parts.getOrNull(2)?.toIntOrNull() ?: return@mapNotNull null
        val category = parts.getOrNull(1)?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        LevelProgressEntity(category = category, level = level)
    }

fun legacyRewards(prefs: Map<String, Any?>): List<RewardEntity> =
    prefs.mapNotNull { (key, value) ->
        if (!key.startsWith("reward.")) return@mapNotNull null
        val text = (value as? String)?.trim().orEmpty()
        if (text.isEmpty()) return@mapNotNull null
        val parts = key.split('.')
        val category = parts.getOrNull(1)?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        val difficulty = parts.getOrNull(2)?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        RewardEntity(category, difficulty, text)
    }

@Suppress("UNCHECKED_CAST")
private fun legacySet(value: Any?): Set<String> = (value as? Set<String>).orEmpty()
