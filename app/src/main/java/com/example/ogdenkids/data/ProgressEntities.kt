package com.example.ogdenkids.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// 每个词一行：lastAnsweredAt 建索引，供后续间隔重复按「最久没答」排序
@Entity(
    tableName = "word_progress",
    indices = [Index("lastAnsweredAt"), Index("mastery")]
)
data class WordProgressEntity(
    @PrimaryKey val word: String,
    val favorite: Boolean = false,
    val mistake: Boolean = false,
    val mastery: Int = 0,
    val attempts: Int = 0,
    val correct: Int = 0,
    val lastAnsweredAt: Long = 0L
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

@Suppress("UNCHECKED_CAST")
private fun legacySet(value: Any?): Set<String> = (value as? Set<String>).orEmpty()
