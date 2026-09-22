package com.example.ogdenkids.data

/**
 * 经典 SM-2（SuperMemo 2）状态。
 * - [repetitions]：连续「质量 ≥ 3」的次数 n
 * - [intervalDays]：当前间隔（天）
 * - [easeFactor]：易度 EF，默认 2.5，下限 1.3
 * - [dueAt]：下次应复习的 epoch millis
 *
 * 质量分 q ∈ 0..5（SuperMemo 约定）：
 * 0 完全忘记 · 1 错误但有印象 · 2 错误较容易想起 ·
 * 3 正确很费力 · 4 正确有犹豫 · 5 完美回忆
 */
data class Sm2State(
    val repetitions: Int = 0,
    val intervalDays: Double = 0.0,
    val easeFactor: Double = DEFAULT_EASE,
    val dueAt: Long = 0L
) {
    companion object {
        const val DEFAULT_EASE = 2.5
        const val MIN_EASE = 1.3
        const val DAY_MS = 86_400_000.0
        /** 答错后的短间隔：约 10 分钟，尽快再练 */
        const val FAIL_INTERVAL_DAYS = 10.0 / (24.0 * 60.0)
    }
}

/**
 * 将「对/错」映射为默认质量分（无细粒度反馈时使用）。
 * 对 → 4（正确有犹豫）；错 → 1（错误但有印象，仍重置间隔）。
 */
fun qualityFromCorrect(correct: Boolean): Int = if (correct) 4 else 1

/**
 * 跟读：用相似度 + 是否过关估质量 0..5。
 * 过关且高相似 → 5；过关 → 4；未过但接近 → 2；明显错 → 0/1。
 */
fun qualityFromSpeak(passed: Boolean, similarity: Float): Int = when {
    passed && similarity >= 0.98f -> 5
    passed && similarity >= 0.90f -> 4
    passed -> 3
    similarity >= 0.75f -> 2
    similarity >= 0.50f -> 1
    else -> 0
}

/**
 * 经典 SM-2 步进。
 * - q < 3：n=0，间隔回到短失败间隔，EF 仍按公式更新
 * - q ≥ 3：n 递增；n=1→1天，n=2→6天，之后 interval×EF
 * - EF' = EF + (0.1 - (5-q)×(0.08+(5-q)×0.02))，下限 1.3
 */
fun nextSm2(current: Sm2State, quality: Int, nowMillis: Long): Sm2State {
    val q = quality.coerceIn(0, 5)
    val ease = current.easeFactor.coerceAtLeast(Sm2State.MIN_EASE)
    val nextEase = (
        ease + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        ).coerceAtLeast(Sm2State.MIN_EASE)

    return if (q < 3) {
        val interval = Sm2State.FAIL_INTERVAL_DAYS
        Sm2State(
            repetitions = 0,
            intervalDays = interval,
            easeFactor = nextEase,
            dueAt = nowMillis + (interval * Sm2State.DAY_MS).toLong()
        )
    } else {
        val n = current.repetitions + 1
        val nextInterval = when {
            n == 1 -> 1.0
            n == 2 -> 6.0
            else -> (current.intervalDays.coerceAtLeast(1.0) * nextEase).coerceAtLeast(6.0)
        }
        Sm2State(
            repetitions = n,
            intervalDays = nextInterval,
            easeFactor = nextEase,
            dueAt = nowMillis + (nextInterval * Sm2State.DAY_MS).toLong()
        )
    }
}

/** 兼容旧调用：仅对/错。 */
fun nextSm2(current: Sm2State, correct: Boolean, nowMillis: Long): Sm2State =
    nextSm2(current, qualityFromCorrect(correct), nowMillis)
