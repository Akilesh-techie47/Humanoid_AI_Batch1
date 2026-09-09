package com.humanoidai.ui.theme

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * The 10 camera-home visual personalities from the Aura 360° design spec
 * (Cyan Tactical -> Blueprint). These are the single source of truth for HUD
 * theme colors, shapes and panel styling.
 */
enum class HudShape { CIRCLE, SQUARE, ROUNDED, HEXAGON, DIAMOND }

fun HudShape.asComposeShape(): Shape = when (this) {
    HudShape.CIRCLE -> androidx.compose.foundation.shape.CircleShape
    HudShape.SQUARE -> RoundedCornerShape(0.dp)
    HudShape.ROUNDED -> RoundedCornerShape(20.dp)
    HudShape.HEXAGON -> HorizontalHexagonShape
    HudShape.DIAMOND -> DiamondShape
}

/** Horizontal (flat top/bottom, pointed sides) hexagon used by Amber Ops. */
private val HorizontalHexagonShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.25f, 0f)
    lineTo(w * 0.75f, 0f)
    lineTo(w, h * 0.5f)
    lineTo(w * 0.75f, h)
    lineTo(w * 0.25f, h)
    lineTo(0f, h * 0.5f)
    close()
}

/** Rotated-square diamond used by Amber Ops guest chips. */
val DiamondShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.5f, 0f)
    lineTo(w, h * 0.5f)
    lineTo(w * 0.5f, h)
    lineTo(0f, h * 0.5f)
    close()
}

data class HudTheme(
    val id: String,
    val name: String,
    val subtitle: String,
    val accent: Color = Color.White,
    val accent2: Color? = null,
    val text: Color = TextPrimary,
    val chipText: Color = TextPrimary,
    val bgTop: Color = Color(0xFF050505),
    val bgBottom: Color = Color.Black,
    val monospace: Boolean = false,
    // Primary ROI
    val roiShape: HudShape = HudShape.CIRCLE,
    val roiDashed: Boolean = false,
    val roiGlow: Boolean = true,
    val roiDoubleRing: Boolean = false,
    val roiThick: Boolean = false,
    val roiGradient: Boolean = false,
    // Guest chip ring
    val chipShape: HudShape = HudShape.CIRCLE,
    val chipDashed: Boolean = false,
    val chipGlass: Boolean = false,
    val chipBg: Color = Color(0x1AFF7A00),
    val chipBorder: Color = Color(0xFF252525),
    val unknownColor: Color = ErrorRed,
    // Panels
    val panelBg: Color = Color(0xFF0B0B0B).copy(alpha = 0.85f),
    val panelBorder: Color = Color(0xFF252525),
    val panelText: Color = TextPrimary,
    val chatBg: Color = Color(0xFF0B0B0B).copy(alpha = 0.9f),
    val chatText: Color = TextPrimary,
    val chatFilled: Boolean = false,
    val micColor: Color = Color.White,
    val grayscale: Boolean = false,
    val gridBackground: Boolean = false
)

