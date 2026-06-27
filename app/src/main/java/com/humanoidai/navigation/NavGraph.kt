package com.humanoidai.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.humanoidai.ai.AIManager
import com.humanoidai.BuildConfig
import com.humanoidai.alerts.AlertEngine
import com.humanoidai.alerts.NotificationHelper
import com.humanoidai.ml.FaceEnrollmentManager
import com.humanoidai.ml.FaceRecognitionManager
import com.humanoidai.ml.OwnerEnrollmentManager
import com.humanoidai.ui.screens.*

@Composable
fun NavGraph(navController: NavHostController, startDestination: String = NavRoutes.LOGIN) {
    val context = LocalContext.current

    // Shared ML managers
    val enrollmentManager  = remember { FaceEnrollmentManager(context) }
    val ownerManager       = remember { OwnerEnrollmentManager(context) }
    val recognitionManager = remember { FaceRecognitionManager() }

    // Context & AI Foundation
    val contextEngine = remember { com.humanoidai.context.ContextEngine(context) }
    val conversationMemory = remember { com.humanoidai.memory.ConversationMemory() }
    val voiceEngine = remember { com.humanoidai.voice.VoiceEngine(context) }
    val aiManager = remember { AIManager(context, BuildConfig.GEMINI_API_KEY, contextEngine, conversationMemory, voiceEngine) }
    val attentionManager = remember { com.humanoidai.attention.AttentionManager() }
    val microphoneManager = remember { com.humanoidai.hearing.SpeechRecognizerManager(context) }

    // Alert engine — shared across EnvironmentScreen and AlertsScreen
    val alertEngine = remember {
        AlertEngine(
            ownerName = ownerManager.getOwnerName(),
            onNewAlert = { alert ->
                NotificationHelper.sendAlert(context, alert)
            }
        )
    }

    // Companion Engine Infrastructure (Milestone C1)
    val ttsManager = remember { com.humanoidai.voice.TTSManager(context) }
    val companionEngine = remember { com.humanoidai.companion.CompanionEngine(context, ttsManager, contextEngine, microphoneManager) }

    // Load saved faces on startup
    LaunchedEffect(Unit) {
        NotificationHelper.createChannels(context)
        enrollmentManager.loadAllInto(recognitionManager)
        ownerManager.getMasterEmbedding()?.let {
            recognitionManager.registerFace(ownerManager.getOwnerName(), it)
        }
        companionEngine.wake()
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(NavRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoutes.SPLASH) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                },
                microphoneManager = microphoneManager,
                voiceEngine = voiceEngine
            )
        }

        composable(NavRoutes.OWNER_ENROLLMENT) {
            OwnerEnrollmentScreen(
                navController      = navController,
                ownerManager       = ownerManager,
                recognitionManager = recognitionManager,
                microphoneManager  = microphoneManager,
                voiceEngine        = voiceEngine
            )
        }

        composable(NavRoutes.SPLASH) {
            SplashScreen(navController, ownerManager)
        }

        composable(NavRoutes.DASHBOARD) {
            DashboardScreen(navController)
        }

        composable(NavRoutes.ENVIRONMENT) {
            EnvironmentScreen(
                navController      = navController,
                recognitionManager = recognitionManager,
                ownerManager       = ownerManager,
                enrollmentManager  = enrollmentManager,
                alertEngine        = alertEngine,
                aiManager          = aiManager,
                companionEngine    = companionEngine,
                contextEngine      = contextEngine,
                voiceEngine        = voiceEngine,
                microphoneManager  = microphoneManager,
                attentionManager   = attentionManager
            )
        }

        composable(NavRoutes.RECOGNITION) {
            RecognitionScreen(
                navController      = navController,
                enrollmentManager  = enrollmentManager,
                recognitionManager = recognitionManager
            )
        }

        composable(NavRoutes.ENROLLMENT) {
            EnrollmentScreen(
                navController      = navController,
                enrollmentManager  = enrollmentManager,
                recognitionManager = recognitionManager,
                microphoneManager  = microphoneManager,
                voiceEngine        = voiceEngine
            )
        }

        composable(NavRoutes.ALERTS) {
            AlertsScreen(
                navController = navController,
                alertEngine   = alertEngine
            )
        }

        composable(NavRoutes.ASSISTANT)    { 
            // Assistant functionality is now merged into the Environment (Home) Screen.
            // We redirect here just in case any old links exist, but NavRoutes should ideally be cleaned up.
            navController.navigate(NavRoutes.ENVIRONMENT)
        }
        composable(NavRoutes.HISTORY)      { HistoryScreen(navController) }
        composable(NavRoutes.SETTINGS)     { SettingsScreen(navController, ownerManager) }
        composable(NavRoutes.ANALYTICS)    { AnalyticsScreen(navController) }
    }
}
