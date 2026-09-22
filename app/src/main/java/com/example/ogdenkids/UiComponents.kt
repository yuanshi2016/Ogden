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

@Composable
fun ThemeModeToggleRow(mode: ThemeMode, onMode: (ThemeMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PaperElevated)
            .border(1.dp, Line)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TogglePill(ThemeMode.Child.label, mode == ThemeMode.Child, { onMode(ThemeMode.Child) }, Modifier.weight(1f))
        TogglePill(ThemeMode.Adult.label, mode == ThemeMode.Adult, { onMode(ThemeMode.Adult) }, Modifier.weight(1f))
    }
}

@Composable
fun SpeakLevelToggleRow(level: SpeakLevel, onLevel: (SpeakLevel) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PaperElevated)
            .border(1.dp, Line)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpeakLevel.values().forEach { l ->
            TogglePill(l.label, level == l, { onLevel(l) }, Modifier.weight(1f))
        }
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
        IconButton(onClick = onBack, modifier = Modifier.size(TouchTarget)) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
        content()
    }
}

@Composable
fun StatsRow(learned: Int, mistakes: Int, favorites: Int, streak: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard("已掌握", learned, "", Category.GeneralThings.tint, Modifier.weight(1f))
        StatCard("错词", mistakes, "", Error, Modifier.weight(1f))
        StatCard("收藏", favorites, "", Category.Opposites.tint, Modifier.weight(1f))
        StatCard("连续", streak, "天", Category.Picturable.tint, Modifier.weight(1f))
    }
}

@Composable
fun StatCard(label: String, value: Int, suffix: String = "", tint: Color, modifier: Modifier = Modifier) {
    val animated by animateIntAsState(value, tween(600), label = "stat")
    Card(
        modifier = modifier.border(1.dp, Line, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // 大字号下四列很窄，允许换行而不是裁掉数字
            Text("$animated$suffix", color = tint, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
            AppText(label, color = InkFaint, fontSize = 12.sp, textAlign = TextAlign.Center)
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
            .heightIn(min = TouchTarget)
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
    val (pressInteraction, pressIndication, pressModifier) = rememberPressScale()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PaperElevated)
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .clickable(interactionSource = pressInteraction, indication = pressIndication, onClick = onOpen)
            .then(pressModifier)
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
                Icon(Icons.Default.VolumeUp, contentDescription = "读单词", tint = Listening)
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

@Composable
internal fun Modifier.answerShake(trigger: Boolean): Modifier {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger) {
            val spec = tween<Float>(50)
            repeat(3) {
                offset.animateTo(12f, spec)
                offset.animateTo(-12f, spec)
            }
            offset.animateTo(0f, spec)
        } else {
            offset.snapTo(0f)
        }
    }
    return graphicsLayer { translationX = offset.value }
}

@Composable
internal fun Modifier.answerPop(trigger: Boolean): Modifier {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger) {
            scale.animateTo(1.06f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        } else {
            scale.snapTo(1f)
        }
    }
    return graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}

@Composable
internal fun rememberPressScale(scaleTo: Float = 0.96f): Triple<MutableInteractionSource, Indication?, Modifier> {
    val interaction = remember { MutableInteractionSource() }
    val indication = LocalIndication.current
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleTo else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pressScale"
    )
    return Triple(interaction, indication, Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
}

// 闯关页顶部的一团缓缓游走的光晕，给界面加一点「科技感」的底
@Composable
internal fun TechGlow(accent: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
        label = "techGlow"
    )
    Box(
        modifier.drawBehind {
            val cx = size.width * (0.15f + 0.7f * t)
            val cy = -size.height * 0.1f
            val r = size.width * 0.9f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r
                ),
                center = Offset(cx, cy),
                radius = r
            )
        }
    )
}

// 页面进入时一次性推进的进度，驱动各卡片错峰淡入，避免每个 item 各自起一个动画
@Composable
internal fun rememberScreenEntrance(): Float {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    return anim.value
}

internal fun Modifier.reveal(progress: Float, order: Int): Modifier {
    val t = ((progress * 8f) - order).coerceIn(0f, 1f)
    val eased = 1f - (1f - t) * (1f - t)
    return graphicsLayer { alpha = eased; translationY = (1f - eased) * 24f }
}

