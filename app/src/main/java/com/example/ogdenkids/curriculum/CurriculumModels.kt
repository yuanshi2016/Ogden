package com.example.ogdenkids.curriculum

import com.example.ogdenkids.OgdenWord

data class CurriculumPhrase(val en: String, val zh: String)

data class CurriculumActivity(val type: String, val titleZh: String, val bodyZh: String)

data class CurriculumUnit(
    val id: String,
    val grade: Int,
    val volume: Int,
    val unit: Int,
    val titleEn: String,
    val titleZh: String,
    val goalsZh: List<String>,
    val words: List<String>,
    val phrases: List<CurriculumPhrase>,
    val activities: List<CurriculumActivity>,
    val weeksHint: List<Int>
)

data class CurriculumVolume(val volume: Int, val titleZh: String, val unitIds: List<String>)

data class CurriculumIndex(val grade: Int, val volumes: List<CurriculumVolume>) {
    /** 解锁顺序的唯一真相：按册序拼出的全部单元 id */
    val orderedUnitIds: List<String> get() = volumes.flatMap { it.unitIds }
}

data class CurriculumBundle(
    val index: CurriculumIndex,
    val units: List<CurriculumUnit>,
    /** 课本专用补词，只喂练习，不进 Ogden 闯关列表 */
    val extraWords: List<OgdenWord>
) {
    val unitsById: Map<String, CurriculumUnit> get() = units.associateBy { it.id }
}

/** 缺词不崩：命中的进 words，找不到的记入 missingKeys */
data class ResolvedUnitWords(val words: List<OgdenWord>, val missingKeys: List<String>)
