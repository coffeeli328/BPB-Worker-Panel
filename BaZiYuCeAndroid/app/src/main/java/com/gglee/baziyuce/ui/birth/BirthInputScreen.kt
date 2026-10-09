package com.gglee.baziyuce.ui.birth

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gglee.baziyuce.BaZiSessionViewModel
import com.gglee.baziyuce.engine.Gender
import com.gglee.baziyuce.ui.theme.Celadon
import com.gglee.baziyuce.ui.theme.CeladonSoft
import com.gglee.baziyuce.ui.theme.Ink
import com.gglee.baziyuce.ui.theme.Line
import com.gglee.baziyuce.ui.theme.ScreenBackground
import com.gglee.baziyuce.ui.theme.Slate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun BirthInputScreen(
    session: BaZiSessionViewModel,
    modifier: Modifier = Modifier,
    onOpenChart: () -> Unit,
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val context = LocalContext.current
    val dateFmt = remember {
        SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA)
    }

    ScreenBackground {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AnimatedVisibility(
                visible = appeared,
                enter = fadeIn() + slideInVertically { it / 4 },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("八字运势", style = MaterialTheme.typography.displayLarge, color = Ink)
                    Text(
                        "输入出生时间，排出四柱，并据流年、流月、流日给出规则化运势提示。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate,
                    )
                }
            }

            Labeled("出生日期与时辰") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Line, RoundedCornerShape(14.dp))
                        .clickable {
                            val cal = Calendar.getInstance().apply { time = session.birthDate }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val next = Calendar.getInstance().apply {
                                        time = session.birthDate
                                        set(Calendar.YEAR, y)
                                        set(Calendar.MONTH, m)
                                        set(Calendar.DAY_OF_MONTH, d)
                                    }
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            next.set(Calendar.HOUR_OF_DAY, hour)
                                            next.set(Calendar.MINUTE, minute)
                                            next.set(Calendar.SECOND, 0)
                                            next.set(Calendar.MILLISECOND, 0)
                                            session.updateBirthDate(next.time)
                                        },
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE),
                                        true,
                                    ).show()
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH),
                            ).show()
                        },
                    color = Color.White.copy(alpha = 0.55f),
                ) {
                    Text(
                        dateFmt.format(session.birthDate),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Ink,
                    )
                }
            }

            Labeled("性别") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Gender.entries.forEach { g ->
                        FilterChip(
                            selected = session.gender == g,
                            onClick = { session.updateGender(g) },
                            label = { Text(g.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Celadon,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.45f),
                                labelColor = Ink,
                            ),
                        )
                    }
                }
            }

            Button(
                onClick = { session.calculate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Celadon,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    if (session.didCalculate) "重新排盘" else "排出八字",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                )
            }

            AnimatedVisibility(
                visible = session.chart != null,
                enter = fadeIn() + slideInVertically { it / 3 },
            ) {
                val chart = session.chart
                if (chart != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Line, RoundedCornerShape(14.dp))
                            .clickable(onClick = onOpenChart),
                        color = Color.White.copy(alpha = 0.55f),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("四柱已成", color = Celadon, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(chart.fourPillarsText, color = Ink, style = MaterialTheme.typography.headlineMedium)
                            }
                            Icon(
                                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Slate,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Labeled(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title.uppercase(Locale.CHINA), style = MaterialTheme.typography.titleMedium, color = CeladonSoft)
        content()
    }
}
