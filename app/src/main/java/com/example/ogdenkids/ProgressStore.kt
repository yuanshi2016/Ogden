package com.example.ogdenkids

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.ogdenkids.data.DailyActivityEntity
import com.example.ogdenkids.data.EarnedRewardEntity
import com.example.ogdenkids.data.LevelProgressEntity
import com.example.ogdenkids.data.ProgressDatabase
import com.example.ogdenkids.data.ProgressSnapshot
import com.example.ogdenkids.data.RewardEntity
import com.example.ogdenkids.data.WordProgressEntity
import com.example.ogdenkids.data.decodeProgressSnapshot
import com.example.ogdenkids.data.encodeProgressSnapshot
import com.example.ogdenkids.data.legacyLevelProgress
import com.example.ogdenkids.data.legacyRewards
import com.example.ogdenkids.data.legacyWordProgress
import com.example.ogdenkids.data.AnswerEventEntity
import com.example.ogdenkids.data.QualitySummary
import com.example.ogdenkids.data.nextSm2
import com.example.ogdenkids.data.qualityFromCorrect
import com.example.ogdenkids.data.selectDueForReview
import com.example.ogdenkids.data.summarizeQuality
import com.example.ogdenkids.data.selectWeakWords
import com.example.ogdenkids.data.Sm2State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class ProgressStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences("ogden-progress", Context.MODE_PRIVATE)
    private val dao = ProgressDatabase.get(context).progressDao()
    // 自己持有作用域而不用 rememberCoroutineScope：退出组合不该取消刚提交的落库。
    // 默认 Main，快照状态只在主线程改；数据库读写各处显式切 IO
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 组合期间只读这份内存快照；数据库读写一律在协程里，写入是「先改快照再落库」
    private val entries = mutableStateMapOf<String, WordProgress>()
    private val levels = mutableStateMapOf<String, Boolean>()
    // lastAnsweredAt 界面不显示，只在整行覆盖写时要带上，放普通 map 不参与重组
    // SM-2 字段（intervalDays/easeFactor/dueAt）随 WordProgress 进 entries 快照
    private val answeredAt = mutableMapOf<String, Long>()
    // 逐日统计：epochDay -> 答题数/答对数，快照在内存、逐条落库
    private val daily = mutableStateMapOf<Long, DailyActivityEntity>()
    private val rewards = mutableStateMapOf<String, String>()
    private val earned = mutableStateMapOf<String, EarnedRewardEntity>()
    private var streak by mutableStateOf(prefs.getInt("streak", 0))
    // 首帧数据库还没读完，界面先等这个标志，避免统计闪一次 0
    var ready by mutableStateOf(false)
        private set

    init {
        scope.launch {
            var words = emptyList<WordProgressEntity>()
            var completed = emptyList<LevelProgressEntity>()
            var dailyRows = emptyList<DailyActivityEntity>()
            var rewardRows = emptyList<RewardEntity>()
            var earnedRows = emptyList<EarnedRewardEntity>()
            withContext(Dispatchers.IO) {
                importLegacyPrefsIfNeeded()
                importLegacyRewardsIfNeeded()
                dao.pruneDailyActivity(localEpochDay(System.currentTimeMillis()) - 365)
                words = dao.allWordProgress()
                completed = dao.allLevelProgress()
                dailyRows = dao.allDailyActivity()
                rewardRows = dao.allRewards()
                earnedRows = dao.allEarnedRewards()
            }
            words.forEach {
                entries[it.word] = it.toWordProgress()
                answeredAt[it.word] = it.lastAnsweredAt
            }
            completed.forEach { levels[levelKey(it.category, it.level)] = true }
            dailyRows.forEach { daily[it.day] = it }
            rewardRows.forEach { rewards[rewardKey(it.category, it.difficulty)] = it.text }
            earnedRows.forEach { earned[rewardKey(it.category, it.difficulty)] = it }
            ready = true
        }
    }

    // 首次跑新版时把旧偏好里的进度搬进数据库：确认写入成功后才记 roomImported 标志，
    // 旧键一律保留不删，万一日后发现映射有误还能人工找回
    private suspend fun importLegacyPrefsIfNeeded() {
        if (prefs.getBoolean(IMPORTED_KEY, false)) return
        val snapshot = prefs.all
        val words = legacyWordProgress(snapshot)
        val completed = legacyLevelProgress(snapshot)
        if (words.isNotEmpty()) dao.saveWordProgress(words)
        if (completed.isNotEmpty()) dao.saveLevelProgress(completed)
        prefs.edit().putBoolean(IMPORTED_KEY, true).commit()
    }

    // 把上一版写在偏好里的 reward.{cat}.{difficulty} 搬进 Room，搬完删掉旧键
    private suspend fun importLegacyRewardsIfNeeded() {
        val snapshot = prefs.all
        val rows = legacyRewards(snapshot)
        if (rows.isNotEmpty()) dao.saveRewards(rows)
        val editor = prefs.edit()
        snapshot.keys.filter { it.startsWith("reward.") }.forEach { editor.remove(it) }
        editor.commit()
    }

    fun progress(word: String): WordProgress = entries[word] ?: Blank

    fun toggleFavorite(word: String) {
        val next = progress(word).let { it.copy(favorite = !it.favorite) }
        entries[word] = next
        persist(word, next)
    }

    /**
     * @param quality SM-2 质量 0..5；默认由 [correct] 映射（对=4，错=1）
     * @param practiceType 可选题型名，写入答题明细
     */
    fun record(
        word: String,
        correct: Boolean,
        quality: Int = qualityFromCorrect(correct),
        practiceType: String = ""
    ) {
        val now = System.currentTimeMillis()
        val q = quality.coerceIn(0, 5)
        val current = progress(word)
        val sm2 = nextSm2(
            Sm2State(
                repetitions = current.repetitions,
                intervalDays = current.intervalDays,
                easeFactor = current.easeFactor,
                dueAt = current.dueAt
            ),
            quality = q,
            nowMillis = now
        )
        val next = nextProgress(current, correct).copy(
            repetitions = sm2.repetitions,
            intervalDays = sm2.intervalDays,
            easeFactor = sm2.easeFactor,
            dueAt = sm2.dueAt
        )
        entries[word] = next
        answeredAt[word] = now
        persist(word, next)
        bumpDailyActivity(correct)
        if (correct) bumpDailyStreak()
        refreshReminderDueCount()
        val day = localEpochDay(now)
        scope.launch(Dispatchers.IO) {
            dao.saveAnswerEvent(
                AnswerEventEntity(
                    word = word,
                    correct = correct,
                    quality = q,
                    answeredAt = now,
                    day = day,
                    practiceType = practiceType
                )
            )
            // 明细最多保留约一年，避免无限膨胀
            dao.pruneAnswerEvents(now - 365L * 86_400_000L)
        }
    }

    private fun persist(word: String, value: WordProgress) {
        val entity = value.toEntity(word, answeredAt[word] ?: 0L)
        scope.launch(Dispatchers.IO) { dao.saveWordProgress(entity) }
    }

    fun masteredCount(words: List<OgdenWord>) = words.count { progress(it.word).mastery >= 3 }

    fun mistakeWords(words: List<OgdenWord>) = words.filter { progress(it.word).mistake }

    fun favoriteWords(words: List<OgdenWord>) = words.filter { progress(it.word).favorite }

    // 智能复习：已练过且未满星；dueAt<=now（dueAt==0 视为已到期），按 dueAt / mastery
    fun dueForReview(words: List<OgdenWord>, limit: Int = 20): List<OgdenWord> {
        val byWord = words.associateBy { it.word }
        val now = System.currentTimeMillis()
        val candidates = entries.map { (word, p) ->
            word to p.toEntity(word, answeredAt[word] ?: 0L)
        }
        return selectDueForReview(candidates, limit, now).mapNotNull { byWord[it] }
    }

    fun dueForReviewCount(): Int {
        val now = System.currentTimeMillis()
        return entries.count { (_, p) ->
            p.mastery < 3 && p.attempts > 0 && (p.dueAt == 0L || p.dueAt <= now)
        }
    }

    fun weakWords(words: List<OgdenWord>, limit: Int = 10): List<Pair<OgdenWord, WordProgress>> {
        val byWord = words.associateBy { it.word }
        val candidates = entries.map { (word, p) ->
            word to p.toEntity(word, answeredAt[word] ?: 0L)
        }
        return selectWeakWords(candidates, limit).mapNotNull { (w, _) ->
            val word = byWord[w] ?: return@mapNotNull null
            word to progress(w)
        }
    }

    // AI 口语：最近薄弱 + 待复习词，去重后最多 20 个
    fun focusWordsForAi(words: List<OgdenWord>, limit: Int = 20): List<String> {
        val due = dueForReview(words, limit).map { it.word }
        if (due.size >= limit) return due
        val weak = weakWords(words, limit).map { it.first.word }
        return (due + weak).distinct().take(limit)
    }

    fun dailyStreak(): Int = streak

    /**
     * 最近 [days] 天答题质量汇总（读 answer_event）。
     * 组合期用 [produceState] 异步拉取，勿在首帧同步调。
     */
    suspend fun qualitySummary(days: Int = 30): QualitySummary = withContext(Dispatchers.IO) {
        val today = localEpochDay(System.currentTimeMillis())
        val from = today - (days - 1).coerceAtLeast(0)
        summarizeQuality(dao.answerEventsSince(from))
    }

    // 最近 N 天（含今天）的逐日统计，缺的补零，倒序旧->新
    fun dailySeries(days: Int): List<DailyActivityEntity> {
        val today = localEpochDay(System.currentTimeMillis())
        return (days - 1 downTo 0).map { off ->
            val d = today - off
            daily[d] ?: DailyActivityEntity(day = d)
        }
    }

    fun lastCategory(): Category = runCatching {
        Category.from(prefs.getString("lastCategory", Category.Operations.code) ?: Category.Operations.code)
    }.getOrDefault(Category.Operations)

    fun lastLevel(): Int = prefs.getInt("lastLevel", 1).coerceAtLeast(1)

    fun saveLastLevel(category: Category, level: Int) {
        prefs.edit()
            .putString("lastCategory", category.code)
            .putInt("lastLevel", level)
            .commit()
    }

    fun savedAccent(): Accent = runCatching {
        Accent.valueOf(prefs.getString("accent", Accent.US.name) ?: Accent.US.name)
    }.getOrDefault(Accent.US)

    fun saveAccent(accent: Accent) {
        prefs.edit().putString("accent", accent.name).commit()
    }

    fun savedThemeMode(): ThemeMode = runCatching {
        ThemeMode.valueOf(prefs.getString("themeMode", ThemeMode.Child.name) ?: ThemeMode.Child.name)
    }.getOrDefault(ThemeMode.Child)

    fun saveThemeMode(mode: ThemeMode) {
        prefs.edit().putString("themeMode", mode.name).commit()
    }

    fun savedSpeakLevel(): SpeakLevel = runCatching {
        SpeakLevel.valueOf(prefs.getString("speakLevel", SpeakLevel.Normal.name) ?: SpeakLevel.Normal.name)
    }.getOrDefault(SpeakLevel.Normal)

    fun saveSpeakLevel(level: SpeakLevel) {
        prefs.edit().putString("speakLevel", level.name).commit()
    }

    fun reviewReminderEnabled(): Boolean = ReviewReminderPrefs.isEnabled(prefs)

    fun reviewReminderHour(): Int = ReviewReminderPrefs.hour(prefs)

    /** 开启/关闭每日复习提醒；开启时写入 prefs 并调度 AlarmManager。 */
    fun setReviewReminderEnabled(enabled: Boolean) {
        ReviewReminderPrefs.setEnabled(prefs, enabled)
        if (enabled) {
            cacheReminderDueCount(prefs, dueForReviewCount())
            ReviewReminderScheduler.schedule(appContext, reviewReminderHour())
        } else {
            ReviewReminderScheduler.cancel(appContext)
        }
    }

    fun setReviewReminderHour(hour: Int) {
        ReviewReminderPrefs.setHour(prefs, hour)
        if (reviewReminderEnabled()) {
            ReviewReminderScheduler.schedule(appContext, ReviewReminderPrefs.hour(prefs))
        }
    }

    /** 刷新通知文案用的待复习缓存（答题后可调） */
    fun refreshReminderDueCount() {
        cacheReminderDueCount(prefs, dueForReviewCount())
    }

    fun reward(category: Category, difficulty: Difficulty): String =
        rewards[rewardKey(category.code, difficulty.name)].orEmpty()

    fun rewardsOf(category: Category): Map<Difficulty, String> =
        Difficulty.values().associateWith { reward(category, it) }

    fun saveReward(category: Category, difficulty: Difficulty, text: String) {
        rewards[rewardKey(category.code, difficulty.name)] = text
        scope.launch(Dispatchers.IO) {
            dao.saveReward(RewardEntity(category.code, difficulty.name, text))
        }
    }

    fun earnedRewards(): List<EarnedRewardEntity> = earned.values.toList()

    fun markRewardEarned(category: Category, difficulty: Difficulty) {
        val text = reward(category, difficulty).trim()
        if (text.isEmpty()) return
        val key = rewardKey(category.code, difficulty.name)
        if (earned.containsKey(key)) return
        val row = EarnedRewardEntity(category.code, difficulty.name, text, System.currentTimeMillis())
        earned[key] = row
        scope.launch(Dispatchers.IO) { dao.saveEarnedReward(row) }
    }

    // AI Key 用 EncryptedSharedPreferences；旧明文 aiKey 读一次后迁入并删除
    private val securePrefs by lazy {
        runCatching {
            val master = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                "ogden-secure",
                master,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrElse {
            // 极少数设备 Keystore 不可用时退回普通偏好，仍优于崩溃
            prefs
        }
    }

    fun savedAiKey(): String {
        val secured = securePrefs.getString("aiKey", "").orEmpty()
        if (secured.isNotEmpty()) return secured
        val legacy = prefs.getString("aiKey", "").orEmpty()
        if (legacy.isNotEmpty()) {
            saveAiKey(legacy)
            prefs.edit().remove("aiKey").commit()
        }
        return legacy
    }

    fun saveAiKey(key: String) {
        securePrefs.edit().putString("aiKey", key).commit()
        if (prefs.contains("aiKey")) prefs.edit().remove("aiKey").commit()
    }

    // 家长密码：空串表示还没设置，首次重置进度时现场设置
    fun savedParentPin(): String = prefs.getString("parentPin", "").orEmpty()

    fun saveParentPin(pin: String) {
        prefs.edit().putString("parentPin", pin).commit()
    }

    fun isLevelComplete(category: Category, level: Int): Boolean =
        levels[levelKey(category.code, level)] == true

    fun isLevelUnlocked(category: Category, level: Int): Boolean =
        level <= 1 || isLevelComplete(category, level - 1)

    fun markLevelComplete(category: Category, level: Int) {
        levels[levelKey(category.code, level)] = true
        val entity = LevelProgressEntity(category.code, level, System.currentTimeMillis())
        scope.launch(Dispatchers.IO) { dao.saveLevelProgress(entity) }
    }

    // 清空学习进度：内存快照立即归零（界面无需重启），数据库与标量随后清
    fun resetProgress() {
        entries.clear()
        levels.clear()
        answeredAt.clear()
        daily.clear()
        earned.clear()
        streak = 0
        // 先清库再清偏好：反过来若中途进程被杀，导入标志还在而库里旧行会复活
        scope.launch(Dispatchers.IO) {
            dao.clearWordProgress()
            dao.clearLevelProgress()
            dao.clearDailyActivity()
            dao.clearEarnedRewards()
            dao.clearAnswerEvents()
            val editor = prefs.edit()
            resettableProgressKeys(prefs.all.keys).forEach { editor.remove(it) }
            editor.commit()
        }
    }

    /** 导出学习进度 JSON（不含 API Key / PIN / 发音偏好）。 */
    suspend fun exportProgressJson(): String = withContext(Dispatchers.IO) {
        // 关卡 completedAt 只在库里完整，内存 levels 只有布尔；导出时以库为准
        val levelRows = dao.allLevelProgress()
        val snapshot = ProgressSnapshot(
            words = entries.map { (word, p) -> p.toEntity(word, answeredAt[word] ?: 0L) },
            levels = levelRows.ifEmpty {
                levels.keys.mapNotNull { key ->
                    val parts = key.split('.')
                    val code = parts.getOrNull(0) ?: return@mapNotNull null
                    val level = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                    LevelProgressEntity(code, level, 0L)
                }
            },
            daily = daily.values.toList(),
            earned = earned.values.toList(),
            streak = streak,
            lastStudyDay = prefs.getLong("lastStudyDay", 0L),
            lastCategory = prefs.getString("lastCategory", Category.Operations.code) ?: Category.Operations.code,
            lastLevel = prefs.getInt("lastLevel", 1)
        )
        encodeProgressSnapshot(snapshot)
    }

    /**
     * 用备份覆盖学习进度。成功后刷新内存快照。
     * 不改 accent / themeMode / speakLevel / aiKey / parentPin。
     */
    suspend fun importProgressJson(json: String) {
        val snapshot = decodeProgressSnapshot(json)
        withContext(Dispatchers.IO) {
            dao.clearWordProgress()
            dao.clearLevelProgress()
            dao.clearDailyActivity()
            dao.clearEarnedRewards()
            dao.clearAnswerEvents()
            if (snapshot.words.isNotEmpty()) dao.saveWordProgress(snapshot.words)
            if (snapshot.levels.isNotEmpty()) dao.saveLevelProgress(snapshot.levels)
            snapshot.daily.forEach { dao.saveDailyActivity(it) }
            snapshot.earned.forEach { dao.saveEarnedReward(it) }
            prefs.edit()
                .putInt("streak", snapshot.streak)
                .putLong("lastStudyDay", snapshot.lastStudyDay)
                .putString("lastCategory", snapshot.lastCategory)
                .putInt("lastLevel", snapshot.lastLevel)
                .commit()
        }
        entries.clear()
        levels.clear()
        answeredAt.clear()
        daily.clear()
        earned.clear()
        snapshot.words.forEach {
            entries[it.word] = it.toWordProgress()
            answeredAt[it.word] = it.lastAnsweredAt
        }
        snapshot.levels.forEach { levels[levelKey(it.category, it.level)] = true }
        snapshot.daily.forEach { daily[it.day] = it }
        snapshot.earned.forEach { earned[rewardKey(it.category, it.difficulty)] = it }
        streak = snapshot.streak
    }

    private fun levelKey(categoryCode: String, level: Int) = "$categoryCode.$level"

    private fun rewardKey(categoryCode: String, difficulty: String) = "$categoryCode.$difficulty"

    private fun bumpDailyActivity(correct: Boolean) {
        val today = localEpochDay(System.currentTimeMillis())
        val cur = daily[today] ?: DailyActivityEntity(day = today)
        val next = cur.copy(answered = cur.answered + 1, correct = cur.correct + if (correct) 1 else 0)
        daily[today] = next
        scope.launch(Dispatchers.IO) { dao.saveDailyActivity(next) }
    }

    private fun bumpDailyStreak() {
        val today = localEpochDay(System.currentTimeMillis())
        val next = nextStreak(prefs.getLong("lastStudyDay", 0L), today, streak)
        streak = next
        prefs.edit().putLong("lastStudyDay", today).putInt("streak", next).apply()
    }

    private companion object {
        const val IMPORTED_KEY = "roomImported"
        val Blank = WordProgress(
            favorite = false,
            mistake = false,
            mastery = 0,
            attempts = 0,
            correct = 0,
            repetitions = 0,
            intervalDays = 0.0,
            easeFactor = Sm2State.DEFAULT_EASE,
            dueAt = 0L
        )
    }
}

