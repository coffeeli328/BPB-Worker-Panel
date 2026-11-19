package com.gglee.meihuayishu.ui.cast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gglee.meihuayishu.data.HistoryRepository
import com.gglee.meihuayishu.engine.CastMethod
import com.gglee.meihuayishu.engine.CastRequest
import com.gglee.meihuayishu.engine.MeihuaChart
import com.gglee.meihuayishu.engine.MeihuaEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CastUiState(
    val request: CastRequest = CastRequest(),
    val loading: Boolean = false,
    val error: String? = null,
    val lastChart: MeihuaChart? = null,
)

class CastViewModel(
    private val historyRepository: HistoryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CastUiState())
    val state: StateFlow<CastUiState> = _state.asStateFlow()

    fun updateRequest(block: CastRequest.() -> Unit) {
        _state.update { s ->
            s.copy(request = s.request.copy().also(block), error = null)
        }
    }

    fun setMethod(method: CastMethod) {
        updateRequest { this.method = method }
    }

    fun cast(onReady: (MeihuaChart) -> Unit) {
        val req = _state.value.request
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val chart = MeihuaEngine.cast(req)
                historyRepository.saveChart(chart)
                _state.update { it.copy(loading = false, lastChart = chart) }
                onReady(chart)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = e.message ?: "起卦失败",
                    )
                }
            }
        }
    }
}

class CastViewModelFactory(
    private val historyRepository: HistoryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CastViewModel(historyRepository) as T
    }
}
