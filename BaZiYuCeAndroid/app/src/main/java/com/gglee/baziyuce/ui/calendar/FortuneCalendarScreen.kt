package com.gglee.baziyuce.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gglee.baziyuce.BaZiSessionViewModel
import com.gglee.baziyuce.engine.DayStrength
import com.gglee.baziyuce.engine.FortuneForecast
import com.gglee.baziyuce.engine.MonthStrength
import com.gglee.baziyuce.engine.YearStrength
import com.gglee.baziyuce.ui.components.InlineNavTitle
import com.gglee.baziyuce.ui.components.SegmentedControl
import com.gglee.baziyuce.ui.theme.Accent
import com.gglee.baziyuce.ui.theme.Celadon
import com.gglee.baziyuce.ui.theme.CeladonSoft
import com.gglee.baziyuce.ui.theme.Ink
import com.gglee.baziyuce.ui.theme.Line
import com.gglee.baziyuce.ui.theme.MistDeep
import com.gglee.baziyuce.ui.theme.ScreenBackground
import com.gglee.baziyuce.ui.theme.Slate
import java.util.Calendar

private enum class CalMode(val label: String) {
    DAY("流日"),
    MONTH("流月"),
    YEAR("流年"),
}

/** 日历强弱指数：可滚动查看过去 / 未来，点选看详情。 */
@Composable
fun FortuneCalendarScreen(
    session: BaZiSessionViewModel,
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableStateOf(CalMode.DAY) }
    LaunchedEffect(session.chart?.id, session.calendarYear, session.calendarMonth) {
        if (session.chart != null) session.refreshCalendarStrengths()
    }

    ScreenBackground {
        Column(modifier = modifier.fillMaxSize()) {
            InlineNavTitle("日历")
            if (session.chart == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, null, tint = CeladonSoft, modifier = Modifier.size(40.dp))
                        Text("请先在档案或起盘中排出八字", color = Slate, fontSize = 17.sp)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val who = session.personName.trim().ifEmpty { "当前" }
                    Text(
                        "$who · 运势强弱",
                        color = Ink,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Serif,
                    )
                    Text(
                        "格子内数字为 PredictionEngine 强弱指数（约 25–95）。可左右切换查看过去与未来。",
                        color = Slate,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )

                    SegmentedControl(
                        options = CalMode.entries.map { it to it.label },
                        selected = mode,
                        onSelect = { mode = it },
                    )

                    MonthYearNav(
                        title = when (mode) {
                            CalMode.DAY -> "${session.calendarYear}年${session.calendarMonth}月"
                            CalMode.MONTH -> "${session.calendarYear}年"
                            CalMode.YEAR -> "流年纵览"
                        },
                        onPrev = {
                            when (mode) {
                                CalMode.DAY -> session.shiftCalendarMonth(-1)
                                CalMode.MONTH, CalMode.YEAR -> session.shiftCalendarYear(-1)
                            }
                        },
                        onNext = {
                            when (mode) {
                                CalMode.DAY -> session.shiftCalendarMonth(1)
                                CalMode.MONTH, CalMode.YEAR -> session.shiftCalendarYear(1)
                            }
                        },
                    )

                    AnimatedContent(
                        targetState = Triple(mode, session.calendarYear, session.calendarMonth),
                        transitionSpec = {
                            (fadeIn() + slideInVertically { 12 }) togetherWith (fadeOut() + slideOutVertically { -8 })
                        },
                        label = "cal-grid",
                    ) { _ ->
                        when (mode) {
                            CalMode.DAY -> DayGrid(
                                year = session.calendarYear,
                                month = session.calendarMonth,
                                days = session.dayStrengths,
                                onDay = { d -> session.openDayDetail(session.calendarYear, session.calendarMonth, d.day) },
                            )
                            CalMode.MONTH -> MonthGrid(
                                months = session.monthStrengths,
                                onMonth = { m -> session.openMonthDetail(m.year, m.month) },
                            )
                            CalMode.YEAR -> YearList(
                                years = session.yearStrengths,
                                onYear = { y -> session.openYearDetail(y.year) },
                            )
                        }
                    }

                    ScoreLegend()

                    AnimatedVisibility(
                        visible = session.selectedDayDetail != null,
                        enter = fadeIn() + slideInVertically { it / 4 },
                        exit = fadeOut(),
                    ) {
                        session.selectedDayDetail?.let { DetailCard(it) { session.clearDayDetail() } }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthYearNav(title: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
            contentDescription = "上一页",
            tint = Celadon,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onPrev)
                .padding(2.dp),
        )
        Text(
            title,
            color = Ink,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Serif,
        )
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = "下一页",
            tint = Celadon,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onNext)
                .padding(2.dp),
        )
    }
}

