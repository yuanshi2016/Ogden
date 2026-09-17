package com.example.ogdenkids

import org.json.JSONArray
import com.example.ogdenkids.data.legacyLevelProgress
import com.example.ogdenkids.data.legacyWordProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
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
            "accent", "roomImported", "favorites", "mistakes", "mastery.cat", "attempts.cat", "correct.cat",
            "last.cat", "level.op.1.complete", "streak", "lastStudyDay", "lastCategory", "lastLevel"
        )
        val dropped = resettableProgressKeys(keys)
        assertFalse(dropped.contains("accent"))
        assertFalse(dropped.contains("roomImported"))
        assertEquals(keys - "accent" - "roomImported", dropped)
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
}
