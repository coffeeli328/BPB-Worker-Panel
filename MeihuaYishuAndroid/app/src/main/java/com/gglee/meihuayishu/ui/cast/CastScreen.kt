package com.gglee.meihuayishu.ui.cast

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gglee.meihuayishu.AppContainer
import com.gglee.meihuayishu.engine.CastMethod
import com.gglee.meihuayishu.engine.MeihuaChart
import com.gglee.meihuayishu.engine.Trigram
import com.gglee.meihuayishu.ui.theme.Cinnabar
import com.gglee.meihuayishu.ui.theme.Ink
import com.gglee.meihuayishu.ui.theme.Muted
import com.gglee.meihuayishu.ui.theme.Pine
import com.gglee.meihuayishu.ui.theme.Wash
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastScreen(
    container: AppContainer,
    onChartReady: (MeihuaChart) -> Unit,
    viewModel: CastViewModel = viewModel(
        factory = CastViewModelFactory(container.historyRepository),
    ),
) {
    val uiState by viewModel.state.collectAsState()
    val request = uiState.request
    val scroll = rememberScrollState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val dateFmt = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text("梅花易数", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Text(
            "先天数起卦 · 体用解盘 · 可导出图片给 AI",
            fontSize = 14.sp,
            color = Muted,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        SectionLabel("所问之事")
        OutlinedTextField(
            value = request.question,
            onValueChange = { viewModel.updateRequest { question = it } },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("如：求职、出行、合作、感情…") },
            minLines = 2,
            maxLines = 4,
            colors = fieldColors(),
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel("起卦方式")
        val methodShort = mapOf(
            CastMethod.TIME to "时间",
            CastMethod.NUMBER to "数字",
            CastMethod.MANUAL to "手动",
            CastMethod.RANDOM to "随机",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CastMethod.entries.forEach { method ->
                FilterChip(
                    selected = request.method == method,
                    onClick = { viewModel.setMethod(method) },
                    label = { Text(methodShort.getValue(method)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Wash,
                        selectedLabelColor = Pine,
                    ),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        when (request.method) {
            CastMethod.TIME -> {
                SectionLabel("起卦时间")
                Text(dateFmt.format(Date(request.dateMillis)), color = Ink, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { showDatePicker = true }) { Text("选日期") }
                    TextButton(onClick = { showTimePicker = true }) { Text("选时间") }
                    TextButton(onClick = {
                        viewModel.updateRequest { dateMillis = System.currentTimeMillis() }
                    }) { Text("此刻") }
                }
                Text(
                    "上卦=(年支+月+日)÷8余；下卦再加时支；动爻=(年支+月+日+时支)÷6余。",
                    color = Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            CastMethod.NUMBER -> {
                SectionLabel("报数（正整数）")
                OutlinedTextField(
                    value = request.numberUpper,
                    onValueChange = { viewModel.updateRequest { numberUpper = it.filter(Char::isDigit) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("上卦数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = request.numberLower,
                    onValueChange = { viewModel.updateRequest { numberLower = it.filter(Char::isDigit) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("下卦数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = request.numberMoving,
                    onValueChange = { viewModel.updateRequest { numberMoving = it.filter(Char::isDigit) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("动爻数（可空，默认上下之和）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors(),
                )
            }
            CastMethod.MANUAL -> {
                SectionLabel("上卦（先天数 1–8）")
                TrigramPicker(request.manualUpper) {
                    viewModel.updateRequest { manualUpper = it }
                }
                Spacer(Modifier.height(12.dp))
                SectionLabel("下卦")
                TrigramPicker(request.manualLower) {
                    viewModel.updateRequest { manualLower = it }
                }
                Spacer(Modifier.height(12.dp))
                SectionLabel("动爻（1–6）")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..6).forEach { n ->
                        FilterChip(
                            selected = request.manualMoving == n,
                            onClick = { viewModel.updateRequest { manualMoving = n } },
                            label = { Text("$n") },
                        )
                    }
                }
            }
            CastMethod.RANDOM -> {
                Text(
                    "将随机生成两组正整数，按数字起卦法成卦。适合练习体用生克。",
                    color = Muted,
                    fontSize = 14.sp,
                )
            }
        }

        uiState.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Cinnabar, fontSize = 14.sp)
        }

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { viewModel.cast(onChartReady) },
            enabled = !uiState.loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Pine, contentColor = androidx.compose.ui.graphics.Color.White),
        ) {
            if (uiState.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(22.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("起卦解盘", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(40.dp))
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = request.dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val day = state.selectedDateMillis ?: return@TextButton
                    val cal = Calendar.getInstance().apply { timeInMillis = request.dateMillis }
                    val picked = Calendar.getInstance().apply { timeInMillis = day }
                    cal.set(Calendar.YEAR, picked.get(Calendar.YEAR))
                    cal.set(Calendar.MONTH, picked.get(Calendar.MONTH))
                    cal.set(Calendar.DAY_OF_MONTH, picked.get(Calendar.DAY_OF_MONTH))
                    viewModel.updateRequest { dateMillis = cal.timeInMillis }
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
        val cal = Calendar.getInstance().apply { timeInMillis = request.dateMillis }
        val state = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true,
        )
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val c = Calendar.getInstance().apply { timeInMillis = request.dateMillis }
                    c.set(Calendar.HOUR_OF_DAY, state.hour)
                    c.set(Calendar.MINUTE, state.minute)
                    viewModel.updateRequest { dateMillis = c.timeInMillis }
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TimePicker(state = state)
            }
        }
    }
}

@Composable
private fun TrigramPicker(selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Trigram.entries.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { t ->
                    FilterChip(
                        selected = selected == t.number,
                        onClick = { onSelect(t.number) },
                        label = { Text("${t.number}${t.hanzi}${t.symbol}") },
                    )
                }
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
        color = Pine,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Pine,
    unfocusedBorderColor = Wash,
    focusedLabelColor = Pine,
    cursorColor = Pine,
)