private fun WordProgressEntity.toWordProgress() = WordProgress(
    favorite = favorite,
    mistake = mistake,
    mastery = mastery,
    attempts = attempts,
    correct = correct,
    repetitions = repetitions,
    intervalDays = intervalDays,
    easeFactor = easeFactor,
    dueAt = dueAt
)

private fun WordProgress.toEntity(word: String, lastAnsweredAt: Long) = WordProgressEntity(
    word = word,
    favorite = favorite,
    mistake = mistake,
    mastery = mastery,
    attempts = attempts,
    correct = correct,
    lastAnsweredAt = lastAnsweredAt,
    repetitions = repetitions,
    intervalDays = intervalDays,
    easeFactor = easeFactor,
    dueAt = dueAt
)

// 重置要删哪些键，抽成纯函数便于单测：accent / themeMode / speakLevel / aiKey / parentPin
// 以及跟读标定、复习提醒开关是偏好设置不属于进度，必须留下；
// roomImported 是迁移标志，删掉会让残留的旧键在下次启动被重新导入，也必须留下
private val PRESERVED_PREF_KEYS = setOf(
    "accent",
    "roomImported",
    "themeMode",
    "speakLevel",
    "aiKey",
    "parentPin",
    SpeakCalibrationStore.SAMPLES_KEY,
    SpeakCalibrationStore.THRESHOLDS_KEY,
    ReviewReminderPrefs.ENABLED_KEY,
    ReviewReminderPrefs.HOUR_KEY,
    "reminderDueCount"
)

