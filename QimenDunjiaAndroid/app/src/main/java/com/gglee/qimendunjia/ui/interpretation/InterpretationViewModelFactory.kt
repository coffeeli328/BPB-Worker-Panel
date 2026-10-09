package com.gglee.qimendunjia.ui.interpretation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gglee.qimendunjia.AppContainer

class InterpretationViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InterpretationViewModel::class.java)) {
            return InterpretationViewModel(
                container.historyRepository,
                container.aiSettings,
                container.aiClient,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
