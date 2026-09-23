package com.example.ogdenkids.curriculum

/**
 * 家长报告汇总：本单元三关、今日听读勾选、句型关跟读（持久）与今日跟读。
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
    /** 句型关已跟读通过（跨日保留） */
    val phraseGatePassedCount: Int,
    /** 今日已跟读通过的句型数（daycheck） */
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

    val speakLine: String = buildString {
        append("句型关 $phraseGatePassedCount/$phraseTotal")
        append(" · 今日跟读 $speakPassedCount/$phraseTotal 句")
    }
}

fun buildParentReport(
    unit: CurriculumUnit,
    completedLevels: Set<Int>,
    dayCheckItems: Set<String>,
    phrasePassItems: Set<String> = emptySet()
): ParentReportSummary {
    val wordsDone = UNIT_LEVEL_WORDS in completedLevels
    val phrasesDone = UNIT_LEVEL_PHRASES in completedLevels
    val mixedDone = UNIT_LEVEL_MIXED in completedLevels
    val activityTotal = unit.activities.size
    val activityDone = unit.activities.indices.count { dayCheckActivityId(it) in dayCheckItems }
    val speakToday = unit.phrases.indices.count { dayCheckSpeakPhraseId(it) in dayCheckItems }
    val gatePassed = unit.phrases.indices.count { phrasePassId(it) in phrasePassItems }
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
        phraseGatePassedCount = gatePassed,
        speakPassedCount = speakToday,
        phraseTotal = unit.phrases.size
    )
}
