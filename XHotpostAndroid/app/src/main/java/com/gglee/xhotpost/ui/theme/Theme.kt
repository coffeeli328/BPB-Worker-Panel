package com.gglee.xhotpost.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Bg = Color(0xFF071411)
private val Surface = Color(0xFF143029)
private val Accent = Color(0xFFD6FF4B)
private val AccentInk = Color(0xFF102018)
private val Muted = Color(0xFF9BB3AA)
private val Coral = Color(0xFFFF6B3D)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = AccentInk,
    secondary = Coral,
    background = Bg,
    surface = Surface,
    onBackground = Color(0xFFF3F7F2),
    onSurface = Color(0xFFF3F7F2),
    outline = Color(0x33D6FF4B),
)

@Composable
fun HotpostTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
