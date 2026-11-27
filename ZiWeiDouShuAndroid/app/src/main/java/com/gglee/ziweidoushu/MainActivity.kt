package com.gglee.ziweidoushu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.gglee.ziweidoushu.ui.ZiWeiApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val container = (application as ZiWeiApplication).container
        setContent {
            ZiWeiApp(container = container)
        }
    }
}
