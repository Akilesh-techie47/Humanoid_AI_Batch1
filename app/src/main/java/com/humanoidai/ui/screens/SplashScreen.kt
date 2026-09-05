package com.humanoidai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.humanoidai.ml.OwnerEnrollmentManager
import com.humanoidai.navigation.NavRoutes
import com.humanoidai.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    navController: NavController, 
    ownerManager: OwnerEnrollmentManager,
    authViewModel: AuthViewModel
) {
    val isReady by com.humanoidai.runtime.SystemReadiness.isReady.collectAsState()
    
    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        }
        launch {
            alpha.animateTo(1f, animationSpec = tween(1000))
        }
    }

    LaunchedEffect(isReady) {
        if (!isReady) return@LaunchedEffect
        
        delay(2000) // Ensure animation finishes and looks deliberate
        
        android.util.Log.d("HumanoidNav", "System Ready. Progressing from Splash.")
        
        try {
            val enrolled = ownerManager.isOwnerEnrolled()
            val voiceEnrolled = ownerManager.isVoiceEnrolled()
            val justLoggedIn = authViewModel.consumeAuthFlag()
            
            android.util.Log.d("HumanoidNav", "Session Check - Enrolled: $enrolled, Voice: $voiceEnrolled, JustLoggedIn: $justLoggedIn")
            
            val target = when {
                // Scenario 1: New User or No Biometrics -> Must enroll
                !enrolled || !voiceEnrolled -> NavRoutes.OWNER_ENROLLMENT
                
                // Scenario 2: Manual Login just happened -> Bypass Gateway
                justLoggedIn -> NavRoutes.ENVIRONMENT
                
                // Scenario 3: Normal cold start while already logged in -> Biometric Gateway
                else -> NavRoutes.BIOMETRIC_VERIFICATION
            }
            
            android.util.Log.i("HumanoidNav", "Navigating to: $target")
            navController.navigate(target) {
                popUpTo(NavRoutes.SPLASH) { inclusive = true }
            }
        } catch (e: Exception) {
            android.util.Log.e("HumanoidNav", "Navigation failure from Splash: ${e.message}")
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            Icon(Icons.Default.SmartToy, contentDescription = null,
                tint = AccentCyan, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(16.dp))
            Text("HUMANOID AI", fontSize = 28.sp,
                fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Context-Aware Intelligence", fontSize = 14.sp, color = TextSecondary)
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(color = AccentCyan, modifier = Modifier.alpha(alpha.value))
        }
    }
}
