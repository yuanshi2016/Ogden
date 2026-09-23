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

    @Query("SELECT * FROM unit_progress")
    suspend fun allUnitProgress(): List<UnitProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUnitProgress(item: UnitProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUnitProgress(items: List<UnitProgressEntity>)

    @Query("DELETE FROM unit_progress")
    suspend fun clearUnitProgress()

    @Query("SELECT * FROM unit_level_progress")
    suspend fun allUnitLevelProgress(): List<UnitLevelProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUnitLevelProgress(item: UnitLevelProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUnitLevelProgress(items: List<UnitLevelProgressEntity>)

    @Query("DELETE FROM unit_level_progress")
    suspend fun clearUnitLevelProgress()

    @Query("SELECT * FROM daily_activity")
    suspend fun allDailyActivity(): List<DailyActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDailyActivity(item: DailyActivityEntity)

    @Query("DELETE FROM daily_activity")
    suspend fun clearDailyActivity()

    @Query("DELETE FROM daily_activity WHERE day < :day")
    suspend fun pruneDailyActivity(day: Long)

    @Query("SELECT * FROM category_reward")
    suspend fun allRewards(): List<RewardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReward(item: RewardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRewards(items: List<RewardEntity>)

    @Query("SELECT * FROM earned_reward")
    suspend fun allEarnedRewards(): List<EarnedRewardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEarnedReward(item: EarnedRewardEntity)

    @Query("DELETE FROM earned_reward")
    suspend fun clearEarnedRewards()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAnswerEvent(item: AnswerEventEntity)

    @Query("SELECT * FROM answer_event ORDER BY answeredAt DESC LIMIT :limit")
    suspend fun recentAnswerEvents(limit: Int = 500): List<AnswerEventEntity>

    @Query("SELECT * FROM answer_event WHERE day >= :fromDay ORDER BY answeredAt ASC")
    suspend fun answerEventsSince(fromDay: Long): List<AnswerEventEntity>

    @Query("DELETE FROM answer_event")
    suspend fun clearAnswerEvents()

    @Query("DELETE FROM answer_event WHERE answeredAt < :beforeMillis")
    suspend fun pruneAnswerEvents(beforeMillis: Long)

    // 复习：mastery 未满星且练过；dueAt<=now（dueAt=0 视为已到期）；按 dueAt ASC, mastery ASC
    @Query(
        "SELECT * FROM word_progress WHERE mastery < :mastery AND attempts > 0 " +
            "AND (dueAt = 0 OR dueAt <= :now) " +
            "ORDER BY dueAt ASC, mastery ASC, word ASC LIMIT :limit"
    )
    suspend fun wordsDueForReview(now: Long, mastery: Int = 3, limit: Int = 20): List<WordProgressEntity>
}
