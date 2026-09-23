package com.example.ogdenkids

import org.json.JSONArray
import com.example.ogdenkids.data.DailyActivityEntity
import com.example.ogdenkids.data.EarnedRewardEntity
import com.example.ogdenkids.data.LevelProgressEntity
import com.example.ogdenkids.data.ProgressSnapshot
import com.example.ogdenkids.data.UnitLevelProgressEntity
import com.example.ogdenkids.data.UnitProgressEntity
import com.example.ogdenkids.data.WordProgressEntity
import com.example.ogdenkids.data.decodeProgressSnapshot
import com.example.ogdenkids.data.encodeProgressSnapshot
import com.example.ogdenkids.data.legacyLevelProgress
import com.example.ogdenkids.data.legacyRewards
import com.example.ogdenkids.data.legacyWordProgress
import com.example.ogdenkids.data.Sm2State
import com.example.ogdenkids.data.nextSm2
import com.example.ogdenkids.data.AnswerEventEntity
import com.example.ogdenkids.data.qualityFromCorrect
import com.example.ogdenkids.data.qualityFromSpeak
import com.example.ogdenkids.data.qualityHistogram
import com.example.ogdenkids.data.selectDueForReview
import com.example.ogdenkids.data.selectWeakWords
import com.example.ogdenkids.data.summarizeQuality
import com.example.ogdenkids.data.tutorFocusAddon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OgdenDataTest {
    private val words = JSONArray(
        File("src/main/assets/ogden_words.json").readText(Charsets.UTF_8).trimStart('\uFEFF')
    )

    private val parsed: List<OgdenWord> = List(words.length()) { index ->
        val item = words.getJSONObject(index)
        val synonyms = item.getJSONArray("s")
        OgdenWord(
            word = item.getString("w"),
            category = Category.from(item.getString("c")),
            zh = item.getString("zh"),
            englishDefinition = item.getString("en"),
            example = item.getString("ex"),
            exampleZh = item.getString("exz"),
            synonyms = List(synonyms.length()) { synonyms.getString(it) },
            ipaUk = "",
            ipaUs = ""
        )
    }

    @Test
    fun wordListHasExpectedTotalAndCategories() {
        assertEquals(1203, words.length())
        val counts = mutableMapOf<String, Int>()
        repeat(words.length()) { index ->
            val item = words.getJSONObject(index)
            counts[item.getString("c")] = (counts[item.getString("c")] ?: 0) + 1
        }
        assertEquals(100, counts["op"])
        assertEquals(400, counts["gt"])
        assertEquals(200, counts["pt"])
        assertEquals(100, counts["qg"])
        assertEquals(50, counts["qo"])
        assertEquals(61, counts["fn"])
        assertEquals(74, counts["vb"])
        assertEquals(73, counts["na"])
        assertEquals(145, counts["th"])
    }

    @Test
    fun requiredFieldsArePresent() {
        repeat(words.length()) { index ->
            val item = words.getJSONObject(index)
            listOf("w", "c", "zh", "en", "ex", "exz", "s").forEach { key ->
                assertFalse("Missing $key at $index", item.isNull(key))
            }
            assertFalse(item.getString("w").isBlank())
            assertFalse(item.getString("zh").isBlank())
            assertFalse(item.getString("en").isBlank())
            assertFalse(item.getString("ex").isBlank())
        }
    }

    @Test
    fun synonymQuestionAlwaysHasFourDistinctOptionsWithAnswer() {
        parsed.forEach { word ->
            val question = buildQuestion(PracticeType.Synonym, word, parsed, seed = 7)
            assertEquals("options for ${word.word}", 4, question.options.size)
            assertEquals("distinct options for ${word.word}", 4, question.options.distinct().size)
            assertEquals(1, question.options.count { it == question.answer })
        }
    }

    @Test
    fun differentSeedsChangeDistractors() {
        val word = parsed.first()
        val a = buildQuestion(PracticeType.Meaning, word, parsed, seed = 1).options.toSet()
        val b = buildQuestion(PracticeType.Meaning, word, parsed, seed = 2).options.toSet()
        assertTrue("distractors should differ across attempts", a != b)
        assertEquals(a, buildQuestion(PracticeType.Meaning, word, parsed, seed = 1).options.toSet())
    }

    @Test
    fun libraryFilterMatchesWordChineseAndDefinition() {
        val word = parsed.first { it.category == Category.Operations }
        assertTrue(matchesLibraryFilter(word, word.word.uppercase(), null))
        assertTrue(matchesLibraryFilter(word, word.zh, null))
        assertTrue(matchesLibraryFilter(word, "", Category.Operations))
        assertFalse(matchesLibraryFilter(word, "", Category.Opposites))
        assertFalse(matchesLibraryFilter(word, "zzzznotaword", null))
    }

    @Test
    fun resetDropsProgressKeysButKeepsAccent() {
        val keys = setOf(
            "accent", "themeMode", "speakLevel", "roomImported", "aiKey", "parentPin",
            "reviewReminderEnabled", "reviewReminderHour", "reminderDueCount",
            SpeakCalibrationStore.SAMPLES_KEY, SpeakCalibrationStore.THRESHOLDS_KEY,
            "favorites", "mistakes", "mastery.cat", "attempts.cat", "correct.cat",
            "last.cat", "level.op.1.complete", "streak", "lastStudyDay", "lastCategory", "lastLevel"
        )
        val dropped = resettableProgressKeys(keys)
        assertFalse(dropped.contains("accent"))
        assertFalse(dropped.contains("themeMode"))
        assertFalse(dropped.contains("speakLevel"))
        assertFalse(dropped.contains("roomImported"))
        assertFalse(dropped.contains("aiKey"))
        assertFalse(dropped.contains("parentPin"))
        assertFalse(dropped.contains("reviewReminderEnabled"))
        assertFalse(dropped.contains("reviewReminderHour"))
        assertFalse(dropped.contains("reminderDueCount"))
        assertFalse(dropped.contains(SpeakCalibrationStore.SAMPLES_KEY))
        assertFalse(dropped.contains(SpeakCalibrationStore.THRESHOLDS_KEY))
        assertTrue(dropped.contains("streak"))
        assertTrue(dropped.contains("favorites"))
        assertTrue(dropped.contains("mastery.cat"))
    }

    @Test
    fun masteryRisesToCapAndFallsToFloor() {
        var progress = WordProgress(favorite = true, mistake = true, mastery = 0, attempts = 0, correct = 0)
        repeat(3) { progress = nextProgress(progress, correct = true) }
        assertEquals(3, progress.mastery)
        assertEquals(3, progress.attempts)
        assertEquals(3, progress.correct)
        assertFalse(progress.mistake)
        // 收藏状态不受答题影响
        assertTrue(progress.favorite)
        // 已到上限继续答对不越界
        progress = nextProgress(progress, correct = true)
        assertEquals(3, progress.mastery)

        // 掌握度 1 答错归零，同时进错词本
        var low = WordProgress(favorite = false, mistake = false, mastery = 1, attempts = 5, correct = 4)
        low = nextProgress(low, correct = false)
        assertEquals(0, low.mastery)
        assertTrue(low.mistake)
        assertEquals(6, low.attempts)
        assertEquals(4, low.correct)
        // 已到下限继续答错不越界
        low = nextProgress(low, correct = false)
        assertEquals(0, low.mastery)
    }

    @Test
    fun streakSameDayHoldsConsecutiveIncrementsGapResets() {
        assertEquals(4, nextStreak(lastDay = 100L, today = 100L, streak = 4))
        assertEquals(5, nextStreak(lastDay = 99L, today = 100L, streak = 4))
        assertEquals(1, nextStreak(lastDay = 97L, today = 100L, streak = 4))
        assertEquals(1, nextStreak(lastDay = 0L, today = 100L, streak = 0))
    }

    @Test
    fun legacyPrefsMapToRoomRows() {
        val prefs = mapOf<String, Any?>(
            "accent" to "UK",
            "favorites" to setOf("cat", "dog"),
            "mistakes" to setOf("dog"),
            "mastery.cat" to 3,
            "attempts.cat" to 5,
            "correct.cat" to 4,
            "last.cat" to 1_700_000_000_000L,
            "mastery.fish" to 1,
            "level.op.1.complete" to true,
            "level.gt.2.complete" to false,
            "streak" to 7,
            "lastCategory" to "gt",
            "lastLevel" to 3
        )
        val words = legacyWordProgress(prefs).associateBy { it.word }
        assertEquals(setOf("cat", "dog", "fish"), words.keys)
        val cat = words.getValue("cat")
        assertTrue(cat.favorite)
        assertFalse(cat.mistake)
        assertEquals(3, cat.mastery)
        assertEquals(5, cat.attempts)
        assertEquals(4, cat.correct)
        assertEquals(1_700_000_000_000L, cat.lastAnsweredAt)
        val dog = words.getValue("dog")
        assertTrue(dog.favorite)
        assertTrue(dog.mistake)
        assertEquals(0, dog.mastery)
        assertEquals(0, words.getValue("fish").lastAnsweredAt)

        val levels = legacyLevelProgress(prefs)
        assertEquals(1, levels.size)
        assertEquals("op", levels.first().category)
        assertEquals(1, levels.first().level)
        // 标量与偏好不会被当成词
        assertNull(words["streak"])
    }

    @Test
    fun legacyPrefsWithoutProgressYieldNothing() {
        val prefs = mapOf<String, Any?>("accent" to "US")
        assertTrue(legacyWordProgress(prefs).isEmpty())
        assertTrue(legacyLevelProgress(prefs).isEmpty())
    }

    @Test
    fun legacyRewardPrefsMapToRows() {
        val rows = legacyRewards(mapOf(
            "reward.op.Easy" to "贴纸",
            "reward.gt.Hard" to "  ",
            "reward.pt" to "缺难度",
            "accent" to "US"
        )).associateBy { it.category }
        assertEquals(1, rows.size)
        assertEquals("贴纸", rows.getValue("op").text)
        assertEquals("Easy", rows.getValue("op").difficulty)
    }

    @Test
    fun examCountClampsToPool() {
        assertEquals(10, examCountDefault(10))
        assertEquals(50, examCountDefault(400))
        assertEquals(1, clampExamCount(0, 400))
        assertEquals(400, clampExamCount(999, 400))
        assertEquals(50, clampExamCount(50, 400))
        val a = practiceWords(parsed, Category.GeneralThings, 0, 1, 50)
        val b = practiceWords(parsed, Category.GeneralThings, 0, 2, 50)
        assertEquals(50, a.size)
        assertEquals(50, b.size)
        assertTrue(a.all { it.category == Category.GeneralThings })
        assertNotEquals(a.map { it.word }, b.map { it.word })
        assertEquals(400, practiceWords(parsed, Category.GeneralThings, 0, 1, 400).size)
        val levelPool = parsed.filter { it.category == Category.Operations }.take(10)
        val level1 = practiceWords(parsed, Category.Operations, 1, 99)
        val level1b = practiceWords(parsed, Category.Operations, 1, 100)
        assertEquals(10, level1.size)
        // 同一关词集合固定，顺序按种子打乱
        assertEquals(levelPool.map { it.word }.toSet(), level1.map { it.word }.toSet())
        assertNotEquals(level1.map { it.word }, level1b.map { it.word })
    }

    @Test
    fun earnedRewardsGroupBySameText() {
        val rows = listOf(
            EarnedRewardEntity("op", "Easy", "手机碎片", 1),
            EarnedRewardEntity("gt", "Easy", "手机碎片", 2),
            EarnedRewardEntity("pt", "Hard", "贴纸", 3),
            EarnedRewardEntity("vb", "Speak", "  ", 4)
        )
        val grouped = groupEarnedRewards(rows)
        assertEquals(listOf("手机碎片", "贴纸"), grouped.map { it.text })
        assertEquals(2, grouped.first().count)
        assertEquals(1, grouped.last().count)
    }

    @Test
    fun normalizeSpokenStripsPunctuationAndCase() {
        assertEquals("cat", normalizeSpoken("  Cat.  "))
        assertEquals("it's", normalizeSpoken("It's"))
        assertEquals("get up", normalizeSpoken("get   up!"))
    }

    @Test
    fun metaphoneGroupsSimilarSounds() {
        assertEquals(metaphone("cat"), metaphone("kat"))
        assertEquals(metaphone("phone"), metaphone("fone"))
        assertNotEquals(metaphone("cat"), metaphone("dog"))
    }

    @Test
    fun spokenSimilarityRanksMatchQuality() {
        assertEquals(1f, spokenSimilarity("cat", "cat"))
        assertEquals(1f, spokenSimilarity("cat", " Cat. "))
        assertTrue(spokenSimilarity("cat", "a cat") >= 0.95f)
        assertTrue(spokenSimilarity("cat", "kat") >= 0.85f)
        assertTrue(spokenSimilarity("cat", "bat") < 0.85f)
        assertEquals(0f, spokenSimilarity("", "cat"))
    }

    @Test
    fun assessPronunciationRespectsLevels() {
        // 精确匹配：任何级别都过
        assertTrue(assessPronunciation("cat", "cat", 0.8f, SpeakLevel.Strict).passed)
        // 音近：宽松/标准过，严格不过
        assertTrue(assessPronunciation("cat", "kat", 0.8f, SpeakLevel.Lenient).passed)
        assertTrue(assessPronunciation("cat", "kat", 0.8f, SpeakLevel.Normal).passed)
        assertFalse(assessPronunciation("cat", "kat", 0.8f, SpeakLevel.Strict).passed)
        // 精确但置信度低：标准/严格不过，宽松过
        assertFalse(assessPronunciation("cat", "cat", 0.1f, SpeakLevel.Normal).passed)
        assertFalse(assessPronunciation("cat", "cat", 0.1f, SpeakLevel.Strict).passed)
        assertTrue(assessPronunciation("cat", "cat", 0.1f, SpeakLevel.Lenient).passed)
        // 完全不对：任何级别都不过
        assertFalse(assessPronunciation("cat", "dog", 0.9f, SpeakLevel.Lenient).passed)
    }

    @Test
    fun assessPronunciationUsesCustomThresholds() {
        val loose = SpeakThresholds(minSimilarity = 0.5f, minConfidence = 0.1f)
        // 默认 Normal 下 "kat"+高置信度过；严格自定义门槛可卡住
        assertTrue(assessPronunciation("cat", "kat", 0.8f, SpeakLevel.Normal).passed)
        val strictCustom = SpeakThresholds(minSimilarity = 0.99f, minConfidence = 0.9f)
        assertFalse(assessPronunciation("cat", "kat", 0.8f, SpeakLevel.Normal, strictCustom).passed)
        // 自定义放宽后低置信度也可过
        assertTrue(assessPronunciation("cat", "cat", 0.15f, SpeakLevel.Normal, loose).passed)
    }

    @Test
    fun adjustSpeakThresholdsLowersWhenUserSaysShouldPass() {
        val base = defaultSpeakThresholds(SpeakLevel.Normal)
        // 相似度刚过默认门槛边缘、机器判不过，用户说其实对了 → 降门槛
        val samples = listOf(
            SpeakFeedbackSample(0.80f, 0.4f, userSaysShouldPass = true, level = SpeakLevel.Normal),
            SpeakFeedbackSample(0.82f, 0.35f, userSaysShouldPass = true, level = SpeakLevel.Normal)
        )
        val adjusted = adjustSpeakThresholds(base, samples, SpeakLevel.Normal)
        assertTrue(adjusted.minSimilarity < base.minSimilarity)
        assertTrue(adjusted.minConfidence <= base.minConfidence)
    }

    @Test
    fun adjustSpeakThresholdsRaisesWhenUserSaysShouldFail() {
        val base = defaultSpeakThresholds(SpeakLevel.Normal)
        // 机器判过、用户说其实错了 → 升门槛
        val samples = listOf(
            SpeakFeedbackSample(0.90f, 0.5f, userSaysShouldPass = false, level = SpeakLevel.Normal),
            SpeakFeedbackSample(0.88f, 0.45f, userSaysShouldPass = false, level = SpeakLevel.Normal)
        )
        val adjusted = adjustSpeakThresholds(base, samples, SpeakLevel.Normal)
        assertTrue(adjusted.minSimilarity > base.minSimilarity)
    }

    @Test
    fun speakThresholdsRoundTrip() {
        val map = mapOf(
            SpeakLevel.Normal to SpeakThresholds(0.8f, 0.25f),
            SpeakLevel.Strict to SpeakThresholds(0.97f, 0.6f)
        )
        val back = decodeSpeakThresholds(encodeSpeakThresholds(map))
        assertEquals(0.8f, back.getValue(SpeakLevel.Normal).minSimilarity, 1e-5f)
        assertEquals(0.25f, back.getValue(SpeakLevel.Normal).minConfidence, 1e-5f)
        assertEquals(0.97f, back.getValue(SpeakLevel.Strict).minSimilarity, 1e-5f)
    }

    @Test
    fun screenSaverRoundTripsPracticeAndDetail() {
        val scope = object : androidx.compose.runtime.saveable.SaverScope {
            override fun canBeSaved(value: Any): Boolean = true
        }
        val practice = Screen.Practice(
            categoryCode = "op",
            level = 2,
            examCount = 0,
            wordKeys = listOf("cat", "dog"),
            title = "收藏夹",
            unitId = "pep.g3.vol1.u1"
        )
        val saved = with(Screen.Saver) { scope.save(practice) }!!
        val restored = Screen.Saver.restore(saved) as Screen.Practice
        assertEquals("op", restored.categoryCode)
        assertEquals(2, restored.level)
        assertEquals(listOf("cat", "dog"), restored.wordKeys)
        assertEquals("收藏夹", restored.title)
        assertEquals("pep.g3.vol1.u1", restored.unitId)

        val unit = Screen.CurriculumUnit("pep.g3.vol2.u3")
        val unitSaved = with(Screen.Saver) { scope.save(unit) }!!
        assertEquals(unit, Screen.Saver.restore(unitSaved))

        val detail = Screen.Detail("cat", listOf("cat", "dog", "fish"))
        val detailSaved = with(Screen.Saver) { scope.save(detail) }!!
        val detailBack = Screen.Saver.restore(detailSaved) as Screen.Detail
        assertEquals("cat", detailBack.wordKey)
        assertEquals(listOf("cat", "dog", "fish"), detailBack.neighborKeys)
    }

    @Test
    fun reviewReminderNextTriggerSkipsPastHour() {
        val cal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.SEPTEMBER, 23, 20, 30, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis
        val next = ReviewReminderScheduler.nextTriggerMillis(now, hour = 19)
        val nextCal = java.util.Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(24, nextCal.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(19, nextCal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(0, nextCal.get(java.util.Calendar.MINUTE))
    }

    @Test
    fun levenshteinDistanceIsCorrect() {
        assertEquals(0, levenshtein("cat", "cat"))
        assertEquals(3, levenshtein("kitten", "sitting"))
        assertEquals(1, levenshtein("cat", "cut"))
    }

    @Test
    fun dueForReviewOrdersByDueAtAndLowMastery() {
        fun row(
            word: String,
            mastery: Int,
            attempts: Int,
            dueAt: Long,
            last: Long = 0L
        ) = word to WordProgressEntity(
            word,
            mastery = mastery,
            attempts = attempts,
            lastAnsweredAt = last,
            dueAt = dueAt
        )

        val now = 1_000_000L
        val keys = selectDueForReview(
            listOf(
                row("never", 0, 0, 0), // 从未答过，不进队列
                row("due-early", 1, 3, dueAt = 100),
                row("due-later", 2, 5, dueAt = 500),
                row("legacy-zero", 0, 2, dueAt = 0L), // dueAt==0 视为已到期，排最前
                row("not-yet", 1, 4, dueAt = now + 10_000), // 未到期，不进
                row("mastered", 3, 10, dueAt = 50)
            ),
            limit = 10,
            nowMillis = now
        )
        // dueAt ASC: 0, 100, 500；同 due 再 mastery
        assertEquals(listOf("legacy-zero", "due-early", "due-later"), keys)
        assertEquals(
            2,
            selectDueForReview(
                listOf(row("a", 1, 1, 1), row("b", 1, 1, 2)),
                limit = 2,
                nowMillis = now
            ).size
        )
        assertTrue(selectDueForReview(emptyList(), 5, now).isEmpty())
    }

    @Test
    fun comboCueMapsExactStreak() {
        assertEquals(SfxCue.Combo, comboCue(2))
        assertEquals(SfxCue.Combo3, comboCue(3))
        assertTrue(comboCue(4) in listOf(SfxCue.Correct, SfxCue.Correct2, SfxCue.Correct3))
        assertNotEquals(SfxCue.Combo3, comboCue(4))
        // 5–10 与连击数文案一一对应（9 绝不能播 combo10）
        assertEquals(SfxCue.Combo5, comboCue(5))
        assertEquals(SfxCue.Combo6, comboCue(6))
        assertEquals(SfxCue.Combo7, comboCue(7))
        assertEquals(SfxCue.Combo8, comboCue(8))
        assertEquals(SfxCue.Combo9, comboCue(9))
        assertEquals(SfxCue.Combo10, comboCue(10))
        assertEquals(SfxCue.ComboMax, comboCue(11))
        assertEquals(SfxCue.ComboMax, comboCue(15))
        assertNotEquals(SfxCue.Combo10, comboCue(9))
    }

    @Test
    fun qualityHistogramAndSummary() {
        val events = listOf(
            AnswerEventEntity(word = "a", correct = false, quality = 0, answeredAt = 1, day = 1),
            AnswerEventEntity(word = "b", correct = false, quality = 1, answeredAt = 2, day = 1),
            AnswerEventEntity(word = "c", correct = true, quality = 4, answeredAt = 3, day = 1),
            AnswerEventEntity(word = "d", correct = true, quality = 5, answeredAt = 4, day = 1),
            AnswerEventEntity(word = "e", correct = true, quality = 5, answeredAt = 5, day = 1)
        )
        val hist = qualityHistogram(events)
        assertEquals(1, hist[0])
        assertEquals(1, hist[1])
        assertEquals(0, hist[2])
        assertEquals(0, hist[3])
        assertEquals(1, hist[4])
        assertEquals(2, hist[5])
        val sum = summarizeQuality(events)
        assertEquals(5, sum.total)
        assertEquals(0.6f, sum.passRate, 1e-5f) // 3/5 with q>=3
        assertEquals((0 + 1 + 4 + 5 + 5) / 5f, sum.averageQuality, 1e-5f)
        assertEquals(0, summarizeQuality(emptyList()).total)
    }

    @Test
    fun nextSm2QualitySchedule() {
        val t0 = 1_000_000L
        val fresh = Sm2State()
        // q=1 失败：重置 n、短间隔、EF 下降
        val afterFail = nextSm2(fresh, quality = 1, nowMillis = t0)
        assertEquals(0, afterFail.repetitions)
        assertEquals(Sm2State.FAIL_INTERVAL_DAYS, afterFail.intervalDays, 1e-9)
        assertTrue(afterFail.easeFactor < Sm2State.DEFAULT_EASE)
        assertEquals(t0 + (Sm2State.FAIL_INTERVAL_DAYS * Sm2State.DAY_MS).toLong(), afterFail.dueAt)

        // q=5 完美：n=1 → 1 天，EF 上升
        val afterPass1 = nextSm2(fresh, quality = 5, nowMillis = t0)
        assertEquals(1, afterPass1.repetitions)
        assertEquals(1.0, afterPass1.intervalDays, 1e-9)
        assertTrue(afterPass1.easeFactor > Sm2State.DEFAULT_EASE)
        assertEquals(t0 + (1.0 * Sm2State.DAY_MS).toLong(), afterPass1.dueAt)

        val afterPass2 = nextSm2(afterPass1, quality = 5, nowMillis = t0 + 1)
        assertEquals(2, afterPass2.repetitions)
        assertEquals(6.0, afterPass2.intervalDays, 1e-9)

        val afterPass3 = nextSm2(afterPass2, quality = 5, nowMillis = t0 + 2)
        assertEquals(3, afterPass3.repetitions)
        assertTrue(afterPass3.intervalDays >= 6.0)
        assertTrue(afterPass3.easeFactor <= 3.0 + 1e-9)

        // q=4 正确有犹豫：EF 不变（经典公式）
        val q4 = nextSm2(fresh, quality = 4, nowMillis = t0)
        assertEquals(Sm2State.DEFAULT_EASE, q4.easeFactor, 1e-9)
        assertEquals(1, q4.repetitions)

        // 兼容 boolean 重载
        assertEquals(4, qualityFromCorrect(true))
        assertEquals(1, qualityFromCorrect(false))
        assertEquals(nextSm2(fresh, quality = 4, nowMillis = t0), nextSm2(fresh, correct = true, nowMillis = t0))

        // 跟读质量
        assertEquals(5, qualityFromSpeak(true, 0.99f))
        assertEquals(0, qualityFromSpeak(false, 0.1f))

        // 易度下限
        val low = nextSm2(Sm2State(easeFactor = Sm2State.MIN_EASE), quality = 0, nowMillis = t0)
        assertEquals(Sm2State.MIN_EASE, low.easeFactor, 1e-9)
    }

    @Test
    fun weakWordsSortByAccuracyThenAttempts() {
        fun row(word: String, correct: Int, attempts: Int) =
            word to WordProgressEntity(word, correct = correct, attempts = attempts)

        val weak = selectWeakWords(
            listOf(
                row("bad", 1, 10),
                row("ok", 5, 10),
                row("worse", 0, 3),
                row("fresh", 0, 0)
            ),
            limit = 3
        ).map { it.first }
        assertEquals(listOf("worse", "bad", "ok"), weak)
    }

    @Test
    fun progressBackupRoundTripKeepsLearningData() {
        val snapshot = ProgressSnapshot(
            words = listOf(
                WordProgressEntity(
                    "cat",
                    favorite = true,
                    mistake = false,
                    mastery = 2,
                    attempts = 4,
                    correct = 3,
                    lastAnsweredAt = 99L,
                    intervalDays = 6.0,
                    easeFactor = 2.6,
                    dueAt = 12345L
                )
            ),
            levels = listOf(LevelProgressEntity("op", 1, 88L)),
            daily = listOf(DailyActivityEntity(10, answered = 5, correct = 4)),
            earned = listOf(EarnedRewardEntity("op", "Easy", "贴纸", 77L)),
            streak = 3,
            lastStudyDay = 10L,
            lastCategory = "gt",
            lastLevel = 2,
            units = listOf(UnitProgressEntity("pep.g3.vol1.u1", 55L)),
            unitLevels = listOf(
                UnitLevelProgressEntity("pep.g3.vol1.u1", 1, 50L),
                UnitLevelProgressEntity("pep.g3.vol1.u1", 2, 52L)
            )
        )
        val json = encodeProgressSnapshot(snapshot, exportedAt = 1_700_000_000_000L)
        val back = decodeProgressSnapshot(json)
        assertEquals(1, back.words.size)
        assertEquals("cat", back.words.first().word)
        assertTrue(back.words.first().favorite)
        assertEquals(2, back.words.first().mastery)
        assertEquals(99L, back.words.first().lastAnsweredAt)
        assertEquals(6.0, back.words.first().intervalDays, 1e-9)
        assertEquals(2.6, back.words.first().easeFactor, 1e-9)
        assertEquals(12345L, back.words.first().dueAt)
        assertEquals("op", back.levels.first().category)
        assertEquals(1, back.levels.first().level)
        assertEquals(5, back.daily.first().answered)
        assertEquals("贴纸", back.earned.first().text)
        assertEquals(3, back.streak)
        assertEquals("gt", back.lastCategory)
        assertEquals(2, back.lastLevel)
        assertEquals("pep.g3.vol1.u1", back.units.single().unitId)
        assertEquals(55L, back.units.single().completedAt)
        assertEquals(2, back.unitLevels.size)
        assertEquals(1, back.unitLevels.first().level)
        assertTrue(json.contains("\"units\""))
        assertTrue(json.contains("\"unitLevels\""))
        assertFalse(json.contains("aiKey"))
        assertFalse(json.contains("parentPin"))
    }

    @Test
    fun progressBackupMissingSm2FieldsUsesDefaults() {
        val json = """
            {"version":1,"words":[{"w":"dog","mastery":1,"attempts":2,"correct":1,"last":10}],
            "levels":[],"daily":[],"earned":[],"streak":0,"lastStudyDay":0,"lastCategory":"op","lastLevel":1}
        """.trimIndent()
        val back = decodeProgressSnapshot(json)
        val dog = back.words.single()
        assertEquals("dog", dog.word)
        assertEquals(0.0, dog.intervalDays, 1e-9)
        assertEquals(Sm2State.DEFAULT_EASE, dog.easeFactor, 1e-9)
        assertEquals(0L, dog.dueAt)
        assertTrue(back.units.isEmpty())
        assertTrue(back.unitLevels.isEmpty())
    }

    @Test
    fun progressBackupV1WithoutUnitsImportsEmptyUnits() {
        val json = """
            {"version":1,"words":[],"levels":[],"daily":[],"earned":[],
            "streak":1,"lastStudyDay":2,"lastCategory":"op","lastLevel":1}
        """.trimIndent()
        val back = decodeProgressSnapshot(json)
        assertEquals(1, back.streak)
        assertTrue(back.units.isEmpty())
        assertTrue(back.unitLevels.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun progressBackupRejectsUnknownVersion() {
        decodeProgressSnapshot("""{"version":99,"words":[]}""")
    }

    @Test
    fun tutorFocusAddonListsWordsAndStaysEmptyWhenNone() {
        assertEquals("", tutorFocusAddon(emptyList()))
        assertEquals("", tutorFocusAddon(listOf("  ", "")))
        val text = tutorFocusAddon(listOf("cat", "dog", "cat", "fish"), maxWords = 2)
        assertTrue(text.contains("cat"))
        assertTrue(text.contains("dog"))
        assertFalse(text.contains("fish"))
        assertTrue(text.contains("Ogden words"))
    }

    @Test
    fun customPracticeWordsUsesKeysAndSeed() {
        val keys = parsed.take(5).map { it.word }
        val a = practiceWords(parsed, Category.Operations, 0, seed = 1, wordKeys = keys)
        val b = practiceWords(parsed, Category.Operations, 0, seed = 2, wordKeys = keys)
        assertEquals(5, a.size)
        assertEquals(keys.toSet(), a.map { it.word }.toSet())
        assertNotEquals(a.map { it.word }, b.map { it.word })
    }

    @Test
    fun customPracticeWordsHonorsExamCount() {
        val keys = parsed.take(12).map { it.word }
        val exam = practiceWords(
            parsed, Category.Operations, 0, seed = 7,
            examCount = 5, wordKeys = keys
        )
        assertEquals(5, exam.size)
        assertTrue(exam.map { it.word }.toSet().all { it in keys })
        // examCount 超过词数时夹到词数
        assertEquals(
            12,
            practiceWords(parsed, Category.Operations, 0, seed = 1, examCount = 50, wordKeys = keys).size
        )
    }
}
