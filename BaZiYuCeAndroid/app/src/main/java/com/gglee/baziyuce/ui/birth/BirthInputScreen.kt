package com.gglee.baziyuce.ui.birth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gglee.baziyuce.BaZiSessionViewModel
import com.gglee.baziyuce.engine.Gender
import com.gglee.baziyuce.engine.ShiChen
import com.gglee.baziyuce.ui.components.SegmentedControl
import com.gglee.baziyuce.ui.components.WheelIntPicker
import com.gglee.baziyuce.ui.theme.Celadon
import com.gglee.baziyuce.ui.theme.CeladonSoft
import com.gglee.baziyuce.ui.theme.ErrorRed
import com.gglee.baziyuce.ui.theme.Ink
import com.gglee.baziyuce.ui.theme.Line
import com.gglee.baziyuce.ui.theme.ScreenBackground
import com.gglee.baziyuce.ui.theme.Slate

/**
 * Strict mirror of iOS `BirthInputView.swift`.
 */
@Composable
fun BirthInputScreen(
    session: BaZiSessionViewModel,
    modifier: Modifier = Modifier,
    onOpenChart: () -> Unit,
) {
    var appeared by remember { mutableStateOf(false) }
    var didCalculate by remember { mutableStateOf(false) }
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            // Hero — iOS VStack spacing 10 + padding.top 12
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AnimatedVisibility(
                    visible = appeared,
                    enter = fadeIn() + slideInVertically { 12 },
                ) {
                    Text(
                        "八字运势",
                        color = Ink,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Serif,
                    )
                }
                AnimatedVisibility(
                    visible = appeared,
                    enter = fadeIn() + slideInVertically { 8 },
                ) {
                    Text(
                        "分别填写出生年、月、日与时辰，排出四柱，并据流年、流月、流日给出规则化运势提示。",
                        color = Slate,
                        fontSize = 17.sp, // iOS .body
                        lineHeight = 22.sp,
                    )
                }
            }

            // Form — iOS VStack spacing 18
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Labeled("出生年月日") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // iOS: equal-width columns; HStack height 140 includes caption + wheel
                        FieldColumn("年", Modifier.weight(1f).fillMaxHeight()) {
                            WheelIntPicker(
                                value = session.birthYear,
                                values = years,
                                label = { "${it}年" },
                                itemHeight = 28.dp,
                                visibleCount = 5,
                                onValueChange = { session.updateBirthYear(it) },
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            )
                        }
                        FieldColumn("月", Modifier.weight(1f).fillMaxHeight()) {
                            WheelIntPicker(
                                value = session.birthMonth,
                                values = months,
                                label = { "${it}月" },
                                itemHeight = 28.dp,
                                visibleCount = 5,
                                onValueChange = { session.updateBirthMonth(it) },
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            )
                        }
                        FieldColumn("日", Modifier.weight(1f).fillMaxHeight()) {
                            WheelIntPicker(
                                value = session.birthDay.coerceIn(1, session.maxDayInMonth),
                                values = days,
                                label = { "${it}日" },
                                itemHeight = 28.dp,
                                visibleCount = 5,
                                onValueChange = { session.updateBirthDay(it) },
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            )
                        }
                    }
                }

                Labeled("时辰") {
                    WheelIntPicker(
                        value = session.birthShiChenIndex,
                        values = shiChenIndices,
                        label = { idx -> ShiChen.all[idx].displayName },
                        itemHeight = 24.dp,
                        visibleCount = 5,
                        onValueChange = { session.updateBirthShiChenIndex(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                    )
                    Text(
                        "当前：${session.birthShiChen.displayName}",
                        color = Slate,
                        fontSize = 13.sp, // footnote
                    )
                }

                Labeled("性别") {
                    SegmentedControl(
                        options = Gender.entries.map { it to it.displayName },
                        selected = session.gender,
                        onSelect = { session.updateGender(it) },
                    )
                }

                session.inputError?.let { err ->
                    Text(err, color = ErrorRed, fontSize = 13.sp)
                }

                // iOS: padding.top 4 + vertical 14 + headline
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Celadon)
                        .clickable {
                            if (session.calculate()) {
                                didCalculate = true
                            }
                        }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        if (didCalculate || session.didCalculate) "重新排盘" else "排出八字",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                AnimatedVisibility(
                    visible = session.chart != null,
                    enter = fadeIn() + slideInVertically { it / 3 },
                ) {
                    val chart = session.chart
                    if (chart != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.55f))
                                .border(1.dp, Line, RoundedCornerShape(14.dp))
                                .clickable(onClick = onOpenChart)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    "四柱已成",
                                    color = Celadon,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp,
                                )
                                Text(
                                    chart.fourPillarsText,
                                    color = Ink,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Serif,
                                )
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
    }
}

@Composable
private fun Labeled(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

@Composable
private fun FieldColumn(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = {
            Text(title, color = Celadon, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            content()
        },
    )
}
