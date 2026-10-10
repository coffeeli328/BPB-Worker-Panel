package com.gglee.xhotpost

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.gglee.xhotpost.ui.HotpostApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val app = application as? HotpostApplication
        if (app == null) {
            Log.e(TAG, "Unexpected Application type")
            finish()
            return
        }
        setContent {
            HotpostApp(container = app.container)
        }
    }

    companion object {
        private const val TAG = "HotpostMain"
    }
}
