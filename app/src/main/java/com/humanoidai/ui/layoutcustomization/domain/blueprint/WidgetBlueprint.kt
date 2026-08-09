package com.humanoidai.ui.layoutcustomization.domain.blueprint

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import com.humanoidai.ui.layoutcustomization.domain.hud.HUDComponentBehavior
import com.humanoidai.ui.layoutcustomization.domain.hud.HUDComponentCategory
import com.humanoidai.ui.layoutcustomization.domain.hud.HUDComponentState

/**
 * Geometric definition for a single HUD widget.
 * Enhanced for Phase 2B Modular HUD Component System.
 */
@Immutable
data class WidgetBlueprint(
    val id: String,
    val type: String = "generic",
    val anchor: WidgetAnchor = WidgetAnchor.CENTER,
    val offsetX: Dp = 0.dp,
    val offsetY: Dp = 0.dp,
    val width: Dp = Dp.Unspecified,
    val height: Dp = Dp.Unspecified,
    val scale: Float = 1.0f,
    val alpha: Float = 1.0f,
    val layer: Int = WidgetLayer.CAMERA,
    val isVisible: Boolean = true,
    
    // Phase 2B Metadata
    val state: HUDComponentState = HUDComponentState.COMPACT,
    val category: HUDComponentCategory = HUDComponentCategory.CONTEXT,
    val priority: Int = LayoutPriority.DECORATIVE,
    val behaviors: HUDComponentBehavior = HUDComponentBehavior(),
    val allowedZones: List<WidgetZone> = emptyList(),
    val isLocked: Boolean = false,
    
    val expansion: Float = 0f, 
    val constraints: WidgetConstraints = WidgetConstraints(),
    val metadata: Map<String, Any> = emptyMap()
)

