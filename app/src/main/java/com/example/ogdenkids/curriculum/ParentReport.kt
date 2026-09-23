package com.example.ogdenkids.curriculum

/**
 * 家长报告汇总：本单元三关、今日听读勾选、跟读通过次数。
 * 纯数据，UI 只负责展示。
 */
data class ParentReportSummary(
    val unitTitleZh: String,
    val unitTitleEn: String,
    val wordsLevelDone: Boolean,
    val phrasesLevelDone: Boolean,
    val mixedLevelDone: Boolean,
    /** 0..3 已完成关卡数 */
    val levelsDoneCount: Int,
    val listenWordsDone: Boolean,
    val speakPhrasesChecklistDone: Boolean,
    val activityDoneCount: Int,
    val activityTotal: Int,
    /** 今日已跟读通过的句型数 */
    val speakPassedCount: Int,
    val phraseTotal: Int
) {
    val levelsLine: String = buildString {
        append(if (wordsLevelDone) "①词汇✓" else "①词汇○")
        append("  ")
        append(if (phrasesLevelDone) "②句型✓" else "②句型○")
        append("  ")
        append(if (mixedLevelDone) "③综合✓" else "③综合○")
        append("（$levelsDoneCount/3）")
    }

    val dayCheckLine: String = buildString {
        append(if (listenWordsDone) "听词✓" else "听词○")
        append(" · ")
        append(if (speakPhrasesChecklistDone) "跟读清单✓" else "跟读清单○")
        append(" · 活动 $activityDoneCount/$activityTotal")
    }

    val speakLine: String = "今日跟读通过 $speakPassedCount/$phraseTotal 句"
}

fun buildParentReport(
    unit: CurriculumUnit,
    completedLevels: Set<Int>,
    dayCheckItems: Set<String>
): ParentReportSummary {
    val wordsDone = UNIT_LEVEL_WORDS in completedLevels
    val phrasesDone = UNIT_LEVEL_PHRASES in completedLevels
    val mixedDone = UNIT_LEVEL_MIXED in completedLevels
    val activityTotal = unit.activities.size
    val activityDone = unit.activities.indices.count { dayCheckActivityId(it) in dayCheckItems }
    val speakPassed = unit.phrases.indices.count { dayCheckSpeakPhraseId(it) in dayCheckItems }
    return ParentReportSummary(
        unitTitleZh = unit.titleZh.ifBlank { unit.titleEn },
        unitTitleEn = unit.titleEn,
        wordsLevelDone = wordsDone,
        phrasesLevelDone = phrasesDone,
        mixedLevelDone = mixedDone,
        levelsDoneCount = listOf(wordsDone, phrasesDone, mixedDone).count { it },
        listenWordsDone = DAYCHECK_LISTEN_WORDS in dayCheckItems,
        speakPhrasesChecklistDone = DAYCHECK_SPEAK_PHRASES in dayCheckItems,
        activityDoneCount = activityDone,
        activityTotal = activityTotal,
        speakPassedCount = speakPassed,
        phraseTotal = unit.phrases.size
    )
}
