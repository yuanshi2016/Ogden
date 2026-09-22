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
fun ReviewScreen(
    words: List<OgdenWord>,
    store: ProgressStore,
    padding: PaddingValues,
    onMistakes: () -> Unit,
    onFavorites: () -> Unit,
    onStartDueReview: () -> Unit,
    onBrowseDue: () -> Unit
) {
    val mistakes by remember(words, store) { derivedStateOf { store.mistakeWords(words) } }
    val favorites by remember(words, store) { derivedStateOf { store.favoriteWords(words) } }
    val due by remember(words, store) { derivedStateOf { store.dueForReview(words) } }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitle("复习中心", "智能队列 · 错词 · 收藏")
        }
        item {
            ReviewEntryCard(
                title = "智能复习",
                subtitle = if (due.isEmpty()) "先去闯几关，再回来巩固" else "最久未答、未满星的词优先",
                count = due.size,
                tint = Listening,
                icon = Icons.Default.School,
                onClick = if (due.isEmpty()) onBrowseDue else onStartDueReview
            )
        }
        if (due.isNotEmpty()) {
            item {
                OutlinedButton(
                    onClick = onBrowseDue,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    AppText("先浏览队列（${due.size}）")
                }
            }
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
    val (pressInteraction, pressIndication, pressModifier) = rememberPressScale()
    Card(
        modifier = pressModifier
            .fillMaxWidth()
            .clickable(interactionSource = pressInteraction, indication = pressIndication, onClick = onClick)
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
    onOpen: (OgdenWord, List<OgdenWord>) -> Unit,
    onPractice: (() -> Unit)? = null
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
                if (onPractice != null) {
                    item {
                        Button(
                            onClick = onPractice,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            AppText("开始练习", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                items(words, key = { it.word }) { word ->
                    CompactWordRow(word, store.progress(word.word), onOpen = { onOpen(word, words) })
                }
            }
        }
    }
}
