package com.humanoidai.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

enum class HudShape { CIRCLE, SQUARE, ROUNDED, HEXAGON, DIAMOND }

fun HudShape.asComposeShape(): Shape = when (this) {
    HudShape.CIRCLE -> CircleShape
    HudShape.SQUARE -> RoundedCornerShape(0.dp)
    HudShape.ROUNDED -> RoundedCornerShape(20.dp)
    HudShape.HEXAGON -> HorizontalHexagonShape
    HudShape.DIAMOND -> DiamondShape
}

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
    val bgTop: Color = Color(0xFF000000),
    val bgBottom: Color = Color.Black,
    val monospace: Boolean = true,
    val roiShape: HudShape = HudShape.CIRCLE,
    val roiDashed: Boolean = false,
    val roiGlow: Boolean = true,
    val roiDoubleRing: Boolean = false,
    val roiThick: Boolean = false,
    val roiGradient: Boolean = false,
    val chipShape: HudShape = HudShape.CIRCLE,
    val chipDashed: Boolean = false,
    val chipGlass: Boolean = false,
    val chipBg: Color = Color(0x1AFFFFFF),
    val chipBorder: Color = Color(0xFF333333),
    val unknownColor: Color = Color(0xFF888888),
    val panelBg: Color = Color(0xFF121212).copy(alpha = 0.85f),
    val panelBorder: Color = Color(0xFF333333),
    val panelText: Color = TextPrimary,
    val chatBg: Color = Color(0xFF121212).copy(alpha = 0.9f),
    val chatText: Color = TextPrimary,
    val chatFilled: Boolean = false,
    val micColor: Color = Color.White,
    val grayscale: Boolean = true,
    val gridBackground: Boolean = false
)