// 渐变 + 扫光的进度条：填充用主色到高光的渐变，一道白色高光来回扫过
@Composable
internal fun TechProgressBar(
    progress: Float,
    tint: Color,
    track: Color,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    val animated by animateFloatAsState(progress, tween(400), label = "techProgress")
    val transition = rememberInfiniteTransition()
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep"
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(99.dp))
            .background(track)
            .drawBehind {
                val w = size.width * animated.coerceIn(0f, 1f)
                if (w <= 0f) return@drawBehind
                val r = CornerRadius(size.height / 2f)
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(tint, lerp(tint, Color.White, 0.35f)),
                        startX = 0f,
                        endX = w
                    ),
                    topLeft = Offset.Zero,
                    size = Size(w, size.height),
                    cornerRadius = r
                )
                val band = size.height * 2.6f
                val x = -band + sweep * (w + 2f * band)
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.5f), Color.Transparent),
                        startX = x,
                        endX = x + band
                    ),
                    topLeft = Offset.Zero,
                    size = Size(w, size.height),
                    cornerRadius = r
                )
            }
    )
}

// 连对徽章：答对 ≥2 连续时弹出，每次递增再弹一下
@Composable
internal fun ComboBadge(combo: Int, modifier: Modifier = Modifier) {
    if (combo < 2) return
    val scale = remember { Animatable(1f) }
    LaunchedEffect(combo) {
        scale.snapTo(0.5f)
        scale.animateTo(1.18f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
    }
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(RoundedCornerShape(99.dp))
            .background(Primary)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        AppText("连对 $combo", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private class ConfettiParticle(
    val x: Float,
    val drift: Float,
    val speed: Float,
    val color: Color,
    val size: Float,
    val spin: Float,
    val spinSpeed: Float
)

@Composable
fun ConfettiOverlay(tick: Int, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    val particles = remember {
        val rng = Random(7)
        val colors = listOf(
            Color(0xFFE11D48), Color(0xFFF59E0B), Color(0xFF10B981),
            Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFFF97316)
        )
        List(40) {
            ConfettiParticle(
                x = rng.nextFloat(),
                drift = rng.nextFloat() * 0.5f - 0.25f,
                speed = 0.7f + rng.nextFloat() * 0.6f,
                color = colors.random(rng),
                size = 7f + rng.nextFloat() * 8f,
                spin = rng.nextFloat() * 360f,
                spinSpeed = rng.nextFloat() * 720f - 360f
            )
        }
    }
    LaunchedEffect(tick) {
        if (tick > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(2200, easing = LinearEasing))
        }
    }
    val t = progress.value
    if (tick > 0 && t < 1f) {
        Canvas(modifier.fillMaxSize()) {
            particles.forEach { part ->
                val tt = t * part.speed
                val x = (part.x + part.drift * tt) * size.width
                val y = -30f + (size.height + 140f) * tt
                val alpha = if (tt > 0.8f) ((1f - tt) / 0.2f).coerceIn(0f, 1f) else 1f
                rotate(part.spin + part.spinSpeed * tt, pivot = Offset(x, y)) {
                    drawRect(
                        color = part.color.copy(alpha = alpha),
                        topLeft = Offset(x - part.size / 2f, y - part.size / 2f),
                        size = Size(part.size, part.size * 0.55f)
                    )
                }
            }
        }
    }
}

@Composable
internal fun RecordingWaveform(
    modifier: Modifier = Modifier,
    bars: Int = 20,
    color: Color = Color.White
) {
    val transition = rememberInfiniteTransition()
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Restart),
        label = "recordWave"
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(bars) { i ->
            val t = phase.toDouble() * 2.0 * PI + i * 0.7
            val h = (0.5 + 0.5 * sin(t)).toFloat()
            Box(
                Modifier
                    .padding(horizontal = 1.5.dp)
                    .width(3.dp)
                    .height((7f + 24f * h).dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(color.copy(alpha = 0.35f + 0.65f * h))
            )
        }
    }
}

@Composable
internal fun RecordingOverlay() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.30f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            RecordingWaveform(Modifier.height(56.dp), bars = 32, color = Color.White)
            AppText("松开结束", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}


@Composable
fun LoadingScreen() {
    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            EmptyCard("正在准备词库……")
        }
    }
}
