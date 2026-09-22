package com.example.ogdenkids

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class ThemeMode(val label: String) { Child("儿童模式"), Adult("成人模式") }

enum class Category(
    val code: String,
    val label: String,
    val zh: String,
    val count: Int,
    val baseTint: Color,
    val baseSoft: Color
) {
    Operations("op", "Operations", "操作词", 100, Color(0xFFB45309), Color(0xFFFEF3C7)),
    GeneralThings("gt", "General Things", "通用词", 400, Color(0xFF166534), Color(0xFFDCFCE7)),
    Picturable("pt", "Picturable", "图示词", 200, Color(0xFFA16207), Color(0xFFFEF9C3)),
    Qualities("qg", "Qualities", "性质词", 100, Color(0xFF1E40AF), Color(0xFFDBEAFE)),
    Opposites("qo", "Opposites", "反义对", 50, Color(0xFF7C3AED), Color(0xFFEDE9FE)),
    Function("fn", "Function", "功能词", 61, Color(0xFF0E7490), Color(0xFFCFFAFE)),
    Verbs("vb", "Verbs", "动词", 74, Color(0xFF0F766E), Color(0xFFCCFBF1)),
    Nouns("na", "Nouns", "名词形容词", 73, Color(0xFFBE185D), Color(0xFFFCE7F3)),
    Topics("th", "Topics", "主题词", 145, Color(0xFF4338CA), Color(0xFFE0E7FF));

    companion object {
        fun from(code: String) = values().first { it.code == code }
    }
}

data class OgdenWord(
    val word: String,
    val category: Category,
    val zh: String,
    val englishDefinition: String,
    val example: String,
    val exampleZh: String,
    val synonyms: List<String>,
    val ipaUk: String,
    val ipaUs: String
)

data class WordProgress(
    val favorite: Boolean,
    val mistake: Boolean,
    val mastery: Int,
    val attempts: Int,
    val correct: Int,
    /** SM-2 连续质量≥3 的次数 n */
    val repetitions: Int = 0,
    /** SM-2 当前间隔（天）；0 表示尚未进入间隔重复 */
    val intervalDays: Double = 0.0,
    /** SM-2 易度因子 */
    val easeFactor: Double = 2.5,
    /** 下次应复习的 epoch millis；0 表示旧数据/已到期兼容 */
    val dueAt: Long = 0L
)

enum class Tab(val title: String, val icon: ImageVector) {
    Challenge("闯关", Icons.Default.Star),
    Library("词库", Icons.Default.Book),
    Review("复习", Icons.Default.Refresh),
    Ai("AI", Icons.Default.ChatBubble)
}

enum class Accent(val label: String, val locale: Locale) {
    UK("UK 英式", Locale.UK),
    US("US 美式", Locale.US)
}

// 跟读判定严格度：宽松只要求音近，严格要求精确匹配 + 高置信度
enum class SpeakLevel(val label: String) {
    Lenient("宽松"),
    Normal("标准"),
    Strict("严格")
}

enum class PracticeType(val title: String) {
    Listen("听音选词"),
    Meaning("看中文选英文"),
    Example("例句填空"),
    Spelling("拼写挑战"),
    Synonym("近义词配对"),
    Speak("跟读发音")
}

// 难度按认知负担递进：识别 → 语境 → 回忆产出；跟读单独成一档，只做发音练习
enum class Difficulty(val title: String, val tint: Color, val types: List<PracticeType>) {
    Easy("简单", Color(0xFF166534), listOf(PracticeType.Listen, PracticeType.Meaning)),
    Medium("中等", Color(0xFFB45309), listOf(PracticeType.Listen, PracticeType.Meaning, PracticeType.Example)),
    Hard("困难", Color(0xFFB91C1C), listOf(PracticeType.Listen, PracticeType.Meaning, PracticeType.Example, PracticeType.Spelling, PracticeType.Synonym)),
    Speak("跟读", Color(0xFF2563EB), listOf(PracticeType.Speak))
}

/**
 * 导航状态只存原始类型，便于 [androidx.compose.runtime.saveable.rememberSaveable]。
 * 调用处仍可传 [OgdenWord] / [Category]（次构造函数转成 key/code）；渲染时在导航层 resolve。
 */
sealed class Screen {
    object Main : Screen()

    // neighbors 是打开详情时的那份列表 key，用来做上一个/下一个
    data class Detail(val wordKey: String, val neighborKeys: List<String>) : Screen() {
        constructor(word: OgdenWord, neighbors: List<OgdenWord>) : this(
            word.word,
            neighbors.map { it.word }
        )
    }

    data class Levels(val categoryCode: String) : Screen() {
        constructor(category: Category) : this(category.code)
        val category: Category get() = Category.from(categoryCode)
    }

