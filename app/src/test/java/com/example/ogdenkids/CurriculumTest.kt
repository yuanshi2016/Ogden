package com.example.ogdenkids

import com.example.ogdenkids.Accent
import com.example.ogdenkids.audioSlug
import com.example.ogdenkids.curriculum.CurriculumUnit
import com.example.ogdenkids.curriculum.UNIT_LEVEL_MIXED
import com.example.ogdenkids.curriculum.UNIT_LEVEL_PHRASES
import com.example.ogdenkids.curriculum.UNIT_LEVEL_WORDS
import com.example.ogdenkids.curriculum.buildParentReport
import com.example.ogdenkids.curriculum.dayCheckSpeakPhraseId
import com.example.ogdenkids.curriculum.isRevisionUnit
import com.example.ogdenkids.curriculum.isUnitLevelUnlocked
import com.example.ogdenkids.curriculum.isUnitUnlocked
import com.example.ogdenkids.curriculum.parseCurriculumIndex
import com.example.ogdenkids.curriculum.parseCurriculumVolume
import com.example.ogdenkids.curriculum.mergePracticeDictionary
import com.example.ogdenkids.curriculum.parsePepExtraWords
import com.example.ogdenkids.curriculum.resolveSuggestedWeek
import com.example.ogdenkids.curriculum.resolveUnitWords
import com.example.ogdenkids.curriculum.suggestedUnitIds
import com.example.ogdenkids.curriculum.unitMatchesWeek
import com.example.ogdenkids.localAudioPath
import com.example.ogdenkids.localExampleAudioPath
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CurriculumTest {
    private fun asset(name: String) =
        File("src/main/assets/curriculum/$name").readText(Charsets.UTF_8).trimStart('\uFEFF')

    private val index = parseCurriculumIndex(asset("pep_grade3_index.json"))
    private val vol1 = parseCurriculumVolume(asset("pep_grade3_vol1.json"))
    private val vol2 = parseCurriculumVolume(asset("pep_grade3_vol2.json"))
    private val units: List<CurriculumUnit> = vol1 + vol2

    private val index4 = parseCurriculumIndex(asset("pep_grade4_index.json"))
    private val vol1g4 = parseCurriculumVolume(asset("pep_grade4_vol1.json"))
    private val vol2g4 = parseCurriculumVolume(asset("pep_grade4_vol2.json"))
    private val units4: List<CurriculumUnit> = vol1g4 + vol2g4

    private val index5 = parseCurriculumIndex(asset("pep_grade5_index.json"))
    private val vol1g5 = parseCurriculumVolume(asset("pep_grade5_vol1.json"))
    private val vol2g5 = parseCurriculumVolume(asset("pep_grade5_vol2.json"))
    private val units5: List<CurriculumUnit> = vol1g5 + vol2g5

    private val index6 = parseCurriculumIndex(asset("pep_grade6_index.json"))
    private val vol1g6 = parseCurriculumVolume(asset("pep_grade6_vol1.json"))
    private val vol2g6 = parseCurriculumVolume(asset("pep_grade6_vol2.json"))
    private val units6: List<CurriculumUnit> = vol1g6 + vol2g6

    private val ogdenWords: List<OgdenWord> = run {
        val arr = JSONArray(
            File("src/main/assets/ogden_words.json").readText(Charsets.UTF_8).trimStart('\uFEFF')
        )
        val ipaRoot = JSONObject(
            File("src/main/assets/ogden_ipa.json").readText(Charsets.UTF_8).trimStart('\uFEFF')
        )
        List(arr.length()) { i ->
            val item = arr.getJSONObject(i)
            val word = item.getString("w")
            val ipaItem = ipaRoot.optJSONObject(word)
            OgdenWord(
                word = word,
                category = Category.from(item.getString("c")),
                zh = item.getString("zh"),
                englishDefinition = item.getString("en"),
                example = item.getString("ex"),
                exampleZh = item.getString("exz"),
                synonyms = emptyList(),
                ipaUk = ipaItem?.optString("uk").orEmpty(),
                ipaUs = ipaItem?.optString("us").orEmpty()
            )
        }
    }

    private val extraWords: List<OgdenWord> = parsePepExtraWords(asset("pep_extra_words.json"))

    private val dictionary: Map<String, OgdenWord> =
        mergePracticeDictionary(ogdenWords, extraWords)

    @Test
    fun indexAndVolumesParseWithStableIds() {
        assertEquals(3, index.grade)
        assertEquals(listOf(7, 7), index.volumes.map { it.unitIds.size })
        assertEquals(14, units.size)
        assertEquals(index.orderedUnitIds, units.map { it.id })
        assertEquals("Making friends", units.first().titleEn)
        assertEquals("Being a good guest", vol1.last().titleEn)
        assertEquals("pep.g3.vol1.rev", vol1.last().id)
        assertEquals("Meeting new people", vol2.first().titleEn)
        assertEquals("Going to a school fair", vol2.last().titleEn)
        assertEquals("pep.g3.vol2.rev", vol2.last().id)
    }

    @Test
    fun everyUnitHasWordsAndPhrasesInRange() {
        units.forEach { unit ->
            assertTrue("${unit.id} words=${unit.words.size}", unit.words.size in 8..18)
            assertTrue("${unit.id} phrases=${unit.phrases.size}", unit.phrases.size in 3..6)
            assertTrue("${unit.id} goals", unit.goalsZh.size in 1..2)
            assertTrue("${unit.id} activities", unit.activities.size in 1..2)
        }
    }

    @Test
    fun allUnitWordsResolveAgainstDictionary() {
        units.forEach { unit ->
            val resolved = resolveUnitWords(unit, dictionary)
            assertEquals("${unit.id} missing=${resolved.missingKeys}", emptyList<String>(), resolved.missingKeys)
            assertEquals(unit.words.size, resolved.words.size)
        }
    }

    @Test
    fun resolveUnitWordsSkipsMissingKeys() {
        val unit = units.first().copy(words = listOf("hello", "no-such-word"))
        val resolved = resolveUnitWords(unit, dictionary)
        assertEquals(listOf("no-such-word"), resolved.missingKeys)
        assertEquals(listOf("hello"), resolved.words.map { it.word })
    }

    @Test
    fun unlockFollowsIndexOrderAcrossVolumes() {
        val ordered = index.orderedUnitIds
        assertTrue(isUnitUnlocked(ordered, emptySet(), "pep.g3.vol1.u1"))
        assertFalse(isUnitUnlocked(ordered, emptySet(), "pep.g3.vol1.u2"))
        assertTrue(isUnitUnlocked(ordered, setOf("pep.g3.vol1.u1"), "pep.g3.vol1.u2"))
        // 跨册：下册 U1 依赖上册 Revision 完成
        assertFalse(isUnitUnlocked(ordered, setOf("pep.g3.vol1.u6"), "pep.g3.vol2.u1"))
        assertTrue(isUnitUnlocked(ordered, setOf("pep.g3.vol1.rev"), "pep.g3.vol2.u1"))
        assertFalse(isUnitUnlocked(ordered, setOf("pep.g3.vol1.rev"), "pep.g3.vol9.u1"))
    }

    @Test
    fun unitLevelsUnlockSequentially() {
        assertTrue(isUnitLevelUnlocked(UNIT_LEVEL_WORDS, emptySet()))
        assertFalse(isUnitLevelUnlocked(UNIT_LEVEL_PHRASES, emptySet()))
        assertTrue(isUnitLevelUnlocked(UNIT_LEVEL_PHRASES, setOf(UNIT_LEVEL_WORDS)))
        assertFalse(isUnitLevelUnlocked(UNIT_LEVEL_MIXED, setOf(UNIT_LEVEL_WORDS)))
        assertTrue(isUnitLevelUnlocked(UNIT_LEVEL_MIXED, setOf(UNIT_LEVEL_WORDS, UNIT_LEVEL_PHRASES)))
        assertFalse(isUnitLevelUnlocked(99, setOf(1, 2, 3)))
    }

    @Test
    fun suggestedWeekManualOverridesTermStart() {
        assertEquals(3, resolveSuggestedWeek(manualWeek = 3, termStartDay = 100L, todayEpochDay = 200L))
        // 开学日 100，今天 114 → 第 3 周 ((114-100)/7)+1
        assertEquals(3, resolveSuggestedWeek(null, 100L, 114L))
        assertEquals(1, resolveSuggestedWeek(null, 100L, 99L))
        assertNull(resolveSuggestedWeek(null, null, 200L))
        // 0 不在 1..20：视为未设手动周，回落开学日
        assertEquals(3, resolveSuggestedWeek(0, 100L, 114L))
        assertEquals(20, resolveSuggestedWeek(null, 100L, 100L + 7 * 30))
    }

    @Test
    fun unitWeekMatchAndSuggestedIds() {
        assertTrue(unitMatchesWeek(listOf(1, 2), 2))
        assertFalse(unitMatchesWeek(listOf(1, 2), 5))
        assertFalse(unitMatchesWeek(emptyList(), 1))
        val ids = suggestedUnitIds(units, week = 1)
        assertTrue(ids.contains("pep.g3.vol1.u1"))
        assertTrue(ids.contains("pep.g3.vol2.u1"))
        assertEquals(emptyList<String>(), suggestedUnitIds(units, null))
    }

    @Test
    fun parentReportCountsLevelsAndSpeak() {
        val unit = units.first()
        val report = buildParentReport(
            unit,
            completedLevels = setOf(UNIT_LEVEL_WORDS, UNIT_LEVEL_PHRASES),
            dayCheckItems = setOf(
                "listen_words",
                dayCheckSpeakPhraseId(0),
                dayCheckSpeakPhraseId(1),
                "activity.0"
            )
        )
        assertEquals(2, report.levelsDoneCount)
        assertTrue(report.wordsLevelDone)
        assertTrue(report.phrasesLevelDone)
        assertFalse(report.mixedLevelDone)
        assertTrue(report.listenWordsDone)
        assertEquals(2, report.speakPassedCount)
        assertEquals(unit.phrases.size, report.phraseTotal)
        assertEquals(1, report.activityDoneCount)
        assertTrue(report.levelsLine.contains("①词汇✓"))
        assertTrue(report.speakLine.contains("2/"))
    }

    @Test
    fun revisionUnitIdEndsWithRev() {
        assertTrue(isRevisionUnit("pep.g3.vol1.rev"))
        assertFalse(isRevisionUnit("pep.g3.vol1.u1"))
        assertTrue(units.any { isRevisionUnit(it.id) })
        assertTrue(isRevisionUnit("pep.g4.vol2.rev"))
        assertFalse(isRevisionUnit("pep.g4.vol1.u1"))
        assertTrue(isRevisionUnit("pep.g5.vol1.rev"))
        assertTrue(isRevisionUnit("pep.g6.vol2.rev"))
    }

    @Test
    fun grade4IndexAndVolumesParse() {
        assertEquals(4, index4.grade)
        assertEquals(listOf(7, 7), index4.volumes.map { it.unitIds.size })
        assertEquals(14, units4.size)
        assertEquals(index4.orderedUnitIds, units4.map { it.id })
        assertEquals("Helping at home", units4.first().titleEn)
        assertEquals("pep.g4.vol1.rev", vol1g4.last().id)
        assertEquals("Class rules", vol2g4.first().titleEn)
        assertEquals("A great weekend plan", vol2g4.last().titleEn)
        assertEquals("pep.g4.vol2.rev", vol2g4.last().id)
    }

    @Test
    fun grade4UnitsHaveWordsPhrasesAndResolve() {
        units4.forEach { unit ->
            assertTrue("${unit.id} words=${unit.words.size}", unit.words.size in 8..18)
            assertTrue("${unit.id} phrases=${unit.phrases.size}", unit.phrases.size in 3..6)
            assertTrue("${unit.id} goals", unit.goalsZh.size in 1..2)
            assertTrue("${unit.id} activities", unit.activities.size in 1..2)
            assertEquals(4, unit.grade)
            val resolved = resolveUnitWords(unit, dictionary)
            assertEquals("${unit.id} missing=${resolved.missingKeys}", emptyList<String>(), resolved.missingKeys)
        }
    }

    @Test
    fun gradeSwitchUnlockUsesOwnOrderedIds() {
        // 三年级完成不影响四年级解锁顺序；四年级 U1 始终可开
        val g3 = index.orderedUnitIds
        val g4 = index4.orderedUnitIds
        assertTrue(isUnitUnlocked(g4, emptySet(), "pep.g4.vol1.u1"))
        assertFalse(isUnitUnlocked(g4, emptySet(), "pep.g4.vol1.u2"))
        // 三年级全通也不解锁四年级 U2（要用四年级自己的 completed）
        assertFalse(isUnitUnlocked(g4, g3.toSet(), "pep.g4.vol1.u2"))
        assertTrue(isUnitUnlocked(g4, setOf("pep.g4.vol1.u1"), "pep.g4.vol1.u2"))
        // 反过来：四年级进度不串进三年级 ordered 判断
        assertFalse(isUnitUnlocked(g3, setOf("pep.g4.vol1.u1"), "pep.g3.vol1.u2"))
        assertTrue(isUnitUnlocked(g3, setOf("pep.g3.vol1.u1"), "pep.g3.vol1.u2"))
        // 五/六年级同样按本年级 orderedUnitIds 隔离
        val g5 = index5.orderedUnitIds
        val g6 = index6.orderedUnitIds
        assertTrue(isUnitUnlocked(g5, emptySet(), "pep.g5.vol1.u1"))
        assertFalse(isUnitUnlocked(g5, g4.toSet(), "pep.g5.vol1.u2"))
        assertTrue(isUnitUnlocked(g6, emptySet(), "pep.g6.vol1.u1"))
        assertFalse(isUnitUnlocked(g6, g5.toSet(), "pep.g6.vol1.u2"))
        assertTrue(isUnitUnlocked(g6, setOf("pep.g6.vol1.u1"), "pep.g6.vol1.u2"))
    }

    @Test
    fun grade5IndexAndVolumesParse() {
        assertEquals(5, index5.grade)
        assertEquals(listOf(7, 7), index5.volumes.map { it.unitIds.size })
        assertEquals(14, units5.size)
        assertEquals(index5.orderedUnitIds, units5.map { it.id })
        assertEquals("Different friends", units5.first().titleEn)
        assertEquals("New Year's party", vol1g5.last().titleEn)
        assertEquals("pep.g5.vol1.rev", vol1g5.last().id)
        assertEquals("Following the rules", vol2g5.first().titleEn)
        assertEquals("Our travel show", vol2g5.last().titleEn)
        assertEquals("pep.g5.vol2.rev", vol2g5.last().id)
    }

    @Test
    fun grade5UnitsHaveWordsPhrasesAndResolve() {
        units5.forEach { unit ->
            assertTrue("${unit.id} words=${unit.words.size}", unit.words.size in 8..18)
            assertTrue("${unit.id} phrases=${unit.phrases.size}", unit.phrases.size in 3..6)
            assertTrue("${unit.id} goals", unit.goalsZh.size in 1..2)
            assertTrue("${unit.id} activities", unit.activities.size in 1..2)
            assertEquals(5, unit.grade)
            val resolved = resolveUnitWords(unit, dictionary)
            assertEquals("${unit.id} missing=${resolved.missingKeys}", emptyList<String>(), resolved.missingKeys)
        }
    }

    @Test
    fun grade6IndexAndVolumesParse() {
        assertEquals(6, index6.grade)
        // 六上 7 单元；六下仅 4 Unit + Recycle = 5
        assertEquals(listOf(7, 5), index6.volumes.map { it.unitIds.size })
        assertEquals(12, units6.size)
        assertEquals(index6.orderedUnitIds, units6.map { it.id })
        assertEquals("Amazing places", units6.first().titleEn)
        assertEquals("Learning in museums", vol1g6.last().titleEn)
        assertEquals("pep.g6.vol1.rev", vol1g6.last().id)
        assertEquals("How tall are you?", vol2g6.first().titleEn)
        assertEquals("Mike's happy days", vol2g6.last().titleEn)
        assertEquals("pep.g6.vol2.rev", vol2g6.last().id)
        assertEquals(5, vol2g6.size)
        assertTrue(isRevisionUnit("pep.g6.vol2.rev"))
    }

    @Test
    fun grade6UnitsHaveWordsPhrasesAndResolve() {
        units6.forEach { unit ->
            assertTrue("${unit.id} words=${unit.words.size}", unit.words.size in 8..18)
            assertTrue("${unit.id} phrases=${unit.phrases.size}", unit.phrases.size in 3..6)
            assertTrue("${unit.id} goals", unit.goalsZh.size in 1..2)
            assertTrue("${unit.id} activities", unit.activities.size in 1..2)
            assertEquals(6, unit.grade)
            val resolved = resolveUnitWords(unit, dictionary)
            assertEquals("${unit.id} missing=${resolved.missingKeys}", emptyList<String>(), resolved.missingKeys)
        }
    }

    @Test
    fun grade6Vol2UnlockWithFiveUnits() {
        val ordered = index6.orderedUnitIds
        assertEquals(12, ordered.size)
        assertTrue(isUnitUnlocked(ordered, emptySet(), "pep.g6.vol2.u1").not())
        // 上册 Revision 通关后下册 U1 开
        assertTrue(isUnitUnlocked(ordered, setOf("pep.g6.vol1.rev"), "pep.g6.vol2.u1"))
        assertFalse(isUnitUnlocked(ordered, setOf("pep.g6.vol1.rev"), "pep.g6.vol2.u2"))
        assertTrue(isUnitUnlocked(ordered, setOf("pep.g6.vol2.u1"), "pep.g6.vol2.u2"))
        // Recycle 依赖 U4
        assertFalse(isUnitUnlocked(ordered, setOf("pep.g6.vol2.u3"), "pep.g6.vol2.rev"))
        assertTrue(isUnitUnlocked(ordered, setOf("pep.g6.vol2.u4"), "pep.g6.vol2.rev"))
    }

    @Test
    fun audioSlugAndLocalPathsPreferPhrases() {
        assertEquals("hello", audioSlug("Hello"))
        assertEquals("hello_what_s_your_name", audioSlug("Hello! What's your name?"))
        assertEquals("audio/us/tomato.mp3", localAudioPath("tomato", Accent.US))
        assertEquals(
            "audio/phrases/us/hello_what_s_your_name.mp3",
            localAudioPath("Hello! What's your name?", Accent.US)
        )
        assertEquals(
            "audio/examples/uk/hello_what_s_your_name.mp3",
            localExampleAudioPath("Hello! What's your name?", Accent.UK)
        )
        assertNull(localExampleAudioPath("tomato", Accent.US))
    }

    @Test
    fun pepExtraWordsHaveSynonymsIpaAndText() {
        assertTrue("extra size=${extraWords.size}", extraWords.size >= 44)
        extraWords.forEach { w ->
            assertTrue("${w.word} zh", w.zh.isNotBlank())
            assertTrue("${w.word} en", w.englishDefinition.isNotBlank())
            assertTrue("${w.word} example", w.example.isNotBlank())
            assertTrue("${w.word} exampleZh", w.exampleZh.isNotBlank())
            assertTrue("${w.word} synonyms", w.synonyms.isNotEmpty())
            assertTrue("${w.word} ipaUk", w.ipaUk.isNotBlank())
            assertTrue("${w.word} ipaUs", w.ipaUs.isNotBlank())
        }
    }

    @Test
    fun unitsThickenByGradeTrajectory() {
        fun check(units: List<CurriculumUnit>, minW: Int, maxW: Int, minP: Int, maxP: Int) {
            units.forEach { u ->
                assertTrue("${u.id} words=${u.words.size}", u.words.size in minW..maxW)
                assertTrue("${u.id} phrases=${u.phrases.size}", u.phrases.size in minP..maxP)
            }
        }
        check(units, 14, 14, 5, 5)
        check(units4, 15, 15, 5, 5)
        check(units5, 16, 16, 5, 5)
        check(units6, 16, 16, 6, 6)
    }

    @Test
    fun mergePracticeDictionaryKeepsIpaOnOverlap() {
        val stamp = dictionary.getValue("stamp")
        assertEquals("邮票", stamp.zh) // extra 释义
        assertTrue(stamp.ipaUk.isNotBlank())
        assertTrue(stamp.synonyms.isNotEmpty())
        // 纯专有词也进词典
        assertTrue(dictionary.containsKey("spaceship"))
        assertTrue(dictionary.getValue("spaceship").ipaUs.isNotBlank())
    }

    @Test
    fun practiceBackTargetReturnsToUnitOrLevels() {
        val unitPractice = Screen.Practice(
            category = Category.Operations,
            level = 0,
            wordKeys = listOf("hello"),
            title = "词汇关",
            unitId = "pep.g5.vol2.u1",
            unitLevel = 1
        )
        assertEquals(Screen.CurriculumUnit("pep.g5.vol2.u1"), unitPractice.backTarget())

        val categoryLevel = Screen.Practice(Category.Operations, level = 2)
        assertEquals(Screen.Levels(Category.Operations), categoryLevel.backTarget())

        val categoryExam = Screen.Practice(Category.Operations, level = 0, examCount = 20)
        assertEquals(Screen.Levels(Category.Operations), categoryExam.backTarget())

        val review = Screen.Practice(
            category = Category.Operations,
            level = 0,
            wordKeys = listOf("a", "b"),
            title = "智能复习"
        )
        assertEquals(Screen.Main, review.backTarget())

        assertEquals(Screen.Main, Screen.CurriculumUnit("pep.g3.vol1.u1").backTarget())
        assertEquals(Screen.Main, Screen.Levels(Category.GeneralThings).backTarget())
    }
}
