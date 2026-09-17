package com.example.ogdenkids

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.ogdenkids.data.LevelProgressEntity
import com.example.ogdenkids.data.ProgressDatabase
import com.example.ogdenkids.data.WordProgressEntity
import com.example.ogdenkids.data.legacyLevelProgress
import com.example.ogdenkids.data.legacyWordProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.ceil
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // edge-to-edge：内容自行处理 inset，状态栏/导航栏由主题决定明暗
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { OgdenKidsApp() }
    }
}

private class Palette(
    val paper: Color,
    val paperElevated: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val line: Color,
    val success: Color,
    val error: Color,
    val dark: Boolean
)

private val LightPalette = Palette(
    paper = Color(0xFFFAF6ED),
    paperElevated = Color(0xFFFFFDF7),
    ink = Color(0xFF1C1917),
    inkSoft = Color(0xFF44403C),
    inkFaint = Color(0xFF78716C),
    line = Color(0xFFE7E2D4),
    success = Color(0xFF166534),
    error = Color(0xFFB91C1C),
    dark = false
)

// 深色仍是暖调「纸与墨」：底色偏棕黑，文字偏米白，不用中性灰
private val DarkPalette = Palette(
    paper = Color(0xFF17130F),
    paperElevated = Color(0xFF221C16),
    ink = Color(0xFFF3EADA),
    inkSoft = Color(0xFFD2C6B4),
    inkFaint = Color(0xFF9D9082),
    line = Color(0xFF3B332B),
    success = Color(0xFF6EE7A8),
    error = Color(0xFFFCA5A5),
    dark = true
)

private val LocalPalette = staticCompositionLocalOf { LightPalette }

// 旧的顶层颜色常量改成按主题取值的组合式属性，几十处调用点写法不变
private val Paper: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.paper
private val PaperElevated: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.paperElevated
private val Ink: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.ink
private val InkSoft: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.inkSoft
private val InkFaint: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.inkFaint
private val Line: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.line
private val Success: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.success
private val Error: Color
    @Composable @ReadOnlyComposable get() = LocalPalette.current.error

// 分类色是深饱和色，直接放在深色底上对比不足，深色主题里统一提亮
private fun Color.forDark(dark: Boolean) = if (dark) lerp(this, Color.White, 0.52f) else this

private val Category.tint: Color
    @Composable @ReadOnlyComposable get() = baseTint.forDark(LocalPalette.current.dark)

private val Category.soft: Color
    @Composable @ReadOnlyComposable get() =
        if (LocalPalette.current.dark) baseTint.forDark(true).copy(alpha = 0.20f) else baseSoft

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
    Opposites("qo", "Opposites", "反义对", 50, Color(0xFF7C3AED), Color(0xFFEDE9FE));

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
    val correct: Int
)

enum class Tab(val title: String, val icon: ImageVector) {
    Challenge("闯关", Icons.Default.Star),
    Library("词库", Icons.Default.Book),
    Review("复习", Icons.Default.Refresh)
}

enum class Accent(val label: String, val locale: Locale) {
    UK("UK 英式", Locale.UK),
    US("US 美式", Locale.US)
}

enum class PracticeType(val title: String) {
    Listen("听音选词"),
    Meaning("看中文选英文"),
    Example("例句填空"),
    Spelling("拼写挑战"),
    Synonym("近义词配对")
}

sealed class Screen {
    object Main : Screen()
    // neighbors 是打开详情时的那份列表，用来做上一个/下一个
    data class Detail(val word: OgdenWord, val neighbors: List<OgdenWord>) : Screen()
    data class Levels(val category: Category) : Screen()
    data class Practice(val category: Category, val level: Int) : Screen()
    data class WordCollection(val title: String, val kind: String) : Screen()
    object Settings : Screen()
    object Privacy : Screen()
    object About : Screen()
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
class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("ogden-progress", Context.MODE_PRIVATE)
    private val dao = ProgressDatabase.get(context).progressDao()
    // 自己持有作用域而不用 rememberCoroutineScope：退出组合不该取消刚提交的落库。
    // 默认 Main，快照状态只在主线程改；数据库读写各处显式切 IO
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 组合期间只读这份内存快照；数据库读写一律在协程里，写入是「先改快照再落库」
    private val entries = mutableStateMapOf<String, WordProgress>()
    private val levels = mutableStateMapOf<String, Boolean>()
    // lastAnsweredAt 界面不显示，只在整行覆盖写时要带上，放普通 map 不参与重组
    private val answeredAt = mutableMapOf<String, Long>()
    private var streak by mutableStateOf(prefs.getInt("streak", 0))
    // 首帧数据库还没读完，界面先等这个标志，避免统计闪一次 0
    var ready by mutableStateOf(false)
        private set

    init {
        scope.launch {
            val (words, completed) = withContext(Dispatchers.IO) {
                importLegacyPrefsIfNeeded()
                dao.allWordProgress() to dao.allLevelProgress()
            }
            words.forEach {
                entries[it.word] = it.toWordProgress()
                answeredAt[it.word] = it.lastAnsweredAt
            }
            completed.forEach { levels[levelKey(it.category, it.level)] = true }
            ready = true
        }
    }

    // 首次跑新版时把旧偏好里的进度搬进数据库：确认写入成功后才记 roomImported 标志，
    // 旧键一律保留不删，万一日后发现映射有误还能人工找回
    private suspend fun importLegacyPrefsIfNeeded() {
        if (prefs.getBoolean(IMPORTED_KEY, false)) return
        val snapshot = prefs.all
        val words = legacyWordProgress(snapshot)
        val completed = legacyLevelProgress(snapshot)
        if (words.isNotEmpty()) dao.saveWordProgress(words)
        if (completed.isNotEmpty()) dao.saveLevelProgress(completed)
        prefs.edit().putBoolean(IMPORTED_KEY, true).commit()
    }

    fun progress(word: String): WordProgress = entries[word] ?: Blank

    fun toggleFavorite(word: String) {
        val next = progress(word).let { it.copy(favorite = !it.favorite) }
        entries[word] = next
        persist(word, next)
    }

    fun record(word: String, correct: Boolean) {
        val next = nextProgress(progress(word), correct)
        entries[word] = next
        answeredAt[word] = System.currentTimeMillis()
        persist(word, next)
        if (correct) bumpDailyStreak()
    }

