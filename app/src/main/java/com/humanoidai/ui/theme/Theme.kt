package com.humanoidai.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Standard Spacing Scale
object AuraSpacing {
    val xSmall = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val xLarge = 20.dp
    val xxLarge = 24.dp
    val xxxLarge = 32.dp
}

// Design Constants
val CornerRadiusSmall = 4.dp
val CornerRadiusMedium = 8.dp
val CornerRadiusLarge = 12.dp
val CornerRadiusExtraLarge = 24.dp

val BorderWidthThin = 1.dp
val BorderWidthMedium = 2.dp

// Brand colors - Monochromatic Core Palette
val BackgroundDark = Color(0xFF050505) // Near Black
val SurfaceDark = Color(0xFF0B0B0B)    // Secondary Surface
val ElevatedDark = Color(0xFF111111)   // Elevated Surface
val BorderDark = Color(0xFF252525)     // Refined Border

val BackgroundLight = Color(0xFFFFFFFF) // White
val SurfaceLight = Color(0xFFF7F7F7)    // Primary Surface
val SecondaryLight = Color(0xFFEFEFEF)  // Secondary Surface
val BorderLight = Color(0xFFD5D5D5)     // Border Light

val FishGold = Color(0xFFFFB300)       // Loading Fish Warm Gold
val SuccessGreen = Color(0xFF4ADE80)   // Mint Green
val ErrorRed = Color(0xFFF87171)       // Vibrant Coral

// Monochromatic Accents
val AccentOrange = Color.White         // Refactored to White for Monochrome UI
val AccentCyan = Color.White           // Added back for compatibility
val AccentPurple = Color.White         // Added back for compatibility
val WarningOrange = Color(0xFFFB923C)  // Kept for status only

// High Contrast Text
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFD6D6D6)
val TextMuted = Color(0xFF9A9A9A)

val TextPrimaryLight = Color(0xFF111111)
val TextSecondaryLight = Color(0xFF444444)
val TextMutedLight = Color(0xFF666666)

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color.White,
    onSecondary = Color.Black,
    tertiary = SuccessGreen,
    background = BackgroundDark,
    surface = SurfaceDark,
    error = ErrorRed,
    onBackground = Color.White,
    onSurface = Color.White,
    outline = BorderDark,
    surfaceVariant = ElevatedDark,
    onSurfaceVariant = TextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color.Black,
    onSecondary = Color.White,
    tertiary = SuccessGreen,
    background = BackgroundLight,
    surface = SurfaceLight,
    error = ErrorRed,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    outline = BorderLight,
    surfaceVariant = SecondaryLight,
    onSurfaceVariant = TextSecondaryLight
)

@Composable
fun Aura360Theme(
    isDarkMode: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkMode) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
