package com.example.ogdenkids.curriculum

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ogdenkids.AppText
import com.example.ogdenkids.Ink
import com.example.ogdenkids.InkFaint
import com.example.ogdenkids.InkSoft
import com.example.ogdenkids.Line
import com.example.ogdenkids.OgdenWord
import com.example.ogdenkids.Paper
import com.example.ogdenkids.PaperElevated
import com.example.ogdenkids.Primary
import com.example.ogdenkids.ProgressStore
import com.example.ogdenkids.SecondaryTopBar
import com.example.ogdenkids.SectionTitle
import com.example.ogdenkids.SpeakLevel
import com.example.ogdenkids.Success
import com.example.ogdenkids.TogglePill
import com.example.ogdenkids.localEpochDayNow

// 课本轨道与基础词轨道并存：共享词级进度，关卡/单元进度各自独立
enum class LearningTrack(val label: String) { Ogden("基础词"), Pep("课本") }

private fun activityTypeLabel(type: String): String = when (type.lowercase()) {
    "chant" -> "🎵 歌谣"
    "roleplay" -> "🎭 角色扮演"
    "parent" -> "👨‍👩‍👧 亲子"
    "listen" -> "👂 听力"
    else -> "📝 活动"
}

/** 年级 → 上册/下册切换 + 周次建议 + 单元列表，嵌在闯关页 LazyColumn 里（自己不再套滚动容器）。 */
fun androidx.compose.foundation.lazy.LazyListScope.curriculumUnitItems(
    bundle: CurriculumBundle,
    store: ProgressStore,
    grade: Int,
    onGrade: (Int) -> Unit,
    volume: Int,
    onVolume: (Int) -> Unit,
    onOpenUnit: (String) -> Unit
) {
    val volumes = bundle.index.volumes
    val ordered = bundle.index.orderedUnitIds
    val byId = bundle.unitsById
    val suggestedWeek = store.pepSuggestedWeek()
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PEP_SUPPORTED_GRADES.forEach { g ->
                TogglePill(
                    "${g}年级",
                    grade == g,
                    { onGrade(g) },
                    Modifier.weight(1f)
                )
            }
        }
    }
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            volumes.forEach { vol ->
                TogglePill(vol.titleZh, volume == vol.volume, { onVolume(vol.volume) }, Modifier.weight(1f))
            }
        }
    }
    item {
        WeekHintBar(
            suggestedWeek = suggestedWeek,
            manualWeek = store.pepCurrentWeek(),
            onSetManualWeek = { store.savePepCurrentWeek(it) },
            onSetTermStartToday = {
                store.savePepTermStartDay(localEpochDayNow())
                store.savePepCurrentWeek(0)
            },
            onClear = {
                store.savePepCurrentWeek(0)
                store.savePepTermStartDay(0L)
            }
        )
    }
    item { SectionTitle("课本单元", "学完词句再闯关，过关解锁下一单元") }
    val units = volumes.firstOrNull { it.volume == volume }?.unitIds.orEmpty().mapNotNull { byId[it] }
    if (units.isEmpty()) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PaperElevated),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Line, RoundedCornerShape(16.dp))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppText("这一册的课本内容还没准备好", fontWeight = FontWeight.Bold)
                    AppText("内容补齐后会自动出现在这里", color = InkFaint, fontSize = 13.sp)
                }
            }
        }
        return
    }
    // 建议周匹配的单元置顶，其余保持册内顺序
    val sorted = units.sortedByDescending { unitMatchesWeek(it.weeksHint, suggestedWeek) }
    items(sorted, key = { it.id }) { unit ->
        val unlocked = store.isUnitUnlocked(ordered, unit.id)
        val suggested = unitMatchesWeek(unit.weeksHint, suggestedWeek)
        UnitCard(
            unit = unit,
            unlocked = unlocked,
            complete = store.isUnitComplete(unit.id),
            suggested = suggested,
            onClick = { if (unlocked) onOpenUnit(unit.id) }
        )
    }
}

