package com.gglee.baziyuce.ui.prediction

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gglee.baziyuce.BaZiSessionViewModel
import com.gglee.baziyuce.engine.FortuneForecast
import com.gglee.baziyuce.engine.FortunePeriod
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

@Composable
fun PredictionHubScreen(
    session: BaZiSessionViewModel,
    modifier: Modifier = Modifier,
) {
    var period by remember { mutableStateOf(FortunePeriod.YEAR) }
    val context = LocalContext.current
    val dateFmt = remember { SimpleDateFormat("yyyy年M月d日", Locale.CHINA) }

    ScreenBackground {
        if (session.chart == null) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = CeladonSoft, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("排盘后可查看流年、流月、流日运势", color = Slate)
                }
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FortunePeriod.entries.forEach { p ->
                        FilterChip(
                            selected = period == p,
                            onClick = { period = p },
                            label = { Text(p.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Celadon,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.45f),
                                labelColor = Ink,
                            ),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("参考日期", style = MaterialTheme.typography.titleMedium, color = CeladonSoft)
                    Surface(
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
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.55f),
                    ) {
                        Text(
                            dateFmt.format(session.referenceDate),
                            modifier = Modifier.padding(14.dp),
                            color = Ink,
                        )
                    }
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

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ForecastCard(f: FortuneForecast) {
    var rulesExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(f.period.sectionTitle, style = MaterialTheme.typography.titleMedium, color = CeladonSoft)
                    Text(f.flowingPillar.name, style = MaterialTheme.typography.headlineLarge, color = Ink)
                    Text("${f.stemGod.displayName} · ${f.tone}", style = MaterialTheme.typography.headlineMedium, color = Celadon)
                }
                ScoreRing(f.score)
            }

            Text(f.summary, style = MaterialTheme.typography.bodyLarge, color = Ink)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("运势提示", style = MaterialTheme.typography.titleMedium, color = CeladonSoft)
                f.tips.forEach { tip ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Canvas(modifier = Modifier.size(6.dp).padding(top = 7.dp)) {
                            drawCircle(Accent)
                        }
                        Text(tip, style = MaterialTheme.typography.bodyLarge, color = Ink, modifier = Modifier.weight(1f))
                    }
                }
            }

            Text(
                if (rulesExpanded) "收起规则说明" else "规则说明（可扩展）",
                color = Celadon,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { rulesExpanded = !rulesExpanded },
            )
            if (rulesExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    f.ruleNotes.forEach { note ->
                        Text("· $note", color = Slate, fontSize = 13.sp)
                    }
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
            Text("$score", fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Ink)
            Text("倾向", fontSize = 10.sp, color = Slate)
        }
    }
}
