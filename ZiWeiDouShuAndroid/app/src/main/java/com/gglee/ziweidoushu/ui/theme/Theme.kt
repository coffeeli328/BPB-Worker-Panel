package com.gglee.ziweidoushu.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Night-observatory palette: deep ink navy + warm gold (not cream/terracotta, not purple). */
val Ink = Color(0xFF15202B)
val Night = Color(0xFF1B2A3A)
val Paper = Color(0xFFF3F0E8)
val Mist = Color(0xFFE6EAF0)
val MistDeep = Color(0xFFD5DCE6)
val Gold = Color(0xFFB08D3E)
val GoldDeep = Color(0xFF8A6B2A)
val Teal = Color(0xFF2F5D62)
val Muted = Color(0xFF66717C)
val Line = Color(0x3315202B)
val MajorStar = Color(0xFF8A6B2A)
val AuxStar = Color(0xFF2F5D62)
val ShaStar = Color(0xFF8B3A3A)

private val LightColorScheme = lightColorScheme(
    primary = Teal,
    onPrimary = Paper,
    primaryContainer = Mist,
    onPrimaryContainer = Ink,
    secondary = GoldDeep,
    onSecondary = Paper,
    secondaryContainer = MistDeep,
    onSecondaryContainer = Ink,
    tertiary = Muted,
    onTertiary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Muted,
    outline = Line,
    outlineVariant = Line,
    error = ShaStar,
    onError = Paper,
)

@Composable
fun ZiWeiTheme(content: @Composable () -> Unit) {
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
    MaterialTheme(colorScheme = LightColorScheme, content = content)
}