@Composable
private fun WeekHintBar(
    suggestedWeek: Int?,
    manualWeek: Int,
    onSetManualWeek: (Int) -> Unit,
    onSetTermStartToday: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line, RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppText(
                if (suggestedWeek != null) "本学期建议 · 第 $suggestedWeek 周"
                else "本学期周次（可选）",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            AppText(
                "用来高亮对应单元，不锁进度。可选手动周，或把今天设为开学日自动推算。",
                color = InkFaint,
                fontSize = 12.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    val cur = (manualWeek.takeIf { it > 0 } ?: suggestedWeek ?: 1)
                    onSetManualWeek((cur - 1).coerceAtLeast(PEP_WEEK_MIN))
                }) { AppText("−周") }
                AppText(
                    when {
                        manualWeek > 0 -> "手动 第 $manualWeek 周"
                        suggestedWeek != null -> "自动 第 $suggestedWeek 周"
                        else -> "未设置"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    val cur = (manualWeek.takeIf { it > 0 } ?: suggestedWeek ?: 0)
                    onSetManualWeek((cur + 1).coerceAtMost(PEP_WEEK_MAX))
                }) { AppText("+周") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSetTermStartToday) {
                    AppText("今天=开学日", color = Primary, fontWeight = FontWeight.Bold)
                }
                if (manualWeek > 0 || suggestedWeek != null) {
                    TextButton(onClick = onClear) { AppText("清除", color = InkSoft) }
                }
            }
        }
    }
}

