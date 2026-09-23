package com.example.ogdenkids.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WordProgressEntity::class,
        LevelProgressEntity::class,
        DailyActivityEntity::class,
        RewardEntity::class,
        EarnedRewardEntity::class,
        AnswerEventEntity::class,
        UnitProgressEntity::class,
        UnitLevelProgressEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class ProgressDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao

    companion object {
        @Volatile
        private var instance: ProgressDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_activity` " +
                        "(`day` INTEGER NOT NULL, `answered` INTEGER NOT NULL, `correct` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`day`))"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `category_reward` " +
                        "(`category` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `text` TEXT NOT NULL, " +
                        "PRIMARY KEY(`category`, `difficulty`))"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `earned_reward` " +
                        "(`category` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `text` TEXT NOT NULL, " +
                        "`earnedAt` INTEGER NOT NULL, PRIMARY KEY(`category`, `difficulty`))"
                )
            }
        }

        // SM-2：word_progress 加 intervalDays / easeFactor / dueAt；旧行保持 dueAt=0 走兼容排序
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `word_progress` ADD COLUMN `intervalDays` REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `word_progress` ADD COLUMN `easeFactor` REAL NOT NULL DEFAULT 2.5")
                db.execSQL("ALTER TABLE `word_progress` ADD COLUMN `dueAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_word_progress_dueAt` ON `word_progress` (`dueAt`)")
            }
        }

        // 完整 SM-2 repetitions + 答题明细表
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `word_progress` ADD COLUMN `repetitions` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `answer_event` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`word` TEXT NOT NULL, " +
                        "`correct` INTEGER NOT NULL, " +
                        "`quality` INTEGER NOT NULL, " +
                        "`answeredAt` INTEGER NOT NULL, " +
                        "`day` INTEGER NOT NULL, " +
                        "`practiceType` TEXT NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_answer_event_word` ON `answer_event` (`word`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_answer_event_answeredAt` ON `answer_event` (`answeredAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_answer_event_day` ON `answer_event` (`day`)")
            }
        }

        // 课本单元进度表：只记 unitId + completedAt，解锁靠 index 顺序推
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `unit_progress` (" +
                        "`unitId` TEXT NOT NULL, `completedAt` INTEGER NOT NULL, PRIMARY KEY(`unitId`))"
                )
            }
        }

        // 单元内多关：词汇 / 句型 / 综合
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `unit_level_progress` (" +
                        "`unitId` TEXT NOT NULL, `level` INTEGER NOT NULL, `completedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`unitId`, `level`))"
                )
            }
        }

        fun get(context: Context): ProgressDatabase = instance ?: synchronized(this) {
            instance ?: Room
                .databaseBuilder(context.applicationContext, ProgressDatabase::class.java, "ogden-progress.db")
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8
                )
                .build()
                .also { instance = it }
        }
    }
}