    private fun persist(word: String, value: WordProgress) {
        val entity = WordProgressEntity(
            word = word,
            favorite = value.favorite,
            mistake = value.mistake,
            mastery = value.mastery,
            attempts = value.attempts,
            correct = value.correct,
            lastAnsweredAt = answeredAt[word] ?: 0L
        )
        scope.launch(Dispatchers.IO) { dao.saveWordProgress(entity) }
    }

    fun masteredCount(words: List<OgdenWord>) = words.count { progress(it.word).mastery >= 3 }

    fun mistakeWords(words: List<OgdenWord>) = words.filter { progress(it.word).mistake }

    fun favoriteWords(words: List<OgdenWord>) = words.filter { progress(it.word).favorite }

    fun dailyStreak(): Int = streak

    fun lastCategory(): Category = runCatching {
        Category.from(prefs.getString("lastCategory", Category.Operations.code) ?: Category.Operations.code)
    }.getOrDefault(Category.Operations)

    fun lastLevel(): Int = prefs.getInt("lastLevel", 1).coerceAtLeast(1)

    fun saveLastLevel(category: Category, level: Int) {
        prefs.edit()
            .putString("lastCategory", category.code)
            .putInt("lastLevel", level)
            .commit()
    }

    fun savedAccent(): Accent = runCatching {
        Accent.valueOf(prefs.getString("accent", Accent.US.name) ?: Accent.US.name)
    }.getOrDefault(Accent.US)

    fun saveAccent(accent: Accent) {
        prefs.edit().putString("accent", accent.name).commit()
    }

    fun isLevelComplete(category: Category, level: Int): Boolean =
        levels[levelKey(category.code, level)] == true

    fun isLevelUnlocked(category: Category, level: Int): Boolean =
        level <= 1 || isLevelComplete(category, level - 1)

    fun markLevelComplete(category: Category, level: Int) {
        levels[levelKey(category.code, level)] = true
        val entity = LevelProgressEntity(category.code, level, System.currentTimeMillis())
        scope.launch(Dispatchers.IO) { dao.saveLevelProgress(entity) }
    }

    // 清空学习进度：内存快照立即归零（界面无需重启），数据库与标量随后清
    fun resetProgress() {
        entries.clear()
        levels.clear()
        answeredAt.clear()
        streak = 0
        // 先清库再清偏好：反过来若中途进程被杀，导入标志还在而库里旧行会复活
        scope.launch(Dispatchers.IO) {
            dao.clearWordProgress()
            dao.clearLevelProgress()
            val editor = prefs.edit()
            resettableProgressKeys(prefs.all.keys).forEach { editor.remove(it) }
            editor.commit()
        }
    }

    private fun levelKey(categoryCode: String, level: Int) = "$categoryCode.$level"

    private fun bumpDailyStreak() {
        val today = System.currentTimeMillis() / 86_400_000L
        val next = nextStreak(prefs.getLong("lastStudyDay", 0L), today, streak)
        streak = next
        prefs.edit().putLong("lastStudyDay", today).putInt("streak", next).apply()
    }

    private companion object {
        const val IMPORTED_KEY = "roomImported"
        val Blank = WordProgress(favorite = false, mistake = false, mastery = 0, attempts = 0, correct = 0)
    }
}

private fun WordProgressEntity.toWordProgress() = WordProgress(
    favorite = favorite,
    mistake = mistake,
    mastery = mastery,
    attempts = attempts,
    correct = correct
)

// 重置要删哪些键，抽成纯函数便于单测：accent 是偏好设置不属于进度，必须留下；
// roomImported 是迁移标志，删掉会让残留的旧键在下次启动被重新导入，也必须留下
fun resettableProgressKeys(keys: Set<String>): Set<String> =
    keys.filterTo(mutableSetOf()) { it != "accent" && it != "roomImported" }

// 答题后的熟练度/错词流转：答对 +1 上限 3，答错 -1 下限 0，掌握度归零也计入错词
fun nextProgress(current: WordProgress, correct: Boolean): WordProgress {
    val mastery = if (correct) (current.mastery + 1).coerceAtMost(3) else (current.mastery - 1).coerceAtLeast(0)
    return current.copy(
        mistake = !correct || mastery == 0,
        mastery = mastery,
        attempts = current.attempts + 1,
        correct = current.correct + if (correct) 1 else 0
    )
}

// 连续学习天数：同一天不变，隔一天 +1，断档从 1 重新开始
fun nextStreak(lastDay: Long, today: Long, streak: Int): Int = when {
    lastDay == today -> streak
    lastDay == today - 1 -> streak + 1
    else -> 1
}

