package com.gglee.qimendunjia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.gglee.qimendunjia.ui.QimenApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep system windows fitting content so adjustResize + IME work reliably.
        // (enableEdgeToEdge + adjustResize often breaks soft keyboard / focus.)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val container = (application as QimenApplication).container
        setContent {
            QimenApp(container = container)
        }
    }
}
