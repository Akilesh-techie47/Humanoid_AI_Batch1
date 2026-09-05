package com.humanoidai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humanoidai.vision.DetectedPerson
import com.humanoidai.ui.theme.AccentCyan
import com.humanoidai.ui.theme.AccentPurple
import com.humanoidai.ui.theme.ErrorRed
import com.humanoidai.ui.theme.TextSecondary

/**
 * Reusable ROI Card used in the 10 layouts.
 * Matches the style of Image 1/2/3.
 */
@Composable
fun RoiCard(
    person: DetectedPerson,
    label: String,
    color: Color,
    isMain: Boolean = false,
    shape: Shape = RoundedCornerShape(8.dp),
    showLabels: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(animation = tween(1500), repeatMode = RepeatMode.Reverse)
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showLabels && !isMain) {
            Text(
                label, 
                color = color, 
                fontSize = 8.sp, 
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(if (isMain) 120.dp else 80.dp)
                .clip(shape)
                .background(color.copy(alpha = 0.05f))
                .border(1.dp, color.copy(alpha = if (isMain) 0.8f else 0.4f), shape)
                .then(if (isMain) Modifier.border(4.dp, color.copy(alpha = glowAlpha * 0.3f), shape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder for Face Thumbnail if available
            if (person.faceBitmap != null) {
                // Image implementation
            }
            
            if (isMain) {
               Text("PRIMARY", color = color.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        if (showLabels) {
            Text(
                person.name, 
                color = com.humanoidai.ui.theme.TextPrimary, 
                fontSize = 11.sp, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                String.format("%.2f", person.confidence),
                color = color.copy(alpha = 0.85f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
