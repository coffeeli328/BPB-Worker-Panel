package com.gglee.meihuayishu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.gglee.meihuayishu.ui.MeihuaApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val container = (application as MeihuaApplication).container
        setContent {
            MeihuaApp(container = container)
        }
    }
}