    // level > 0 闯关；level == 0 且 wordKeys 空 = 分类考试；wordKeys 非空 = 智能复习/错词/收藏
    data class Practice(
        val categoryCode: String,
        val level: Int,
        val examCount: Int = 0,
        val wordKeys: List<String> = emptyList(),
        val title: String = ""
    ) : Screen() {
        constructor(
            category: Category,
            level: Int,
            examCount: Int = 0,
            wordKeys: List<String> = emptyList(),
            title: String = ""
        ) : this(category.code, level, examCount, wordKeys, title)

        val category: Category get() = Category.from(categoryCode)
    }

    data class WordCollection(val title: String, val kind: String) : Screen()
    object Stats : Screen()
    object Settings : Screen()
    object Privacy : Screen()
    object About : Screen()

    companion object {
        /** Bundle 可存的 list：tag + 原始字段；进程恢复后若词 key 失效由导航层回 Main。 */
        val Saver: Saver<Screen, Any> = listSaver(
                save = { screen ->
                    when (screen) {
                        Main -> listOf("main")
                        is Detail -> listOf("detail", screen.wordKey) + screen.neighborKeys
                        is Levels -> listOf("levels", screen.categoryCode)
                        is Practice -> listOf(
                            "practice",
                            screen.categoryCode,
                            screen.level,
                            screen.examCount,
                            screen.title,
                            screen.wordKeys.size
                        ) + screen.wordKeys
                        is WordCollection -> listOf("collection", screen.title, screen.kind)
                        Stats -> listOf("stats")
                        Settings -> listOf("settings")
                        Privacy -> listOf("privacy")
                        About -> listOf("about")
                    }
                },
                restore = { list ->
                    if (list.isEmpty()) return@listSaver Main
                    when (list[0] as String) {
                        "main" -> Main
                        "detail" -> Detail(
                            wordKey = list.getOrNull(1) as? String ?: return@listSaver Main,
                            neighborKeys = list.drop(2).mapNotNull { it as? String }
                        )
                        "levels" -> {
                            val code = list.getOrNull(1) as? String ?: return@listSaver Main
                            if (runCatching { Category.from(code) }.isFailure) Main else Levels(code)
                        }
                        "practice" -> {
                            val code = list.getOrNull(1) as? String ?: return@listSaver Main
                            if (runCatching { Category.from(code) }.isFailure) return@listSaver Main
                            val level = (list.getOrNull(2) as? Number)?.toInt() ?: 0
                            val examCount = (list.getOrNull(3) as? Number)?.toInt() ?: 0
                            val title = list.getOrNull(4) as? String ?: ""
                            val keyCount = (list.getOrNull(5) as? Number)?.toInt() ?: 0
                            val keys = list.drop(6).take(keyCount).mapNotNull { it as? String }
                            Practice(code, level, examCount, keys, title)
                        }
                        "collection" -> WordCollection(
                            title = list.getOrNull(1) as? String ?: "",
                            kind = list.getOrNull(2) as? String ?: ""
                        )
                        "stats" -> Stats
                        "settings" -> Settings
                        "privacy" -> Privacy
                        "about" -> About
                        else -> Main
                    }
                }
            )
    }
}

fun loadWords(context: Context): List<OgdenWord> {
    val wordsJson = context.assets.open("ogden_words.json").bufferedReader().use { it.readText() }.trimStart('\uFEFF')
    val ipaJson = context.assets.open("ogden_ipa.json").bufferedReader().use { it.readText() }.trimStart('\uFEFF')
    val words = JSONArray(wordsJson)
    val ipa = JSONObject(ipaJson)
    return List(words.length()) { index ->
        val item = words.getJSONObject(index)
        val word = item.getString("w")
        val ipaItem = ipa.optJSONObject(word)
        val synonyms = item.getJSONArray("s")
        OgdenWord(
            word = word,
            category = Category.from(item.getString("c")),
            zh = item.getString("zh"),
            englishDefinition = item.getString("en"),
            example = item.getString("ex"),
            exampleZh = item.getString("exz"),
            synonyms = List(synonyms.length()) { synonyms.getString(it) },
            ipaUk = ipaItem?.optString("uk").orEmpty(),
            ipaUs = ipaItem?.optString("us").orEmpty()
        )
    }
}

// 存储分工：逐词进度与关卡完成放 Room（未来要按时间/正确率排序查询），
// streak / lastStudyDay / lastCategory / lastLevel / accent 仍留在 SharedPreferences
// —— 它们是单值标量，组合期间同步读写，为四个数字单开一张单行表只会多一层异步。

fun matchesLibraryFilter(word: OgdenWord, query: String, category: Category?): Boolean =
    (category == null || word.category == category) &&
        (query.isBlank() ||
            word.word.contains(query, ignoreCase = true) ||
            word.zh.contains(query) ||
            word.englishDefinition.contains(query, ignoreCase = true))