@Composable
fun OgdenKidsApp() {
    val context = LocalContext.current
    // 词库 JSON 约 380 KB，放到 IO 线程解析，首帧只显示加载态
    val loadedWords by produceState<List<OgdenWord>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { loadWords(context) }
    }
    val words = loadedWords
    val progressStore = remember { ProgressStore(context) }
    // 词库解析与数据库首次读取都是异步的，两者都就绪后才渲染，避免统计先闪一次 0
    if (words == null || !progressStore.ready) {
        LoadingScreen()
        return
    }
    var screen by remember { mutableStateOf<Screen>(Screen.Main) }
    var selectedTab by rememberSaveable { mutableStateOf(Tab.Challenge) }
    var accent by remember { mutableStateOf(progressStore.savedAccent()) }
    val speak = rememberSpeaker(accent)
    val dark = isSystemInDarkTheme()
    val palette = if (dark) DarkPalette else LightPalette
    val view = LocalView.current
    SideEffect {
        // 状态栏/导航栏背景透明，只切换图标明暗
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }

    // 二级页面先回主页；主页非闯关 tab 先回闯关；闯关 tab 不拦截，交给系统退出
    BackHandler(enabled = screen != Screen.Main || selectedTab != Tab.Challenge) {
        if (screen != Screen.Main) screen = Screen.Main else selectedTab = Tab.Challenge
    }

    val colorScheme = if (dark) {
        darkColorScheme(
            primary = Category.Operations.baseTint.forDark(true),
            secondary = Category.GeneralThings.baseTint.forDark(true),
            background = palette.paper,
            surface = palette.paperElevated,
            onPrimary = Color(0xFF1B1410),
            onBackground = palette.ink,
            onSurface = palette.ink
        )
    } else {
        lightColorScheme(
            primary = Category.Operations.baseTint,
            secondary = Category.GeneralThings.baseTint,
            background = palette.paper,
            surface = palette.paperElevated,
            onPrimary = Color.White,
            onBackground = palette.ink,
            onSurface = palette.ink
        )
    }

    CompositionLocalProvider(LocalPalette provides palette) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography.copy(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif)
        )
    ) {
        Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
            when (val current = screen) {
                Screen.Main -> MainScaffold(
                    selectedTab = selectedTab,
                    onTab = { selectedTab = it },
                    content = { padding ->
                        when (selectedTab) {
                            Tab.Challenge -> ChallengeScreen(
                                words = words,
                                store = progressStore,
                                padding = padding,
                                onContinue = {
                                    screen = Screen.Practice(progressStore.lastCategory(), progressStore.lastLevel())
                                },
                                onCategory = { category -> screen = Screen.Levels(category) },
                                onOpenLibrary = { selectedTab = Tab.Library },
                                onOpenSettings = { screen = Screen.Settings }
                            )
                            Tab.Library -> LibraryScreen(
                                words = words,
                                store = progressStore,
                                padding = padding,
                                accent = accent,
                                onSpeak = speak,
                                onOpen = { word, siblings -> screen = Screen.Detail(word, siblings) }
                            )
                            Tab.Review -> ReviewScreen(
                                words = words,
                                store = progressStore,
                                padding = padding,
                                onMistakes = { screen = Screen.WordCollection("错词本", "mistakes") },
                                onFavorites = { screen = Screen.WordCollection("收藏夹", "favorites") }
                            )
                        }
                    }
                )
                is Screen.Detail -> WordDetailScreen(
                    word = current.word,
                    progress = progressStore.progress(current.word.word),
                    accent = accent,
                    onAccent = { accent = it; progressStore.saveAccent(it) },
                    onBack = { screen = Screen.Main },
                    onSpeak = speak,
                    onFavorite = { progressStore.toggleFavorite(current.word.word) },
                    neighbors = current.neighbors,
                    onNavigate = { next -> screen = Screen.Detail(next, current.neighbors) }
                )
                is Screen.Levels -> LevelSelectionScreen(
                    words = words,
                    store = progressStore,
                    category = current.category,
                    onBack = { screen = Screen.Main },
                    onStart = { level -> screen = Screen.Practice(current.category, level) }
                )
                is Screen.Practice -> {
                    LaunchedEffect(current.category, current.level) {
                        progressStore.saveLastLevel(current.category, current.level)
                    }
                    PracticeScreen(
                        allWords = words,
                        category = current.category,
                        level = current.level,
                        onSpeak = speak,
                        onBack = { screen = Screen.Main },
                        onComplete = {
                            progressStore.markLevelComplete(current.category, current.level)
                        },
                        onRecord = { word, correct ->
                            progressStore.record(word.word, correct)
                        }
                    )
                }
                is Screen.WordCollection -> WordCollectionScreen(
                    title = current.title,
                    words = if (current.kind == "mistakes") progressStore.mistakeWords(words) else progressStore.favoriteWords(words),
                    store = progressStore,
                    onBack = { screen = Screen.Main },
                    onOpen = { word, siblings -> screen = Screen.Detail(word, siblings) }
                )
                Screen.Settings -> SettingsScreen(
                    accent = accent,
                    onAccent = { accent = it; progressStore.saveAccent(it) },
                    onReset = { progressStore.resetProgress() },
                    onBack = { screen = Screen.Main },
                    onPrivacy = { screen = Screen.Privacy },
                    onAbout = { screen = Screen.About }
                )
                Screen.Privacy -> LegalInfoScreen(
                    title = "隐私声明",
                    onBack = { screen = Screen.Main },
                    sections = privacySections(),
                    links = emptyList()
                )
                Screen.About -> LegalInfoScreen(
                    title = "关于作者",
                    onBack = { screen = Screen.Main },
                    sections = aboutSections(),
                    links = listOf(
                        "原作地址" to "https://ogden.munch.love/",
                        "Skivein 的主页" to "https://longlong-skyligo.github.io/about/",
                        "创作初衷" to "https://longlong-skyligo.github.io/posts/basic-english/"
                    )
                )
            }
        }
    }
    }
}

@Composable
fun rememberSpeaker(accent: Accent): (String) -> Unit {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var ready by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
            }
        }
        tts = engine
        onDispose {
            player?.release()
            engine.shutdown()
        }
    }
    LaunchedEffect(accent, tts) {
        tts?.setBestLanguage(accent)
    }
    return remember(accent, tts, ready, player) {
        { rawText: String ->
            val text = rawText.trim()
            val engine = tts
            if (text.isNotEmpty()) {
                player?.release()
                fun fallbackTts() {
                    if (engine != null) {
                        engine.setBestLanguage(accent)
                        engine.setSpeechRate(0.82f)
                        engine.setPitch(1.04f)
                        val params = Bundle().apply {
                            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                        }
                        engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "ogden-${System.nanoTime()}")
                    }
                }
                fun playOnline() {
                    runCatching {
                        val mediaPlayer = MediaPlayer()
                        player = mediaPlayer
                        mediaPlayer.setDataSource(ogdenTtsUrl(text, accent))
                        mediaPlayer.setOnPreparedListener { it.start() }
                        mediaPlayer.setOnCompletionListener {
                            it.release()
                            if (player === it) player = null
                        }
                        mediaPlayer.setOnErrorListener { mp, _, _ ->
                            mp.release()
                            if (player === mp) player = null
                            fallbackTts()
                            true
                        }
                        mediaPlayer.prepareAsync()
                    }.onFailure { fallbackTts() }
                }
                val localPath = localAudioPath(text, accent)
                if (localPath == null) {
                    playOnline()
                } else {
                    runCatching {
                        val fd = context.assets.openFd(localPath)
                        val mediaPlayer = MediaPlayer()
                        player = mediaPlayer
                        mediaPlayer.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                        fd.close()
                        mediaPlayer.setOnPreparedListener { it.start() }
                        mediaPlayer.setOnCompletionListener {
                            it.release()
                            if (player === it) player = null
                        }
                        mediaPlayer.setOnErrorListener { mp, _, _ ->
                            mp.release()
                            if (player === mp) player = null
                            playOnline()
                            true
                        }
                        mediaPlayer.prepareAsync()
                    }.onFailure { playOnline() }
                }
            }
        }
    }
}

