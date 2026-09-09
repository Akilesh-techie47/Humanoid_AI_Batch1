package com.humanoidai.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.humanoidai.ui.components.ArmsunSplashScreen
import kotlinx.coroutines.delay

/**
 * Branded Loading Sequence: ARMSUN Logo (3s) -> Fish Animation.
 */
@Composable
fun FishLoadingAnimation(
    modifier: Modifier = Modifier,
) {
    var showBranding by remember { mutableStateOf(value = true) }

    LaunchedEffect(key1 = Unit) {
        delay(3000) // Mandatory 3s ARMSUN branding
        showBranding = false
    }

    if (showBranding) {
        ArmsunSplashScreen()
    } else {
        FishAnimationContent(modifier)
    }
}

@Composable
fun FishAnimationContent(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "fish_swim")
    
    // Smooth entry from right to center
    val xPos by infiniteTransition.animateFloat(
        initialValue = 1.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "xPos"
    )

    // Subtle swimming oscillation
    val swimOscillation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swim"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        PhoneLogoFrame(
            modifier = Modifier
                .size(200.dp)
                .scale(if (xPos < 0.55f) 1.05f else 1.0f)
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val scope = this
            val fishX = scope.maxWidth * (xPos - 0.1f) // Adjusted to center fish in box
            
            FishIcon(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = fishX, y = swimOscillation.dp)
                    .size(70.dp),
                color = FishGold
            )
        }
    }
}

@Composable
fun PhoneLogoFrame(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.75f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .border(2.dp, Color.White, CircleShape) // Changed from AccentOrange to White
        )
    }
}

@Composable
fun FishIcon(
    modifier: Modifier = Modifier,
    color: Color = FishGold
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Body - yellowish orange / warm gold
        val bodyPath = Path().apply {
            moveTo(w * 0.1f, h * 0.5f)
            quadraticTo(w * 0.4f, h * 0.2f, w * 0.75f, h * 0.5f)
            quadraticTo(w * 0.4f, h * 0.8f, w * 0.1f, h * 0.5f)
            close()
        }
        drawPath(bodyPath, color)
        
        // Tail
        val tailPath = Path().apply {
            moveTo(w * 0.75f, h * 0.5f)
            lineTo(w * 0.95f, h * 0.25f)
            lineTo(w * 0.95f, h * 0.75f)
            close()
        }
        drawPath(tailPath, color)
        
        // Eye
        drawCircle(
            color = Color.White,
            radius = w * 0.07f,
            center = Offset(w * 0.3f, h * 0.42f)
        )
        drawCircle(
            color = Color.Black,
            radius = w * 0.03f,
            center = Offset(w * 0.31f, h * 0.42f)
        )
    }
}
