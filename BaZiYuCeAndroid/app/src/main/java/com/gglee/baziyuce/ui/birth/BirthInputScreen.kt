package com.gglee.baziyuce.ui.birth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gglee.baziyuce.BaZiSessionViewModel
import com.gglee.baziyuce.engine.Gender
import com.gglee.baziyuce.engine.ShiChen
import com.gglee.baziyuce.ui.components.SegmentedControl
import com.gglee.baziyuce.ui.components.WheelIntPicker
import com.gglee.baziyuce.ui.theme.Celadon
import com.gglee.baziyuce.ui.theme.ErrorRed
import com.gglee.baziyuce.ui.theme.FrostedCard
import com.gglee.baziyuce.ui.theme.Ink
import com.gglee.baziyuce.ui.theme.ScreenBackground
import com.gglee.baziyuce.ui.theme.SectionLabeled
import com.gglee.baziyuce.ui.theme.Slate

@Composable
fun BirthInputScreen(
    session: BaZiSessionViewModel,
    modifier: Modifier = Modifier,
    onOpenChart: () -> Unit,
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val years = remember { (BaZiSessionViewModel.MIN_YEAR..BaZiSessionViewModel.MAX_YEAR).toList() }
    val months = remember { (1..12).toList() }
    val days = (1..session.maxDayInMonth).toList()
    val shiChenIndices = remember { ShiChen.all.map { it.branch.ordinal } }

    ScreenBackground {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            AnimatedVisibility(
                visible = appeared,
                enter = fadeIn() + slideInVertically { it / 4 },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("八字运势", style = MaterialTheme.typography.displayLarge, color = Ink)
                    Text(
                        "分别填写出生年、月、日与时辰，排出四柱，并据流年、流月、流日给出规则化运势提示。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                SectionLabeled("出生年月日") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FieldColumn("年", Modifier.weight(1.2f)) {
                            WheelIntPicker(
                                value = session.birthYear,
                                values = years,
                                label = { "${it}年" },
                                onValueChange = { session.updateBirthYear(it) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        FieldColumn("月", Modifier.weight(1f)) {
                            WheelIntPicker(
                                value = session.birthMonth,
                                values = months,
                                label = { "${it}月" },
                                onValueChange = { session.updateBirthMonth(it) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        FieldColumn("日", Modifier.weight(1f)) {
                            WheelIntPicker(
                                value = session.birthDay.coerceIn(1, session.maxDayInMonth),
                                values = days,
                                label = { "${it}日" },
                                onValueChange = { session.updateBirthDay(it) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                SectionLabeled("时辰") {
                    WheelIntPicker(
                        value = session.birthShiChenIndex,
                        values = shiChenIndices,
                        label = { idx -> ShiChen.all[idx].displayName },
                        onValueChange = { session.updateBirthShiChenIndex(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                    )
                    Text(
                        "当前：${session.birthShiChen.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate,
                    )
                }

                SectionLabeled("性别") {
                    SegmentedControl(
                        options = Gender.entries.map { it to it.displayName },
                        selected = session.gender,
                        onSelect = { session.updateGender(it) },
                    )
                }

                session.inputError?.let { err ->
                    Text(err, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
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
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontSize = 17.sp),
                    )
                }

                AnimatedVisibility(
                    visible = session.chart != null,
                    enter = fadeIn() + slideInVertically { it / 3 },
                ) {
                    val chart = session.chart
                    if (chart != null) {
                        FrostedCard(
                            modifier = Modifier.clickable(onClick = onOpenChart),
                            radius = 14.dp,
                            alpha = 0.55f,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("四柱已成", color = Celadon, style = MaterialTheme.typography.titleMedium)
                                    Text(chart.fourPillarsText, color = Ink, style = MaterialTheme.typography.headlineMedium)
                                    Text(session.birthSummary, color = Slate, fontSize = 12.sp)
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
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FieldColumn(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = Celadon, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        content()
    }
}
