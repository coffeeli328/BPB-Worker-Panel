package com.gglee.qimendunjia.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val Ink = Color(0xFF1F241F)
val Paper = Color(0xFFF7F3E8)
val Wash = Color(0xFFEDE6D6)
val WashDeep = Color(0xFFE0D6C4)
val Pine = Color(0xFF335C4D)
val Cinnabar = Color(0xFF943328)
val Muted = Color(0xFF6B665C)
val Line = Color(0x331F241F)
val HeavenStem = Color(0xFF2A4A6B)
val EarthStem = Color(0xFF5C4A32)

private val LightColorScheme = lightColorScheme(
    primary = Pine,
    onPrimary = Paper,
    primaryContainer = Wash,
    onPrimaryContainer = Ink,
    secondary = Cinnabar,
    onSecondary = Paper,
    secondaryContainer = WashDeep,
    onSecondaryContainer = Ink,
    tertiary = Muted,
    onTertiary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Wash,
    onSurfaceVariant = Muted,
    outline = Line,
    outlineVariant = Line,
    error = Cinnabar,
    onError = Paper,
)

@Composable
fun QimenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Paper.toArgb()
            window.navigationBarColor = Paper.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
