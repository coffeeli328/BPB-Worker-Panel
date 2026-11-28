package com.gglee.ziweidoushu.ui.interpretation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gglee.ziweidoushu.AppContainer
import com.gglee.ziweidoushu.ai.AiError
import com.gglee.ziweidoushu.ai.AiPrompt
import com.gglee.ziweidoushu.engine.InterpretationTone
import com.gglee.ziweidoushu.engine.ZiWeiChart
import com.gglee.ziweidoushu.ui.theme.GoldDeep
import com.gglee.ziweidoushu.ui.theme.Ink
import com.gglee.ziweidoushu.ui.theme.Mist
import com.gglee.ziweidoushu.ui.theme.Muted
import com.gglee.ziweidoushu.ui.theme.Paper
import com.gglee.ziweidoushu.ui.theme.ShaStar
import com.gglee.ziweidoushu.ui.theme.Teal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InterpretationUiState(
    val aiText: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

class InterpretationViewModel(
    private val chart: ZiWeiChart,
    private val container: AppContainer,
) : ViewModel() {
    private val _state = MutableStateFlow(InterpretationUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val row = container.historyRepository.getById(chart.id.toString()) ?: return@launch
            val key = AiPrompt.cacheKey(chart, container.aiSettings.resolvedModel)
            if (row.aiReadingText != null && row.aiReadingCacheKey == key) {
                _state.update { it.copy(aiText = row.aiReadingText) }
            }
        }
    }

    fun askAi() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val text = container.aiClient.interpret(chart, container.aiSettings)
                val key = AiPrompt.cacheKey(chart, container.aiSettings.resolvedModel)
                container.historyRepository.updateAiReading(chart.id.toString(), text, key)
                _state.update { it.copy(loading = false, aiText = text) }
            } catch (e: AiError) {
                _state.update { it.copy(loading = false, error = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }
}

class InterpretationViewModelFactory(
    private val chart: ZiWeiChart,
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        InterpretationViewModel(chart, container) as T
}

@Composable
fun InterpretationScreen(
    chart: ZiWeiChart,
    container: AppContainer,
    onBack: () -> Unit,
    viewModel: InterpretationViewModel = viewModel(
        factory = InterpretationViewModelFactory(chart, container),
    ),
) {
    val ui by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("命盘解读", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Text(chart.title, fontSize = 14.sp, color = Muted, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

        chart.interpretations.forEach { item ->
            val accent = when (item.tone) {
                InterpretationTone.POSITIVE -> Teal
                InterpretationTone.CAUTION -> ShaStar
                InterpretationTone.NEUTRAL -> GoldDeep
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(Mist.copy(alpha = 0.4f))
                    .padding(12.dp),
            ) {
                Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = accent)
                Text(item.detail, fontSize = 14.sp, color = Ink, modifier = Modifier.padding(top = 6.dp), lineHeight = 22.sp)
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = viewModel::askAi,
            enabled = !ui.loading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = GoldDeep, contentColor = Paper),
        ) {
            if (ui.loading) CircularProgressIndicator(color = Paper, modifier = Modifier.height(22.dp))
            else Text(if (ui.aiText == null) "AI 深度解读（可选）" else "重新生成 AI 解读")
        }
        ui.error?.let {
            Text(it, color = ShaStar, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        ui.aiText?.let { text ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(Mist.copy(alpha = 0.5f))
                    .padding(12.dp),
            ) {
                Text("AI 解读", fontWeight = FontWeight.SemiBold, color = GoldDeep)
                Text(text, color = Ink, fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
        ) { Text("返回命盘") }
    }
}