private fun localAudioPath(text: String, accent: Accent): String? {
    if (!text.matches(Regex("[A-Za-z][A-Za-z0-9-]*"))) return null
    val file = text.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_')
    if (file.isBlank()) return null
    val dir = if (accent == Accent.US) "us" else "uk"
    return "audio/$dir/$file.mp3"
}

private fun ogdenTtsUrl(text: String, accent: Accent): String {
    val encoded = URLEncoder.encode(text, "UTF-8")
    val accentParam = if (accent == Accent.US) "us" else "uk"
    val rate = if (text.split(Regex("\\s+")).size <= 1) "+0%" else "-6%"
    return "https://ogden.munch.love/api/tts?text=$encoded&accent=$accentParam&rate=${URLEncoder.encode(rate, "UTF-8")}&v=android"
}

private fun TextToSpeech.setBestLanguage(accent: Accent) {
    val result = setLanguage(accent.locale)
    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.ERROR) {
        setLanguage(Locale.ENGLISH)
    }
}

@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    fontStyle: FontStyle? = null,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
    softWrap: Boolean = true
) {
    Text(
        text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        fontStyle = fontStyle,
        lineHeight = lineHeight,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
        softWrap = softWrap
    )
}

@Composable
fun MainScaffold(
    selectedTab: Tab,
    onTab: (Tab) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding(),
        // 整个 Scaffold 已避让系统栏，内容不再重复加 inset
        contentWindowInsets = WindowInsets(0),
        containerColor = Paper,
        bottomBar = {
            NavigationBar(containerColor = PaperElevated) {
                Tab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { onTab(tab) },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { AppText(tab.title) }
                    )
                }
            }
        },
        content = content
    )
}

@Composable
fun SettingsToggleRow(accent: Accent, onAccent: (Accent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PaperElevated)
            .border(1.dp, Line)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TogglePill(Accent.UK.label, accent == Accent.UK, { onAccent(Accent.UK) }, Modifier.weight(1f))
        TogglePill(Accent.US.label, accent == Accent.US, { onAccent(Accent.US) }, Modifier.weight(1f))
    }
}

// 二级页顶栏统一在这里处理状态栏 inset（edge-to-edge 后普通 Row 不会自动避让）
@Composable
fun SecondaryTopBar(onBack: () -> Unit, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PaperElevated)
            .border(1.dp, Line)
            .statusBarsPadding()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
        content()
    }
}

data class Proverb(val en: String, val zh: String)

@Composable
fun ProverbCard(proverb: Proverb) {
    var flipped by remember(proverb.en) { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 176.dp)
            .clickable { flipped = !flipped }
            .border(1.dp, Line, RoundedCornerShape(18.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!flipped) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        proverb.en,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        color = Ink
                    )
                    Text("Tap to turn", color = InkFaint, fontSize = 11.sp, fontFamily = FontFamily.Serif)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        proverb.zh,
                        color = InkSoft.copy(alpha = 0.72f),
                        fontSize = 17.sp,
                        lineHeight = 25.sp,
                        fontFamily = FontFamily.Cursive,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                    AppText("轻触返回英文", color = InkFaint, fontSize = 11.sp, fontFamily = FontFamily.Serif)
                }
            }
        }
    }
}

fun proverbs() = listOf(
    Proverb("Small words can carry a great light.", "微小的词，也能承载辽阔的光。"),
    Proverb("A clear word opens a quiet door.", "一个清楚的词，能推开一扇安静的门。"),
    Proverb("Learn the simple things, and the hard things grow kind.", "先学会简单的事，艰深的事也会变得温和。"),
    Proverb("One word today is one step tomorrow.", "今日一词，明日一步。"),
    Proverb("The child who listens well speaks with courage.", "善于聆听的孩子，也会勇敢表达。"),
    Proverb("A good sentence is a small bridge between minds.", "一句好句子，是心灵之间的小桥。"),
    Proverb("Slow study makes deep roots.", "缓慢的学习，会长出深深的根。"),
    Proverb("Words are seeds; practice is rain.", "词语是种子，练习是雨水。"),
    Proverb("To know a word is to find a new window.", "认识一个词，就是发现一扇新窗。"),
    Proverb("Little by little, the voice becomes clear.", "一点一点，声音终会清晰。")
)