object HudThemes {
    val all: List<HudTheme> = listOf(
        HudTheme(
            id = "t1", name = "Core Monolith", subtitle = "Monochromatic command interface",
            accent = Color(0xFFFFFFFF), text = Color(0xFFFFFFFF), chipText = Color(0xFFFFFFFF),
            bgTop = Color(0xFF101010), bgBottom = Color(0xFF000000), monospace = true,
            roiShape = HudShape.CIRCLE, roiGlow = false,
            chipShape = HudShape.CIRCLE, chipBg = Color(0x1AFFFFFF), chipBorder = Color(0xFF252525),
            unknownColor = Color(0xFFF87171),
            panelBg = Color(0xCC101010), panelBorder = Color(0xFF252525), panelText = Color(0xFFFFFFFF),
            chatBg = Color(0xE6101010), chatText = Color(0xFFFFFFFF), micColor = Color(0xFFFFFFFF)
        ),
        HudTheme(
            id = "t2", name = "Crystal Frost", subtitle = "Pristine glass interface",
            accent = Color(0xFFF8FAFC), text = Color(0xFFF8FAFC), chipText = Color(0xFFF1F5F9),
            bgTop = Color(0xFF334155), bgBottom = Color(0xFF0F172A),
            roiShape = HudShape.ROUNDED, roiGlow = true,
            chipShape = HudShape.CIRCLE, chipGlass = true, chipBg = Color(0x26FFFFFF), chipBorder = Color(0x4DFFFFFF),
            unknownColor = Color(0xFFFDA4AF),
            panelBg = Color(0x33FFFFFF), panelBorder = Color(0x66FFFFFF), panelText = Color(0xFFFFFFFF),
            chatBg = Color(0x40FFFFFF), chatText = Color(0xFFFFFFFF), micColor = Color(0xFFFFFFFF)
        ),
        HudTheme(
            id = "t3", name = "Golden Amber", subtitle = "Warm technical display",
            accent = Color(0xFFFFD54F), text = Color(0xFFFFF8E1), chipText = Color(0xFFFFECB3),
            bgTop = Color(0xFF451A03), bgBottom = Color(0xFF0C0A09), monospace = true,
            roiShape = HudShape.HEXAGON,
            chipShape = HudShape.DIAMOND, chipBg = Color(0x1AFFFF00), chipBorder = Color(0x80FFD54F),
            unknownColor = Color(0xFFFB7185),
            panelBg = Color(0xCC451A03), panelBorder = Color(0x80FFD54F), panelText = Color(0xFFFFF8E1),
            chatBg = Color(0xE6451A03), chatText = Color(0xFFFFD54F), micColor = Color(0xFFF59E0B)
        ),
        HudTheme(
            id = "t4", name = "Electric Iris", subtitle = "Vivid purple spectrum",
            accent = Color(0xFFA855F7), text = Color(0xFFFAF5FF), chipText = Color(0xFFF3E8FF),
            bgTop = Color(0xFF2E1065), bgBottom = Color(0xFF020617),
            roiShape = HudShape.CIRCLE, roiDoubleRing = true,
            chipShape = HudShape.CIRCLE, chipBg = Color(0x1AA855F7), chipBorder = Color(0x80A855F7),
            unknownColor = Color(0xFFF472B6),
            panelBg = Color(0xCC2E1065), panelBorder = Color(0x80A855F7), panelText = Color(0xFFFAF5FF),
            chatBg = Color(0xE62E1065), chatText = Color(0xFFA855F7), micColor = Color(0xFFA855F7)
        ),
        HudTheme(
            id = "t5", name = "Matrix Green", subtitle = "Sharp terminal aesthetic",
            accent = Color(0xFF22C55E), text = Color(0xFFF0FDF4), chipText = Color(0xFFDCFCE7),
            bgTop = Color(0xFF064E3B), bgBottom = Color(0xFF020617), monospace = true,
            roiShape = HudShape.SQUARE, roiDashed = true,
            chipShape = HudShape.SQUARE, chipDashed = true, chipBg = Color(0x1A22C55E), chipBorder = Color(0x8022C55E),
            unknownColor = Color(0xFFF87171),
            panelBg = Color(0xCC064E3B), panelBorder = Color(0x8022C55E), panelText = Color(0xFFF0FDF4),
            chatBg = Color(0xE6064E3B), chatText = Color(0xFF22C55E), micColor = Color(0xFF22C55E)
        ),
        HudTheme(
            id = "t6", name = "Slate Projection", subtitle = "Subtle monochromatic grid",
            accent = Color(0xFF94A3B8), text = Color(0xFFF1F5F9), chipText = Color(0xFFE2E8F0),
            bgTop = Color(0xFF0F172A), bgBottom = Color(0xFF020617),
            roiShape = HudShape.CIRCLE,
            chipShape = HudShape.CIRCLE, chipBg = Color(0x1A94A3B8), chipBorder = Color(0x8094A3B8),
            unknownColor = Color(0xFFCBD5E1),
            panelBg = Color(0x3394A3B8), panelBorder = Color(0x8094A3B8), panelText = Color(0xFFF1F5F9),
            chatBg = Color(0x4D94A3B8), chatText = Color(0xFFF1F5F9), micColor = Color(0xFFF1F5F9)
        ),
        HudTheme(
            id = "t7", name = "Sunset Coral", subtitle = "Warm friendly glow",
            accent = Color(0xFFFB7185), text = Color(0xFFFFF1F2), chipText = Color(0xFFFFE4E6),
            bgTop = Color(0xFF4C0519), bgBottom = Color(0xFF020617),
            roiShape = HudShape.CIRCLE, roiThick = true,
            chipShape = HudShape.CIRCLE, chipBg = Color(0x1AFB7185), chipBorder = Color(0x80FB7185),
            unknownColor = Color(0xFFFECDD3),
            panelBg = Color(0xCC4C0519), panelBorder = Color(0x80FB7185), panelText = Color(0xFFFFF1F2),
            chatBg = Color(0xFFFB7185), chatText = Color(0xFF4C0519), chatFilled = true, micColor = Color(0xFF4C0519)
        ),
        HudTheme(
            id = "t8", name = "Pure Modern", subtitle = "High-contrast minimalism",
            accent = Color(0xFFFFFFFF), text = Color(0xFFFFFFFF), chipText = Color(0xFFE2E8F0),
            bgTop = Color(0xFF475569), bgBottom = Color(0xFF020617), grayscale = false, monospace = true,
            roiShape = HudShape.SQUARE,
            chipShape = HudShape.CIRCLE, chipBg = Color(0x26FFFFFF), chipBorder = Color(0x80FFFFFF),
            unknownColor = Color(0xFFFFFFFF),
            panelBg = Color(0x4D0F172A), panelBorder = Color(0x80FFFFFF), panelText = Color(0xFFFFFFFF),
            chatBg = Color(0xFFFFFFFF), chatText = Color(0xFF0F172A), chatFilled = true, micColor = Color(0xFF0F172A)
        ),
        HudTheme(
            id = "t9", name = "Nebula Dream", subtitle = "Deep space gradient",
            accent = Color(0xFF34D399), accent2 = Color(0xFF818CF8), text = Color(0xFFECFDF5), chipText = Color(0xFFD1FAE5),
            bgTop = Color(0xFF312E81), bgBottom = Color(0xFF020617),
            roiShape = HudShape.CIRCLE, roiGradient = true, roiDoubleRing = true,
            chipShape = HudShape.CIRCLE, chipBg = Color(0x1A34D399), chipBorder = Color(0x80818CF8),
            unknownColor = Color(0xFFFCA5A5),
            panelBg = Color(0xCC312E81), panelBorder = Color(0x80818CF8), panelText = Color(0xFFECFDF5),
            chatBg = Color(0xFF34D399), chatText = Color(0xFF020617), chatFilled = true, micColor = Color(0xFF020617)
        ),
        HudTheme(
            id = "t10", name = "Schematic Plus", subtitle = "Enhanced amber schematic grid",
            accent = Color(0xFFFFB300), text = Color(0xFFF1F5F9), chipText = Color(0xFFF59E0B),
            bgTop = Color(0xFF0A0A0A), bgBottom = Color(0xFF000000), monospace = true, gridBackground = true,
            roiShape = HudShape.CIRCLE, roiDashed = true,
            chipShape = HudShape.SQUARE, chipDashed = true, chipBg = Color(0x1AFFFF00), chipBorder = Color(0x80FFB300),
            unknownColor = Color(0xFFCBD5E1),
            panelBg = Color(0xCC171717), panelBorder = Color(0x80FFB300), panelText = Color(0xFFF1F5F9),
            chatBg = Color(0xE6171717), chatText = Color(0xFFFFB300), micColor = Color(0xFFFFB300)
        )
    )

    fun forId(id: String?): HudTheme = all.firstOrNull { it.id == id } ?: all.first()

    val default: HudTheme get() = all.first()
}