@Composable
private fun UnitCard(
    unit: CurriculumUnit,
    unlocked: Boolean,
    complete: Boolean,
    suggested: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                suggested && unlocked -> Color(0xFFFFF6E8)
                unlocked -> PaperElevated
                else -> Color(0xFFF2EDE4)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = unlocked, onClick = onClick)
            .border(
                width = if (suggested) 2.dp else 1.dp,
                color = if (suggested) Primary else if (unlocked) Line else Color(0xFFE8E1D4),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (complete) Primary else Primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    when {
                        complete -> "✓"
                        unit.id.endsWith(".rev") -> "R"
                        else -> unit.unit.toString()
                    },
                    color = if (complete) Paper else Primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText(
                        unit.titleZh.ifBlank { unit.titleEn },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (unlocked) Ink else InkFaint,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (suggested) {
                        Spacer(Modifier.width(6.dp))
                        AppText("本周", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                AppText(
                    when {
                        complete -> "已通关 · 可重复练习"
                        unlocked -> {
                            val hint = weekHintLabel(unit.weeksHint)
                            val base = "${unit.words.size} 词 · ${unit.phrases.size} 个句型"
                            if (hint.isNotEmpty()) "$base · $hint" else base
                        }
                        else -> "先完成上一个单元"
                    },
                    color = InkFaint,
                    fontSize = 13.sp
                )
            }
            AppText(if (unlocked) "进入" else "锁定", color = if (unlocked) Primary else InkFaint, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CurriculumUnitScreen(
    unit: CurriculumUnit,
    resolved: ResolvedUnitWords,
    store: ProgressStore,
    speakLevel: SpeakLevel,
    listState: LazyListState = rememberLazyListState(),
    onBack: () -> Unit,
    onSpeak: (String) -> Unit,
    /** unitLevel, wordKeys, title, examCount（0=全量；Revision 测评传固定题量） */
    onStartLevelPractice: (unitLevel: Int, wordKeys: List<String>, title: String, examCount: Int) -> Unit
) {
    LaunchedEffect(unit.id) { store.savePepLastUnitId(unit.id) }

    var speakTarget by remember { mutableStateOf<CurriculumPhrase?>(null) }
    var phraseGateOpen by remember { mutableStateOf(false) }
    var parentReportOpen by remember { mutableStateOf(false) }

    // unitLevels / dayChecks 为 SnapshotStateMap，读方法会订阅重组
    val wordsDone = store.isUnitLevelComplete(unit.id, UNIT_LEVEL_WORDS)
    val phrasesDone = store.isUnitLevelComplete(unit.id, UNIT_LEVEL_PHRASES)
    val mixedDone = store.isUnitLevelComplete(unit.id, UNIT_LEVEL_MIXED)
    val revisionExamDone = store.isUnitLevelComplete(unit.id, UNIT_LEVEL_REVISION_EXAM)
    val phrasesUnlocked = store.isUnitLevelUnlocked(unit.id, UNIT_LEVEL_PHRASES)
    val mixedUnlocked = store.isUnitLevelUnlocked(unit.id, UNIT_LEVEL_MIXED)
    val isRevision = isRevisionUnit(unit.id)
    val suggestedWeek = store.pepSuggestedWeek()
    val weekSuggested = unitMatchesWeek(unit.weeksHint, suggestedWeek)

    val listenDone = store.isDayCheckDone(unit.id, DAYCHECK_LISTEN_WORDS)
    val speakCheckDone = store.isDayCheckDone(unit.id, DAYCHECK_SPEAK_PHRASES)
    val passedPhraseCount = unit.phrases.indices.count { i ->
        store.isDayCheckDone(unit.id, dayCheckSpeakPhraseId(i))
    }
    val parentReport = remember(
        unit.id, wordsDone, phrasesDone, mixedDone,
        listenDone, speakCheckDone, passedPhraseCount,
        store.dayCheckItems(unit.id)
    ) {
        buildParentReport(unit, store.completedUnitLevels(unit.id), store.dayCheckItems(unit.id))
    }

    speakTarget?.let { phrase ->
        val idx = unit.phrases.indexOf(phrase)
        PhraseSpeakDialog(
            phrase = phrase,
            speakLevel = speakLevel,
            onSpeak = onSpeak,
            onDismiss = { speakTarget = null },
            onPassed = {
                if (idx >= 0) {
                    store.setDayCheckDone(unit.id, dayCheckSpeakPhraseId(idx), true)
                    val passed = unit.phrases.indices.count { store.isDayCheckDone(unit.id, dayCheckSpeakPhraseId(it)) }
                    if (passed >= 2) {
                        store.setDayCheckDone(unit.id, DAYCHECK_SPEAK_PHRASES, true)
                    }
                    // 句型关：全部句型跟读通过 → 记关
                    if (unit.phrases.isNotEmpty() &&
                        unit.phrases.indices.all { store.isDayCheckDone(unit.id, dayCheckSpeakPhraseId(it)) }
                    ) {
                        store.markUnitLevelComplete(unit.id, UNIT_LEVEL_PHRASES)
                    }
                }
            }
        )
    }

    if (phraseGateOpen) {
        PhraseLevelSheet(
            unit = unit,
            store = store,
            speakLevel = speakLevel,
            onSpeak = onSpeak,
            onClose = { phraseGateOpen = false },
            onBump = { }
        )
    }

    if (parentReportOpen) {
        ParentReportDialog(summary = parentReport, onDismiss = { parentReportOpen = false })
    }

    Scaffold(containerColor = Paper, topBar = {
        SecondaryTopBar(onBack) {
            Column(Modifier.weight(1f)) {
                Text(unit.titleZh.ifBlank { unit.titleEn }, fontWeight = FontWeight.Bold)
                AppText(
                    buildString {
                        if (unit.id.endsWith(".rev")) {
                            append(revisionLabel(unit))
                            append(" · ${unit.titleEn}")
                        } else {
                            append("Unit ${unit.unit} · ${unit.titleEn}")
                        }
                        if (weekSuggested && suggestedWeek != null) {
                            append(" · 建议第 $suggestedWeek 周")
                        }
                    },
                    color = InkFaint,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif
                )
            }
        }
    }) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (weekSuggested && suggestedWeek != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6E8)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    ) {
                        AppText(
                            text = "建议第 $suggestedWeek 周 · ${weekHintLabel(unit.weeksHint)}",
                            modifier = Modifier.padding(12.dp),
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            if (unit.goalsZh.isNotEmpty()) {
                item {
                    UnitSection("本单元目标") {
                        unit.goalsZh.forEach { AppText("· $it", fontSize = 15.sp, color = InkSoft) }
                    }
                }
            }
            item {
                UnitSection("今日听读") {
                    DayCheckRow(
                        label = "听单词一遍",
                        done = listenDone,
                        onToggle = {
                            store.setDayCheckDone(unit.id, DAYCHECK_LISTEN_WORDS, !listenDone)
                        }
                    )
                    DayCheckRow(
                        label = "跟读 2 个句型（已过 $passedPhraseCount）",
                        done = speakCheckDone || passedPhraseCount >= 2,
                        onToggle = {
                            val next = !(speakCheckDone || passedPhraseCount >= 2)
                            store.setDayCheckDone(unit.id, DAYCHECK_SPEAK_PHRASES, next)
                        }
                    )
                    val anyActivityDone = unit.activities.indices.any {
                        store.isDayCheckDone(unit.id, dayCheckActivityId(it))
                    }
                    DayCheckRow(
                        label = "完成 1 个活动",
                        done = anyActivityDone,
                        onToggle = {
                            if (unit.activities.isNotEmpty()) {
                                store.setDayCheckDone(unit.id, dayCheckActivityId(0), !anyActivityDone)
                            }
                        }
                    )
                    AppText("换一天会自动变成新清单", color = InkFaint, fontSize = 12.sp)
                }
            }
            item {
                UnitSection("核心词汇（${resolved.words.size}）") {
                    if (resolved.words.isEmpty()) {
                        AppText("这个单元的词还没接进词库", color = InkFaint, fontSize = 13.sp)
                    }
                    resolved.words.forEach { word ->
                        WordRow(word) {
                            onSpeak(word.word)
                            if (!store.isDayCheckDone(unit.id, DAYCHECK_LISTEN_WORDS)) {
                                // 轻触听词即勾选「听单词」
                                store.setDayCheckDone(unit.id, DAYCHECK_LISTEN_WORDS, true)
                            }
                        }
                    }
                    if (resolved.missingKeys.isNotEmpty()) {
                        AppText("有 ${resolved.missingKeys.size} 个词暂时跳过（词库还没有）", color = InkFaint, fontSize = 12.sp)
                    }
                }
            }
            if (unit.phrases.isNotEmpty()) {
                item {
                    UnitSection("关键句型") {
                        unit.phrases.forEachIndexed { index, phrase ->
                            val phrasePassed = store.isDayCheckDone(unit.id, dayCheckSpeakPhraseId(index))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSpeak(phrase.en) },
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(phrase.en, fontFamily = FontFamily.Serif, fontSize = 17.sp, color = Ink)
                                    AppText(phrase.zh, color = InkSoft, fontSize = 14.sp)
                                    if (phrasePassed) {
                                        AppText("今日已跟读 ✓", color = Success, fontSize = 12.sp)
                                    }
                                }
                                TextButton(onClick = { speakTarget = phrase }) {
                                    AppText("跟读", color = Primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        AppText("轻触句子可以听一遍；点跟读开口练", color = InkFaint, fontSize = 12.sp)
                    }
                }
            }
            if (unit.activities.isNotEmpty()) {
                item {
                    UnitSection("课堂 / 亲子活动") {
                        unit.activities.forEachIndexed { index, activity ->
                            val done = store.isDayCheckDone(unit.id, dayCheckActivityId(index))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppText(
                                        activityTypeLabel(activity.type),
                                        color = Primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.weight(1f))
                                    DayCheckRow(
                                        label = "今日已做",
                                        done = done,
                                        compact = true,
                                        onToggle = {
                                            store.setDayCheckDone(unit.id, dayCheckActivityId(index), !done)
                                        }
                                    )
                                }
                                AppText(activity.titleZh, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                AppText(activity.bodyZh, color = InkSoft, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            item {
                UnitSection("单元闯关") {
                    AppText("过词汇关 → 句型关 → 综合关，综合关通过才算通关", color = InkFaint, fontSize = 12.sp)
                    LevelGateRow(
                        title = "① 词汇关",
                        subtitle = "听音选词 · 看中文选英文",
                        unlocked = true,
                        complete = wordsDone,
                        button = if (wordsDone) "再练" else "开始",
                        onClick = {
                            val keys = resolved.words.map { it.word }
                            if (keys.isNotEmpty()) {
                                onStartLevelPractice(
                                    UNIT_LEVEL_WORDS,
                                    keys,
                                    unitPracticeTitle(unit, "词汇关"),
                                    0
                                )
                            }
                        }
                    )
                    LevelGateRow(
                        title = "② 句型关",
                        subtitle = if (unit.phrases.isEmpty()) "本单元暂无句型" else "跟读全部句型",
                        unlocked = phrasesUnlocked,
                        complete = phrasesDone,
                        button = when {
                            !phrasesUnlocked -> "锁定"
                            phrasesDone -> "再练"
                            else -> "开始"
                        },
                        onClick = {
                            if (phrasesUnlocked && unit.phrases.isNotEmpty()) {
                                phraseGateOpen = true
                            } else if (phrasesUnlocked && unit.phrases.isEmpty()) {
                                // 无句型时直接通关，避免卡死
                                store.markUnitLevelComplete(unit.id, UNIT_LEVEL_PHRASES)
                            }
                        }
                    )
                    LevelGateRow(
                        title = "③ 综合关",
                        subtitle = "建议选中等/困难（含拼写）",
                        unlocked = mixedUnlocked,
                        complete = mixedDone,
                        button = when {
                            !mixedUnlocked -> "锁定"
                            mixedDone -> "再练"
                            else -> "开始"
                        },
                        onClick = {
                            val keys = resolved.words.map { it.word }
                            if (mixedUnlocked && keys.isNotEmpty()) {
                                onStartLevelPractice(
                                    UNIT_LEVEL_MIXED,
                                    keys,
                                    unitPracticeTitle(unit, "综合关"),
                                    0
                                )
                            }
                        }
                    )
                    if (isRevision) {
                        LevelGateRow(
                            title = "综合测评",
                            subtitle = "固定 $REVISION_EXAM_COUNT 题 · 不计入单元通关",
                            unlocked = true,
                            complete = revisionExamDone,
                            button = if (revisionExamDone) "再测" else "开始",
                            onClick = {
                                val keys = resolved.words.map { it.word }
                                if (keys.isNotEmpty()) {
                                    onStartLevelPractice(
                                        UNIT_LEVEL_REVISION_EXAM,
                                        keys,
                                        unitPracticeTitle(unit, "综合测评"),
                                        REVISION_EXAM_COUNT
                                    )
                                }
                            }
                        )
                    }
                }
            }
            item {
                UnitSection("给家长看") {
                    AppText(parentReport.levelsLine, fontSize = 14.sp, color = InkSoft)
                    AppText(parentReport.dayCheckLine, fontSize = 13.sp, color = InkFaint)
                    AppText(parentReport.speakLine, fontSize = 13.sp, color = InkFaint)
                    Button(
                        onClick = { parentReportOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { AppText("打开家长报告") }
                }
            }
        }
    }
}

@Composable
private fun ParentReportDialog(summary: ParentReportSummary, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("家长报告 · ${summary.unitTitleZh}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppText(summary.unitTitleEn, color = InkFaint, fontSize = 13.sp, fontFamily = FontFamily.Serif)
                AppText("闯关进度", fontWeight = FontWeight.Bold)
                AppText(summary.levelsLine, fontSize = 15.sp)
                AppText("今日听读", fontWeight = FontWeight.Bold)
                AppText(summary.dayCheckLine, fontSize = 15.sp)
                AppText("跟读", fontWeight = FontWeight.Bold)
                AppText(summary.speakLine, fontSize = 15.sp)
                AppText("仅本机今日数据，不会上传。", color = InkFaint, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { AppText("知道了") }
        }
    )
}

private fun gradeZh(grade: Int): String = when (grade) {
    3 -> "三"
    4 -> "四"
    5 -> "五"
    6 -> "六"
    else -> grade.toString()
}

/** 六下收官单元课本称 Recycle；其余册仍用 Revision（id 均以 .rev 结尾以走测评路径） */
private fun revisionLabel(unit: CurriculumUnit): String =
    if (unit.grade == 6 && unit.volume == 2) "Recycle" else "Revision"

private fun unitPracticeTitle(unit: CurriculumUnit, gate: String): String = buildString {
    append(gradeZh(unit.grade))
    append(if (unit.volume == 2) "下" else "上")
    append(" ")
    append(if (unit.id.endsWith(".rev")) revisionLabel(unit) else "U${unit.unit}")
    append(" · ")
    append(gate)
}

@Composable
private fun LevelGateRow(
    title: String,
    subtitle: String,
    unlocked: Boolean,
    complete: Boolean,
    button: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (unlocked) Ink else InkFaint)
                if (complete) {
                    Spacer(Modifier.width(6.dp))
                    AppText("✓", color = Success, fontWeight = FontWeight.Bold)
                }
            }
            AppText(subtitle, color = InkFaint, fontSize = 12.sp)
        }
        if (unlocked) {
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.heightIn(min = 40.dp)
            ) { AppText(button) }
        } else {
            AppText(button, color = InkFaint, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DayCheckRow(
    label: String,
    done: Boolean,
    compact: Boolean = false,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = done, onCheckedChange = { onToggle() })
        AppText(label, fontSize = if (compact) 13.sp else 15.sp, color = if (done) Success else Ink)
    }
}

@Composable
private fun PhraseLevelSheet(
    unit: CurriculumUnit,
    store: ProgressStore,
    speakLevel: SpeakLevel,
    onSpeak: (String) -> Unit,
    onClose: () -> Unit,
    onBump: () -> Unit
) {
    var speakTarget by remember { mutableStateOf<CurriculumPhrase?>(null) }

    speakTarget?.let { phrase ->
        val idx = unit.phrases.indexOf(phrase)
        PhraseSpeakDialog(
            phrase = phrase,
            speakLevel = speakLevel,
            onSpeak = onSpeak,
            onDismiss = { speakTarget = null },
            onPassed = {
                if (idx >= 0) {
                    store.setDayCheckDone(unit.id, dayCheckSpeakPhraseId(idx), true)
                    val all = unit.phrases.indices.all { store.isDayCheckDone(unit.id, dayCheckSpeakPhraseId(it)) }
                    if (all) {
                        store.markUnitLevelComplete(unit.id, UNIT_LEVEL_PHRASES)
                        store.setDayCheckDone(unit.id, DAYCHECK_SPEAK_PHRASES, true)
                    }
                    onBump()
                }
            }
        )
    }

    AlertDialogLike(
        onDismiss = onClose,
        title = "句型关 · 跟读全部句子",
        content = {
            unit.phrases.forEachIndexed { index, phrase ->
                val done = store.isDayCheckDone(unit.id, dayCheckSpeakPhraseId(index))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(phrase.en, fontFamily = FontFamily.Serif, fontSize = 16.sp, color = Ink)
                        AppText(if (done) "已通过 ✓" else phrase.zh, color = if (done) Success else InkSoft, fontSize = 13.sp)
                    }
                    TextButton(onClick = { speakTarget = phrase }) {
                        AppText(if (done) "再读" else "跟读", color = Primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    )
}

@Composable
private fun AlertDialogLike(
    onDismiss: () -> Unit,
    title: String,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { AppText("关闭") }
        }
    )
}

@Composable
private fun UnitSection(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line, RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppText(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = FontFamily.Serif)
            content()
        }
    }
}

@Composable
private fun WordRow(word: OgdenWord, onSpeak: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSpeak),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(word.word, fontFamily = FontFamily.Serif, fontSize = 17.sp, color = Ink, modifier = Modifier.weight(1f))
        AppText(word.zh, color = InkSoft, fontSize = 14.sp)
    }
}