fun resettableProgressKeys(keys: Set<String>): Set<String> =
    keys.filterTo(mutableSetOf()) { key -> key !in PRESERVED_PREF_KEYS }

// 答题后的熟练度/错词流转：答对 +1 上限 3，答错 -1 下限 0，掌握度归零也计入错词
fun nextProgress(current: WordProgress, correct: Boolean): WordProgress {
    val mastery = if (correct) (current.mastery + 1).coerceAtMost(3) else (current.mastery - 1).coerceAtLeast(0)
    return current.copy(
        mistake = !correct || mastery == 0,
        mastery = mastery,
        attempts = current.attempts + 1,
        correct = current.correct + if (correct) 1 else 0
    )
}

data class EarnedRewardSummary(val text: String, val count: Int)

fun groupEarnedRewards(rows: List<EarnedRewardEntity>): List<EarnedRewardSummary> =
    rows.filter { it.text.isNotBlank() }
        .groupingBy { it.text.trim() }
        .eachCount()
        .map { EarnedRewardSummary(it.key, it.value) }
        .sortedWith(compareByDescending<EarnedRewardSummary> { it.count }.thenBy { it.text })

// 连续学习天数：同一天不变，隔一天 +1，断档从 1 重新开始
fun nextStreak(lastDay: Long, today: Long, streak: Int): Int = when {
    lastDay == today -> streak
    lastDay == today - 1 -> streak + 1
    else -> 1
}

// 本地时区的「纪元日」：以当天 0 点为界，日图表按天分桶时避免 UTC 凌晨 8 点切天
private fun localEpochDay(millis: Long): Long {
    val now = Calendar.getInstance().apply { timeInMillis = millis }
    val start = Calendar.getInstance().apply {
        clear()
        set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
    }
    return start.timeInMillis / 86_400_000L
}
