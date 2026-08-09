package com.humanoidai.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand colors - CEA v1.1 Modern HUD
val BackgroundDark = Color(0xFF0B0F17)
val SurfaceDark = Color(0xFF151C2C)
val CardDark = Color(0xFF1E2436)
val AccentCyan = Color(0xFF00D4FF) // Primary Neon
val AccentPurple = Color(0xFF7C4DFF) // Ro1 Purple
val SuccessGreen = Color(0xFF00E676)
val WarningOrange = Color(0xFFFFA500)
val ErrorRed = Color(0xFFFF5252)

// Compatibility Aliases for older screens
val PrimaryBlue = AccentCyan
val AlertGreen = SuccessGreen
val AlertOrange = WarningOrange
val AlertRed = ErrorRed

val TextPrimary = Color(0xFFE8EAF6)
val TextSecondary = Color(0xFF8892B0)

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    secondary = AccentPurple,
    background = BackgroundDark,
    surface = SurfaceDark,
    error = ErrorRed,
    onPrimary = Color.Black,
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