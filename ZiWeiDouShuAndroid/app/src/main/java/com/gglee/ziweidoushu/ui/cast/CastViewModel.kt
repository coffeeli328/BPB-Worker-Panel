package com.gglee.ziweidoushu.ui.cast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gglee.ziweidoushu.data.HistoryRepository
import com.gglee.ziweidoushu.engine.CastRequest
import com.gglee.ziweidoushu.engine.Gender
import com.gglee.ziweidoushu.engine.ZiWeiChart
import com.gglee.ziweidoushu.engine.ZiWeiEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class CastUiState(
    val request: CastRequest = defaultRequest(),
    val loading: Boolean = false,
    val error: String? = null,
)

private fun defaultRequest(): CastRequest {
    val cal = Calendar.getInstance()
    return CastRequest(
        year = cal.get(Calendar.YEAR) - 25,
        month = cal.get(Calendar.MONTH) + 1,
        day = cal.get(Calendar.DAY_OF_MONTH),
        hour = 12,
        minute = 0,
        gender = Gender.MALE,
    )
}

class CastViewModel(
    private val historyRepository: HistoryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CastUiState())
    val state: StateFlow<CastUiState> = _state.asStateFlow()

    fun updateRequest(block: CastRequest.() -> Unit) {
        _state.update { it.copy(request = it.request.copy().apply(block), error = null) }
    }

    fun cast(onReady: (ZiWeiChart) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val chart = ZiWeiEngine.cast(_state.value.request)
                historyRepository.saveChart(chart)
                _state.update { it.copy(loading = false) }
                onReady(chart)
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = e.message ?: "排盘失败，请检查生辰输入")
                }
            }
        }
    }
}

class CastViewModelFactory(
    private val historyRepository: HistoryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CastViewModel(historyRepository) as T
}