@Composable
private fun DayGrid(
    year: Int,
    month: Int,
    days: List<DayStrength>,
    onDay: (DayStrength) -> Unit,
) {
    val byDay = remember(days) { days.associateBy { it.day } }
    val firstDow = remember(year, month) {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(year, month - 1, 1)
        // Calendar: SUN=1 … SAT=7 → map to Mon-first 0..6
        val sunFirst = cal.get(Calendar.DAY_OF_WEEK)
        (sunFirst + 5) % 7
    }
    val dayCount = days.size.coerceAtLeast(28)
    val cells = firstDow + dayCount
    val rows = (cells + 6) / 7

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.55f))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
                Text(
                    w,
                    color = CeladonSoft,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        for (r in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (c in 0..6) {
                    val idx = r * 7 + c
                    val dayNum = idx - firstDow + 1
                    val ds = byDay[dayNum]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.85f)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (ds != null) {
                                    Modifier
                                        .background(scoreFill(ds.score))
                                        .clickable { onDay(ds) }
                                } else {
                                    Modifier
                                },
                            )
                            .padding(2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (ds != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${ds.day}", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("${ds.score}", color = scoreTint(ds.score), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(months: List<MonthStrength>, onMonth: (MonthStrength) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.55f))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        months.chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { m ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(scoreFill(m.score))
                            .clickable { onMonth(m) }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("${m.month}月", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text("${m.score} · ${m.tone}", color = scoreTint(m.score), fontSize = 12.sp)
                        Text(m.flowing.name, color = Slate, fontSize = 11.sp)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun YearList(years: List<YearStrength>, onYear: (YearStrength) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.55f))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        years.forEach { y ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(scoreFill(y.score))
                    .clickable { onYear(y) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${y.year}年",
                    color = Ink,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.weight(1f),
                )
                Text(y.flowing.name, color = Slate, fontSize = 14.sp, modifier = Modifier.padding(end = 10.dp))
                Text("${y.score}", color = scoreTint(y.score), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("  ${y.tone}", color = scoreTint(y.score), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ScoreLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            "偏弱" to MistDeep.copy(alpha = 0.55f),
            "平稳" to CeladonSoft.copy(alpha = 0.22f),
            "偏强" to Celadon.copy(alpha = 0.28f),
            "旺盛" to Accent.copy(alpha = 0.28f),
        ).forEach { (label, color) ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
            Text(label, color = Slate, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DetailCard(f: FortuneForecast, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.72f))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                f.period.sectionTitle,
                color = Celadon,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                "收起",
                color = Slate,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${f.score}",
                color = scoreTint(f.score),
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Serif,
            )
            Spacer(Modifier.size(8.dp))
            Text(f.tone, color = scoreTint(f.score), fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(f.flowingPillar.name, color = Ink, fontSize = 20.sp, fontFamily = FontFamily.Serif)
        }
        Text(f.summary, color = Ink, fontSize = 15.sp, lineHeight = 21.sp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("提示", color = CeladonSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            f.tips.forEach { tip ->
                Text("· $tip", color = Slate, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

private fun scoreFill(score: Int): Color = when {
    score < 45 -> MistDeep.copy(alpha = 0.45f)
    score < 60 -> CeladonSoft.copy(alpha = 0.16f)
    score < 75 -> Celadon.copy(alpha = 0.22f)
    else -> Accent.copy(alpha = 0.24f)
}

private fun scoreTint(score: Int): Color = when {
    score < 45 -> Slate
    score < 60 -> CeladonSoft
    score < 75 -> Celadon
    else -> Accent
}
