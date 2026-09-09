package com.humanoidai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humanoidai.R
import com.humanoidai.ui.theme.TextSecondary

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PreviewArmsunSplashScreen() {
    ArmsunSplashScreen(skipAnimation = true)
}

@Preview(showBackground = true)
@Composable
fun PreviewArmsunFooter() {
    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
        ArmsunFooter()
    }
}

@Composable
fun ArmsunFooter(modifier: Modifier = Modifier) {
    // Branding footer removed from normal screens as per Aura 360° guidelines.
    // Attribution is now moved to the About screen.
    Box(modifier = modifier)
}

@Composable
fun ArmsunSplashScreen(
    skipAnimation: Boolean = false,
    onFinished: () -> Unit = {}
) {
    val alpha = remember { Animatable(if (skipAnimation) 1f else 0f) }
    val scale = remember { Animatable(if (skipAnimation) 1f else 0.9f) }

    LaunchedEffect(Unit) {
        if (!skipAnimation) {
            alpha.animateTo(1f, animationSpec = tween(1000))
            scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alpha.value)
                .scale(scale.value)
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Image(
                    painter = painterResource(id = R.drawable.ic_aura_logo),
                    contentDescription = "Aura Logo",
                    modifier = Modifier.size(160.dp)
                )
                Text(
                    text = "®",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.offset(x = 10.dp, y = 20.dp)
                )
            }
            
            Spacer(Modifier.height(24.dp))
            // Branding Text
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Aura 360°",
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
            
            Spacer(Modifier.height(4.dp))
            
            Text(
                text = "Fisheye contextual proactive assistant",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.2.sp
            )
        }
    }
}