@Composable
fun ChallengeScreen(
    words: List<OgdenWord>,
    store: ProgressStore,
    padding: PaddingValues,
    onContinue: () -> Unit,
    onCategory: (Category) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit
) {
    // 谚语从原首页移到闯关页顶部，只取一句，避免把学习内容挤到折叠线以下
    val proverb = remember { proverbs().shuffled().first() }
    // 统计只在进度状态变化时重算，不随每次重组遍历 850 词
    val mastered by remember(words, store) { derivedStateOf { store.masteredCount(words) } }
    val mistakes by remember(words, store) { derivedStateOf { store.mistakeWords(words).size } }
    val favorites by remember(words, store) { derivedStateOf { store.favoriteWords(words).size } }
    val categoryStats by remember(words, store) {
        derivedStateOf {
            Category.values().associateWith { category ->
                val categoryWords = words.filter { it.category == category }
                categoryWords.count { store.progress(it.word).mastery >= 3 } to categoryWords.size
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Ogden's Basic English",
                        fontFamily = FontFamily.Cursive,
                        fontWeight = FontWeight.Normal,
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        color = Ink
                    )
                    AppText(
                        "今日读一句，再学十个词",
                        color = InkFaint,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Serif
                    )
                }
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Info, contentDescription = "设置与关于软件", tint = InkSoft)
                }
            }
        }
        item {
            ProverbCard(proverb)
        }
        item {
            HeroCard(
                title = "今日闯关",
                subtitle = "850 词闯关 · 中英双语 · 离线可学",
                action = "继续之前",
                onAction = onContinue
            )
        }
        item {
            StatsRow(
                learned = mastered,
                mistakes = mistakes,
                favorites = favorites,
                streak = store.dailyStreak()
            )
        }
        item {
            SectionTitle("分类闯关", "每 10 个词一关，先短跑，再复习")
        }
        items(Category.values()) { category ->
            val (learned, total) = categoryStats.getValue(category)
            CategoryProgressCard(
                category = category,
                learned = learned,
                total = total,
                onClick = { onCategory(category) }
            )
        }
        item {
            OutlinedButton(
                onClick = onOpenLibrary,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                AppText("打开完整词库")
            }
        }
        item {
            Text(
                "从850个词开始，做一个有情有义的人……",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = InkSoft,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HeroCard(title: String, subtitle: String, action: String, onAction: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.border(1.dp, Line, RoundedCornerShape(18.dp))
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                title,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            )
            AppText(subtitle, color = InkSoft, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onAction, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    AppText(action)
                }
                AppText("今日完成 10 词就很好", color = InkFaint, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun StatsRow(learned: Int, mistakes: Int, favorites: Int, streak: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard("已掌握", learned.toString(), Category.GeneralThings.tint, Modifier.weight(1f))
        StatCard("错词", mistakes.toString(), Error, Modifier.weight(1f))
        StatCard("收藏", favorites.toString(), Category.Opposites.tint, Modifier.weight(1f))
        StatCard("连续", "${streak}天", Category.Picturable.tint, Modifier.weight(1f))
    }
}

@Composable
fun StatCard(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.border(1.dp, Line, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // 大字号下四列很窄，允许换行而不是裁掉数字
            Text(value, color = tint, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
            AppText(label, color = InkFaint, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun CategoryProgressCard(category: Category, learned: Int, total: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, Line, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryDot(category)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(category.label, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    AppText("${category.zh} · $total WORDS", color = InkFaint, fontSize = 12.sp)
                }
                AppText("${ceil(total / 10.0).toInt()} 关", color = category.tint, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = learned / total.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = category.tint,
                trackColor = category.soft
            )
            AppText("掌握 $learned / $total", color = InkSoft, fontSize = 13.sp)
        }
    }
}

@Composable
fun LevelSelectionScreen(
    words: List<OgdenWord>,
    store: ProgressStore,
    category: Category,
    onBack: () -> Unit,
    onStart: (Int) -> Unit
) {
    val categoryWords = words.filter { it.category == category }
    val levels = ceil(categoryWords.size / 10.0).toInt()
    Scaffold(containerColor = Paper, topBar = {
        SecondaryTopBar(onBack) {
            Column(Modifier.weight(1f)) {
                Text(category.label, fontWeight = FontWeight.Bold)
                AppText("${category.zh} · $levels 个关卡", color = InkFaint, fontSize = 12.sp)
            }
        }
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionTitle("选择关卡", "通关上一关后，下一关才会解锁")
            }
            items((1..levels).toList()) { level ->
                val unlocked = store.isLevelUnlocked(category, level)
                val complete = store.isLevelComplete(category, level)
                val levelWords = categoryWords.drop((level - 1) * 10).take(10)
                val mastered = levelWords.count { store.progress(it.word).mastery >= 3 }
                LevelCard(
                    category = category,
                    level = level,
                    complete = complete,
                    unlocked = unlocked,
                    mastered = mastered,
                    total = levelWords.size,
                    onClick = { if (unlocked) onStart(level) }
                )
            }
        }
    }
}

@Composable
fun LevelCard(
    category: Category,
    level: Int,
    complete: Boolean,
    unlocked: Boolean,
    mastered: Int,
    total: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = unlocked, onClick = onClick)
            .border(1.dp, if (unlocked) Line else Color(0xFFE8E1D4), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = if (unlocked) PaperElevated else Color(0xFFF2EDE4)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (complete) category.tint else category.soft),
                contentAlignment = Alignment.Center
            ) {
                    AppText(
                    if (complete) "✓" else level.toString(),
                    color = if (complete) Paper else category.tint,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                AppText("第 $level 关", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = if (unlocked) Ink else InkFaint)
                AppText(
                    when {
                        complete -> "已通关 · 可重复练习"
                        unlocked -> "本关 $total 个词 · 已掌握 $mastered"
                        else -> "先完成上一关"
                    },
                    color = InkFaint,
                    fontSize = 13.sp
                )
                LinearProgressIndicator(
                    progress = if (total == 0) 0f else mastered / total.toFloat(),
                    color = category.tint,
                    trackColor = category.soft,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(99.dp))
                )
            }
            Spacer(Modifier.width(8.dp))
            AppText(
                if (unlocked) "进入" else "锁定",
                color = if (unlocked) category.tint else InkFaint,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    words: List<OgdenWord>,
    store: ProgressStore,
    padding: PaddingValues,
    accent: Accent,
    onSpeak: (String) -> Unit,
    onOpen: (OgdenWord, List<OgdenWord>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<Category?>(null) }
    // 输入框即时响应，过滤延迟 250ms，避免逐字符遍历 850 词
    var debouncedQuery by remember { mutableStateOf("") }
    LaunchedEffect(query) {
        delay(250)
        debouncedQuery = query
    }
    val filtered = remember(words, debouncedQuery, category) {
        words.filter { matchesLibraryFilter(it, debouncedQuery, category) }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 4 } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        // 搜索与筛选常驻在列表之上，不随 850 行滚走
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Paper)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "清空搜索", tint = InkFaint)
                        }
                    }
                },
                placeholder = { AppText("搜索单词、中文或释义") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = category == null, onClick = { category = null }, label = { Text("All · 850") })
                Category.values().forEach {
                    FilterChip(
                        selected = category == it,
                        onClick = { category = it },
                        label = { AppText("${it.zh} ${it.count}") }
                    )
                }
            }
            AppText("显示 ${filtered.size} 个词", color = InkFaint, fontSize = 13.sp)
        }
        Box(Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                // 底部留出回到顶部按钮的位置，避免遮住最后一行的收藏按钮
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filtered.isEmpty()) {
                    item { EmptyCard("没有匹配的单词，换个词试试。") }
                } else {
                    items(filtered, key = { it.word }) { word ->
                        val progress = store.progress(word.word)
                        CompactWordRow(
                            word = word,
                            progress = progress,
                            onOpen = { onOpen(word, filtered) },
                            ipa = if (accent == Accent.UK) word.ipaUk else word.ipaUs,
                            onSpeak = onSpeak,
                            onFavorite = { store.toggleFavorite(word.word) }
                        )
                    }
                }
            }
            if (showTop) {
                FloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    containerColor = PaperElevated,
                    contentColor = InkSoft,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "回到顶部")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordDetailScreen(
    word: OgdenWord,
    progress: WordProgress,
    accent: Accent,
    onAccent: (Accent) -> Unit,
    onBack: () -> Unit,
    onSpeak: (String) -> Unit,
    onFavorite: () -> Unit,
    neighbors: List<OgdenWord>,
    onNavigate: (OgdenWord) -> Unit
) {
    // 邻词按打开详情时那份列表的顺序（词库为当前过滤结果，收藏/错词为该集合）
    val index = neighbors.indexOfFirst { it.word == word.word }
    val previous = if (index > 0) neighbors[index - 1] else null
    val next = if (index >= 0 && index < neighbors.lastIndex) neighbors[index + 1] else null
    Scaffold(containerColor = Paper, topBar = {
        SecondaryTopBar(onBack) {
            AppText("单词详情", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onFavorite) {
                Icon(
                    if (progress.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (progress.favorite) "取消收藏" else "收藏",
                    tint = if (progress.favorite) Error else InkFaint
                )
            }
        }
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperElevated),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.border(1.dp, Line, RoundedCornerShape(18.dp))
                ) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(word.word, fontSize = 46.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onSpeak(word.word) }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "读单词", tint = word.category.tint)
                            }
                        }
                        // 详情页是对比英美读音的地方，音标旁直接给切换入口（设置页仍是同一个全局状态）
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (accent == Accent.UK) word.ipaUk else word.ipaUs,
                                color = InkFaint,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TogglePill(Accent.UK.label, accent == Accent.UK, { onAccent(Accent.UK) })
                            Spacer(Modifier.width(8.dp))
                            TogglePill(Accent.US.label, accent == Accent.US, { onAccent(Accent.US) })
                        }
                        CategoryBadge(word.category)
                        Text(word.zh, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        Text(word.englishDefinition, color = InkSoft, fontStyle = FontStyle.Italic)
                    }
                }
            }
            item {
                InfoBlock("例句", word.example, word.exampleZh, word.category.tint) {
                    onSpeak(word.example)
                }
            }
            item {
                SectionTitle("近义词", "点击可听发音")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    word.synonyms.forEach { syn ->
                        AssistChip(onClick = { onSpeak(syn) }, label = { Text(syn) })
                    }
                }
            }
            item {
                SectionTitle("熟练度", "答对会增加星星，答错会进入复习")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { index ->
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = if (index < progress.mastery) Category.Picturable.tint else Line,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                AppText("练习 ${progress.attempts} 次 · 答对 ${progress.correct} 次", color = InkFaint)
            }
            if (previous != null || next != null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { previous?.let(onNavigate) },
                            enabled = previous != null,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            AppText(
                                if (previous == null) "上一个" else "上一个 · ${previous.word}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = { next?.let(onNavigate) },
                            enabled = next != null,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            AppText(
                                if (next == null) "下一个" else "下一个 · ${next.word}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewScreen(
    words: List<OgdenWord>,
    store: ProgressStore,
    padding: PaddingValues,
    onMistakes: () -> Unit,
    onFavorites: () -> Unit
) {
    val mistakes by remember(words, store) { derivedStateOf { store.mistakeWords(words) } }
    val favorites by remember(words, store) { derivedStateOf { store.favoriteWords(words) } }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitle("复习中心", "错词与收藏先收好，需要时再打开")
        }
        item {
            ReviewEntryCard(
                title = "错词本",
                subtitle = if (mistakes.isEmpty()) "暂时没有错词" else "先把不熟的词变熟",
                count = mistakes.size,
                tint = Error,
                icon = Icons.Default.Refresh,
                onClick = onMistakes
            )
        }
        item {
            ReviewEntryCard(
                title = "收藏夹",
                subtitle = if (favorites.isEmpty()) "还没有收藏" else "适合睡前再看一遍",
                count = favorites.size,
                tint = Category.Opposites.tint,
                icon = Icons.Default.Favorite,
                onClick = onFavorites
            )
        }
    }
}

@Composable
fun ReviewEntryCard(
    title: String,
    subtitle: String,
    count: Int,
    tint: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, Line, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                AppText(title, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                AppText(subtitle, color = InkFaint, fontSize = 13.sp)
            }
            Text("$count", color = tint, fontWeight = FontWeight.Bold, fontSize = 28.sp)
        }
    }
}

