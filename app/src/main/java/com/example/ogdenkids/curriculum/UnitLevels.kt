package com.example.ogdenkids.curriculum

import com.example.ogdenkids.Difficulty

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

/** 听读/活动勾选条目 id（与 prefs daycheck 共用；按日历日重置） */
const val DAYCHECK_LISTEN_WORDS = "listen_words"
const val DAYCHECK_SPEAK_PHRASES = "speak_phrases"

fun isRevisionUnit(unitId: String): Boolean = unitId.endsWith(".rev")

fun dayCheckActivityId(index: Int): String = "activity.$index"

fun dayCheckSpeakPhraseId(index: Int): String = "speak_phrase.$index"

/** 句型关跟读进度 id（持久，不跟日期走） */
fun phrasePassId(index: Int): String = "phrase.$index"

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

/**
 * 课本练习默认难度：低年级偏认词/跟读，高年级与测评偏难。
 * 返回 null 时仍弹出完整难度选择器（非课本或未知关）。
 */
fun defaultDifficultyForCurriculum(grade: Int, unitLevel: Int): Difficulty? = when (unitLevel) {
    UNIT_LEVEL_WORDS -> if (grade <= 4) Difficulty.Easy else Difficulty.Medium
    UNIT_LEVEL_MIXED -> if (grade <= 4) Difficulty.Medium else Difficulty.Hard
    UNIT_LEVEL_REVISION_EXAM -> Difficulty.Hard
    else -> null
}

/**
 * Revision/Recycle 综合测评词池：本册普通单元词去重合并，再并入本单元词。
 * 非 Revision 单元原样返回 [revUnit].words。
 */
fun revisionExamWordKeys(bundle: CurriculumBundle, revUnit: CurriculumUnit): List<String> {
    if (!isRevisionUnit(revUnit.id)) return revUnit.words
    val seen = LinkedHashSet<String>()
    bundle.units.asSequence()
        .filter { it.grade == revUnit.grade && it.volume == revUnit.volume && !isRevisionUnit(it.id) }
        .forEach { u -> seen.addAll(u.words) }
    seen.addAll(revUnit.words)
    return seen.toList()
}

/** 从单元 id（如 pep.g5.vol1.u2）解析年级；失败返回 null */
fun gradeFromUnitId(unitId: String): Int? {
    val m = Regex("""\.g(\d+)\.""").find(unitId) ?: return null
    return m.groupValues[1].toIntOrNull()?.takeIf { it in PEP_SUPPORTED_GRADES }
}
