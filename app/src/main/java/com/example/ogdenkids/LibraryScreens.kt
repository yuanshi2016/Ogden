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
    // 输入框即时响应，过滤延迟 250ms，避免逐字符遍历 1203 词
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
        // 搜索与筛选常驻在列表之上，不随 1203 行滚走
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
                FilterChip(selected = category == null, onClick = { category = null }, label = { Text("All · ${words.size}") })
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
                                Icon(Icons.Default.VolumeUp, contentDescription = "读单词", tint = Listening)
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
