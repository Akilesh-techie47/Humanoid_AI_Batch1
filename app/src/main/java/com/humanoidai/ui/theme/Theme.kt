package com.humanoidai.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand colors
val PrimaryBlue = Color(0xFF1E88E5)
val AccentCyan = Color(0xFF00BCD4)
val BackgroundDark = Color(0xFF0A0E1A)
val SurfaceDark = Color(0xFF141824)
val CardDark = Color(0xFF1E2436)
val TextPrimary = Color(0xFFE8EAF6)
val TextSecondary = Color(0xFF8892B0)
val AlertRed = Color(0xFFEF5350)
val AlertOrange = Color(0xFFFF7043)
val AlertGreen = Color(0xFF66BB6A)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = AccentCyan,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@Composable
fun HumanoidAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}