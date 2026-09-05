package com.humanoidai.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand colors - Monochromatic Core Palette
val BackgroundDark = Color(0xFF0A0A0A) // Near Black
val SurfaceDark = Color(0xFF171717)    // Dark Grey
val CardDark = Color(0xFF262626)       // Neutral Grey
val AccentGrey = Color(0xFFFFB300)     // Orangish Yellow
val AccentSlate = Color(0xFFB45309)    // Dark Amber
val SuccessGreen = Color(0xFF4ADE80)   // Mint Green (Kept for status)
val WarningOrange = Color(0xFFFB923C)  // Bright Amber (Kept for status)
val ErrorRed = Color(0xFFF87171)      // Vibrant Coral (Kept for status)

// Compatibility Aliases
val PrimaryBlue = AccentGrey
val AccentCyan = AccentGrey
val AccentPurple = AccentSlate
val AlertGreen = SuccessGreen
val AlertOrange = WarningOrange
val AlertRed = ErrorRed

// High Contrast Text
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFFD6D3D1)

private val DarkColorScheme = darkColorScheme(
    primary = AccentGrey,
    secondary = AccentSlate,
    tertiary = SuccessGreen,
    background = BackgroundDark,
    surface = SurfaceDark,
    error = ErrorRed,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextPrimary
)

@Composable
fun HumanoidAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
