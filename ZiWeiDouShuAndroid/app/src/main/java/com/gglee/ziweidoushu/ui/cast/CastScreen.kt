package com.gglee.ziweidoushu.ui.cast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gglee.ziweidoushu.AppContainer
import com.gglee.ziweidoushu.engine.Gender
import com.gglee.ziweidoushu.engine.ZiWeiChart
import com.gglee.ziweidoushu.ui.theme.Gold
import com.gglee.ziweidoushu.ui.theme.GoldDeep
import com.gglee.ziweidoushu.ui.theme.Ink
import com.gglee.ziweidoushu.ui.theme.Mist
import com.gglee.ziweidoushu.ui.theme.Muted
import com.gglee.ziweidoushu.ui.theme.Paper
import com.gglee.ziweidoushu.ui.theme.ShaStar
import com.gglee.ziweidoushu.ui.theme.Teal
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastScreen(
    container: AppContainer,
    onChartReady: (ZiWeiChart) -> Unit,
    viewModel: CastViewModel = viewModel(factory = CastViewModelFactory(container.historyRepository)),
) {
    val uiState by viewModel.state.collectAsState()
    val request = uiState.request
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Mist.copy(alpha = 0.55f), Paper, Mist.copy(alpha = 0.25f))),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text("紫微斗数", fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(
                "三合派安星 · 本地排盘学习参考",
                fontSize = 14.sp,
                color = Muted,
                modifier = Modifier.padding(bottom = 22.dp),
            )

            SectionLabel("命主")
            OutlinedTextField(
                value = request.name,
                onValueChange = { viewModel.updateRequest { name = it } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("可选：姓名或备注") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = fieldColors(),
            )
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Gender.entries.forEachIndexed { index, g ->
                    SegmentedButton(
                        selected = request.gender == g,
                        onClick = { viewModel.updateRequest { gender = g } },
                        shape = SegmentedButtonDefaults.itemShape(index, Gender.entries.size),
                    ) { Text(g.displayName) }
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel("历法")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(false to "公历", true to "农历").forEachIndexed { index, (lunar, label) ->
                    SegmentedButton(
                        selected = request.useLunar == lunar,
                        onClick = { viewModel.updateRequest { useLunar = lunar } },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                    ) { Text(label) }
                }
            }
            if (request.useLunar) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("闰月", color = Ink, modifier = Modifier.weight(1f))
                    Switch(
                        checked = request.lunarLeapMonth,
                        onCheckedChange = { viewModel.updateRequest { lunarLeapMonth = it } },
                        colors = SwitchDefaults.colors(checkedTrackColor = Teal),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("生辰")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = false,
                    onClick = { showDatePicker = true },
                    label = {
                        Text(
                            "%04d-%02d-%02d".format(request.year, request.month, request.day),
                            fontSize = 14.sp,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(containerColor = Mist, labelColor = Ink),
                )
                FilterChip(
                    selected = false,
                    onClick = { showTimePicker = true },
                    label = {
                        Text("%02d:%02d".format(request.hour, request.minute), fontSize = 14.sp)
                    },
                    colors = FilterChipDefaults.filterChipColors(containerColor = Mist, labelColor = Ink),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NumberField("年", request.year, 1900..2100, Modifier.weight(1f)) {
                    viewModel.updateRequest { year = it }
                }
                NumberField("月", request.month, 1..12, Modifier.weight(1f)) {
                    viewModel.updateRequest { month = it }
                }
                NumberField("日", request.day, 1..30, Modifier.weight(1f)) {
                    viewModel.updateRequest { day = it }
                }
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { viewModel.cast(onChartReady) },
                enabled = !uiState.loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Paper),
            ) {
                if (uiState.loading) {
                    CircularProgressIndicator(color = Paper, modifier = Modifier.height(22.dp))
                } else {
                    Text("起盘", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                }
            }
            uiState.error?.let {
                Text(it, color = ShaStar, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
            }
            Text(
                "排盘结果仅供学习参考，不构成命运断言。",
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 14.dp, bottom = 24.dp),
            )
        }
    }

    if (showDatePicker) {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(request.year, request.month - 1, request.day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = cal.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
                        viewModel.updateRequest {
                            year = c.get(Calendar.YEAR)
                            month = c.get(Calendar.MONTH) + 1
                            day = c.get(Calendar.DAY_OF_MONTH)
                        }
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) { DatePicker(state = pickerState) }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(request.hour, request.minute, is24Hour = true)
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateRequest {
                        hour = timeState.hour
                        minute = timeState.minute
                    }
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
        ) {
            Column(
                Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TimePicker(state = timeState)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = GoldDeep,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    range: IntRange,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit,
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { raw ->
            raw.filter { it.isDigit() }.toIntOrNull()?.let { n ->
                if (n in range) onChange(n)
            }
        },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = fieldColors(),
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Teal,
    unfocusedBorderColor = Muted.copy(alpha = 0.4f),
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    cursorColor = Gold,
    focusedContainerColor = Mist.copy(alpha = 0.35f),
    unfocusedContainerColor = Mist.copy(alpha = 0.2f),
)
