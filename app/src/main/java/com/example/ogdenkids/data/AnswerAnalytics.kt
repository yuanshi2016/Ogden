package com.example.ogdenkids.data

/**
 * 从答题明细聚合报表数据（纯函数，便于单测）。
 */

/** 质量分 0..5 的次数分布（下标即质量分）。 */
fun qualityHistogram(events: List<AnswerEventEntity>): IntArray {
    val counts = IntArray(6)
    events.forEach { e ->
        val q = e.quality.coerceIn(0, 5)
        counts[q]++
    }
    return counts
}

data class QualitySummary(
    val total: Int,
    /** 质量 ≥ 3 的比例 0..1 */
    val passRate: Float,
    /** 平均质量 0..5，无数据为 0 */
    val averageQuality: Float,
    val histogram: IntArray
)

fun summarizeQuality(events: List<AnswerEventEntity>): QualitySummary {
    val hist = qualityHistogram(events)
    val total = hist.sum()
    if (total == 0) {
        return QualitySummary(0, 0f, 0f, hist)
    }
    val pass = hist.slice(3..5).sum()
    val sumQ = hist.mapIndexed { q, n -> q * n }.sum()
    return QualitySummary(
        total = total,
        passRate = pass.toFloat() / total,
        averageQuality = sumQ.toFloat() / total,
        histogram = hist
    )
}
