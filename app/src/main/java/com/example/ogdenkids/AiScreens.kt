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
fun AiTab(
    apiKey: String,
    focusWords: List<String>,
    speak: (String) -> Unit,
    padding: PaddingValues,
    onOpenSettings: () -> Unit
) {
    var mode by rememberSaveable { mutableStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "AI 学习助手",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = Ink
                )
                AppText("口语对练 · 拍照答疑，需要联网", color = InkFaint, fontSize = 13.sp)
            }
        }
        if (focusWords.isNotEmpty() && mode == 0) {
            AppText(
                "本轮会优先用你的待复习词：${focusWords.take(8).joinToString(" · ")}${if (focusWords.size > 8) "…" else ""}",
                color = InkSoft,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PaperElevated)
                .border(1.dp, Line)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TogglePill("口语对练", mode == 0, { mode = 0 }, Modifier.weight(1f))
            TogglePill("拍照答疑", mode == 1, { mode = 1 }, Modifier.weight(1f))
        }
        if (apiKey.isBlank()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EmptyCard("还没设置 DeepSeek API Key")
                Button(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    AppText("去设置里填写")
                }
            }
        } else {
            if (mode == 0) AiTalkCard(apiKey, focusWords, speak) else AiVisionCard(apiKey, speak)
        }
    }
}

private data class ChatMsg(val role: String, val text: String)

