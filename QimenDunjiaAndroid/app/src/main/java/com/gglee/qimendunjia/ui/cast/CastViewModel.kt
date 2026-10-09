package com.gglee.qimendunjia.ui.cast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gglee.qimendunjia.data.HistoryRepository
import com.gglee.qimendunjia.engine.ChartRequest
import com.gglee.qimendunjia.engine.JuMethod
import com.gglee.qimendunjia.engine.LocationPreset
import com.gglee.qimendunjia.engine.QimenChart
import com.gglee.qimendunjia.engine.QimenEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CastUiState(
    val request: ChartRequest = ChartRequest(),
    val presetId: String = "beijing",
    val isCasting: Boolean = false,
    val showQuestionHint: Boolean = false,
)

class CastViewModel(
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CastUiState())
    val state: StateFlow<CastUiState> = _state.asStateFlow()

    fun updateRequest(block: ChartRequest.() -> Unit) {
        // Must copy ChartRequest: in-place apply() mutates the same instance so
        // CastUiState equals() sees no change and StateFlow skips emission —
        // controlled TextFields then appear to reject all input.
        _state.update { state ->
            val next = state.request.copy().apply(block)
            state.copy(
                request = next,
                showQuestionHint = if (next.question.trim().isNotEmpty()) false else state.showQuestionHint,
            )
        }
    }

    fun setPresetId(id: String) {
        val preset = LocationPreset.all.firstOrNull { it.id == id } ?: return
        _state.update { state ->
            state.copy(
                presetId = id,
                request = state.request.copy(
                    locationNote = preset.name,
                    longitude = preset.longitude,
                ),
            )
        }
    }

    fun setShowQuestionHint(show: Boolean) {
        _state.update { it.copy(showQuestionHint = show) }
    }

    fun cast(onSuccess: (QimenChart) -> Unit) {
        val req = _state.value.request
        if (req.question.trim().isEmpty()) {
            _state.update { it.copy(showQuestionHint = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isCasting = true) }
            try {
                val chart = QimenEngine.generate(req)
                historyRepository.saveChart(chart)
                onSuccess(chart)
            } finally {
                _state.update { it.copy(isCasting = false) }
            }
        }
    }

    fun setJuMethod(method: JuMethod) {
        _state.update { state ->
            state.copy(request = state.request.copy(juMethod = method))
        }
    }
}
