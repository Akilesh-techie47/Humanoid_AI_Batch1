package com.humanoidai.ui.theme

import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.asImageBitmap
import com.humanoidai.vision.DetectedPerson

private fun gridConfig(n: Int): Triple<Int, Dp, Float> = when {
    n <= 2 -> Triple(2, 54.dp, 8f)
    n <= 4 -> Triple(2, 48.dp, 7.5f)
    n <= 6 -> Triple(3, 42.dp, 7f)
    else -> Triple(3, 36.dp, 6.5f)
}

@Composable
fun CameraHomeHud(
    theme: HudTheme,
    detectedPersons: List<DetectedPerson>,
    ownerName: String,
    isListening: Boolean,
    inputText: String,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMicToggle: () -> Unit,
    onMenu: () -> Unit,
    onSwitchCamera: () -> Unit,
    onAddRoi: () -> Unit,
    previewView: (PreviewView) -> Unit,
    timeText: String = "09:41",
    statusText: String = "OBSERVING",
    modifier: Modifier = Modifier
) {
    val primary = detectedPersons.find { it.isPrimary } ?: detectedPersons.firstOrNull { it.name.equals(ownerName, true) }
    val others = detectedPersons.filter { it != primary }
    val totalInFrame = detectedPersons.size.coerceAtLeast(others.size + 1)
    val knownCount = others.count { it.name != "UNKNOWN" && it.name != "Unknown" }
    val unknownCount = others.count { it.name == "UNKNOWN" || it.name == "Unknown" }

    // Animations
    val infinite = rememberInfiniteTransition(label = "hud")
    val roiPulse by infinite.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "roiPulse"
    )
    val micPulse by infinite.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "mic"
    )
    val scanOffset by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "scan"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.bgTop, theme.bgBottom)))
            .then(if (theme.gridBackground) Modifier.drawBehind {
                val step = 20.dp.toPx()
                val lineColor = theme.accent.copy(alpha = 0.08f)
                var x = 0f
                while (x < size.width) {
                    drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), 0.6f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(lineColor, Offset(0f, y), Offset(size.width, y), 0.6f)
                    y += step
                }
            } else Modifier)
    ) {
        // Scanning line overlay for blueprint / tactical
        if (theme.id == "t10" || theme.id == "t1") {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val y = size.height * scanOffset
                drawLine(
                    color = theme.accent.copy(alpha = 0.12f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.2f
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, theme.accent.copy(alpha = 0.06f), Color.Transparent),
                        startY = y - 18f, endY = y + 18f
                    ),
                    topLeft = Offset(0f, y - 18f),
                    size = androidx.compose.ui.geometry.Size(size.width, 36f)
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Status bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    timeText, color = theme.text, fontSize = 11.sp,
                    fontFamily = if (theme.monospace) FontFamily.Monospace else FontFamily.Default,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    statusText, color = theme.text, fontSize = 10.sp,
                    fontFamily = if (theme.monospace) FontFamily.Monospace else FontFamily.Default,
                    letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                "$totalInFrame PEOPLE IN FRAME",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = theme.text.copy(alpha = 0.95f),
                fontSize = 10.sp,
                fontFamily = if (theme.monospace) FontFamily.Monospace else FontFamily.Default,
                letterSpacing = 0.6.sp
            )

            Spacer(Modifier.height(10.dp))

            // Primary ROI
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PrimaryRoi(
                    theme = theme,
                    pulse = roiPulse,
                    previewView = previewView,
                    roiSize = 76.dp
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "${ownerName.uppercase()} · OWNER",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = theme.text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                fontFamily = if (theme.monospace) FontFamily.Monospace else FontFamily.Default
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "OTHERS DETECTED (${others.size})",
                modifier = Modifier.padding(horizontal = 14.dp),
                color = theme.text.copy(alpha = 0.7f),
                fontSize = 8.sp,
                letterSpacing = 0.5.sp,
                fontFamily = if (theme.monospace) FontFamily.Monospace else FontFamily.Default
            )

            Spacer(Modifier.height(6.dp))

            // Guest grid — adaptive, no scroll, wraps
            if (others.isNotEmpty()) {
                val cfg = gridConfig(others.size)
                val cols = cfg.first
                val ring = cfg.second
                val fontScale = cfg.third

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    others.chunked(cols).forEachIndexed { rowIdx, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEachIndexed { idx, person ->
                                val globalIdx = rowIdx * cols + idx
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(tween(300, delayMillis = globalIdx * 70)) + scaleIn(tween(300, delayMillis = globalIdx * 70), initialScale = 0.85f)
                                ) {
                                    GuestChip(
                                        person = person,
                                        theme = theme,
                                        ringSize = ring,
                                        fontScale = fontScale
                                    )
                                }
                            }
                            // Fill row to keep centering
                            repeat(cols - row.size) {
                                Spacer(Modifier.width(ring).height(ring))
                            }
                        }
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            // Info bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.panelBg)
                    .border(1.dp, theme.panelBorder, RoundedCornerShape(10.dp))
                    .clickable { onAddRoi() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$knownCount known · $unknownCount unknown",
                        color = theme.panelText,
                        fontSize = 10.sp,
                        fontFamily = if (theme.monospace) FontFamily.Monospace else FontFamily.Default
                    )
                    Text(
                        "+ ROI", color = theme.panelText, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        fontFamily = if (theme.monospace) FontFamily.Monospace else FontFamily.Default,
                        modifier = Modifier.clickable { onAddRoi() }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Chat bar
            val chatShape = RoundedCornerShape(22.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp)
                    .height(42.dp)
                    .clip(chatShape)
                    .background(
                        if (theme.chatFilled) theme.chatBg
                        else theme.chatBg
                    )
                    .then(
                        if (!theme.chatFilled) Modifier.border(1.dp, theme.panelBorder, chatShape) else Modifier
                    )
                    .clickable { onMicToggle() }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(theme.micColor.copy(alpha = if (isListening) micPulse else 1f))
                            .then(
                                if (theme.roiGlow && isListening) Modifier.drawBehind {
                                    drawCircle(theme.micColor.copy(alpha = 0.18f * micPulse), radius = 14.dp.toPx())
                                } else Modifier
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isListening) "Listening..." else "Listening...",
                        color = theme.chatText,
                        fontSize = 11.sp,
                        fontFamily = if (theme.monospace) FontFamily.Monospace else FontFamily.Default
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryRoi(
    theme: HudTheme,
    pulse: Float,
    previewView: (PreviewView) -> Unit,
    roiSize: Dp
) {
    val shape = theme.roiShape.asComposeShape()
    val borderColor = theme.accent
    Box(
        modifier = Modifier.size(roiSize),
        contentAlignment = Alignment.Center
    ) {
        if (theme.roiGlow) {
            Box(
                modifier = Modifier
                    .size(roiSize + 16.dp)
                    .clip(shape)
                    .background(borderColor.copy(alpha = 0.10f * pulse))
            )
        }
        if (theme.roiDoubleRing) {
            Box(
                modifier = Modifier
                    .size(roiSize + 10.dp)
                    .clip(shape)
                    .border(1.dp, borderColor.copy(alpha = 0.45f), shape)
            )
        }

        val innerModifier = Modifier
            .size(roiSize)
            .clip(shape)
            .then(
                when {
                    theme.roiGradient && theme.accent2 != null -> Modifier.border(
                        width = if (theme.roiThick) 3.dp else 2.dp,
                        brush = Brush.linearGradient(listOf(theme.accent, theme.accent2!!)),
                        shape = shape
                    )
                    theme.roiDashed -> Modifier.drawBehind {
                        val stroke = Stroke(width = 1.8.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f))
                        val outline = shape.createOutline(this.size, layoutDirection, this)
                        when (outline) {
                            is Outline.Generic -> drawPath(outline.path, color = borderColor, style = stroke)
                            is Outline.Rounded -> drawRoundRect(color = borderColor, style = stroke, cornerRadius = outline.roundRect.bottomLeftCornerRadius)
                            is Outline.Rectangle -> drawRect(color = borderColor, style = stroke)
                        }
                    }.border(if (theme.roiThick) 3.dp else 1.5.dp, Color.Transparent, shape)
                    theme.roiThick -> Modifier.border(3.dp, borderColor, shape)
                    else -> Modifier.border(2.dp, borderColor, shape)
                }
            )

        Box(modifier = innerModifier, contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { ctx -> PreviewView(ctx).also(previewView) },
                modifier = Modifier.fillMaxSize().clip(shape)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (theme.grayscale) 0.15f else 0.08f))
            )
            Text(
                "ROI", color = if (theme.grayscale) Color.White else borderColor,
                fontSize = 9.sp, fontWeight = FontWeight.Bold,
                fontFamily = if (theme.monospace) FontFamily.Monospace else FontFamily.Default
            )
            if (theme.roiShape == HudShape.CIRCLE && !theme.roiDashed) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = this.size.width / 2
                    val cy = this.size.height / 2
                    val col = borderColor.copy(alpha = 0.22f * pulse)
                    val len = 7.dp.toPx()
                    drawLine(col, Offset(cx, 4.dp.toPx()), Offset(cx, 4.dp.toPx() + len), 0.9f)
                    drawLine(col, Offset(cx, this.size.height - 4.dp.toPx()), Offset(cx, this.size.height - 4.dp.toPx() - len), 0.9f)
                    drawLine(col, Offset(4.dp.toPx(), cy), Offset(4.dp.toPx() + len, cy), 0.9f)
                    drawLine(col, Offset(this.size.width - 4.dp.toPx(), cy), Offset(this.size.width - 4.dp.toPx() - len, cy), 0.9f)
                }
            }
        }
    }
}

