package com.humanoidai.ui.screens

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.humanoidai.ml.OwnerEnrollmentManager
import com.humanoidai.navigation.NavRoutes
import com.humanoidai.runtime.SystemReadiness
import com.humanoidai.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController, 
    ownerManager: OwnerEnrollmentManager,
    authViewModel: AuthViewModel,
) {
    val isReady by SystemReadiness.isReady.collectAsState()
    
    LaunchedEffect(key1 = isReady) {
        if (!isReady) return@LaunchedEffect
        
        delay(6000) // 3s ARMSUN + 3s Fish for visual satisfaction
        
        try {
            val enrolled = ownerManager.isOwnerEnrolled()
            val voiceEnrolled = ownerManager.isVoiceEnrolled()
            val justLoggedIn = authViewModel.consumeAuthFlag()
            
            val target = when {
                !enrolled || !voiceEnrolled -> NavRoutes.OWNER_ENROLLMENT
                justLoggedIn -> NavRoutes.ENVIRONMENT
                else -> NavRoutes.BIOMETRIC_VERIFICATION
            }
            
            navController.navigate(target) {
                popUpTo(NavRoutes.SPLASH) { inclusive = true }
            }
        } catch (e: Exception) {
            Log.e("Aura360Nav", "Navigation failure from Splash: ${e.message}")
        }
    }

    FishLoadingAnimation()
}
