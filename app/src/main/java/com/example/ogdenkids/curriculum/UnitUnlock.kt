package com.example.ogdenkids.curriculum

/**
 * 解锁规则：顺序表里的第一个单元默认开放，其余要求「前一个单元已完成」。
 * 不在顺序表里的 id 视为未解锁。
 */
fun isUnitUnlocked(orderedIds: List<String>, completed: Set<String>, unitId: String): Boolean {
    val index = orderedIds.indexOf(unitId)
    return when {
        index < 0 -> false
        index == 0 -> true
        else -> orderedIds[index - 1] in completed
    }
}
