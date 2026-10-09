package com.gglee.qimendunjia.ui.interpretation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gglee.qimendunjia.ai.AiClient
import com.gglee.qimendunjia.ai.AiError
import com.gglee.qimendunjia.ai.AiPrompt
import com.gglee.qimendunjia.ai.AiSettings
import com.gglee.qimendunjia.data.HistoryRepository
import com.gglee.qimendunjia.engine.QimenChart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InterpretationUiState(
    val aiText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loadedFromCache: Boolean = false,
)

class InterpretationViewModel(
    private val historyRepository: HistoryRepository,
    private val aiSettings: AiSettings,
    private val aiClient: AiClient,
) : ViewModel() {

    private val _state = MutableStateFlow(InterpretationUiState())
    val state: StateFlow<InterpretationUiState> = _state.asStateFlow()

    fun loadCached(chart: QimenChart) {
        viewModelScope.launch {
            val row = historyRepository.getById(chart.id.toString())
            val model = aiSettings.resolvedModel
            val key = AiPrompt.cacheKey(chart, model)
            if (row?.aiReadingCacheKey == key && !row.aiReadingText.isNullOrBlank()) {
                _state.update {
                    it.copy(aiText = row.aiReadingText!!, loadedFromCache = true, errorMessage = null)
                }
            }
        }
    }

    fun requestAi(chart: QimenChart) {
        if (!aiSettings.isConfigured) {
            _state.update { it.copy(errorMessage = AiError.NotConfigured().message) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val config = AiClient.Config(
                    baseUrl = aiSettings.resolvedBaseUrl,
                    apiKey = aiSettings.apiKey,
                    model = aiSettings.resolvedModel,
                )
                val text = aiClient.interpret(chart, config)
                val cacheKey = AiPrompt.cacheKey(chart, config.model)
                historyRepository.updateAiReading(chart.id.toString(), text, cacheKey)
                _state.update { it.copy(aiText = text, isLoading = false, loadedFromCache = false) }
            } catch (e: AiError) {
                _state.update { it.copy(isLoading = false, errorMessage = e.message) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "未知错误")
                }
            }
        }
    }
}
