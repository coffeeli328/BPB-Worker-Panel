package com.gglee.baziyuce

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.gglee.baziyuce.ui.BaZiApp

class MainActivity : ComponentActivity() {
    private val session: BaZiSessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BaZiApp(session = session)
        }
    }
}