@Composable
internal fun AiTalkCard(apiKey: String, focusWords: List<String>, speak: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusAddon = remember(focusWords) { tutorFocusAddon(focusWords) }
    val opening = remember(focusWords) {
        if (focusWords.isEmpty()) {
            "Hi! I am your English friend. What is your name?\n（你好！我是你的英语朋友，你叫什么名字？）"
        } else {
            val sample = focusWords.take(3).joinToString(", ")
            "Hi! Today we can practice words like $sample. Shall we start?\n（你好！今天我们可以练习像 $sample 这些词，开始吧？）"
        }
    }
    val messages = remember {
        mutableStateListOf(ChatMsg("assistant", opening))
    }
    var thinking by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    var tooShort by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val recorder = remember { SpeechRecorder() }
    val modelStatus = WhisperEngine.status
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { WhisperEngine.ensureLoaded(context) }
    LaunchedEffect(messages.size) { listState.animateScrollToItem(messages.size - 1) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages.size) { index ->
                val msg = messages[index]
                if (msg.role == "user") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Surface(
                            color = Listening,
                            shape = RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp),
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            AppText(msg.text, color = Color.White, modifier = Modifier.padding(12.dp), lineHeight = 22.sp)
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.widthIn(max = 340.dp)) {
                        Surface(
                            color = PaperElevated,
                            shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
                            border = BorderStroke(1.dp, Line)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                msg.text.split("\n").filter { it.isNotBlank() }.forEachIndexed { i, line ->
                                    AppText(
                                        line.trim(),
                                        color = if (i == 0) Ink else InkSoft,
                                        fontSize = if (i == 0) 15.sp else 13.sp,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                        TextButton(
                            onClick = {
                                val english = msg.text.split("\n").firstOrNull { it.isNotBlank() } ?: msg.text
                                speak(english)
                            }
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "播放", modifier = Modifier.size(16.dp), tint = Listening)
                            Spacer(Modifier.width(4.dp))
                            AppText("播放", fontSize = 13.sp)
                        }
                    }
                }
            }
            if (thinking) item { AppText("AI 正在思考…", color = InkSoft, fontSize = 13.sp) }
            errorText?.let { item { AppText(it, color = Error, fontSize = 13.sp) } }
        }

        when (val status = modelStatus) {
            is WhisperStatus.Copying -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppText("正在准备语音模型 ${(status.progress * 100).toInt()}%", color = InkSoft)
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)).background(Line)) {
                        Box(
                            Modifier
                                .fillMaxWidth(status.progress.coerceIn(0f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(Listening)
                        )
                    }
                }
            }
            is WhisperStatus.Downloading -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppText("正在下载语音模型 ${(status.progress * 100).toInt()}%", color = InkSoft)
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)).background(Line)) {
                        Box(
                            Modifier
                                .fillMaxWidth(status.progress.coerceIn(0f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(Listening)
                        )
                    }
                }
            }
            WhisperStatus.Initializing -> AppText("正在加载模型…", color = InkSoft)
            is WhisperStatus.Failed -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppText("语音模型加载失败：${status.message}", color = Error)
                    Button(
                        onClick = { scope.launch { WhisperEngine.ensureLoaded(context) } },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        AppText("重试")
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (recording) Color(0xFF2E6FDB) else Listening)
                        .pointerInput(modelStatus, thinking, processing) {
                            detectTapGestures(onPress = {
                                try {
                                    var started = false
                                    tooShort = false
                                    if (!thinking && !processing && modelStatus == WhisperStatus.Ready) {
                                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            started = recorder.start()
                                            recording = started
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                    tryAwaitRelease()
                                    if (started) {
                                        recording = false
                                        val samples = recorder.stop()
                                        if (samples == null || samples.size < 4000) {
                                            tooShort = true
                                        } else {
                                            scope.launch {
                                                processing = true
                                                errorText = null
                                                val result = WhisperEngine.transcribe(context, samples, "")
                                                processing = false
                                                val spoken = result.text.trim()
                                                if (spoken.isEmpty()) {
                                                    errorText = "没有听清，再试一次？"
                                                    return@launch
                                                }
                                                messages.add(ChatMsg("user", spoken))
                                                thinking = true
                                                val reply = try {
                                                    DeepSeekClient.chat(
                                                        apiKey,
                                                        messages.takeLast(12).map { it.role to it.text },
                                                        focusAddon = focusAddon
                                                    )
                                                } catch (e: Exception) {
                                                    errorText = "AI 请求失败：${e.message}"
                                                    null
                                                }
                                                thinking = false
                                                if (reply != null) {
                                                    messages.add(ChatMsg("assistant", reply))
                                                    val english = reply.split("\n").firstOrNull { it.isNotBlank() } ?: reply
                                                    speak(english)
                                                }
                                            }
                                        }
                                    }
                                } catch (t: Throwable) {
                                    Log.e("OgdenAi", "onPress 异常: ${t.javaClass.name}: ${t.message}", t)
                                }
                            })
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (recording) {
                            RecordingWaveform(Modifier.height(34.dp))
                            Spacer(Modifier.width(10.dp))
                            AppText("松开结束", color = Color.White, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            AppText(
                                when {
                                    thinking -> "AI 思考中…"
                                    processing -> "识别中…"
                                    else -> "按住说话"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (tooShort) {
                    AppText("按住说话，说完再松开", color = Error, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
internal fun AiVisionCard(apiKey: String, speak: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var answer by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            errorText = null
            answer = null
            try {
                val (mime, bytes) = withContext(Dispatchers.IO) {
                    val type = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val output = ByteArrayOutputStream()
                    context.contentResolver.openInputStream(uri)?.use { it.copyTo(output) }
                    type to output.toByteArray()
                }
                answer = DeepSeekClient.describeImage(apiKey, bytes, mime)
            } catch (e: Exception) {
                errorText = "AI 请求失败：${e.message}"
            }
            busy = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppText(
            "拍下作业题目，AI 老师讲解思路。图片仅发给 DeepSeek，不会保存在手机或服务器上。",
            color = InkSoft,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
        Button(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            AppText(if (busy) "AI 正在读题…" else "选择题目图片", fontWeight = FontWeight.Bold)
        }
        when {
            busy -> AppText("正在识别题目，请稍候…", color = InkSoft)
            answer != null -> Card(
                colors = CardDefaults.cardColors(containerColor = PaperElevated),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Line, RoundedCornerShape(18.dp))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppText("AI 老师的讲解", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (answer?.any { it in '\u4e00'..'\u9fff' } == false) {
                            TextButton(onClick = { speak(answer ?: "") }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "播放", modifier = Modifier.size(16.dp), tint = Listening)
                                Spacer(Modifier.width(4.dp))
                                AppText("播放", fontSize = 13.sp)
                            }
                        }
                    }
                    AppText(answer ?: "", lineHeight = 24.sp)
                }
            }
            errorText != null -> AppText(errorText ?: "", color = Error, fontSize = 13.sp)
        }
    }
}