@Composable
fun WordCollectionScreen(
    title: String,
    words: List<OgdenWord>,
    store: ProgressStore,
    onBack: () -> Unit,
    onOpen: (OgdenWord, List<OgdenWord>) -> Unit
) {
    Scaffold(containerColor = Paper, topBar = {
        SecondaryTopBar(onBack) {
            AppText(
                "$title · ${words.size}",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (words.isEmpty()) {
                item { EmptyCard("这里暂时还没有单词。") }
            } else {
                items(words, key = { it.word }) { word ->
                    CompactWordRow(word, store.progress(word.word), onOpen = { onOpen(word, words) })
                }
            }
        }
    }
}

data class LegalSection(val heading: String, val body: String)

@Composable
fun SettingsScreen(
    accent: Accent,
    onAccent: (Accent) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    onPrivacy: () -> Unit,
    onAbout: () -> Unit
) {
    var confirming by remember { mutableStateOf(false) }
    var resetDone by remember { mutableStateOf(false) }
    Scaffold(containerColor = Paper, topBar = {
        SecondaryTopBar(onBack) {
            AppText("设置与关于软件", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionTitle("应用信息", "离线词库与发音说明")
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperElevated),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Line, RoundedCornerShape(18.dp))
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Ogden Basic", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                        AppText("英语单词学习 · 离线词库 · US/UK 单词发音", color = InkSoft)
                        AppText("当前版本：1.0", color = InkFaint, fontSize = 13.sp)
                    }
                }
            }
            item {
                SectionTitle("发音设置", "全局英式 / 美式发音")
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperElevated),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Line, RoundedCornerShape(18.dp))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsToggleRow(accent = accent, onAccent = onAccent)
                    }
                }
            }
            item {
                SectionTitle("学习数据", "掌握星星、错词本、收藏与关卡进度")
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperElevated),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Line, RoundedCornerShape(18.dp))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AppText(
                            if (resetDone) "学习进度已清空，发音设置保持不变。" else "重置后全部学习记录会被清空，且无法恢复。",
                            color = if (resetDone) Success else InkSoft,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                        OutlinedButton(
                            onClick = { confirming = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Error),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
                        ) {
                            AppText("重置学习进度")
                        }
                    }
                }
            }
            item {
                SectionTitle("关于", "隐私声明与作者信息")
            }
            item {
                OutlinedButton(
                    onClick = onPrivacy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    AppText("隐私声明")
                }
            }
            item {
                OutlinedButton(
                    onClick = onAbout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    AppText("关于作者")
                }
            }
        }
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            containerColor = PaperElevated,
            shape = RoundedCornerShape(18.dp),
            title = { AppText("重置学习进度？", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                AppText(
                    "将清空掌握星星、错词本、收藏、关卡解锁和连续天数，操作无法撤销。英式 / 美式发音设置会保留。",
                    color = InkSoft,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReset()
                        resetDone = true
                        confirming = false
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Error, contentColor = Paper)
                ) {
                    AppText("确认重置")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirming = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    AppText("取消", color = InkSoft)
                }
            }
        )
    }
}

