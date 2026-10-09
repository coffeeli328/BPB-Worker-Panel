package com.gglee.baziyuce.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val Ink = Color(0xFF1A2429)
val Mist = Color(0xFFE6EDEF)
val MistDeep = Color(0xFFC7D9DB)
val Celadon = Color(0xFF386B61)
val CeladonSoft = Color(0xFF598C80)
val Slate = Color(0xFF5C6B70)
val Line = Color(0x382D474D)
val Accent = Color(0xFF8C6138)
val MistTop = Color(0xFFD1E6E8)
val MistBottom = Color(0xFFE0E6DB)

private val LightColorScheme = lightColorScheme(
    primary = Celadon,
    onPrimary = Mist,
    primaryContainer = MistDeep,
    onPrimaryContainer = Ink,
    secondary = Accent,
    onSecondary = Mist,
    secondaryContainer = MistDeep,
    onSecondaryContainer = Ink,
    tertiary = Slate,
    onTertiary = Mist,
    background = Mist,
    onBackground = Ink,
    surface = Mist,
    onSurface = Ink,
    surfaceVariant = Color(0xCCFFFFFF),
    onSurfaceVariant = Slate,
    outline = Line,
)

val BaZiTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        color = Ink,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        color = Ink,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        color = Ink,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = CeladonSoft,
        letterSpacing = 1.2.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = Ink,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        color = Slate,
    ),
)

@Composable
@Suppress("UNUSED_PARAMETER")
fun BaZiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Light mist/celadon only — matches iOS AppTheme (no dark mode in v1)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Mist.toArgb()
            window.navigationBarColor = Mist.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = BaZiTypography,
        content = content,
    )
}

@Composable
fun ScreenBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Mist)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MistTop.copy(alpha = 0.95f),
                        Mist,
                        MistBottom.copy(alpha = 0.7f),
                    ),
                    start = Offset.Zero,
                    end = Offset(900f, 1600f),
                ),
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(Celadon.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(900f, 80f),
                    radius = 420f,
                ),
            ),
        content = content,
    )
}
