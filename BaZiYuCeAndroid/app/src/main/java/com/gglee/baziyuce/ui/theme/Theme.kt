package com.gglee.baziyuce.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Exact iOS AppTheme RGB (0–1 → 8-bit)
val Ink = Color(0xFF1A2429) // 0.10, 0.14, 0.16
val Mist = Color(0xFFE6EDEF) // 0.90, 0.93, 0.94
val MistDeep = Color(0xFFC7D9DB) // 0.78, 0.85, 0.86
val Celadon = Color(0xFF386B61) // 0.22, 0.42, 0.38
val CeladonSoft = Color(0xFF598C80) // 0.35, 0.55, 0.50
val Slate = Color(0xFF5C6B70) // 0.36, 0.42, 0.44
val Line = Color(0x382D474D) // 0.18, 0.28, 0.30 @ 0.22
val Accent = Color(0xFF8C6138) // 0.55, 0.38, 0.22
val MistTop = Color(0xFFD1E6E8) // 0.82, 0.90, 0.91
val MistBottom = Color(0xFFE0E6DB) // 0.88, 0.90, 0.86
val ErrorRed = Color(0xFFA6382E) // iOS 0.65, 0.22, 0.18

private val LightColorScheme = lightColorScheme(
    primary = Celadon,
    onPrimary = Mist,
    primaryContainer = MistDeep,
    onPrimaryContainer = Ink,
    secondary = Accent,
    onSecondary = Mist,
    background = Mist,
    onBackground = Ink,
    surface = Mist,
    onSurface = Ink,
    surfaceVariant = Color.White.copy(alpha = 0.55f),
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
        letterSpacing = 1.2.sp,
        color = CeladonSoft,
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
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = Slate,
    ),
)

val PillarTextStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Medium,
    fontSize = 28.sp,
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun BaZiTheme(
    darkTheme: Boolean = false,
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

/** iOS-style section label + content (title not uppercased). */
@Composable
fun SectionLabeled(
    title: String,
    spacing: Dp = 10.dp,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        androidx.compose.material3.Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = CeladonSoft,
        )
        content()
    }
}

@Composable
fun FrostedCard(
    modifier: Modifier = Modifier,
    radius: Dp = 14.dp,
    alpha: Float = 0.55f,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Line, RoundedCornerShape(radius))
            .background(Color.White.copy(alpha = alpha), RoundedCornerShape(radius))
            .padding(contentPadding),
    ) {
        content()
    }
}