@Composable
fun LegalInfoScreen(
    title: String,
    onBack: () -> Unit,
    sections: List<LegalSection>,
    links: List<Pair<String, String>>
) {
    val context = LocalContext.current
    Scaffold(containerColor = Paper, topBar = {
        SecondaryTopBar(onBack) {
            AppText(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(sections) { section ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperElevated),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Line, RoundedCornerShape(14.dp))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppText(section.heading, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        AppText(section.body, color = InkSoft, lineHeight = 22.sp)
                    }
                }
            }
            if (links.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PaperElevated),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Line, RoundedCornerShape(14.dp))
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AppText("相关链接", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            links.forEach { (label, url) ->
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    AppText(label)
                                }
                                Text(url, color = InkFaint, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun privacySections() = listOf(
    LegalSection(
        "基本说明",
        "Ogden Basic 是一款面向英语单词学习的应用。本应用尊重并保护用户隐私，不会收集、上传、出售或共享任何用户个人信息。"
    ),
    LegalSection(
        "可能使用的权限",
        "网络访问权限：用于在例句、长文本或本地音频不可用时访问在线发音服务作为备用。音频播放能力：用于播放单词和例句发音。应用不会录音，不会访问麦克风。本地存储能力：用于在设备本地保存学习进度、收藏、错词和熟练度。"
    ),
    LegalSection(
        "不会进行的行为",
        "本应用不会收集姓名、手机号、邮箱、账号、定位等个人身份信息；不会读取通讯录、短信、相册、摄像头、麦克风等敏感权限；不会追踪用户用于广告或商业分析；不会向第三方共享学习记录。"
    ),
    LegalSection(
        "适用范围",
        "本应用适合希望学习 Ogden Basic English 850 词的用户使用，不要求提供个人信息，也不会主动收集身份、位置、联系方式或其他敏感数据。"
    ),
    LegalSection(
        "本地数据与删除",
        "学习记录、收藏、错词和熟练度均保存在本机。用户可以通过系统设置清除应用数据，或卸载应用来删除全部本地数据。"
    ),
    LegalSection(
        "发音服务",
        "应用已内置 US / UK 两套单词发音音频，用于离线播放。对于例句、长文本或本地音频不可用的情况，应用可能访问在线发音服务作为备用，该请求仅包含需要发音的英文文本，不包含用户身份信息。"
    )
)

fun aboutSections() = listOf(
    LegalSection(
        "应用来源",
        "本应用基于 Ogden Basic English 850 词学习内容进行二次创作，面向中文英语学习场景重新设计为 Android App。"
    ),
    LegalSection(
        "原作说明",
        "原始内容与视觉风格参考 Ogden's Basic English · 850 词学习手册。原网站与词汇学习内容由 ogden.munch.love 的原创作者整理与设计。本应用尊重原作内容与设计风格，并保留原作地址以便用户访问原始版本。"
    ),
    LegalSection(
        "二次创作",
        "本 Android App 由 Skivein 基于原作内容进行二次创作，主要包括 Android 原生界面、离线词库、学习进度、收藏、错词本、熟练度记录、闯关复习流程，以及 US / UK 单词离线发音音频。"
    ),
    LegalSection(
        "作者",
        "二次创作者：Skivein。个人主页：Aetheris，记录科技、人文、人工智能、金融市场与地缘历史相关思考。"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    allWords: List<OgdenWord>,
    category: Category,
    level: Int,
    onSpeak: (String) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onRecord: (OgdenWord, Boolean) -> Unit
) {
    val source = remember(allWords, category, level) {
        allWords.filter { it.category == category }.drop((level - 1) * 10).take(10)
    }
    // 每次进入关卡（或重练）生成一个种子：决定题型顺序与干扰项，同一次尝试内保持稳定
    var attemptSeed by remember(source) { mutableStateOf(Random.nextInt()) }
    val types = remember(attemptSeed) { PracticeType.values().toList().shuffled(Random(attemptSeed)) }
    var index by remember(source) { mutableStateOf(0) }
    var selected by remember(source) { mutableStateOf<String?>(null) }
    var typed by remember(source) { mutableStateOf("") }
    var answerShown by remember(source) { mutableStateOf(false) }
    var correctCount by remember(source) { mutableStateOf(0) }
    val word = source.getOrNull(index)

    Scaffold(containerColor = Paper, topBar = {
        SecondaryTopBar(onBack) {
            Column(Modifier.weight(1f)) {
                AppText("${category.zh} · 第 $level 关", fontWeight = FontWeight.Bold)
                AppText("${index.coerceAtMost(source.size)} / ${source.size} · 答对 $correctCount", color = InkFaint, fontSize = 12.sp)
            }
        }
    }) { padding ->
        if (word == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyCard("这一关没有词了。")
            }
            return@Scaffold
        }
        val type = types[index % types.size]
        // 记忆题目，避免重组时选项重新洗牌
        val question = remember(word, level, index, attemptSeed) { buildQuestion(type, word, allWords, attemptSeed) }
        val isCorrect = selected?.equals(question.answer, ignoreCase = true) == true
        val lastQuestion = index >= source.lastIndex
        val needed = ceil(source.size * 0.6).toInt()
        val passed = correctCount >= needed

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LinearProgressIndicator(
                    progress = (index + if (answerShown) 1 else 0) / source.size.toFloat(),
                    color = category.tint,
                    trackColor = category.soft,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(99.dp))
                )
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperElevated),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.border(1.dp, Line, RoundedCornerShape(18.dp))
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        CategoryBadge(word.category)
                        AppText(type.title, color = word.category.tint, fontWeight = FontWeight.Bold)
                        if (type == PracticeType.Listen) {
                            Button(onClick = { onSpeak(word.word) }, shape = CircleShape, modifier = Modifier.size(96.dp)) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "播放", modifier = Modifier.size(44.dp))
                            }
                        } else {
                            Text(question.prompt, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp)
                        }
                        if (type == PracticeType.Spelling) {
                            AppText("输入英文单词，不区分大小写", color = InkFaint)
                        }
                    }
                }
            }
            if (type == PracticeType.Spelling) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = typed,
                            onValueChange = { if (!answerShown) typed = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { AppText("输入英文拼写") },
                            placeholder = { AppText("在这里拼出单词") },
                            singleLine = true,
                            readOnly = answerShown,
                            shape = RoundedCornerShape(16.dp)
                        )
                        Button(
                            onClick = {
                                if (!answerShown && typed.isNotBlank()) {
                                    val guess = typed.trim()
                                    selected = guess
                                    val ok = guess.equals(question.answer, ignoreCase = true)
                                    if (ok) correctCount++
                                    onRecord(word, ok)
                                    answerShown = true
                                }
                            },
                            enabled = !answerShown && typed.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            AppText("提交答案")
                        }
                    }
                }
            } else {
            items(question.options) { option ->
                val selectedThis = selected == option
                val correctThis = answerShown && option == question.answer
                val wrongThis = answerShown && selectedThis && option != question.answer
                OutlinedButton(
                    onClick = {
                        if (!answerShown) {
                            selected = option
                            val ok = option == question.answer
                            if (ok) correctCount++
                            onRecord(word, ok)
                            answerShown = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = when {
                            correctThis -> Color(0xFFDCFCE7)
                            wrongThis -> Color(0xFFFEE2E2)
                            selectedThis -> word.category.soft
                            else -> PaperElevated
                        }
                    ),
                    border = BorderStroke(1.dp, Line)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(option, modifier = Modifier.weight(1f), fontSize = 18.sp)
                        if (correctThis) Icon(Icons.Default.Check, contentDescription = null, tint = Success)
                        if (wrongThis) Icon(Icons.Default.Close, contentDescription = null, tint = Error)
                    }
                }
            }
            }
            item {
                AnimatedVisibility(answerShown) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isCorrect) Color(0xFFECFDF5) else Color(0xFFFEF2F2)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppText(if (isCorrect) "答对了！" else "这题先记到错词本", fontWeight = FontWeight.Bold, color = if (isCorrect) Success else Error)
                            if (type == PracticeType.Spelling && !isCorrect) {
                                AppText("你写的是 ${selected.orEmpty()}，正确拼写是 ${word.word}", color = Error)
                            }
                            Text("${word.word} · ${word.zh}", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Text(word.example, fontFamily = FontFamily.Serif, color = InkSoft)
                            if (lastQuestion && !passed) {
                                // 正确率不足 60%：不解锁下一关，只给返回或重练
                                AppText(
                                    "这一关答对 $correctCount / ${source.size}，答对 $needed 个就能解锁下一关。再练一次会换新题目，慢慢来。",
                                    color = InkSoft,
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            attemptSeed = Random.nextInt()
                                            index = 0
                                            selected = null
                                            typed = ""
                                            answerShown = false
                                            correctCount = 0
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        AppText("再练一次")
                                    }
                                    OutlinedButton(
                                        onClick = onBack,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        AppText("先返回")
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (lastQuestion) {
                                            onComplete()
                                            onBack()
                                        } else {
                                            index++
                                            selected = null
                                            typed = ""
                                            answerShown = false
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    AppText(if (lastQuestion) "完成并返回" else "下一题")
                                }
                            }
                        }
                    }
                }
            }
        }
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
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        AppText(title, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        AppText(subtitle, color = InkFaint, fontSize = 13.sp)
    }
}

