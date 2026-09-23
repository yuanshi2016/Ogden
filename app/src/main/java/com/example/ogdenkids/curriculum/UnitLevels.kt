package com.example.ogdenkids.curriculum

/** 课本单元内关卡：1 词汇 → 2 句型 → 3 综合；仅第 3 关通过才 markUnitComplete */
const val UNIT_LEVEL_WORDS = 1
const val UNIT_LEVEL_PHRASES = 2
const val UNIT_LEVEL_MIXED = 3
const val UNIT_LEVEL_MAX = UNIT_LEVEL_MIXED

/**
 * Revision 综合测评（固定题量 exam）：不走 1→2→3 解锁链，不触发 markUnitComplete。
 * 完成记在 unit_level_progress(level=10)。
 */
const val UNIT_LEVEL_REVISION_EXAM = 10

/** Revision 测评默认题量（不足词数时由 practiceWords 夹紧） */
const val REVISION_EXAM_COUNT = 20

/** 听读/活动勾选条目 id（与 prefs daycheck 共用） */
const val DAYCHECK_LISTEN_WORDS = "listen_words"
const val DAYCHECK_SPEAK_PHRASES = "speak_phrases"

fun isRevisionUnit(unitId: String): Boolean = unitId.endsWith(".rev")

fun dayCheckActivityId(index: Int): String = "activity.$index"

fun dayCheckSpeakPhraseId(index: Int): String = "speak_phrase.$index"

/**
 * 单元内关卡是否解锁（假定单元本身已对用户开放）。
 * level1 总开；其后依赖上一关在 [completedLevels] 中。
 */
fun isUnitLevelUnlocked(level: Int, completedLevels: Set<Int>): Boolean = when {
    level <= UNIT_LEVEL_WORDS -> true
    level in (UNIT_LEVEL_WORDS + 1)..UNIT_LEVEL_MAX -> (level - 1) in completedLevels
    else -> false
}

fun isUnitLevelComplete(level: Int, completedLevels: Set<Int>): Boolean =
    level in completedLevels