object HudThemes {
    val all: List<HudTheme> = listOf(
        HudTheme(
            id = "t1", name = "Pure Monolith", subtitle = "Monochromatic black and white interface",
            accent = Color(0xFFFFFFFF), text = Color(0xFFFFFFFF), chipText = Color(0xFFFFFFFF),
            bgTop = Color(0xFF121212), bgBottom = Color(0xFF000000), monospace = true,
            roiShape = HudShape.CIRCLE, roiGlow = false,
            chipShape = HudShape.CIRCLE, chipBg = Color(0x1AFFFFFF), chipBorder = Color(0xFF333333),
            unknownColor = Color(0xFF888888),
            panelBg = Color(0xCC121212), panelBorder = Color(0xFF333333), panelText = Color(0xFFFFFFFF),
            chatBg = Color(0xE6121212), chatText = Color(0xFFFFFFFF), micColor = Color(0xFFFFFFFF)
        ),
        HudTheme(
            id = "t2", name = "Monochrome Frost", subtitle = "Pristine white glass interface",
            accent = Color(0xFFFFFFFF), text = Color(0xFFFFFFFF), chipText = Color(0xFFCCCCCC),
            bgTop = Color(0xFF1E1E1E), bgBottom = Color(0xFF000000),
            roiShape = HudShape.ROUNDED, roiGlow = true,
            chipShape = HudShape.CIRCLE, chipGlass = true, chipBg = Color(0x26FFFFFF), chipBorder = Color(0x4DFFFFFF),
            unknownColor = Color(0xFF888888),
            panelBg = Color(0x33FFFFFF), panelBorder = Color(0x66FFFFFF), panelText = Color(0xFFFFFFFF),
            chatBg = Color(0x40FFFFFF), chatText = Color(0xFFFFFFFF), micColor = Color(0xFFFFFFFF)
        ),
        HudTheme(
            id = "t3", name = "Silver Tech", subtitle = "Technical silver and black display",
            accent = Color(0xFFCCCCCC), text = Color(0xFFFFFFFF), chipText = Color(0xFFDDDDDD),
            bgTop = Color(0xFF1A1A1A), bgBottom = Color(0xFF000000), monospace = true,
            roiShape = HudShape.HEXAGON,
            chipShape = HudShape.DIAMOND, chipBg = Color(0x1A222222), chipBorder = Color(0x80CCCCCC),
            unknownColor = Color(0xFF888888),
            panelBg = Color(0xCC1A1A1A), panelBorder = Color(0x80CCCCCC), panelText = Color(0xFFFFFFFF),
            chatBg = Color(0xE61A1A1A), chatText = Color(0xFFCCCCCC), micColor = Color(0xFFFFFFFF)
        ),
        HudTheme(
            id = "t4", name = "High Contrast", subtitle = "Pure white spectrum",
            accent = Color(0xFFFFFFFF), text = Color(0xFFFFFFFF), chipText = Color(0xFFCCCCCC),
            bgTop = Color(0xFF222222), bgBottom = Color(0xFF000000),
            roiShape = HudShape.CIRCLE, roiDoubleRing = true,
            chipShape = HudShape.CIRCLE, chipBg = Color(0x1AFFFFFF), chipBorder = Color(0x80FFFFFF),
            unknownColor = Color(0xFF888888),
            panelBg = Color(0xCC222222), panelBorder = Color(0x80FFFFFF), panelText = Color(0xFFFFFFFF),
            chatBg = Color(0xE6222222), chatText = Color(0xFFFFFFFF), micColor = Color(0xFFFFFFFF)
        ),
        HudTheme(
            id = "t5", name = "Terminal Grid", subtitle = "Sharp terminal monochrome",
            accent = Color(0xFFCCCCCC), text = Color(0xFFFFFFFF), chipText = Color(0xFFDDDDDD),
            bgTop = Color(0xFF111111), bgBottom = Color(0xFF000000), monospace = true,
            roiShape = HudShape.SQUARE, roiDashed = true,
            chipShape = HudShape.SQUARE, chipDashed = true, chipBg = Color(0x1AFFFFFF), chipBorder = Color(0x80CCCCCC),
            unknownColor = Color(0xFF888888),
            panelBg = Color(0xCC111111), panelBorder = Color(0x80CCCCCC), panelText = Color(0xFFFFFFFF),
            chatBg = Color(0xE6111111), chatText = Color(0xFFCCCCCC), micColor = Color(0xFFCCCCCC)
        ),
        HudTheme(
            id = "t6", name = "Slate Mono", subtitle = "Subtle grayscale grid",
            accent = Color(0xFFAAAAAA), text = Color(0xFFFFFFFF), chipText = Color(0xFFDDDDDD),
            bgTop = Color(0xFF1A1A1A), bgBottom = Color(0xFF000000),
            roiShape = HudShape.CIRCLE,
            chipShape = HudShape.CIRCLE, chipBg = Color(0x1AAAAAAA), chipBorder = Color(0x80AAAAAA),
            unknownColor = Color(0xFF777777),
            panelBg = Color(0x33AAAAAA), panelBorder = Color(0x80AAAAAA), panelText = Color(0xFFFFFFFF),
            chatBg = Color(0x4DAAAAAA), chatText = Color(0xFFFFFFFF), micColor = Color(0xFFFFFFFF)
        ),
        HudTheme(
            id = "t7", name = "Midnight Glow", subtitle = "Deep grayscale glow",
            accent = Color(0xFFFFFFFF), text = Color(0xFFFFFFFF), chipText = Color(0xFFCCCCCC),
            bgTop = Color(0xFF222222), bgBottom = Color(0xFF000000),
            roiShape = HudShape.CIRCLE, roiThick = true,
            chipShape = HudShape.CIRCLE, chipBg = Color(0x1AFFFFFF), chipBorder = Color(0x80FFFFFF),
            unknownColor = Color(0xFF888888),
            panelBg = Color(0xCC222222), panelBorder = Color(0x80FFFFFF), panelText = Color(0xFFFFFFFF),
            chatBg = Color(0xFFFFFFFF), chatText = Color(0xFF000000), chatFilled = true, micColor = Color(0xFF000000)
        ),
        HudTheme(
            id = "t8", name = "Pure Minimal", subtitle = "High-contrast black & white",
            accent = Color(0xFFFFFFFF), text = Color(0xFFFFFFFF), chipText = Color(0xFFCCCCCC),
            bgTop = Color(0xFF1E1E1E), bgBottom = Color(0xFF000000), grayscale = true, monospace = true,
            roiShape = HudShape.SQUARE,
            chipShape = HudShape.CIRCLE, chipBg = Color(0x26FFFFFF), chipBorder = Color(0x80FFFFFF),
            unknownColor = Color(0xFF888888),
            panelBg = Color(0x4D121212), panelBorder = Color(0x80FFFFFF), panelText = Color(0xFFFFFFFF),
            chatBg = Color(0xFFFFFFFF), chatText = Color(0xFF000000), chatFilled = true, micColor = Color(0xFF000000)
        ),
        HudTheme(
            id = "t9", name = "Quantum Mono", subtitle = "Quantum grayscale gradient",
            accent = Color(0xFFDDDDDD), accent2 = Color(0xFF888888), text = Color(0xFFFFFFFF), chipText = Color(0xFFCCCCCC),
            bgTop = Color(0xFF1E1E1E), bgBottom = Color(0xFF000000),
            roiShape = HudShape.CIRCLE, roiGradient = true, roiDoubleRing = true,
            chipShape = HudShape.CIRCLE, chipBg = Color(0x1ADDDDDD), chipBorder = Color(0x80888888),
            unknownColor = Color(0xFF777777),
            panelBg = Color(0xCC1E1E1E), panelBorder = Color(0x80888888), panelText = Color(0xFFFFFFFF),
            chatBg = Color(0xFFDDDDDD), chatText = Color(0xFF000000), chatFilled = true, micColor = Color(0xFF000000)
        ),
        HudTheme(
            id = "t10", name = "Schematic Mono", subtitle = "Enhanced monochrome schematic grid",
            accent = Color(0xFFFFFFFF), text = Color(0xFFFFFFFF), chipText = Color(0xFFCCCCCC),
            bgTop = Color(0xFF121212), bgBottom = Color(0xFF000000), monospace = true, gridBackground = true,
            roiShape = HudShape.CIRCLE, roiDashed = true,
            chipShape = HudShape.SQUARE, chipDashed = true, chipBg = Color(0x1AFFFFFF), chipBorder = Color(0x80FFFFFF),
            unknownColor = Color(0xFF888888),
            panelBg = Color(0xCC121212), panelBorder = Color(0x80FFFFFF), panelText = Color(0xFFFFFFFF),
            chatBg = Color(0xE6121212), chatText = Color(0xFFFFFFFF), micColor = Color(0xFFFFFFFF)
        )
    )

    fun forId(id: String?): HudTheme = all.firstOrNull { it.id == id } ?: all.first()

    val default: HudTheme get() = all.first()
}
