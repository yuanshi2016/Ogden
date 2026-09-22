package com.example.ogdenkids

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowCompat
import com.example.ogdenkids.speech.WhisperEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import android.os.Bundle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // edge-to-edge：内容自行处理 inset，状态栏/导航栏由主题决定明暗
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { OgdenKidsApp() }
    }
}

@OptIn(ExperimentalAnimationApi::class)
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
    // 空闲预热 whisper：避开首帧，减轻第一次跟读/口语的等待
    LaunchedEffect(Unit) {
        delay(2_500)
        WhisperEngine.ensureLoaded(context)
    }
    // 导航只存可序列化 route；进程被杀/旋转后恢复，Detail 用 word key 回查
    var screen by rememberSaveable(stateSaver = Screen.Saver) { mutableStateOf<Screen>(Screen.Main) }
    var selectedTab by rememberSaveable { mutableStateOf(Tab.Challenge) }
    val wordsByKey = remember(words) { words.associateBy { it.word } }
    var accent by remember { mutableStateOf(progressStore.savedAccent()) }
    var themeMode by remember { mutableStateOf(progressStore.savedThemeMode()) }
    var speakLevel by remember { mutableStateOf(progressStore.savedSpeakLevel()) }
    var aiKey by remember { mutableStateOf(progressStore.savedAiKey()) }
    var parentPin by remember { mutableStateOf(progressStore.savedParentPin()) }
    var reviewReminderEnabled by remember { mutableStateOf(progressStore.reviewReminderEnabled()) }
    // 进程被杀后重启：若开关开着则重新排闹钟
    LaunchedEffect(Unit) { ReviewReminderScheduler.rescheduleIfEnabled(context) }
    val speak = rememberSpeaker(accent)
    // 深色只对成人开放：儿童模式始终用亮色
    val dark = themeMode == ThemeMode.Adult && isSystemInDarkTheme()
    val palette = when {
        themeMode == ThemeMode.Child -> ChildPalette
        dark -> AdultDarkPalette
        else -> AdultLightPalette
    }
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
            primary = palette.primary,
            secondary = palette.accent,
            tertiary = palette.listening,
            background = palette.paper,
            surface = palette.paperElevated,
            onPrimary = Color(0xFF0B1F16),
            onSecondary = Color(0xFF241505),
            onBackground = palette.ink,
            onSurface = palette.ink,
            error = palette.error
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            secondary = palette.accent,
            tertiary = palette.listening,
            background = palette.paper,
            surface = palette.paperElevated,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = palette.ink,
            onSurface = palette.ink,
            error = palette.error
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
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    ContentTransform(
                        targetContentEnter = slideInHorizontally(initialOffsetX = { it / 5 }, animationSpec = tween(260)) + fadeIn(tween(260)),
                        initialContentExit = slideOutHorizontally(targetOffsetX = { -it / 5 }, animationSpec = tween(260)) + fadeOut(tween(220))
                    )
                },
                label = "screen"
            ) { current ->
                when (current) {
                    Screen.Main -> MainScaffold(
                        selectedTab = selectedTab,
                        onTab = { selectedTab = it },
                        content = { padding ->
                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = {
                                    ContentTransform(
                                        targetContentEnter = fadeIn(tween(220)),
                                        initialContentExit = fadeOut(tween(180))
                                    )
                                },
                                label = "tab"
                            ) { tab ->
                                when (tab) {
                                    Tab.Challenge -> ChallengeScreen(
                                words = words,
                                store = progressStore,
                                padding = padding,
                                onContinue = {
                                    screen = Screen.Practice(progressStore.lastCategory(), progressStore.lastLevel())
                                },
                                onCategory = { category -> screen = Screen.Levels(category) },
                                onOpenLibrary = { selectedTab = Tab.Library },
                                onOpenSettings = { screen = Screen.Settings },
                                onOpenStats = { screen = Screen.Stats },
                                onStartDueReview = {
                                    val due = progressStore.dueForReview(words)
                                    if (due.isNotEmpty()) {
                                        screen = Screen.Practice(
                                            category = Category.Operations,
                                            level = 0,
                                            wordKeys = due.map { it.word },
                                            title = "智能复习"
                                        )
                                    }
                                }
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
                                onFavorites = { screen = Screen.WordCollection("收藏夹", "favorites") },
                                onStartDueReview = {
                                    val due = progressStore.dueForReview(words)
                                    if (due.isNotEmpty()) {
                                        screen = Screen.Practice(
                                            category = Category.Operations,
                                            level = 0,
                                            wordKeys = due.map { it.word },
                                            title = "智能复习"
                                        )
                                    }
                                },
                                onBrowseDue = {
                                    screen = Screen.WordCollection("智能复习", "due")
                                }
                            )
                            Tab.Ai -> AiTab(
                                apiKey = aiKey,
                                focusWords = progressStore.focusWordsForAi(words),
                                speak = speak,
                                padding = padding,
                                onOpenSettings = { screen = Screen.Settings }
                            )
                                }
                            }
                        }
                    )
                    is Screen.Detail -> {
                    val detailWord = wordsByKey[current.wordKey]
                    if (detailWord == null) {
                        // 词库变更或 key 失效：回主页，避免卡在空详情
                        LaunchedEffect(current.wordKey) { screen = Screen.Main }
                        Box(Modifier.fillMaxSize())
                    } else {
                        val neighbors = current.neighborKeys.mapNotNull { wordsByKey[it] }
                        WordDetailScreen(
                            word = detailWord,
                            progress = progressStore.progress(detailWord.word),
                            accent = accent,
                            onAccent = { accent = it; progressStore.saveAccent(it) },
                            onBack = { screen = Screen.Main },
                            onSpeak = speak,
                            onFavorite = { progressStore.toggleFavorite(detailWord.word) },
                            neighbors = neighbors,
                            onNavigate = { next -> screen = Screen.Detail(next.word, current.neighborKeys) }
                        )
                    }
                }
                is Screen.Levels -> LevelSelectionScreen(
                    words = words,
                    store = progressStore,
                    category = current.category,
                    onBack = { screen = Screen.Main },
                    onStart = { level -> screen = Screen.Practice(current.category, level) },
                    onExam = { count -> screen = Screen.Practice(current.category, 0, count) }
                )
                is Screen.Practice -> {
                    val custom = current.wordKeys.isNotEmpty()
                    val exam = current.level == 0 && !custom
                    LaunchedEffect(current.category, current.level, custom) {
                        if (!exam && !custom) progressStore.saveLastLevel(current.category, current.level)
                    }
                    PracticeScreen(
                        allWords = words,
                        category = current.category,
                        level = current.level,
                        examCount = current.examCount,
                        wordKeys = current.wordKeys,
                        sessionTitle = current.title,
                        speakLevel = speakLevel,
                        rewards = if (exam || custom) emptyMap() else progressStore.rewardsOf(current.category),
                        onSpeak = speak,
                        onBack = { screen = Screen.Main },
                        onComplete = { difficulty ->
                            if (!exam && !custom) {
                                progressStore.markLevelComplete(current.category, current.level)
                                progressStore.markRewardEarned(current.category, difficulty)
                            }
                        },
                        onRecord = { word, correct, quality ->
                            progressStore.record(word.word, correct, quality = quality)
                        }
                    )
                }
                is Screen.WordCollection -> {
                    val collectionWords = when (current.kind) {
                        "mistakes" -> progressStore.mistakeWords(words)
                        "favorites" -> progressStore.favoriteWords(words)
                        "due" -> progressStore.dueForReview(words, limit = 50)
                        else -> emptyList()
                    }
                    WordCollectionScreen(
                        title = current.title,
                        words = collectionWords,
                        store = progressStore,
                        onBack = { screen = Screen.Main },
                        onOpen = { word, siblings -> screen = Screen.Detail(word, siblings) },
                        onPractice = if (collectionWords.isNotEmpty()) {
                            {
                                screen = Screen.Practice(
                                    category = Category.Operations,
                                    level = 0,
                                    wordKeys = collectionWords.map { it.word },
                                    title = current.title
                                )
                            }
                        } else null
                    )
                }
                Screen.Stats -> StatsScreen(
                    words = words,
                    store = progressStore,
                    onBack = { screen = Screen.Main }
                )
                Screen.Settings -> SettingsScreen(
                    accent = accent,
                    onAccent = { accent = it; progressStore.saveAccent(it) },
                    themeMode = themeMode,
                    onThemeMode = { themeMode = it; progressStore.saveThemeMode(it) },
                    speakLevel = speakLevel,
                    onSpeakLevel = { speakLevel = it; progressStore.saveSpeakLevel(it) },
                    aiKey = aiKey,
                    onAiKey = { aiKey = it; progressStore.saveAiKey(it) },
                    parentPin = parentPin,
                    onParentPin = { parentPin = it; progressStore.saveParentPin(it) },
                    reviewReminderEnabled = reviewReminderEnabled,
                    onReviewReminderEnabled = { enabled ->
                        reviewReminderEnabled = enabled
                        progressStore.setReviewReminderEnabled(enabled)
                    },
                    reviewReminderHour = progressStore.reviewReminderHour(),
                    onReset = { progressStore.resetProgress() },
                    onExport = { progressStore.exportProgressJson() },
                    onImport = { json -> progressStore.importProgressJson(json) },
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
}
