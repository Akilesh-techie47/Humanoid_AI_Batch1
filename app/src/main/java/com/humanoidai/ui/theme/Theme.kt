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

// Brand colors - Pure White, Black & Grayscale Palette (Zero Blue)
val BackgroundDark = Color(0xFF000000) // Pure Black
val SurfaceDark = Color(0xFF121212)    // Dark Gray Surface
val ElevatedDark = Color(0xFF1E1E1E)   // Elevated Surface
val BorderDark = Color(0xFF333333)     // Refined Gray Border

val BackgroundLight = Color(0xFFFFFFFF) // Pure White
val SurfaceLight = Color(0xFFF5F5F5)    // Light Gray Surface
val SecondaryLight = Color(0xFFE0E0E0)  // Secondary Surface
val BorderLight = Color(0xFFCCCCCC)     // Border Light

// Monochrome replacements for status colors
val FishGold = Color(0xFFE0E0E0)       // Silver Gray
val SuccessGreen = Color(0xFFFFFFFF)   // Pure White
val ErrorRed = Color(0xFF888888)       // Muted Gray
val WarningOrange = Color(0xFFCCCCCC)  // Light Gray

// Monochrome Accents
val AccentCyan = Color(0xFFFFFFFF)     // Pure White
val AccentPurple = Color(0xFFCCCCCC)   // Light Gray
val AccentOrange = Color(0xFF888888)   // Muted Gray

// High Contrast Text
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFAAAAAA)
val TextMuted = Color(0xFF777777)

val TextPrimaryLight = Color(0xFF000000)
val TextSecondaryLight = Color(0xFF555555)
val TextMutedLight = Color(0xFF888888)

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color(0xFFCCCCCC),
    onSecondary = Color.Black,
    tertiary = Color(0xFF888888),
    background = BackgroundDark,
    surface = SurfaceDark,
    error = Color(0xFF888888),
    onBackground = Color.White,
    onSurface = Color.White,
    outline = BorderDark,
    surfaceVariant = ElevatedDark,
    onSurfaceVariant = TextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color(0xFF333333),
    onSecondary = Color.White,
    tertiary = Color(0xFF666666),
    background = BackgroundLight,
    surface = SurfaceLight,
    error = Color(0xFF333333),
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
