package com.example.ogdenkids.curriculum

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ogdenkids.AppText
import com.example.ogdenkids.Error
import com.example.ogdenkids.Ink
import com.example.ogdenkids.InkSoft
import com.example.ogdenkids.Listening
import com.example.ogdenkids.Line
import com.example.ogdenkids.SpeakLevel
import com.example.ogdenkids.Success
import com.example.ogdenkids.assessPronunciation
import com.example.ogdenkids.speech.SpeechRecorder
import com.example.ogdenkids.speech.WhisperEngine
import com.example.ogdenkids.speech.WhisperStatus
import kotlinx.coroutines.launch

/**
 * 单句跟读对话框：复用 Whisper + assessPronunciation，不复制 PracticeScreen。
 */
@Composable
fun PhraseSpeakDialog(
    phrase: CurriculumPhrase,
    speakLevel: SpeakLevel,
    onSpeak: (String) -> Unit,
    onDismiss: () -> Unit,
    onPassed: () -> Unit
) {
    var passed by remember(phrase.en) { mutableStateOf(false) }
    var detail by remember(phrase.en) { mutableStateOf<String?>(null) }
    var lastSpoken by remember(phrase.en) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("跟读句子", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(phrase.en, fontFamily = FontFamily.Serif, fontSize = 20.sp, color = Ink, fontWeight = FontWeight.Bold)
                AppText(phrase.zh, color = InkSoft, fontSize = 14.sp)
                PhraseSpeakPad(
                    targetEn = phrase.en,
                    speakLevel = speakLevel,
                    onSpeak = onSpeak,
                    answered = passed,
                    onResult = { ok, spoken, info, _, _ ->
                        lastSpoken = spoken
                        detail = info
                        if (ok) {
                            passed = true
                            onPassed()
                        }
                    }
                )
                if (detail != null) {
                    AppText(
                        if (passed) "真棒！$detail" else "再试一次：$detail",
                        color = if (passed) Success else Error,
                        fontSize = 13.sp
                    )
                    if (lastSpoken.isNotBlank()) {
                        AppText("你说的：$lastSpoken", color = InkSoft, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                AppText(if (passed) "完成" else "关闭")
            }
        }
    )
}

@Composable
fun PhraseSpeakPad(
    targetEn: String,
    speakLevel: SpeakLevel,
    onSpeak: (String) -> Unit,
    answered: Boolean,
    onResult: (Boolean, String, String, Float, Float) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var recording by remember(targetEn) { mutableStateOf(false) }
    var processing by remember(targetEn) { mutableStateOf(false) }
    var tooShort by remember(targetEn) { mutableStateOf(false) }
    val recorder = remember { SpeechRecorder() }
    val modelStatus = WhisperEngine.status

    LaunchedEffect(Unit) { WhisperEngine.ensureLoaded(context) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText("先听示范，再按住说话", color = InkSoft, fontSize = 13.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { onSpeak(targetEn) }) {
                Icon(Icons.Default.VolumeUp, contentDescription = "播放", tint = Listening)
            }
        }
        when (val s = modelStatus) {
            is WhisperStatus.Copying -> AppText("准备语音模型 ${(s.progress * 100).toInt()}%", color = InkSoft)
            is WhisperStatus.Downloading -> AppText("下载语音模型 ${(s.progress * 100).toInt()}%", color = InkSoft)
            WhisperStatus.Initializing -> AppText("正在加载模型…", color = InkSoft)
            is WhisperStatus.Failed -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppText("模型加载失败：${s.message}", color = Error, fontSize = 13.sp)
                    Button(onClick = { scope.launch { WhisperEngine.ensureLoaded(context) } }, shape = RoundedCornerShape(12.dp)) {
                        AppText("重试")
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (recording) Color(0xFF2E6FDB) else Listening)
                        .pointerInput(answered, modelStatus, targetEn) {
                            detectTapGestures(onPress = {
                                try {
                                    var started = false
                                    tooShort = false
                                    if (!answered && !processing && modelStatus == WhisperStatus.Ready) {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            started = recorder.start()
                                            recording = started
                                        } else {
                                            launcher.launch(Manifest.permission.RECORD_AUDIO)
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
                                                val result = WhisperEngine.transcribe(context, samples, targetEn)
                                                processing = false
                                                val assessment = assessPronunciation(
                                                    targetEn,
                                                    result.text,
                                                    result.confidence,
                                                    speakLevel
                                                )
                                                val info =
                                                    "相似度 ${(assessment.similarity * 100).toInt()}% · 置信度 ${(result.confidence * 100).toInt()}%"
                                                Log.d(
                                                    "OgdenPhraseSpeak",
                                                    "目标=$targetEn 识别=${result.text} sim=${assessment.similarity} pass=${assessment.passed}"
                                                )
                                                onResult(
                                                    assessment.passed,
                                                    result.text,
                                                    info,
                                                    assessment.similarity,
                                                    result.confidence
                                                )
                                            }
                                        }
                                    }
                                } catch (t: Throwable) {
                                    Log.e("OgdenPhraseSpeak", "跟读异常", t)
                                }
                            })
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        AppText(
                            when {
                                processing -> "识别中…"
                                recording -> "松开结束"
                                answered -> "已通过"
                                else -> "按住说话"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (tooShort) {
                    AppText("按住说话，读完再松开", color = Error, fontSize = 12.sp)
                }
            }
        }
    }
}
