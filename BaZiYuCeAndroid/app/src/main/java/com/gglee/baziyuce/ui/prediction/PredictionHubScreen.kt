package com.gglee.baziyuce.ui.prediction

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gglee.baziyuce.BaZiSessionViewModel
import com.gglee.baziyuce.engine.FortuneForecast
import com.gglee.baziyuce.engine.FortunePeriod
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Strict mirror of iOS `PredictionHubView.swift`. */
@Composable
fun PredictionHubScreen(
    session: BaZiSessionViewModel,
    modifier: Modifier = Modifier,
) {
    var period by remember { mutableStateOf(FortunePeriod.YEAR) }
    val context = LocalContext.current
    val dateFmt = remember { SimpleDateFormat("yyyy年M月d日", Locale.CHINA) }

    ScreenBackground {
        Column(modifier = modifier.fillMaxSize()) {
            InlineNavTitle("运势")
            if (session.chart == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, null, tint = CeladonSoft, modifier = Modifier.size(40.dp))
                        Text("排盘后可查看流年、流月、流日运势", color = Slate, fontSize = 17.sp)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                        .padding(bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    SegmentedControl(
                        options = FortunePeriod.entries.map { it to it.displayName },
                        selected = period,
                        onSelect = { period = it },
                    )

                    Labeled("参考日期", spacing = 8.dp) {
                        // iOS compact DatePicker equivalent: tappable date row (system picker sheet)
                        Text(
                            dateFmt.format(session.referenceDate),
                            color = Ink,
                            fontSize = 17.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val cal = Calendar.getInstance().apply { time = session.referenceDate }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val next = Calendar.getInstance().apply {
                                                time = session.referenceDate
                                                set(Calendar.YEAR, y)
                                                set(Calendar.MONTH, m)
                                                set(Calendar.DAY_OF_MONTH, d)
                                            }
                                            session.updateReferenceDate(next.time)
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH),
                                    ).show()
                                },
                        )
                    }

                    AnimatedContent(
                        targetState = period,
                        transitionSpec = {
                            (fadeIn() + slideInHorizontally { it / 4 }) togetherWith
                                (fadeOut() + slideOutHorizontally { -it / 4 })
                        },
                        label = "forecast",
                    ) { p ->
                        session.forecast(p)?.let { ForecastCard(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastCard(f: FortuneForecast) {
    var rulesExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.55f))
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    f.period.sectionTitle,
                    color = CeladonSoft,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    f.flowingPillar.name,
                    color = Ink,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif,
                )
                Text(
                    "${f.stemGod.displayName} · ${f.tone}",
                    color = Celadon,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Serif,
                )
            }
            ScoreRing(f.score)
        }

        Text(f.summary, color = Ink, fontSize = 17.sp, lineHeight = 22.sp)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "运势提示",
                color = CeladonSoft,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
            )
            f.tips.forEach { tip ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Canvas(modifier = Modifier.padding(top = 7.dp).size(6.dp)) {
                        drawCircle(Accent)
                    }
                    Text(tip, color = Ink, fontSize = 17.sp, lineHeight = 22.sp, modifier = Modifier.weight(1f))
                }
            }
        }

        // iOS DisclosureGroup label
        Text(
            if (rulesExpanded) "规则说明（可扩展）" else "规则说明（可扩展）",
            color = Celadon,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { rulesExpanded = !rulesExpanded },
        )
        if (rulesExpanded) {
            Column(
                modifier = Modifier.padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                f.ruleNotes.forEach { note ->
                    Text("· $note", color = Slate, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ScoreRing(score: Int) {
    Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            drawArc(
                color = MistDeep,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = Size(size.width - stroke, size.height - stroke),
                topLeft = Offset(stroke / 2, stroke / 2),
            )
            drawArc(
                color = Celadon,
                startAngle = -90f,
                sweepAngle = 360f * (score / 100f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = Size(size.width - stroke, size.height - stroke),
                topLeft = Offset(stroke / 2, stroke / 2),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score", fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = Ink)
            Text("倾向", fontSize = 10.sp, color = Slate)
        }
    }
}

@Composable
private fun Labeled(title: String, spacing: androidx.compose.ui.unit.Dp = 10.dp, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        Text(
            title,
            color = CeladonSoft,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        )
        content()
    }
}