@Composable
private fun GuestChip(
    person: DetectedPerson,
    theme: HudTheme,
    ringSize: Dp,
    fontScale: Float
) {
    val isUnknown = person.name == "UNKNOWN" || person.name == "Unknown"
    val ringColor = if (isUnknown) theme.unknownColor else theme.accent
    val ringShape = theme.chipShape.asComposeShape()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp)
    ) {
        Box(
            modifier = Modifier.size(ringSize),
            contentAlignment = Alignment.Center
        ) {
            val ringMod = Modifier
                .fillMaxSize()
                .clip(ringShape)
                .background(
                    when {
                        theme.chipGlass -> Color.White.copy(alpha = 0.08f)
                        theme.chipBg.alpha > 0.01f -> theme.chipBg
                        else -> Color.Transparent
                    }
                )
                .then(
                    when {
                        theme.chipDashed -> Modifier.drawBehind {
                            val stroke = Stroke(width = 1.4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 5f), 0f))
                            val outline = ringShape.createOutline(size, layoutDirection, this)
                            when (outline) {
                                is Outline.Generic -> drawPath(outline.path, color = ringColor, style = stroke)
                                is Outline.Rounded -> drawRoundRect(color = ringColor, style = stroke, cornerRadius = outline.roundRect.bottomLeftCornerRadius)
                                is Outline.Rectangle -> drawRect(color = ringColor, style = stroke)
                            }
                        }
                        theme.id == "t8" && isUnknown -> Modifier.drawBehind {
                            val stroke = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
                            val outline = ringShape.createOutline(size, layoutDirection, this)
                            when (outline) {
                                is Outline.Generic -> drawPath(outline.path, color = ringColor, style = stroke)
                                is Outline.Rounded -> drawRoundRect(color = ringColor, style = stroke, cornerRadius = outline.roundRect.bottomLeftCornerRadius)
                                is Outline.Rectangle -> drawRect(color = ringColor, style = stroke)
                            }
                        }
                        theme.chipShape == HudShape.DIAMOND && theme.accent2 == null -> Modifier.border(1.6.dp, ringColor, ringShape)
                        theme.id == "t9" && !isUnknown -> Modifier.border(1.6.dp, Brush.linearGradient(listOf(theme.accent, theme.accent2 ?: theme.accent)), ringShape)
                        else -> Modifier.border(1.5.dp, ringColor, ringShape)
                    }
                )
            Box(modifier = ringMod, contentAlignment = Alignment.Center) {
                if (person.faceBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = person.faceBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(ringShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    // subtle inner dot
                    Box(
                        Modifier.size(ringSize * 0.28f).clip(CircleShape)
                            .background(ringColor.copy(alpha = 0.18f))
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isUnknown) "Unknown" else person.name,
            color = if (isUnknown) theme.chipText.copy(alpha = 0.9f) else theme.chipText,
            fontSize = fontScale.sp,
            fontFamily = if (theme.monospace) FontFamily.Monospace else FontFamily.Default,
            maxLines = 1, textAlign = TextAlign.Center
        )
        Text(
            text = String.format(java.util.Locale.getDefault(), "%.2f", person.confidence),
            color = theme.chipText.copy(alpha = 0.75f),
            fontSize = (fontScale - 1.2f).coerceAtLeast(6f).sp,
            fontFamily = FontFamily.Monospace
        )
    }
}


