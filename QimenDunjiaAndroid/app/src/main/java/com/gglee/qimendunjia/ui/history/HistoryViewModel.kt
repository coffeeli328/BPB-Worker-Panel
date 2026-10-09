package com.gglee.qimendunjia.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gglee.qimendunjia.data.HistoryEntity
import com.gglee.qimendunjia.data.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(
    repository: HistoryRepository,
) : ViewModel() {
    val items: StateFlow<List<HistoryEntity>> =
        repository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
