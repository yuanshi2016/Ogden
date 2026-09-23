package com.example.ogdenkids

import java.util.Locale
import kotlin.random.Random

fun examCountDefault(total: Int) = minOf(50, total)

fun clampExamCount(requested: Int, total: Int) = requested.coerceIn(1, total.coerceAtLeast(1))

fun practiceWords(
    allWords: List<OgdenWord>,
    category: Category,
    level: Int,
    seed: Int,
    examCount: Int = 0,
    wordKeys: List<String> = emptyList()
): List<OgdenWord> {
    if (wordKeys.isNotEmpty()) {
        val byWord = allWords.associateBy { it.word }
        val ordered = wordKeys.mapNotNull { byWord[it] }
        // 自定义队列按种子洗牌；examCount>0 时按考试题量截断（Revision 综合测评），否则最多 50
        val cap = if (examCount > 0) {
            clampExamCount(examCount, ordered.size.coerceAtLeast(1))
        } else {
            minOf(50, ordered.size.coerceAtLeast(1))
        }
        return ordered.shuffled(Random(seed)).take(cap)
    }
    val pool = allWords.filter { it.category == category }
    return if (level <= 0) {
        pool.shuffled(Random(seed)).take(
            clampExamCount(if (examCount > 0) examCount else examCountDefault(pool.size), pool.size)
        )
    } else {
        // 关卡词池固定（每 10 词一关），但出题顺序按种子打乱，避免每次同一顺序
        pool.drop((level - 1) * 10).take(10).shuffled(Random(seed))
    }
}

data class Question(val prompt: String, val answer: String, val options: List<String>)

fun buildQuestion(type: PracticeType, word: OgdenWord, allWords: List<OgdenWord>, seed: Int = 0): Question {
    val distractors = allWords
        .filter { it.word != word.word }
        .shuffled(Random(word.word.hashCode() + type.ordinal + seed))
        .take(6)
    return when (type) {
        PracticeType.Listen -> Question(
            prompt = "听声音，选出正确单词",
            answer = word.word,
            options = (distractors.take(3).map { it.word } + word.word).shuffled()
        )
        PracticeType.Meaning -> Question(
            prompt = word.zh,
            answer = word.word,
            options = (distractors.take(3).map { it.word } + word.word).shuffled()
        )
        PracticeType.Example -> Question(
            prompt = word.example.replace(Regex("\\b${Regex.escape(word.word)}\\b", RegexOption.IGNORE_CASE), "____"),
            answer = word.word,
            options = (distractors.take(3).map { it.word } + word.word).shuffled()
        )
        PracticeType.Spelling -> Question(
            prompt = "${word.zh}\n${word.englishDefinition}",
            answer = word.word,
            options = (distractors.take(3).map { it.word } + word.word).shuffled()
        )
        PracticeType.Synonym -> {
            val answer = word.synonyms.firstOrNull() ?: word.word
            // 先用其他词的近义词补干扰项，不足时退回单词本身，保证 4 个互异且不与答案重复
            val options = linkedSetOf(answer)
            (distractors.flatMap { it.synonyms } + distractors.map { it.word }).forEach { candidate ->
                if (options.size < 4 && !candidate.equals(answer, ignoreCase = true)) options.add(candidate)
            }
            Question(
                prompt = "哪个词接近 ${word.word} 的意思？",
                answer = answer,
                options = options.toList().shuffled()
            )
        }
        PracticeType.Speak -> Question(
            prompt = "请大声读出这个单词",
            answer = word.word,
            options = emptyList()
        )
    }
}

