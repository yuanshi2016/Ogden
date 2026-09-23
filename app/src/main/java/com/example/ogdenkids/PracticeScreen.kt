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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    allWords: List<OgdenWord>,
    category: Category,
    level: Int,
    examCount: Int = 0,
    wordKeys: List<String> = emptyList(),
    sessionTitle: String = "",
    /** 非空表示课本单元练习：走 60% 过关门槛，通过后由调用方写单元进度 */
    unitId: String = "",
    speakLevel: SpeakLevel,
    rewards: Map<Difficulty, String>,
    onSpeak: (String) -> Unit,
    onBack: () -> Unit,
    onComplete: (Difficulty) -> Unit,
    /** 第二参数 correct；第三参数 SM-2 质量 0..5 */
    onRecord: (OgdenWord, Boolean, Int) -> Unit
) {
    val custom = wordKeys.isNotEmpty()
    val exam = level == 0 && !custom
    // 自选词表（错词/收藏/智能复习）本来不设门槛；课本单元要解锁下一单元，必须过 60%
    val gated = !exam && (!custom || unitId.isNotBlank())
    // 每次进入关卡（或重练）生成一个种子：决定题型顺序与干扰项，考试还会重抽词
    var attemptSeed by remember(allWords, category, level, examCount, wordKeys) { mutableStateOf(Random.nextInt()) }
    val source = remember(allWords, category, level, examCount, wordKeys, attemptSeed) {
        practiceWords(allWords, category, level, attemptSeed, examCount, wordKeys)
    }
    var difficulty by remember(source) { mutableStateOf<Difficulty?>(null) }
    var index by remember(source) { mutableStateOf(0) }
    var selected by remember(source) { mutableStateOf<String?>(null) }
    var typed by remember(source) { mutableStateOf("") }
    var speakCorrect by remember(source) { mutableStateOf(false) }
    var speakDetail by remember(source) { mutableStateOf("") }
    var speakSimilarity by remember(source) { mutableStateOf(0f) }
    var speakConfidence by remember(source) { mutableStateOf(0f) }
    var speakFeedbackGiven by remember(source) { mutableStateOf(false) }
    var speakFeedbackHint by remember(source) { mutableStateOf<String?>(null) }
    var speakRecording by remember(source) { mutableStateOf(false) }
    var answerShown by remember(source) { mutableStateOf(false) }
    val practiceContext = LocalContext.current
    val speakCalibration = remember(practiceContext) { SpeakCalibrationStore(practiceContext) }
    var correctCount by remember(source) { mutableStateOf(0) }
    var combo by remember(source) { mutableStateOf(0) }
    var celebrateTick by remember(source) { mutableStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val sfx = rememberSfx()
    val word = source.getOrNull(index)

    Box(Modifier.fillMaxSize()) {
    TechGlow(category.tint, Modifier.fillMaxSize())
    val headerTitle = when {
        sessionTitle.isNotBlank() -> sessionTitle
        exam -> "${category.zh} · 考试"
        else -> "${category.zh} · 第 $level 关"
    }
    // 顶栏：当前第几题（1-based），不是「已完成数」——第一题应是 1/10 而非 0/10
    val questionNo = (index + 1).coerceIn(1, source.size.coerceAtLeast(1))
    // 进度条：未作答只算到当前题之前；作答后含本题
    val progressDone = (index + if (answerShown) 1 else 0).coerceAtMost(source.size)
    Scaffold(containerColor = Paper, topBar = {
        SecondaryTopBar(onBack) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AppText(headerTitle, fontWeight = FontWeight.Bold, maxLines = 1)
                AppText(
                    "$questionNo / ${source.size} · 答对 $correctCount",
                    color = InkFaint,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }) { padding ->
        if (word == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyCard(if (custom) "这批词暂时练不了。" else "这一关没有词了。")
            }
            return@Scaffold
        }
        val currentDifficulty = difficulty
        if (currentDifficulty == null) {
            DifficultyPicker(
                rewards = rewards,
                onSelect = { difficulty = it },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            return@Scaffold
        }
        val types = remember(attemptSeed, currentDifficulty) { currentDifficulty.types.shuffled(Random(attemptSeed)) }
        val type = types[index % types.size]
        // 记忆题目，避免重组时选项重新洗牌
        val question = remember(word, level, index, attemptSeed) { buildQuestion(type, word, allWords, attemptSeed) }
        val isCorrect = when (type) {
            PracticeType.Speak -> speakCorrect
            else -> selected?.equals(question.answer, ignoreCase = true) == true
        }
        val lastQuestion = index >= source.lastIndex
        val needed = ceil(source.size * 0.6).toInt()
        val passed = correctCount >= needed

        LaunchedEffect(answerShown, lastQuestion, passed) {
            if (answerShown && lastQuestion && passed) {
                celebrateTick++
                sfx(if (correctCount == source.size) SfxCue.Perfect else SfxCue.Complete)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                // 细进度条按「已作答比例」；顶栏数字是当前题号
                PracticeThinProgress(
                    progress = if (source.isEmpty()) 0f else progressDone / source.size.toFloat(),
                    tint = category.tint
                )
            }
            item {
                PracticePromptCard(
                    category = word.category,
                    type = type,
                    prompt = question.prompt,
                    onSpeak = { onSpeak(word.word) }
                )
            }
            when (type) {
            PracticeType.Spelling -> {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { if (!answerShown) typed = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .answerShake(answerShown && !isCorrect),
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
                                if (ok) {
                                    correctCount++
                                    combo++
                                    sfx(comboCue(combo))
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } else {
                                    combo = 0
                                    sfx(SfxCue.Wrong)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onRecord(word, ok, if (ok) 4 else 1)
                                answerShown = true
                            }
                        },
                        enabled = !answerShown && typed.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .answerPop(answerShown && isCorrect),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        AppText("提交答案")
                    }
                }
            }
            }
            PracticeType.Speak -> {
            item {
                SpeakPracticeCard(
                    word = word,
                    speakLevel = speakLevel,
                    thresholds = speakCalibration.thresholdsFor(speakLevel),
                    onSpeak = onSpeak,
                    answered = answerShown,
                    onRecordingChange = { speakRecording = it },
                    onResult = { ok, heard, detail, sim, conf ->
                        speakCorrect = ok
                        selected = heard
                        speakDetail = detail
                        speakSimilarity = sim
                        speakConfidence = conf
                        speakFeedbackGiven = false
                        speakFeedbackHint = null
                        if (ok) {
                            correctCount++
                            combo++
                            sfx(comboCue(combo))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else {
                            combo = 0
                            sfx(SfxCue.Wrong)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onRecord(word, ok, com.example.ogdenkids.data.qualityFromSpeak(ok, sim))
                        answerShown = true
                    }
                )
            }
            }
            else -> {
            items(question.options) { option ->
                val selectedThis = selected == option
                val correctThis = answerShown && option == question.answer
                val wrongThis = answerShown && selectedThis && option != question.answer
                OutlinedButton(
                    onClick = {
                        if (!answerShown) {
                            selected = option
                            val ok = option == question.answer
                            if (ok) {
                                correctCount++
                                combo++
                                sfx(comboCue(combo))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } else {
                                combo = 0
                                sfx(SfxCue.Wrong)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onRecord(word, ok, if (ok) 4 else 1)
                            answerShown = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .answerPop(correctThis)
                        .answerShake(wrongThis),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
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
                        Text(option, modifier = Modifier.weight(1f), fontSize = 17.sp)
                        if (correctThis) Icon(Icons.Default.Check, contentDescription = null, tint = Success, modifier = Modifier.size(20.dp))
                        if (wrongThis) Icon(Icons.Default.Close, contentDescription = null, tint = Error, modifier = Modifier.size(20.dp))
                    }
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
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppText(if (isCorrect) "答对了！" else "这题先记到错词本", fontWeight = FontWeight.Bold, color = if (isCorrect) Success else Error)
                                if (isCorrect) ComboBadge(combo)
                            }
                            if (type == PracticeType.Spelling && !isCorrect) {
                                AppText("你写的是 ${selected.orEmpty()}，正确拼写是 ${word.word}", color = Error)
                            }
                            if (type == PracticeType.Speak) {
                                if (!isCorrect) {
                                    AppText("我听到你说的是 ${selected.orEmpty()}，目标是 ${word.word}", color = Error)
                                }
                                if (speakDetail.isNotBlank()) {
                                    AppText(speakDetail, color = InkFaint, fontSize = 13.sp)
                                }
                                if (!speakFeedbackGiven) {
                                    AppText("这次判定准不准？", color = InkSoft, fontSize = 13.sp)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // 机器判错了 → 用户认为应与机器相反
                                        OutlinedButton(
                                            onClick = {
                                                val calibrated = speakCalibration.recordFeedback(
                                                    SpeakFeedbackSample(
                                                        similarity = speakSimilarity,
                                                        confidence = speakConfidence,
                                                        userSaysShouldPass = !isCorrect,
                                                        level = speakLevel
                                                    )
                                                )
                                                speakFeedbackGiven = true
                                                speakFeedbackHint = if (calibrated) "已记下，并微调了阈值" else "已记下，谢谢反馈"
                                            },
                                            modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            AppText(
                                                // 不同意机器：过了→偏松；没过→其实对了/偏严
                                                if (isCorrect) "判松了" else "我其实对了",
                                                fontSize = 13.sp
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val calibrated = speakCalibration.recordFeedback(
                                                    SpeakFeedbackSample(
                                                        similarity = speakSimilarity,
                                                        confidence = speakConfidence,
                                                        userSaysShouldPass = isCorrect,
                                                        level = speakLevel
                                                    )
                                                )
                                                speakFeedbackGiven = true
                                                speakFeedbackHint = if (calibrated) "已记下，并微调了阈值" else "已记下，谢谢反馈"
                                            },
                                            modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            AppText(
                                                // 同意机器，或反过来表达「我其实错了」
                                                if (isCorrect) "判定合适" else "我其实错了",
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                } else {
                                    speakFeedbackHint?.let {
                                        AppText(it, color = Success, fontSize = 13.sp)
                                    }
                                }
                            }
                            Text("${word.word} · ${word.zh}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    word.example,
                                    fontFamily = FontFamily.Serif,
                                    color = InkSoft,
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { onSpeak(word.example) }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "读例句", tint = word.category.tint)
                                }
                            }
                            Text(word.exampleZh, color = InkSoft, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (lastQuestion && passed && gated && !custom) {
                                val prize = rewards[currentDifficulty].orEmpty().trim()
                                if (prize.isNotEmpty()) {
                                    AppText("闯关奖励：$prize", color = currentDifficulty.tint, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                            if (lastQuestion && exam) {
                                AppText("考试结束，答对 $correctCount / ${source.size}", color = InkSoft, fontSize = 14.sp)
                            }
                            if (lastQuestion && custom && unitId.isBlank()) {
                                AppText(
                                    "本轮结束，答对 $correctCount / ${source.size}。进度已记入掌握度。",
                                    color = InkSoft,
                                    fontSize = 14.sp
                                )
                            }
                            if (lastQuestion && passed && unitId.isNotBlank()) {
                                AppText(
                                    "本单元过关，答对 $correctCount / ${source.size}，下一单元已解锁。",
                                    color = InkSoft,
                                    fontSize = 14.sp
                                )
                            }
                            if (lastQuestion && !passed && gated) {
                                // 正确率不足 60%：不解锁下一关，只给返回或重练
                                AppText(
                                    "这一轮答对 $correctCount / ${source.size}，答对 $needed 个就能解锁下一${if (unitId.isNotBlank()) "单元" else "关"}。再练一次会换新题目，慢慢来。",
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
                                            speakCorrect = false
                                            speakDetail = ""
                                            speakSimilarity = 0f
                                            speakConfidence = 0f
                                            speakFeedbackGiven = false
                                            speakFeedbackHint = null
                                            answerShown = false
                                            correctCount = 0
                                            combo = 0
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
                                            // 只在过关时上报：未达 60% 的分支不会走到这里（gated），
                                            // 非 gated（考试/自选复习）保持原样照常上报
                                            if (!gated || passed) onComplete(currentDifficulty)
                                            onBack()
                                        } else {
                                            index++
                                            selected = null
                                            typed = ""
                                            speakCorrect = false
                                            speakDetail = ""
                                            speakSimilarity = 0f
                                            speakConfidence = 0f
                                            speakFeedbackGiven = false
                                            speakFeedbackHint = null
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
        ConfettiOverlay(tick = celebrateTick, modifier = Modifier.fillMaxSize())
        if (speakRecording) {
            RecordingOverlay()
        }
    }
}

@Composable
internal fun SpeakPracticeCard(
    word: OgdenWord,
    speakLevel: SpeakLevel,
    thresholds: SpeakThresholds? = null,
    onSpeak: (String) -> Unit,
    answered: Boolean,
    onRecordingChange: (Boolean) -> Unit,
    onResult: (Boolean, String, String, Float, Float) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var recording by remember(word) { mutableStateOf(false) }
    var processing by remember(word) { mutableStateOf(false) }
    var tooShort by remember(word) { mutableStateOf(false) }
    val recorder = remember { SpeechRecorder() }
    val modelStatus = WhisperEngine.status

    // 进入跟读就先触发模型预加载，拷贝/加载进度经 WhisperEngine.status 反映到界面
    LaunchedEffect(Unit) { WhisperEngine.ensureLoaded(context) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Log.d("OgdenSpeak", "麦克风授权结果: granted=$granted")
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                word.word,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = word.category.tint
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(word.zh, color = InkSoft, fontSize = 16.sp)
                IconButton(onClick = { onSpeak(word.word) }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "播放示范", tint = Listening)
                }
            }
        }
        when (val s = modelStatus) {
            is WhisperStatus.Copying -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppText("正在准备语音模型 ${(s.progress * 100).toInt()}%", color = InkSoft)
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)).background(Line)) {
                        Box(
                            Modifier
                                .fillMaxWidth(s.progress.coerceIn(0f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(Listening)
                        )
                    }
                }
            }
            is WhisperStatus.Downloading -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppText("正在下载语音模型 ${(s.progress * 100).toInt()}%", color = InkSoft)
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)).background(Line)) {
                        Box(
                            Modifier
                                .fillMaxWidth(s.progress.coerceIn(0f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(Listening)
                        )
                    }
                }
            }
            WhisperStatus.Initializing -> {
                AppText("正在加载模型…", color = InkSoft)
            }
            is WhisperStatus.Failed -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    AppText("语音模型加载失败：${s.message}", color = Error)
                    Button(
                        onClick = { scope.launch { WhisperEngine.ensureLoaded(context) } },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        AppText("重试")
                    }
                }
            }
            else -> {
                // 按住说话：手指按下开始录音，松开立即识别并判定
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (recording) Color(0xFF2E6FDB) else Listening)
                        .pointerInput(answered, modelStatus, word) {
                            detectTapGestures(onPress = {
                                try {
                                    var started = false
                                    tooShort = false
                                    Log.d("OgdenSpeak", "按下：answered=$answered processing=$processing status=$modelStatus")
                                    if (!answered && !processing && modelStatus == WhisperStatus.Ready) {
                                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                        Log.d("OgdenSpeak", "麦克风权限: granted=$granted")
                                        if (granted) {
                                            started = recorder.start()
                                            recording = started
                                            if (started) onRecordingChange(true)
                                            Log.d("OgdenSpeak", "recorder.start 返回 started=$started")
                                        } else {
                                            Log.d("OgdenSpeak", "无权限，发起授权请求")
                                            launcher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                    tryAwaitRelease()
                                    if (started) {
                                        recording = false
                                        onRecordingChange(false)
                                        val samples = recorder.stop()
                                        Log.d("OgdenSpeak", "松开：采集样本数=${samples?.size ?: 0}")
                                        // 少于 250ms 当成误触，提示重按，不拿去识别
                                        if (samples == null || samples.size < 4000) {
                                            tooShort = true
                                        } else {
                                            scope.launch {
                                                processing = true
                                                Log.d("OgdenSpeak", "开始识别，样本数=${samples.size}")
                                                val result = WhisperEngine.transcribe(context, samples, word.word)
                                                processing = false
                                                val assessment = assessPronunciation(
                                                    word.word,
                                                    result.text,
                                                    result.confidence,
                                                    speakLevel,
                                                    thresholds
                                                )
                                                val detail = "相似度 ${(assessment.similarity * 100).toInt()}% · 置信度 ${(result.confidence * 100).toInt()}%"
                                                Log.d("OgdenSpeak", "识别文本=\"${result.text}\" 目标=${word.word} 相似度=${assessment.similarity} 置信度=${result.confidence} 判定=${assessment.passed}")
                                                onResult(assessment.passed, result.text, detail, assessment.similarity, result.confidence)
                                            }
                                        }
                                    }
                                } catch (t: Throwable) {
                                    Log.e("OgdenSpeak", "onPress 异常: ${t.javaClass.name}: ${t.message}", t)
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
                                if (processing) "识别中…" else "按住说话",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (tooShort) {
                    AppText("按住说话，读完再松开", color = Error, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun PracticeThinProgress(progress: Float, tint: Color) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "practiceProgress"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Line)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(3.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(tint)
        )
    }
}

/**
 * 紧凑题干：听音把「说明 + 播放」合成一块可点区域，其它题型题面 + 小播放，尽量一屏放下选项。
 */
@Composable
private fun PracticePromptCard(
    category: Category,
    type: PracticeType,
    prompt: String,
    onSpeak: () -> Unit
) {
    val (pressInteraction, pressIndication, pressModifier) = rememberPressScale(scaleTo = 0.98f)
    Card(
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(category.tint)
                )
                AppText(type.title, color = category.tint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                AppText(category.zh, color = InkFaint, fontSize = 12.sp)
            }

            when (type) {
                PracticeType.Listen -> {
                    // 整块可点：一次完成「听题」引导，省掉第二块按钮
                    Row(
                        modifier = pressModifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Listening.copy(alpha = 0.10f))
                            .border(1.dp, Listening.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = pressInteraction,
                                indication = pressIndication,
                                onClick = onSpeak
                            )
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "播放题目",
                            tint = Listening,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            AppText("听声音，选出正确单词", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            AppText("点这里播放", color = InkFaint, fontSize = 12.sp)
                        }
                    }
                }
                PracticeType.Spelling -> {
                    Text(
                        text = prompt,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 26.sp,
                        color = Ink,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    AppText("输入英文，不区分大小写", color = InkFaint, fontSize = 12.sp)
                    CompactSpeakChip(label = "播放发音", onSpeak = onSpeak, pressModifier = pressModifier, pressInteraction = pressInteraction, pressIndication = pressIndication)
                }
                else -> {
                    Text(
                        text = prompt,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = if (prompt.length > 24) 20.sp else 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = if (prompt.length > 24) 26.sp else 28.sp,
                        color = Ink,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    CompactSpeakChip(label = "播放发音", onSpeak = onSpeak, pressModifier = pressModifier, pressInteraction = pressInteraction, pressIndication = pressIndication)
                }
            }
        }
    }
}

@Composable
private fun CompactSpeakChip(
    label: String,
    onSpeak: () -> Unit,
    pressModifier: Modifier,
    pressInteraction: MutableInteractionSource,
    pressIndication: Indication?
) {
    Row(
        modifier = pressModifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Listening.copy(alpha = 0.08f))
            .border(1.dp, Listening.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = pressInteraction,
                indication = pressIndication,
                onClick = onSpeak
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.VolumeUp, contentDescription = label, tint = Listening, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        AppText(label, color = Listening, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
internal fun DifficultyPicker(rewards: Map<Difficulty, String>, onSelect: (Difficulty) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PaperElevated),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Line, RoundedCornerShape(18.dp))
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppText("选择难度", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                AppText("简单认词，困难要拼写，跟读练发音", color = InkFaint, fontSize = 13.sp)
            }
        }
        Difficulty.values().forEach { d ->
            val (interaction, indication, press) = rememberPressScale()
            Card(
                colors = CardDefaults.cardColors(containerColor = PaperElevated),
                shape = RoundedCornerShape(16.dp),
                modifier = press
                    .fillMaxWidth()
                    .clickable(interactionSource = interaction, indication = indication, onClick = { onSelect(d) })
                    .border(1.dp, Line, RoundedCornerShape(16.dp))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(d.tint))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AppText(d.title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        AppText(d.types.joinToString(" · ") { it.title }, color = InkFaint, fontSize = 12.sp)
                        val prize = rewards[d].orEmpty().trim()
                        if (prize.isNotEmpty()) {
                            AppText("奖励：$prize", color = d.tint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    AppText("开始 →", color = d.tint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
