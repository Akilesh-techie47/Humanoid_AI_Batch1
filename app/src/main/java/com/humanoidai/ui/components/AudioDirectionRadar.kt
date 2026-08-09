package com.humanoidai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.humanoidai.hearing.AudioDirectionState
import com.humanoidai.hearing.CompassSector
import com.humanoidai.ui.theme.AccentCyan
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AudioDirectionRadar(state: AudioDirectionState) {
    val angle = remember(state.sector) {
        when (state.sector) {
            CompassSector.N -> 270f
            CompassSector.NE -> 315f
            CompassSector.E -> 0f
            CompassSector.SE -> 45f
            CompassSector.S -> 90f
            CompassSector.SW -> 135f
            CompassSector.W -> 180f
            CompassSector.NW -> 225f
            else -> null
        }
    }

    val pulseScale by rememberInfiniteTransition().animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
    )

    Canvas(modifier = Modifier.size(100.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2
        
        // Background Ring
        drawCircle(
            color = Color.White.copy(alpha = 0.1f),
            radius = radius,
            style = Stroke(width = 1.dp.toPx())
        )

        // Direction Blip
        angle?.let { a ->
            val rad = Math.toRadians(a.toDouble())
            val blipPos = Offset(
                center.x + (radius * cos(rad)).toFloat(),
                center.y + (radius * sin(rad)).toFloat()
            )
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentCyan, Color.Transparent),
                    center = blipPos,
                    radius = 20.dp.toPx() * pulseScale
                ),
                radius = 15.dp.toPx() * pulseScale,
                center = blipPos
            )
        }
    }
}
