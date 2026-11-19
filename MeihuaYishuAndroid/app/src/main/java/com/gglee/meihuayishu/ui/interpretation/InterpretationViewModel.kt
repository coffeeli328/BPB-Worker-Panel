package com.gglee.meihuayishu.ui.interpretation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gglee.meihuayishu.AppContainer
import com.gglee.meihuayishu.ai.AiClient
import com.gglee.meihuayishu.ai.AiPrompt
import com.gglee.meihuayishu.engine.MeihuaChart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InterpretationUiState(
    val loading: Boolean = false,
    val text: String? = null,
    val error: String? = null,
)

class InterpretationViewModel(
    private val chart: MeihuaChart,
    private val container: AppContainer,
) : ViewModel() {
    private val _state = MutableStateFlow(InterpretationUiState())
    val state: StateFlow<InterpretationUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val cached = container.historyRepository.getById(chart.id.toString())
            val key = AiPrompt.cacheKey(chart, container.aiSettings.resolvedModel)
            if (cached?.aiReadingCacheKey == key && !cached.aiReadingText.isNullOrBlank()) {
                _state.update { it.copy(text = cached.aiReadingText) }
            }
        }
    }

    fun run() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val settings = container.aiSettings
                val text = container.aiClient.interpret(
                    chart,
                    AiClient.Config(
                        baseUrl = settings.resolvedBaseUrl,
                        apiKey = settings.apiKey,
                        model = settings.resolvedModel,
                    ),
                )
                val key = AiPrompt.cacheKey(chart, settings.resolvedModel)
                container.historyRepository.updateAiReading(chart.id.toString(), text, key)
                _state.update { it.copy(loading = false, text = text) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = e.message ?: "解读失败")
                }
            }
        }
    }
}

class InterpretationViewModelFactory(
    private val chart: MeihuaChart,
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return InterpretationViewModel(chart, container) as T
    }
}