@Composable
fun CategoryDot(category: Category) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(category.tint)
    )
}

@Composable
fun CategoryBadge(category: Category) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(category.soft)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryDot(category)
        Spacer(Modifier.width(7.dp))
        AppText(category.zh, color = category.tint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun TogglePill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) Ink else PaperElevated)
            .border(1.dp, if (selected) Ink else Line, RoundedCornerShape(99.dp)),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        AppText(label, color = if (selected) Paper else InkSoft, maxLines = 1, fontSize = 12.sp)
    }
}

@Composable
fun InfoBlock(title: String, en: String, zh: String, tint: Color, onSpeak: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.border(1.dp, Line, RoundedCornerShape(14.dp))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onSpeak) { Icon(Icons.Default.VolumeUp, contentDescription = "读例句", tint = tint) }
            }
            Text(en, fontFamily = FontFamily.Serif, fontSize = 20.sp, color = Ink)
            Text(zh, color = InkSoft)
        }
    }
}

@Composable
fun CompactWordRow(
    word: OgdenWord,
    progress: WordProgress,
    onOpen: () -> Unit,
    ipa: String? = null,
    onSpeak: ((String) -> Unit)? = null,
    onFavorite: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PaperElevated)
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryDot(word.category)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(word.word, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, fontSize = 22.sp)
                if (!ipa.isNullOrBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        ipa,
                        color = InkFaint,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(word.zh, color = InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row {
            repeat(progress.mastery) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Category.Picturable.tint, modifier = Modifier.size(16.dp))
            }
        }
        if (onSpeak != null) {
            IconButton(onClick = { onSpeak(word.word) }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.VolumeUp, contentDescription = "读单词", tint = word.category.tint)
            }
        }
        if (onFavorite != null) {
            IconButton(onClick = onFavorite, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (progress.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (progress.favorite) "取消收藏" else "收藏",
                    tint = if (progress.favorite) Error else InkFaint
                )
            }
        }
    }
}

@Composable
fun EmptyCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line, RoundedCornerShape(14.dp))
    ) {
        AppText(text, color = InkFaint, modifier = Modifier.padding(18.dp))
    }
}

fun matchesLibraryFilter(word: OgdenWord, query: String, category: Category?): Boolean =
    (category == null || word.category == category) &&
        (query.isBlank() ||
            word.word.contains(query, ignoreCase = true) ||
            word.zh.contains(query) ||
            word.englishDefinition.contains(query, ignoreCase = true))

@Composable
fun LoadingScreen() {
    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            EmptyCard("正在准备词库……")
        }
    }
}

