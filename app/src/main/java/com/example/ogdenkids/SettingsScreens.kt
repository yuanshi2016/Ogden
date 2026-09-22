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

data class LegalSection(val heading: String, val body: String)

@Composable
fun SettingsScreen(
    accent: Accent,
    onAccent: (Accent) -> Unit,
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    speakLevel: SpeakLevel,
    onSpeakLevel: (SpeakLevel) -> Unit,
    aiKey: String,
    onAiKey: (String) -> Unit,
    parentPin: String,
    onParentPin: (String) -> Unit,
    reviewReminderEnabled: Boolean,
    onReviewReminderEnabled: (Boolean) -> Unit,
    reviewReminderHour: Int,
    onReset: () -> Unit,
    onExport: suspend () -> String,
    onImport: suspend (String) -> Unit,
    onBack: () -> Unit,
    onPrivacy: () -> Unit,
    onAbout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var resetPrompt by remember { mutableStateOf(false) }
    var resetDone by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var pinDialog by remember { mutableStateOf(false) }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var pinDialogError by remember { mutableStateOf(false) }
    var pinSaved by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var backupBusy by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var importConfirm by remember { mutableStateOf(false) }
    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var reminderMessage by remember { mutableStateOf<String?>(null) }
    /** null=无；true=待开启；false=待关闭（需家长 PIN） */
    var pendingReminderToggle by remember { mutableStateOf<Boolean?>(null) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onReviewReminderEnabled(true)
            reminderMessage = "已开启每日 ${reviewReminderHour}:00 复习提醒"
        } else {
            reminderMessage = "未授予通知权限，提醒无法显示"
        }
    }

    fun tryEnableReminder() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onReviewReminderEnabled(true)
            reminderMessage = "已开启每日 ${reviewReminderHour}:00 复习提醒"
        }
    }
    // 已设 parentPin 时改 AI Key / 跟读严格度 / 复习提醒需先验证（与重置进度同一套 4 位对话框）
    var gatePrompt by remember { mutableStateOf(false) }
    var gatePin by remember { mutableStateOf("") }
    var gatePinError by remember { mutableStateOf(false) }
    var gateUnlocked by remember { mutableStateOf(false) }
    var pendingSpeakLevel by remember { mutableStateOf<SpeakLevel?>(null) }
    var draftAiKey by remember(aiKey) { mutableStateOf(aiKey) }
    val settingsLocked = parentPin.isNotBlank() && !gateUnlocked

    fun requestParentGate(onUnlocked: () -> Unit = {}) {
        if (!settingsLocked) {
            onUnlocked()
            return
        }
        gatePrompt = true
        gatePin = ""
        gatePinError = false
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingExport
        pendingExport = null
        if (uri == null || json == null) {
            if (uri == null) backupMessage = "已取消导出"
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            backupBusy = true
            backupMessage = try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("无法写入文件")
                }
                "进度已导出"
            } catch (e: Exception) {
                "导出失败：${e.message}"
            }
            backupBusy = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            backupMessage = "已取消导入"
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            backupBusy = true
            try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: error("无法读取文件")
                }
                // 先解码校验，再弹确认
                decodeProgressSnapshot(text)
                pendingImportJson = text
                importConfirm = true
                backupMessage = null
            } catch (e: Exception) {
                backupMessage = "导入失败：${e.message}"
            }
            backupBusy = false
        }
    }
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
                SectionTitle("主题模式", "儿童大按钮更鲜艳 · 成人更紧凑，可跟随系统深色")
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
                        ThemeModeToggleRow(mode = themeMode, onMode = onThemeMode)
                    }
                }
            }
            item {
                SectionTitle(
                    "跟读判定",
                    if (settingsLocked) "已设家长密码，修改严格度需先验证"
                    else "严格度越高，越要求读得标准、识别置信度越高"
                )
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
                        SpeakLevelToggleRow(
                            level = speakLevel,
                            onLevel = { next ->
                                if (settingsLocked) {
                                    pendingSpeakLevel = next
                                    requestParentGate()
                                } else {
                                    onSpeakLevel(next)
                                }
                            }
                        )
                    }
                }
            }
            item {
                SectionTitle(
                    "每日复习提醒",
                    if (settingsLocked) "已设家长密码，开启/关闭需先验证"
                    else "本地时区每天 ${reviewReminderHour}:00 提醒复习到期单词"
                )
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                AppText("开启每日复习提醒", fontWeight = FontWeight.Bold)
                                AppText(
                                    if (reviewReminderEnabled) "已开启 · 每天 ${reviewReminderHour}:00"
                                    else "默认关闭；开启后需通知权限",
                                    color = InkSoft,
                                    fontSize = 13.sp
                                )
                            }
                            Switch(
                                checked = reviewReminderEnabled,
                                onCheckedChange = { wantOn ->
                                    if (wantOn == reviewReminderEnabled) return@Switch
                                    if (settingsLocked) {
                                        pendingReminderToggle = wantOn
                                        requestParentGate()
                                        return@Switch
                                    }
                                    if (wantOn) tryEnableReminder()
                                    else {
                                        onReviewReminderEnabled(false)
                                        reminderMessage = "已关闭复习提醒"
                                    }
                                }
                            )
                        }
                        reminderMessage?.let {
                            AppText(it, color = Success, fontSize = 13.sp)
                        }
                    }
                }
            }
            item {
                SectionTitle(
                    "AI 助手",
                    if (settingsLocked) "已设家长密码，修改 API Key 需先验证"
                    else "口语对练与拍照答疑需要 DeepSeek API Key"
                )
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
                            "Key 经本机加密存储（EncryptedSharedPreferences），用于 AI 页面的语音对话与题目讲解。可在 platform.deepseek.com 获取。",
                            color = InkSoft,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                        OutlinedTextField(
                            value = draftAiKey,
                            onValueChange = { next ->
                                if (settingsLocked) {
                                    draftAiKey = aiKey
                                    requestParentGate()
                                } else {
                                    draftAiKey = next
                                    onAiKey(next)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            readOnly = settingsLocked,
                            visualTransformation = PasswordVisualTransformation(),
                            placeholder = { AppText("sk-…", color = InkFaint) }
                        )
                        if (settingsLocked) {
                            OutlinedButton(
                                onClick = { requestParentGate() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                AppText("验证家长密码后修改")
                            }
                        }
                    }
                }
            }
            item {
                SectionTitle("家长密码", "重置进度、修改密码、改 AI Key / 跟读严格度 / 复习提醒都需要它")
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
                            when {
                                pinSaved -> "家长密码已更新。"
                                parentPin.isBlank() -> "还没设置家长密码，第一次重置进度时会要求先设置。"
                                else -> "已设置家长密码，修改需要先输入当前密码。"
                            },
                            color = if (pinSaved) Success else InkSoft,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                        OutlinedButton(
                            onClick = {
                                pinDialog = true
                                currentPin = ""
                                newPin = ""
                                pinDialogError = false
                                pinSaved = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            AppText(if (parentPin.isBlank()) "设置家长密码" else "修改家长密码")
                        }
                    }
                }
            }
            item {
                SectionTitle("学习数据", "备份、恢复与重置（不含 API Key / 家长密码）")
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
                            backupMessage
                                ?: if (resetDone) "学习进度已清空，发音设置保持不变。"
                                else "导出可换机恢复；导入会覆盖当前学习记录。API Key 与家长密码不会写入备份。",
                            color = when {
                                backupMessage?.startsWith("导出失败") == true ||
                                    backupMessage?.startsWith("导入失败") == true -> Error
                                backupMessage != null -> Success
                                resetDone -> Success
                                else -> InkSoft
                            },
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                        Button(
                            onClick = {
                                if (backupBusy) return@Button
                                scope.launch {
                                    backupBusy = true
                                    backupMessage = null
                                    try {
                                        val json = onExport()
                                        pendingExport = json
                                        exportLauncher.launch("ogden-progress-backup.json")
                                    } catch (e: Exception) {
                                        backupMessage = "导出失败：${e.message}"
                                    }
                                    backupBusy = false
                                }
                            },
                            enabled = !backupBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            AppText(if (backupBusy) "处理中…" else "导出学习进度")
                        }
                        OutlinedButton(
                            onClick = {
                                if (!backupBusy) importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            },
                            enabled = !backupBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            AppText("导入学习进度")
                        }
                        OutlinedButton(
                            onClick = {
                                resetPrompt = true
                                pin = ""
                                pinError = false
                            },
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
    if (resetPrompt) {
        AlertDialog(
            onDismissRequest = { resetPrompt = false },
            containerColor = PaperElevated,
            shape = RoundedCornerShape(18.dp),
            title = { AppText(if (parentPin.isBlank()) "设置家长密码" else "输入家长密码", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppText(
                        if (parentPin.isBlank()) "首次重置：请先设置 4 位数字家长密码，设置后将立即清空学习记录。" else "重置会清空全部学习记录，请输入 4 位数字密码。",
                        color = InkSoft,
                        lineHeight = 22.sp
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            pin = it.filter { c -> c.isDigit() }.take(4)
                            pinError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = pinError,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { AppText("4 位数字密码") }
                    )
                    if (pinError) AppText(if (parentPin.isBlank()) "请输入 4 位数字密码。" else "密码错误，请重试。", color = Error, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (parentPin.isBlank()) {
                            if (pin.length == 4) {
                                onParentPin(pin)
                                onReset()
                                resetDone = true
                                resetPrompt = false
                            } else {
                                pinError = true
                            }
                        } else if (pin == parentPin) {
                            onReset()
                            resetDone = true
                            resetPrompt = false
                        } else {
                            pinError = true
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Error, contentColor = Paper)
                ) {
                    AppText(if (parentPin.isBlank()) "设置并重置" else "确认重置")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { resetPrompt = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    AppText("取消", color = InkSoft)
                }
            }
        )
    }
    if (gatePrompt) {
        AlertDialog(
            onDismissRequest = {
                gatePrompt = false
                pendingSpeakLevel = null
                pendingReminderToggle = null
            },
            containerColor = PaperElevated,
            shape = RoundedCornerShape(18.dp),
            title = { AppText("输入家长密码", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppText(
                        "修改 AI Key、跟读严格度或复习提醒需要家长密码。",
                        color = InkSoft,
                        lineHeight = 22.sp
                    )
                    OutlinedTextField(
                        value = gatePin,
                        onValueChange = {
                            gatePin = it.filter { c -> c.isDigit() }.take(4)
                            gatePinError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = gatePinError,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { AppText("4 位数字密码") }
                    )
                    if (gatePinError) AppText("密码错误，请重试。", color = Error, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (gatePin == parentPin) {
                            gateUnlocked = true
                            gatePrompt = false
                            pendingSpeakLevel?.let { onSpeakLevel(it) }
                            pendingSpeakLevel = null
                            when (pendingReminderToggle) {
                                true -> tryEnableReminder()
                                false -> {
                                    onReviewReminderEnabled(false)
                                    reminderMessage = "已关闭复习提醒"
                                }
                                null -> Unit
                            }
                            pendingReminderToggle = null
                        } else {
                            gatePinError = true
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    AppText("确认")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        gatePrompt = false
                        pendingSpeakLevel = null
                        pendingReminderToggle = null
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    AppText("取消", color = InkSoft)
                }
            }
        )
    }
    if (importConfirm) {
        AlertDialog(
            onDismissRequest = {
                importConfirm = false
                pendingImportJson = null
            },
            containerColor = PaperElevated,
            shape = RoundedCornerShape(18.dp),
            title = { AppText("确认导入？", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                AppText(
                    "将用备份覆盖本机的掌握度、错词、收藏、关卡与连续天数。发音、主题、API Key 与家长密码不受影响。此操作无法撤销。",
                    color = InkSoft,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val json = pendingImportJson
                        importConfirm = false
                        pendingImportJson = null
                        if (json == null) return@Button
                        scope.launch {
                            backupBusy = true
                            backupMessage = try {
                                onImport(json)
                                "进度已导入"
                            } catch (e: Exception) {
                                "导入失败：${e.message}"
                            }
                            backupBusy = false
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    AppText("确认导入")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        importConfirm = false
                        pendingImportJson = null
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    AppText("取消", color = InkSoft)
                }
            }
        )
    }
    if (pinDialog) {
        AlertDialog(
            onDismissRequest = { pinDialog = false },
            containerColor = PaperElevated,
            shape = RoundedCornerShape(18.dp),
            title = { AppText(if (parentPin.isBlank()) "设置家长密码" else "修改家长密码", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (parentPin.isNotBlank()) {
                        OutlinedTextField(
                            value = currentPin,
                            onValueChange = {
                                currentPin = it.filter { c -> c.isDigit() }.take(4)
                                pinDialogError = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = pinDialogError,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { AppText("当前密码") }
                        )
                    }
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = {
                            newPin = it.filter { c -> c.isDigit() }.take(4)
                            pinDialogError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = pinDialogError,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { AppText("新的 4 位数字密码") }
                    )
                    if (pinDialogError) {
                        AppText(
                            if (parentPin.isNotBlank() && currentPin != parentPin) "当前密码错误。" else "请输入新的 4 位数字密码。",
                            color = Error,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ok = newPin.length == 4 && (parentPin.isBlank() || currentPin == parentPin)
                        if (ok) {
                            onParentPin(newPin)
                            pinSaved = true
                            pinDialog = false
                        } else {
                            pinDialogError = true
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    AppText("保存")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pinDialog = false },
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
        "网络访问权限：用于在线发音备用、以及可选的 AI 口语对练 / 拍照答疑（需你自行填写 DeepSeek API Key）。麦克风：仅在跟读练习与 AI 口语对练时，经你授权后用于本地语音识别。相册读取：仅在你主动选择「拍照答疑」图片时使用。本地存储：保存学习进度、收藏、错词与熟练度。"
    ),
    LegalSection(
        "不会进行的行为",
        "本应用不会收集姓名、手机号、邮箱、账号、定位等个人身份信息；不会读取通讯录、短信；不会追踪用户用于广告或商业分析；不会向第三方共享学习记录。AI 功能仅在你配置 Key 并主动使用时，将对话或所选图片发往 DeepSeek，Key 与学习进度默认只留在本机。"
    ),
    LegalSection(
        "适用范围",
        "本应用适合希望学习 Ogden Basic English 850 词的用户使用，不要求提供个人信息，也不会主动收集身份、位置、联系方式或其他敏感数据。"
    ),
    LegalSection(
        "本地数据与删除",
        "学习记录、收藏、错词和熟练度均保存在本机。可在设置中导出 / 导入学习进度备份（不含 API Key 与家长密码），或通过重置进度、清除应用数据、卸载应用删除本地数据。"
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
