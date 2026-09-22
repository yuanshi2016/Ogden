package com.example.ogdenkids

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
// produceState for async quality summary
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ogdenkids.data.DailyActivityEntity
import com.example.ogdenkids.data.QualitySummary
import java.util.Calendar

internal fun epochDayLabel(day: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = day * 86_400_000L }
    return "${c.get(Calendar.MONTH) + 1}/${c.get(Calendar.DAY_OF_MONTH)}"
}

@Composable
fun StatsScreen(words: List<OgdenWord>, store: ProgressStore, onBack: () -> Unit) {
    val series = store.dailySeries(30)
    val totalAnswered = series.sumOf { it.answered }
    val totalCorrect = series.sumOf { it.correct }
    val weak by remember(words, store) { derivedStateOf { store.weakWords(words, 10) } }
    val quality by produceState<QualitySummary?>(initialValue = null, store) {
        value = store.qualitySummary(30)
    }

    Scaffold(containerColor = Paper, topBar = {
        SecondaryTopBar(onBack) {
            AppText("学习图表", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
                SectionTitle("最近 30 天", "每天答题数与答对数，历史最多保留一年")
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("30天答题", totalAnswered, "", Category.Function.tint, Modifier.weight(1f))
                    StatCard("30天答对", totalCorrect, "", Success, Modifier.weight(1f))
                    StatCard("连续天数", store.dailyStreak(), "天", Category.Picturable.tint, Modifier.weight(1f))
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
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (totalAnswered == 0) {
                            AppText("还没有学习记录，先去闯一关吧。", color = InkFaint, fontSize = 14.sp)
                        } else {
                            DayBarChart(series)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(Success))
                                AppText("答对", color = InkFaint, fontSize = 12.sp)
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.size(10.dp).clip(CircleShape).background(Category.Function.tint))
                                AppText("未答对", color = InkFaint, fontSize = 12.sp)
                                Spacer(Modifier.weight(1f))
                                AppText(
                                    "${epochDayLabel(series.first().day)} – ${epochDayLabel(series.last().day)}",
                                    color = InkFaint,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
            item {
                SectionTitle("回忆质量", "SM-2 质量分 0–5，来自每次答题明细")
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperElevated),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Line, RoundedCornerShape(18.dp))
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val q = quality
                        when {
                            q == null -> AppText("正在汇总质量分…", color = InkFaint, fontSize = 14.sp)
                            q.total == 0 -> AppText("还没有带质量分的答题记录。", color = InkFaint, fontSize = 14.sp)
                            else -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    StatCard(
                                        "明细条数",
                                        q.total,
                                        "",
                                        Category.Qualities.tint,
                                        Modifier.weight(1f)
                                    )
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        AppText("合格率 (q≥3)", color = InkFaint, fontSize = 12.sp)
                                        Text(
                                            "${(q.passRate * 100).toInt()}%",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp,
                                            color = Success
                                        )
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        AppText("平均质量", color = InkFaint, fontSize = 12.sp)
                                        Text(
                                            String.format("%.1f", q.averageQuality),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp,
                                            color = Ink
                                        )
                                    }
                                }
                                QualityBarChart(q.histogram)
                                AppText(
                                    "0 全忘 · 1–2 错 · 3 费力对 · 4 犹豫对 · 5 完美",
                                    color = InkFaint,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
            item {
                SectionTitle("薄弱词 Top10", "正确率从低到高，至少答过一次")
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
                        if (weak.isEmpty()) {
                            AppText("还没有足够的答题记录。", color = InkFaint, fontSize = 14.sp)
                        } else {
                            weak.forEach { (word, progress) ->
                                val rate = if (progress.attempts == 0) 0
                                else (progress.correct * 100 / progress.attempts)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(word.word, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink)
                                        AppText(word.zh, color = InkFaint, fontSize = 12.sp)
                                    }
                                    AppText(
                                        "$rate% · ${progress.correct}/${progress.attempts}",
                                        color = if (rate < 60) Error else InkSoft,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayBarChart(series: List<DailyActivityEntity>, modifier: Modifier = Modifier) {
    val total = Category.Function.tint
    val good = Success
    val max = (series.maxOfOrNull { it.answered } ?: 0).coerceAtLeast(1)
    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        val slot = size.width / series.size
        val bw = slot * 0.6f
        series.forEachIndexed { i, d ->
            if (d.answered <= 0) return@forEachIndexed
            val x = slot * i + (slot - bw) / 2f
            val totalH = size.height * d.answered / max
            val goodH = size.height * d.correct / max
            drawRect(total, topLeft = Offset(x, size.height - totalH), size = Size(bw, totalH))
            drawRect(good, topLeft = Offset(x, size.height - goodH), size = Size(bw, goodH))
        }
    }
}

@Composable
fun QualityBarChart(histogram: IntArray, modifier: Modifier = Modifier) {
    val max = histogram.maxOrNull()?.coerceAtLeast(1) ?: 1
    // 主题色必须在组合阶段解析，Canvas 的 draw 作用域不是 @Composable
    val barColors = listOf(
        Error,
        Color(0xFFE85D4C),
        Category.Opposites.tint,
        Category.Picturable.tint,
        Listening,
        Success
    )
    val fallback = Success
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val n = 6
            val slot = size.width / n
            val bw = slot * 0.55f
            for (q in 0 until n) {
                val count = histogram.getOrElse(q) { 0 }
                if (count <= 0) continue
                val h = size.height * count / max
                val x = slot * q + (slot - bw) / 2f
                drawRect(
                    barColors.getOrElse(q) { fallback },
                    topLeft = Offset(x, size.height - h),
                    size = Size(bw, h)
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (q in 0..5) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AppText("$q", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    AppText("${histogram.getOrElse(q) { 0 }}", color = InkFaint, fontSize = 11.sp)
                }
            }
        }
    }
}
