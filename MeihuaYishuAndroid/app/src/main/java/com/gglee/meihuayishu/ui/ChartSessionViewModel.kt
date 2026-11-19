package com.gglee.meihuayishu.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.gglee.meihuayishu.engine.MeihuaChart

class ChartSessionViewModel : ViewModel() {
    var currentChart: MeihuaChart? by mutableStateOf(null)
}
