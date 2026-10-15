package com.gglee.baziyuce.ui.birth

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gglee.baziyuce.BaZiSessionViewModel
import com.gglee.baziyuce.engine.Gender
import com.gglee.baziyuce.ui.theme.Celadon
import com.gglee.baziyuce.ui.theme.CeladonSoft
import com.gglee.baziyuce.ui.theme.Ink
import com.gglee.baziyuce.ui.theme.Line
import com.gglee.baziyuce.ui.theme.ScreenBackground
import com.gglee.baziyuce.ui.theme.Slate
import java.util.Locale

@Composable
fun BirthInputScreen(
    session: BaZiSessionViewModel,
    modifier: Modifier = Modifier,
    onOpenChart: () -> Unit,
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    // Local text buffers so typing "199" mid-edit does not clamp oddly
    var yearText by remember { mutableStateOf(session.birthYear.toString()) }
    var dayText by remember { mutableStateOf(session.birthDay.toString()) }

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
                        "分别填写出生年、月、日、时，排出四柱，并据流年、流月、流日给出规则化运势提示。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate,
                    )
                }
            }

            Labeled("出生年月日时") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        NumberField(
                            label = "年",
                            value = yearText,
                            modifier = Modifier.weight(1.2f),
                            onValueChange = { raw ->
                                val filtered = raw.filter { it.isDigit() }.take(4)
                                yearText = filtered
                                filtered.toIntOrNull()?.let { session.updateBirthYear(it) }
                            },
                        )
                        CompactDropdown(
                            label = "月",
                            selected = session.birthMonth,
                            options = (1..12).map { it to "${it}月" },
                            modifier = Modifier.weight(1f),
                            onSelect = {
                                session.updateBirthMonth(it)
                                val max = BaZiSessionViewModel.daysInMonth(session.birthYear, it)
                                if (session.birthDay > max) {
                                    dayText = max.toString()
                                }
                            },
                        )
                        NumberField(
                            label = "日",
                            value = dayText,
                            modifier = Modifier.weight(1f),
                            onValueChange = { raw ->
                                val filtered = raw.filter { it.isDigit() }.take(2)
                                dayText = filtered
                                filtered.toIntOrNull()?.let { session.updateBirthDay(it) }
                            },
                        )
                    }

                    CompactDropdown(
                        label = "时（时辰）",
                        selected = nearestShichenHour(session.birthHour),
                        options = BaZiSessionViewModel.SHICHEN_OPTIONS,
                        modifier = Modifier.fillMaxWidth(),
                        onSelect = { session.updateBirthHour(it) },
                    )

                    Text(
                        "子时若按 23 点起算会换日柱；本列表子时默认取 0 点。如需 23 点，可在下方选「精确钟点」。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate,
                    )

                    CompactDropdown(
                        label = "精确钟点（0–23）",
                        selected = session.birthHour,
                        options = (0..23).map { h ->
                            h to String.format(Locale.CHINA, "%02d 点 · %s", h, shortShichenName(h))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        onSelect = { session.updateBirthHour(it) },
                    )
                }
            }

            session.birthInputError?.let { err ->
                Text(err, color = Color(0xFF943328), style = MaterialTheme.typography.bodyMedium)
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
                onClick = {
                    // Sync text fields in case year/day were left incomplete
                    yearText.toIntOrNull()?.let { session.updateBirthYear(it) }
                    dayText.toIntOrNull()?.let { session.updateBirthDay(it) }
                    session.calculate()
                },
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

private fun nearestShichenHour(hour: Int): Int {
    val idx = ((hour.coerceIn(0, 23) + 1) / 2) % 12
    return if (idx == 0) 0 else idx * 2
}

private fun shortShichenName(hour: Int): String {
    val names = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    val idx = ((hour.coerceIn(0, 23) + 1) / 2) % 12
    return names[idx] + "时"
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Celadon,
        unfocusedBorderColor = Line,
        focusedLabelColor = CeladonSoft,
        unfocusedLabelColor = Slate,
        cursorColor = Celadon,
        focusedTextColor = Ink,
        unfocusedTextColor = Ink,
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        colors = colors,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactDropdown(
    label: String,
    selected: Int,
    options: List<Pair<Int, String>>,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val display = options.firstOrNull { it.first == selected }?.second
        ?: selected.toString()
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Celadon,
        unfocusedBorderColor = Line,
        focusedLabelColor = CeladonSoft,
        unfocusedLabelColor = Slate,
        focusedTextColor = Ink,
        unfocusedTextColor = Ink,
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = colors,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    },
                )
            }
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
