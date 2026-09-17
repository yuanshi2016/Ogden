package com.example.ogdenkids.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProgressDao {
    @Query("SELECT * FROM word_progress")
    suspend fun allWordProgress(): List<WordProgressEntity>

    @Query("SELECT * FROM level_progress")
    suspend fun allLevelProgress(): List<LevelProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWordProgress(item: WordProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWordProgress(items: List<WordProgressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLevelProgress(item: LevelProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLevelProgress(items: List<LevelProgressEntity>)

    @Query("DELETE FROM word_progress")
    suspend fun clearWordProgress()

    @Query("DELETE FROM level_progress")
    suspend fun clearLevelProgress()

    // 复习排序：没掌握的词里，先给从未答过（lastAnsweredAt = 0）与最久没答的
    // 目前还没有界面调用，是本次改用数据库的直接理由；真正的间隔重复算法留待后续
    @Query(
        "SELECT * FROM word_progress WHERE mastery < :mastery " +
            "ORDER BY lastAnsweredAt ASC, mastery ASC LIMIT :limit"
    )
    suspend fun wordsDueForReview(mastery: Int = 3, limit: Int = 20): List<WordProgressEntity>
}
