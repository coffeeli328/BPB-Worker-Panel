package com.gglee.ziweidoushu.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.gglee.ziweidoushu.engine.ZiWeiChart

class ChartSessionViewModel : ViewModel() {
    var currentChart: ZiWeiChart? by mutableStateOf(null)
}
