package com.humanoidai.ui.layouts

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.humanoidai.ui.widgets.HexagonShape

/**
 * Definition of the 10 Visual Personalities for Humanoid AI.
 * Each preset maps to specific visual cues found in the design images.
 */
sealed class HomeLayoutPreset(
    val id: String,
    val name: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val roiShape: String, // "circle", "hexagon", "square"
    val showGrid: Boolean = true
) {
    object ClassicHud : HomeLayoutPreset(
        id = "classic",
        name = "Classic HUD",
        primaryColor = Color(0xFF00D4FF),
        secondaryColor = Color(0xFFFFFFFF),
        roiShape = "circle"
    )

    object Minimal : HomeLayoutPreset(
        id = "minimal",
        name = "Nothing Minimal",
        primaryColor = Color(0xFFFFFFFF),
        secondaryColor = Color(0xFF888888),
        roiShape = "square",
        showGrid = false
    )

    object DashboardSplit : HomeLayoutPreset(
        id = "split",
        name = "Dashboard Split",
        primaryColor = Color(0xFF0055FF),
        secondaryColor = Color(0xFF00AAFF),
        roiShape = "circle"
    )

    object RadialJarvis : HomeLayoutPreset(
        id = "radial",
        name = "Radial JARVIS",
        primaryColor = Color(0xFFFF9800),
        secondaryColor = Color(0xFFFFD54F),
        roiShape = "hexagon"
    )

    object CardStack : HomeLayoutPreset(
        id = "cards",
        name = "Glass Card Stack",
        primaryColor = Color(0xFFBD00FF),
        secondaryColor = Color(0xFFE040FB),
        roiShape = "circle"
    )

    object SecurityNight : HomeLayoutPreset(
        id = "security",
        name = "Security Night",
        primaryColor = Color(0xFFFF3D00),
        secondaryColor = Color(0xFFFF8A65),
        roiShape = "hexagon"
    )

    object CompactWidget : HomeLayoutPreset(
        id = "widget",
        name = "Compact Utility",
        primaryColor = Color(0xFF00FF41),
        secondaryColor = Color(0xFFB9F6CA),
        roiShape = "square"
    )

    object FocusPerson : HomeLayoutPreset(
        id = "focus",
        name = "Focus Primary",
        primaryColor = Color(0xFF00D4FF),
        secondaryColor = Color(0xFFB2EBF2),
        roiShape = "circle"
    )

    object MultiZone : HomeLayoutPreset(
        id = "zones",
        name = "Multi-Zone Grid",
        primaryColor = Color(0xFF00E5FF),
        secondaryColor = Color(0xFF80DEEA),
        roiShape = "square"
    )

    object ConversationFirst : HomeLayoutPreset(
        id = "chat_first",
        name = "Talk with JARVIS",
        primaryColor = Color(0xFFFFB300),
        secondaryColor = Color(0xFFFFE082),
        roiShape = "hexagon"
    )

    companion object {
        fun fromId(id: String): HomeLayoutPreset = when (id) {
            "minimal" -> Minimal
            "split" -> DashboardSplit
            "radial" -> RadialJarvis
            "cards" -> CardStack
            "security" -> SecurityNight
            "widget" -> CompactWidget
            "focus" -> FocusPerson
            "zones" -> MultiZone
            "chat_first" -> ConversationFirst
            else -> ClassicHud
        }

        val all = listOf(
            ClassicHud, Minimal, DashboardSplit, RadialJarvis,
            CardStack, SecurityNight, CompactWidget, FocusPerson,
            MultiZone, ConversationFirst
        )
    }
}
