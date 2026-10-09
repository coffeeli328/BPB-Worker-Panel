package com.gglee.qimendunjia.ui.cast

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gglee.qimendunjia.AppContainer
import com.gglee.qimendunjia.engine.CalendarInputMode
import com.gglee.qimendunjia.engine.GanzhiCalendar
import com.gglee.qimendunjia.engine.JuMethod
import com.gglee.qimendunjia.engine.LocationPreset
import com.gglee.qimendunjia.engine.QimenChart
import com.gglee.qimendunjia.engine.QuestionTopic
import com.gglee.qimendunjia.ui.theme.Cinnabar
import com.gglee.qimendunjia.ui.theme.Ink
import com.gglee.qimendunjia.ui.theme.Muted
import com.gglee.qimendunjia.ui.theme.Pine
import com.gglee.qimendunjia.ui.theme.Wash
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastScreen(
    container: AppContainer,
    onChartReady: (QimenChart) -> Unit,
    viewModel: CastViewModel = viewModel(
        factory = CastViewModelFactory(container.historyRepository),
    ),
) {
    val uiState by viewModel.state.collectAsState()
    val request = uiState.request
    val scroll = rememberScrollState()

    val tz = remember(request.timeZoneSecondsFromGMT) {
        val offset = request.timeZoneSecondsFromGMT
        if (offset != null) {
            val gmt = TimeZone.getTimeZone(String.format("GMT%+d", offset / 3600))
            if (gmt.rawOffset == offset) gmt else java.util.SimpleTimeZone(offset, "CustomGMT$offset")
        } else {
            TimeZone.getDefault()
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = "奇门遁甲",
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
        )
        Text(
            text = "时家奇门 · 学习参考",
            fontSize = 14.sp,
            color = Muted,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        SectionLabel("所问之事")
        OutlinedTextField(
            value = request.question,
            onValueChange = { viewModel.updateRequest { question = it } },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("必填：如求财、出行、婚姻、合作…") },
            minLines = 2,
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Pine,
                cursorColor = Pine,
            ),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuestionTopic.quickTags.forEach { tag ->
                FilterChip(
                    selected = false,
                    onClick = {
                        viewModel.updateRequest {
                            val t = question.trim()
                            question = when {
                                t.isEmpty() -> tag.rawValue
                                t.contains(tag.rawValue) -> t
                                t.endsWith("、") -> t + tag.rawValue
                                else -> "$t、${tag.rawValue}"
                            }
                        }
                    },
                    label = { Text(tag.rawValue, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Wash,
                        labelColor = Ink,
                    ),
                )
            }
        }
        val questionTrimmed = request.question.trim()
        if (uiState.showQuestionHint && questionTrimmed.isEmpty()) {
            Text(
                "请先填写所问之事，解读才会针对该问题展开。",
                color = Cinnabar,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        } else if (questionTrimmed.isNotEmpty()) {
            Text(
                "将按「${QuestionTopic.detect(questionTrimmed).rawValue}」用神侧重解读。",
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Spacer(Modifier.height(22.dp))

        SectionLabel("时刻")
        val modes = CalendarInputMode.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = request.calendarMode == mode,
                    onClick = { viewModel.updateRequest { calendarMode = mode } },
                    shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                ) {
                    Text(mode.rawValue)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = { showDatePicker = true }) {
                Text("日期：${formatDate(request.date, tz)}")
            }
            TextButton(onClick = { showTimePicker = true }) {
                Text("时间：${formatTime(request.date, tz)}")
            }
        }
        if (request.calendarMode == CalendarInputMode.LUNAR) {
            Text(
                "农历参考：${GanzhiCalendar.lunarDescription(request.date, tz)}",
                color = Muted,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(22.dp))

        SectionLabel("地点与时制")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("东八区 UTC+8", modifier = Modifier.weight(1f))
            Switch(
                checked = request.timeZoneSecondsFromGMT == 8 * 3600,
                onCheckedChange = { on ->
                    viewModel.updateRequest {
                        timeZoneSecondsFromGMT = if (on) 8 * 3600 else null
                    }
                },
                colors = SwitchDefaults.colors(checkedTrackColor = Pine),
            )
        }
        Spacer(Modifier.height(8.dp))
        var expandedPreset by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = LocationPreset.all.firstOrNull { it.id == uiState.presetId }?.let {
                "${it.name}  ${"%.1f".format(it.longitude)}°E"
            } ?: "",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("地点") },
            trailingIcon = {
                TextButton(onClick = { expandedPreset = !expandedPreset }) {
                    Text(if (expandedPreset) "收起" else "选择")
                }
            },
        )
        if (expandedPreset) {
            LocationPreset.all.forEach { preset ->
                TextButton(
                    onClick = {
                        viewModel.setPresetId(preset.id)
                        expandedPreset = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("${preset.name}  ${"%.1f".format(preset.longitude)}°E", color = Ink)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("真太阳时", modifier = Modifier.weight(1f))
            Switch(
                checked = request.useTrueSolarTime,
                onCheckedChange = { viewModel.updateRequest { useTrueSolarTime = it } },
                colors = SwitchDefaults.colors(checkedTrackColor = Pine),
            )
        }
        Spacer(Modifier.height(22.dp))

        SectionLabel("定局方法")
        val juMethods = JuMethod.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            juMethods.forEachIndexed { index, method ->
                SegmentedButton(
                    selected = request.juMethod == method,
                    onClick = { viewModel.setJuMethod(method) },
                    shape = SegmentedButtonDefaults.itemShape(index, juMethods.size),
                ) {
                    Text(method.rawValue)
                }
            }
        }
        Text(
            request.juMethod.detail,
            color = Muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { viewModel.cast(onChartReady) },
            enabled = !uiState.isCasting,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Pine),
        ) {
            if (uiState.isCasting) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                    color = Wash,
                )
            } else {
                Text("起局排盘")
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = request.date.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        viewModel.updateRequest { date = mergeDate(request.date, millis, tz) }
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance(tz).apply { time = request.date }
        val timeState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
        )
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateRequest {
                        date = mergeTime(date, timeState.hour, timeState.minute, tz)
                    }
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
        ) {
            TimePicker(state = timeState)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = Muted,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

private fun formatDate(date: Date, tz: TimeZone): String {
    val c = Calendar.getInstance(tz).apply { time = date }
    return "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH) + 1}-${c.get(Calendar.DAY_OF_MONTH)}"
}

private fun formatTime(date: Date, tz: TimeZone): String {
    val c = Calendar.getInstance(tz).apply { time = date }
    return "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
}

private fun mergeDate(existing: Date, dateMillis: Long, tz: TimeZone): Date {
    val src = Calendar.getInstance(tz).apply { time = existing }
    val pick = Calendar.getInstance(tz).apply { time = Date(dateMillis) }
    pick.set(Calendar.HOUR_OF_DAY, src.get(Calendar.HOUR_OF_DAY))
    pick.set(Calendar.MINUTE, src.get(Calendar.MINUTE))
    pick.set(Calendar.SECOND, 0)
    pick.set(Calendar.MILLISECOND, 0)
    return pick.time
}

private fun mergeTime(existing: Date, hour: Int, minute: Int, tz: TimeZone): Date {
    val cal = Calendar.getInstance(tz).apply { time = existing }
    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, minute)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.time
}
