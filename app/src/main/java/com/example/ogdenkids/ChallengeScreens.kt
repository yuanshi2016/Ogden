package com.example.ogdenkids

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.ogdenkids.ai.DeepSeekClient
import com.example.ogdenkids.curriculum.CurriculumBundle
import com.example.ogdenkids.curriculum.LearningTrack
import com.example.ogdenkids.curriculum.curriculumUnitItems
import com.example.ogdenkids.data.DailyActivityEntity
import com.example.ogdenkids.data.EarnedRewardEntity
import com.example.ogdenkids.data.decodeProgressSnapshot
import com.example.ogdenkids.data.tutorFocusAddon
import com.example.ogdenkids.speech.SpeechRecorder
import com.example.ogdenkids.speech.WhisperEngine
import com.example.ogdenkids.speech.WhisperStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin
import kotlin.random.Random

data class Proverb(val en: String, val zh: String)

@Composable
fun ProverbCard(proverb: Proverb) {
    var flipped by remember(proverb.en) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(520),
        label = "proverbFlip"
    )
    val density = LocalDensity.current
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
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density.density
                }
                .padding(22.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
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
                Column(
                    modifier = Modifier.graphicsLayer { rotationY = 180f },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
    curriculum: CurriculumBundle,
    padding: PaddingValues,
    onContinue: () -> Unit,
    onCategory: (Category) -> Unit,
    onOpenCurriculumUnit: (String) -> Unit,
    onPepGrade: (Int) -> Unit = {},
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onStartDueReview: () -> Unit
) {
    var track by rememberSaveable { mutableStateOf(store.learningTrack()) }
    var pepGrade by rememberSaveable { mutableStateOf(store.pepLastGrade()) }
    var volume by rememberSaveable { mutableStateOf(store.pepLastVolume()) }
    // 谚语从原首页移到闯关页顶部，只取一句，避免把学习内容挤到折叠线以下
    val proverb = remember { proverbs().shuffled().first() }
    // 统计只在进度状态变化时重算，不随每次重组遍历 1203 词
    val mastered by remember(words, store) { derivedStateOf { store.masteredCount(words) } }
    val mistakes by remember(words, store) { derivedStateOf { store.mistakeWords(words).size } }
    val favorites by remember(words, store) { derivedStateOf { store.favoriteWords(words).size } }
    val dueCount by remember(store) { derivedStateOf { store.dueForReviewCount() } }
    val categoryStats by remember(words, store) {
        derivedStateOf {
            Category.values().associateWith { category ->
                val categoryWords = words.filter { it.category == category }
                categoryWords.count { store.progress(it.word).mastery >= 3 } to categoryWords.size
            }
        }
    }

    val entrance = rememberScreenEntrance()
    Box(
        Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        TechGlow(Primary, Modifier.fillMaxSize())
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.reveal(entrance, 0)
                ) {
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .reveal(entrance, 0),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LearningTrack.values().forEach { t ->
                        TogglePill(
                            t.label,
                            track == t,
                            {
                                track = t
                                store.saveLearningTrack(t)
                            },
                            Modifier.weight(1f)
                        )
                    }
                }
            }
            item {
                Box(Modifier.reveal(entrance, 1)) {
                    ProverbCard(proverb)
                }
            }
            item {
                Box(Modifier.reveal(entrance, 2)) {
                    HeroCard(
                        title = "今日闯关",
                        subtitle = "1203 词闯关 · 中英双语 · 离线可学",
                        action = "继续之前",
                        onAction = onContinue
                    )
                }
            }
            if (dueCount > 0) {
                item {
                    Box(Modifier.reveal(entrance, 2)) {
                        val (dueInteraction, dueIndication, duePress) = rememberPressScale()
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PaperElevated),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(0.dp),
                            modifier = duePress
                                .fillMaxWidth()
                                .border(1.dp, Line, RoundedCornerShape(16.dp))
                                .clickable(
                                    interactionSource = dueInteraction,
                                    indication = dueIndication,
                                    onClick = onStartDueReview
                                )
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Listening.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.School, contentDescription = null, tint = Listening)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("今日待复习", fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = FontFamily.Serif)
                                    AppText(
                                        "有 $dueCount 个词该巩固了，点此开练",
                                        color = InkFaint,
                                        fontSize = 13.sp
                                    )
                                }
                                AppText("复习 →", color = Listening, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            item {
                Box(Modifier.reveal(entrance, 3)) {
                    StatsRow(
                        learned = mastered,
                        mistakes = mistakes,
                        favorites = favorites,
                        streak = store.dailyStreak()
                    )
                }
            }
            item {
                Box(Modifier.reveal(entrance, 4)) {
                    EarnedRewardsCard(store.earnedRewards())
                }
            }
            item {
                val (statsInteraction, statsIndication, statsPress) = rememberPressScale()
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperElevated),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = statsPress
                        .fillMaxWidth()
                        .border(1.dp, Line, RoundedCornerShape(16.dp))
                        .clickable(interactionSource = statsInteraction, indication = statsIndication, onClick = onOpenStats)
                        .reveal(entrance, 5)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("学习图表", fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = FontFamily.Serif)
                            AppText("最近 30 天答题 · 答对情况", color = InkFaint, fontSize = 13.sp)
                        }
                        AppText("查看 →", color = InkSoft, fontSize = 13.sp)
                    }
                }
            }
            if (track == LearningTrack.Ogden) {
                item {
                    Box(Modifier.reveal(entrance, 6)) {
                        SectionTitle("分类闯关", "每 10 个词一关，先短跑，再复习")
                    }
                }
                items(Category.values()) { category ->
                    val (learned, total) = categoryStats.getValue(category)
                    Box(Modifier.reveal(entrance, 6)) {
                        CategoryProgressCard(
                            category = category,
                            learned = learned,
                            total = total,
                            onClick = { onCategory(category) }
                        )
                    }
                }
            } else {
                curriculumUnitItems(
                    bundle = curriculum,
                    store = store,
                    grade = pepGrade,
                    onGrade = { g ->
                        if (g != pepGrade) {
                            pepGrade = g
                            volume = 1
                            store.savePepLastVolume(1)
                            onPepGrade(g)
                        }
                    },
                    volume = volume,
                    onVolume = {
                        volume = it
                        store.savePepLastVolume(it)
                    },
                    onOpenUnit = { unitId ->
                        store.saveLearningTrack(LearningTrack.Pep)
                        store.savePepLastVolume(volume)
                        onOpenCurriculumUnit(unitId)
                    }
                )
            }
            item {
                Box(Modifier.reveal(entrance, 7)) {
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
            }
            item {
                Text(
                    "从1203个词开始，做一个有情有义的人……",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .reveal(entrance, 7),
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
}

@Composable
fun EarnedRewardsCard(rows: List<EarnedRewardEntity>) {
    val items = remember(rows) { groupEarnedRewards(rows) }
    if (items.isEmpty()) return
    Card(
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line, RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppText("已得到的奖励", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            AppText("同名奖励会叠在一起，方便看碎片凑了几份", color = InkFaint, fontSize = 12.sp)
            items.forEach { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AppText(item.text, modifier = Modifier.weight(1f), fontSize = 15.sp)
                    AppText("×${item.count}", color = Primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HeroCard(title: String, subtitle: String, action: String, onAction: () -> Unit) {
    val transition = rememberInfiniteTransition()
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "heroPulse"
    )
    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.border(1.dp, Line, RoundedCornerShape(18.dp))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Primary, Listening)))
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    title,
                    fontSize = 28.sp,
                    lineHeight = 36.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                AppText(subtitle, color = Color.White.copy(alpha = 0.88f), fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onAction,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Primary),
                        modifier = Modifier.graphicsLayer {
                            val s = 1f + pulse * 0.05f
                            scaleX = s
                            scaleY = s
                        }
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        AppText(action)
                    }
                    AppText("今日完成 10 词就很好", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun CategoryProgressCard(category: Category, learned: Int, total: Int, onClick: () -> Unit) {
    val (pressInteraction, pressIndication, pressModifier) = rememberPressScale()
    Card(
        modifier = pressModifier
            .fillMaxWidth()
            .clickable(interactionSource = pressInteraction, indication = pressIndication, onClick = onClick)
            .border(1.dp, Line, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryDot(category)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(category.zh, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    AppText("$total 词", color = InkFaint, fontSize = 12.sp)
                }
                AppText("${ceil(total / 10.0).toInt()} 关", color = category.tint, fontWeight = FontWeight.Bold)
            }
            TechProgressBar(
                progress = learned / total.toFloat(),
                tint = category.tint,
                track = category.soft
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
    onStart: (Int) -> Unit,
    onExam: (Int) -> Unit
) {
    val categoryWords = words.filter { it.category == category }
    val levels = ceil(categoryWords.size / 10.0).toInt()
    var rewardsOpen by remember { mutableStateOf(false) }
    var examInput by remember { mutableStateOf(examCountDefault(categoryWords.size).toString()) }
    Scaffold(containerColor = Paper, topBar = {
        SecondaryTopBar(onBack) {
            Column(Modifier.weight(1f)) {
                Text(category.zh, fontWeight = FontWeight.Bold)
                AppText("$levels 个关卡", color = InkFaint, fontSize = 12.sp)
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
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperElevated),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Line, RoundedCornerShape(18.dp))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { rewardsOpen = !rewardsOpen },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                AppText("闯关奖励", fontWeight = FontWeight.Bold)
                                AppText("简单 / 中等 / 困难 / 跟读可分别填写", color = InkFaint, fontSize = 12.sp)
                            }
                            AppText(if (rewardsOpen) "收起" else "设置", color = category.tint, fontWeight = FontWeight.Bold)
                        }
                        if (rewardsOpen) {
                            Difficulty.values().forEach { d ->
                                OutlinedTextField(
                                    value = store.reward(category, d),
                                    onValueChange = { store.saveReward(category, d, it) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    label = { AppText(d.title) },
                                    placeholder = { AppText("过关奖励", color = InkFaint) }
                                )
                            }
                        }
                    }
                }
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
                        AppText("考试模式", fontWeight = FontWeight.Bold)
                        AppText("本分类 ${categoryWords.size} 词，输入题量后开始", color = InkFaint, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = examInput,
                                onValueChange = { examInput = it.filter { ch -> ch.isDigit() } },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { AppText("题量") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Button(
                                onClick = { onExam(clampExamCount(examInput.toIntOrNull() ?: 0, categoryWords.size)) },
                                modifier = Modifier.heightIn(min = 48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                AppText("开始")
                            }
                        }
                    }
                }
            }
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
    val (pressInteraction, pressIndication, pressModifier) = rememberPressScale()
    // 只有「下一个可玩」的关卡（已解锁但未通关）需要呼吸提示，避免每个关卡都挂一个动画循环
    val isNext = unlocked && !complete
    val pulse = if (isNext) {
        val t = rememberInfiniteTransition()
        val v by t.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
            label = "levelPulse"
        )
        v
    } else 0f
    Card(
        modifier = pressModifier
            .fillMaxWidth()
            .clickable(interactionSource = pressInteraction, indication = pressIndication, enabled = unlocked, onClick = onClick)
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
                    .graphicsLayer {
                        if (isNext) {
                            val s = 1f + pulse * 0.08f
                            scaleX = s
                            scaleY = s
                        }
                    }
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
                TechProgressBar(
                    progress = if (total == 0) 0f else mastered / total.toFloat(),
                    tint = category.tint,
                    track = category.soft,
                    height = 7.dp
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
