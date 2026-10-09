package com.gglee.qimendunjia.ui.cast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gglee.qimendunjia.data.HistoryRepository

class CastViewModelFactory(
    private val historyRepository: HistoryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CastViewModel::class.java)) {
            return CastViewModel(historyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
