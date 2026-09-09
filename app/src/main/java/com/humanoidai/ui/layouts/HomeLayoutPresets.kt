package com.humanoidai.ui.layouts

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.humanoidai.ui.widgets.HexagonShape

/**
 * Definition of the 10 Visual Personalities for Aura 360°.
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
        name = "Core Monolith",
        primaryColor = Color(0xFFFFFFFF),
        secondaryColor = Color(0xFFD6D6D6),
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
        name = "Dashboard Dark",
        primaryColor = Color(0xFF64748B),
        secondaryColor = Color(0xFF475569),
        roiShape = "circle"
    )

    object RadialJarvis : HomeLayoutPreset(
        id = "radial",
        name = "Radial JARVIS",
        primaryColor = Color(0xFFFFFFFF),
        secondaryColor = Color(0xFFD6D6D6),
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
        primaryColor = Color(0xFFF87171),
        secondaryColor = Color(0xFFFF5C5C),
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
        name = "Focus Monotone",
        primaryColor = Color(0xFFFFFFFF),
        secondaryColor = Color(0xFFD6D6D6),
        roiShape = "circle"
    )

    object MultiZone : HomeLayoutPreset(
        id = "zones",
        name = "Multi-Zone Core",
        primaryColor = Color(0xFFFFFFFF),
        secondaryColor = Color(0xFFD6D6D6),
        roiShape = "square"
    )

    object ConversationFirst : HomeLayoutPreset(
        id = "chat_first",
        name = "Talk with JARVIS",
        primaryColor = Color(0xFFFFFFFF),
        secondaryColor = Color(0xFFD6D6D6),
        roiShape = "hexagon"
    )

    object BlueprintPlus : HomeLayoutPreset(
        id = "blueprint",
        name = "Schematic Amber",
        primaryColor = Color(0xFFFFFFFF),
        secondaryColor = Color(0xFFF1F5F9),
        roiShape = "circle"
    )

    companion object {
        fun fromId(id: String): HomeLayoutPreset = when (id.lowercase()) {
            "minimal" -> Minimal
            "split" -> DashboardSplit
            "radial" -> RadialJarvis
            "cards" -> CardStack
            "security" -> SecurityNight
            "widget" -> CompactWidget
            "focus" -> FocusPerson
            "zones" -> MultiZone
            "chat_first" -> ConversationFirst
            "blueprint" -> BlueprintPlus
            else -> ClassicHud
        }

        val all = listOf(
            ClassicHud, Minimal, DashboardSplit, RadialJarvis,
            CardStack, SecurityNight, CompactWidget, FocusPerson,
            MultiZone, ConversationFirst, BlueprintPlus
        )
    }
}

