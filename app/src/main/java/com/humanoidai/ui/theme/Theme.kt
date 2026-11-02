package com.humanoidai.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Standard Spacing Scale
object AuraSpacing {
    val xxSmall = 2.dp
    val xSmall = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val xLarge = 20.dp
    val xxLarge = 24.dp
    val xxxLarge = 32.dp
    val xxxxLarge = 48.dp
}

// Design Constants - Refined modern radii
val CornerRadiusExtraSmall = 6.dp
val CornerRadiusSmall = 10.dp
val CornerRadiusMedium = 14.dp
val CornerRadiusLarge = 18.dp
val CornerRadiusExtraLarge = 24.dp
val CornerRadiusPill = 999.dp

val BorderWidthThin = 1.dp
val BorderWidthMedium = 1.5.dp

// Brand colors - Modern Minimalist Palette (ChatGPT / Perplexity / Claude aesthetic)
val BackgroundDark = Color(0xFF111113)      // Deep modern charcoal
val SurfaceDark = Color(0xFF1B1B1F)         // Secondary surface
val ElevatedDark = Color(0xFF24242A)        // Elevated cards and dialogs
val ElevatedSurfaceDark = Color(0xFF2D2D34) // Interactive hovered / active surface
val BorderDark = Color(0xFF33333C)          // Subtle structural border

val BackgroundLight = Color(0xFFFFFFFF)     // Clean white
val SurfaceLight = Color(0xFFF6F6F8)        // Soft light surface
val SecondaryLight = Color(0xFFECECEE)      // Secondary neutral surface
val ElevatedLight = Color(0xFFFFFFFF)       // Elevated card surface in light mode
val BorderLight = Color(0xFFE4E4E8)         // Subtle light border

// Status Colors - Clear semantic communication
val SuccessGreen = Color(0xFF10B981)        // Refined emerald
val ErrorRed = Color(0xFFEF4444)            // Modern red
val WarningOrange = Color(0xFFF59E0B)       // Warm amber
val InfoBlue = Color(0xFF3B82F6)            // Clean blue

// Monochrome and Accent colors
val FishGold = Color(0xFFE2E8F0)            // Slate light
val AccentCyan = Color(0xFF10A37F)          // AI Emerald (Primary interactive accent)
val AccentTeal = Color(0xFF0D9488)          // Teal accent
val AccentPurple = Color(0xFF8B5CF6)        // Subtle AI violet
val AccentOrange = Color(0xFFF97316)        // Energetic orange

// Chat Bubble specific colors
val UserBubbleDark = Color(0xFF2E2E35)      // Modern rounded bubble for user
val UserBubbleLight = Color(0xFFE9E9EE)     // User bubble in light mode
val AssistantBubbleDark = Color(0xFF18181C) // Assistant response card dark
val AssistantBubbleLight = Color(0xFFF9F9FB)// Assistant response card light

// High Contrast Text
val TextPrimary = Color(0xFFF4F4F6)         // Off-white primary text
val TextSecondary = Color(0xFFA1A1AA)       // Zinc-400 secondary text
val TextMuted = Color(0xFF71717A)           // Zinc-500 helper / timestamp text

val TextPrimaryLight = Color(0xFF18181B)    // Zinc-900 primary text
val TextSecondaryLight = Color(0xFF52525B)  // Zinc-600 secondary text
val TextMutedLight = Color(0xFF71717A)      // Zinc-500 helper text

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF4F4F6),
    onPrimary = Color(0xFF111113),
    primaryContainer = Color(0xFF24242A),
    onPrimaryContainer = Color(0xFFF4F4F6),
    secondary = Color(0xFFA1A1AA),
    onSecondary = Color(0xFF111113),
    secondaryContainer = Color(0xFF2D2D34),
    onSecondaryContainer = Color(0xFFF4F4F6),
    tertiary = AccentCyan,
    onTertiary = Color(0xFF111113),
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = ElevatedDark,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = BorderDark,
    outlineVariant = BorderDark.copy(alpha = 0.5f)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF18181B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAEAEA),
    onPrimaryContainer = Color(0xFF18181B),
    secondary = Color(0xFF52525B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0F0F3),
    onSecondaryContainer = Color(0xFF18181B),
    tertiary = AccentCyan,
    onTertiary = Color.White,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SecondaryLight,
    onSurfaceVariant = TextSecondaryLight,
    error = ErrorRed,
    onError = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    outline = BorderLight,
    outlineVariant = BorderLight.copy(alpha = 0.6f)
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
