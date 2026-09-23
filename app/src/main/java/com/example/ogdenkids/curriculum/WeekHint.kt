package com.example.ogdenkids.curriculum

/**
 * 课本周次建议：手动「第 N 周」优先；否则按开学日（本地纪元日）推算。
 * 不强制锁进度，仅用于列表高亮 / 单元页提示。
 */
const val PEP_WEEK_MIN = 1
const val PEP_WEEK_MAX = 20

/** 手动周优先；否则用开学日推算；都没有则 null（不显示建议）。 */
fun resolveSuggestedWeek(
    manualWeek: Int?,
    termStartDay: Long?,
    todayEpochDay: Long
): Int? {
    manualWeek?.takeIf { it in PEP_WEEK_MIN..PEP_WEEK_MAX }?.let { return it }
    val start = termStartDay ?: return null
    if (todayEpochDay < start) return PEP_WEEK_MIN
    val week = ((todayEpochDay - start) / 7L).toInt() + 1
    return week.coerceIn(PEP_WEEK_MIN, PEP_WEEK_MAX)
}

/** 单元 weeksHint 是否覆盖当前建议周。 */
fun unitMatchesWeek(weeksHint: List<Int>, week: Int?): Boolean {
    if (week == null || weeksHint.isEmpty()) return false
    return week in weeksHint
}

/** 本册单元中匹配建议周的 id（按列表顺序）。 */
fun suggestedUnitIds(units: List<CurriculumUnit>, week: Int?): List<String> {
    if (week == null) return emptyList()
    return units.filter { unitMatchesWeek(it.weeksHint, week) }.map { it.id }
}

fun weekHintLabel(weeksHint: List<Int>): String {
    if (weeksHint.isEmpty()) return ""
    val sorted = weeksHint.sorted()
    return if (sorted.size == 1) "建议第 ${sorted.first()} 周"
    else "建议第 ${sorted.first()}–${sorted.last()} 周"
}
