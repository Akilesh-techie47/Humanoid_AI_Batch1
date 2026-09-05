package com.humanoidai.ui.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humanoidai.ui.customization.AppearanceSettings
import kotlin.math.cos
import kotlin.math.sin

/**
 * Modular high-fidelity widgets for the Humanoid AI OS.
 */

@Composable
fun TacticalBrackets(modifier: Modifier, color: Color, size: Dp = 200.dp) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = 2.dp.toPx()
        val len = 20.dp.toPx()
        val gap = 4.dp.toPx()
        
        // Top Left
        drawLine(color, Offset(gap, gap), Offset(gap + len, gap), stroke)
        drawLine(color, Offset(gap, gap), Offset(gap, gap + len), stroke)
        
        // Top Right
        drawLine(color, Offset(this.size.width - gap, gap), Offset(this.size.width - gap - len, gap), stroke)
        drawLine(color, Offset(this.size.width - gap, gap), Offset(this.size.width - gap, gap + len), stroke)
        
        // Bottom Left
        drawLine(color, Offset(gap, this.size.height - gap), Offset(gap + len, this.size.height - gap), stroke)
        drawLine(color, Offset(gap, this.size.height - gap), Offset(gap, this.size.height - gap - len), stroke)
        
        // Bottom Right
        drawLine(color, Offset(this.size.width - gap, this.size.height - gap), Offset(this.size.width - gap - len, this.size.height - gap), stroke)
        drawLine(color, Offset(this.size.width - gap, this.size.height - gap), Offset(this.size.width - gap, this.size.height - gap - len), stroke)
    }
}

@Composable
fun ScanningAperture(
    modifier: Modifier,
    settings: AppearanceSettings,
    content: @Composable () -> Unit
) {
    val accent = Color(settings.accentColor)
    val infiniteTransition = rememberInfiniteTransition()
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing))
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2000), repeatMode = RepeatMode.Reverse)
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        // Rotating Tech Rings
        if (settings.layoutPreset == "radial") {
            Canvas(modifier = Modifier.size(300.dp)) {
                withTransform({ rotate(rotation) }) {
                    drawArc(
                        color = accent.copy(alpha = 0.3f),
                        startAngle = 0f, sweepAngle = 60f, useCenter = false,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = accent.copy(alpha = 0.3f),
                        startAngle = 180f, sweepAngle = 60f, useCenter = false,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Main Shape
        val shape = when(settings.layoutPreset) {
            "radial" -> CircleShape
            "security" -> HexagonShape
            else -> RoundedCornerShape(settings.cornerRadius)
        }

        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(shape)
                .background(Color.Black)
                .border(2.dp, accent.copy(alpha = settings.hudGlowIntensity), shape)
                .then(if (settings.animationProfile != com.humanoidai.ui.customization.AnimationProfile.MINIMAL) {
                    Modifier.border(8.dp, accent.copy(alpha = glowAlpha * 0.2f), shape)
                } else Modifier)
        ) {
            content()
            
            // Crosshair overlay
            HudCrosshair(accent.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun HudCrosshair(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val len = 10.dp.toPx()
        val gap = 6.dp.toPx()
        
        drawLine(color, Offset(center.x, gap), Offset(center.x, gap + len), 1.dp.toPx())
        drawLine(color, Offset(center.x, size.height - gap), Offset(center.x, size.height - gap - len), 1.dp.toPx())
        drawLine(color, Offset(gap, center.y), Offset(gap + len, center.y), 1.dp.toPx())
        drawLine(color, Offset(size.width - gap, center.y), Offset(size.width - gap - len, center.y), 1.dp.toPx())
        
        drawCircle(color, 2.dp.toPx(), center)
    }
}

@Composable
fun TacticalRadar(state: com.humanoidai.hearing.AudioDirectionState, settings: AppearanceSettings) {
    if (!settings.showRadar) return
    
    val accent = Color(settings.accentColor)
    val infiniteTransition = rememberInfiniteTransition()
    val sweep by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing))
    )

    Canvas(modifier = Modifier.size(120.dp)) {
        val radius = size.width / 2
        val center = Offset(radius, radius)
        
        // Grid Circles
        drawCircle(accent.copy(alpha = 0.1f), radius, style = Stroke(0.5.dp.toPx()))
        drawCircle(accent.copy(alpha = 0.05f), radius * 0.6f, style = Stroke(0.5.dp.toPx()))
        
        // Sweep
        if (settings.radarSweepEnabled) {
            drawArc(
                brush = Brush.sweepGradient(listOf(Color.Transparent, accent.copy(alpha = 0.2f), Color.Transparent), center),
                startAngle = sweep, sweepAngle = 60f, useCenter = true
            )
        }

        // Blip
        val angle = when(state.sector) {
            com.humanoidai.hearing.CompassSector.N -> 270f
            com.humanoidai.hearing.CompassSector.E -> 0f
            com.humanoidai.hearing.CompassSector.S -> 90f
            com.humanoidai.hearing.CompassSector.W -> 180f
            else -> null
        }

        angle?.let { a ->
            val rad = Math.toRadians(a.toDouble())
            val blip = Offset(center.x + (radius * 0.8f * cos(rad)).toFloat(), center.y + (radius * 0.8f * sin(rad)).toFloat())
            drawCircle(accent, 4.dp.toPx(), blip)
            drawCircle(accent.copy(alpha = 0.3f), 8.dp.toPx() * (1f + state.amplitudeDb / 100f), blip)
        }
    }
}

val HexagonShape = GenericShape { size, _ ->
    val radius = size.width / 2f
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    for (i in 0 until 6) {
        val angle = Math.toRadians((i * 60).toDouble())
        val x = centerX + radius * cos(angle).toFloat()
        val y = centerY + radius * sin(angle).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    settings: AppearanceSettings,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = com.humanoidai.ui.theme.SurfaceDark.copy(alpha = settings.cardTransparency),
        shape = RoundedCornerShape(settings.cornerRadius),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        // Apply blur if strength > 0 (requires API 31+ for RenderEffect, or simplified here)
        Box(modifier = Modifier.padding(12.dp), content = content)
    }
}