// 把识别结果归一化：小写、去掉标点、合并空白，方便与目标单词比对
fun normalizeSpoken(text: String): String =
    text.lowercase(Locale.US)
        .replace(Regex("[^a-z0-9'\\-\\s]"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

fun levenshtein(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    var prev = IntArray(b.length + 1) { it }
    var cur = IntArray(b.length + 1)
    for (i in 1..a.length) {
        cur[0] = i
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
        }
        val tmp = prev; prev = cur; cur = tmp
    }
    return prev[b.length]
}

// 跟读判定结果：passed 是否过关，similarity 0..1 供界面反馈
data class SpeakResult(val passed: Boolean, val similarity: Float)

// 单 Metaphone 音近编码：把 c/k、ph/f 等归一到同一发音键，用于"kat"≈"cat"这类音近兜底。
// 只做辅音骨架，不区分长短音；元音差异（cat/cut）它分不出，交给 whisper 转写+置信度把关。
fun metaphone(word: String): String {
    val w = word.uppercase(Locale.US).filter { it in 'A'..'Z' }
    if (w.isEmpty()) return ""
    fun isVowel(c: Char?) = c != null && c in "AEIOU"

    val sb = StringBuilder()
    var i = 0
    // 首字母特殊规则：KN/GN/PN/WR→N，WH→W，X→S
    if (w.length >= 2) {
        val two = w.substring(0, 2)
        when (two) {
            "KN", "GN", "PN", "WR" -> { sb.append('N'); i = 2 }
            "WH" -> { sb.append('W'); i = 2 }
            else -> if (w[0] == 'X') { sb.append('S'); i = 1 }
        }
    }
    while (i < w.length) {
        val c = w[i]
        val p = w.getOrNull(i - 1)
        val n = w.getOrNull(i + 1)
        when (c) {
            'A', 'E', 'I', 'O', 'U' -> if (i == 0) sb.append(c)
            'B' -> if (p != 'M') sb.append('B')
            'C' -> when {
                n == 'H' -> { sb.append('X'); i++ }
                n == 'I' || n == 'E' || n == 'Y' -> sb.append('S')
                else -> sb.append('K')
            }
            'D' -> if (n == 'G' && (w.getOrNull(i + 2) == 'E' || w.getOrNull(i + 2) == 'I' || w.getOrNull(i + 2) == 'Y')) { sb.append('J'); i++ } else sb.append('T')
            'F' -> sb.append('F')
            'G' -> {
                val nn = w.getOrNull(i + 2)
                when {
                    n == 'H' && (nn == null || !isVowel(nn)) -> i++
                    n == 'N' && (nn == null || isVowel(nn)) -> i++
                    n == 'I' || n == 'E' || n == 'Y' -> sb.append('J')
                    else -> sb.append('K')
                }
            }
            'H' -> if (!(isVowel(p) && !isVowel(n))) sb.append('H')
            'J' -> sb.append('J')
            'K' -> if (p != 'C') sb.append('K')
            'L' -> sb.append('L')
            'M' -> sb.append('M')
            'N' -> sb.append('N')
            'P' -> if (n == 'H') { sb.append('F'); i++ } else sb.append('P')
            'Q' -> sb.append('K')
            'R' -> sb.append('R')
            'S' -> if (n == 'H') { sb.append('X'); i++ } else sb.append('S')
            'T' -> if (n == 'H') { sb.append('0'); i++ } else sb.append('T')
            'V' -> sb.append('F')
            'W' -> if (isVowel(n)) sb.append('W')
            'X' -> sb.append("KS")
            'Y' -> if (isVowel(n)) sb.append('Y')
            'Z' -> sb.append('S')
        }
        i++
    }
    return sb.toString()
}

// 目标词与识别文本的相似度 0..1：精确 > 子串 > 音近 > 编辑距离
fun spokenSimilarity(target: String, spoken: String): Float {
    val a = normalizeSpoken(target)
    val b = normalizeSpoken(spoken)
    if (a.isBlank() || b.isBlank()) return 0f
    if (a == b) return 1f
    if (a.contains(b) || b.contains(a)) return 0.95f
    if (metaphone(a) == metaphone(b)) return 0.85f
    val dist = levenshtein(a, b)
    return (1f - dist.toFloat() / maxOf(a.length, b.length)).coerceIn(0f, 0.84f)
}

// assessPronunciation 见 SpeakCalibration.kt（支持可调阈值）
