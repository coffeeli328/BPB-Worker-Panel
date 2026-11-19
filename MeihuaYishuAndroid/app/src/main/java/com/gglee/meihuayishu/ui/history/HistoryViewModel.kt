package com.gglee.meihuayishu.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gglee.meihuayishu.data.HistoryEntity
import com.gglee.meihuayishu.data.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(
    historyRepository: HistoryRepository,
) : ViewModel() {
    val rows: StateFlow<List<HistoryEntity>> = historyRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

class HistoryViewModelFactory(
    private val historyRepository: HistoryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HistoryViewModel(historyRepository) as T
    }
}
