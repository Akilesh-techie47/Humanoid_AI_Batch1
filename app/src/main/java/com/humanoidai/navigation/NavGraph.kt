package com.humanoidai.navigation

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.launch
import com.humanoidai.ai.AIManager
import com.humanoidai.BuildConfig
import com.humanoidai.alerts.AlertEngine
import com.humanoidai.alerts.NotificationHelper
import com.humanoidai.attention.AttentionManager
import com.humanoidai.behavior.BehaviorEngine
import com.humanoidai.ml.FaceEnrollmentManager
import com.humanoidai.ml.FaceRecognitionManager
import com.humanoidai.ml.OwnerEnrollmentManager
import com.humanoidai.ui.screens.*
import com.humanoidai.ui.settings.AppearanceScreen
import com.humanoidai.ui.layoutcustomization.data.repository.LayoutCustomizationRepository
import com.humanoidai.ui.layoutcustomization.domain.manager.LayoutCustomizationManager
import com.humanoidai.ui.layoutcustomization.domain.manager.LayoutCustomizationEngine
import com.humanoidai.ui.layoutcustomization.domain.manager.LayoutConstraintResolver
import com.humanoidai.ui.layoutcustomization.domain.validator.LayoutCustomizationValidator
import com.humanoidai.ui.layoutcustomization.presentation.viewmodel.LayoutCustomizationViewModel
import com.humanoidai.ui.layoutcustomization.LayoutCustomizationScreen
import com.humanoidai.runtime.AIRuntimeManager
import com.humanoidai.runtime.ui.RuntimeInspectorScreen
import com.humanoidai.runtime.ui.RuntimeInspectorViewModel
import com.humanoidai.security.TrustFramework
import com.humanoidai.security.ui.PrivacyDashboardScreen
import com.humanoidai.security.ui.PrivacyDashboardViewModel
import com.humanoidai.security.ui.SecurityInspectorScreen
import com.humanoidai.planning.ui.GoalInspectorScreen
import com.humanoidai.planning.ui.GoalInspectorViewModel
import com.humanoidai.behavior.ui.BehaviorInspectorScreen
import com.humanoidai.behavior.ui.BehaviorInspectorViewModel
import com.humanoidai.communication.CommunicationIntelligenceEngine
import com.humanoidai.companion.CompanionEngine
import com.humanoidai.context.ContextEngine
import com.humanoidai.context.proactive.ProactiveTriggerEngine
import com.humanoidai.embodiment.phone.PhoneEmbodiment
import com.humanoidai.embodiment.ui.EmbodimentInspectorScreen
import com.humanoidai.embodiment.ui.EmbodimentInspectorViewModel
import com.humanoidai.distributed.ui.CoordinationInspectorScreen
import com.humanoidai.distributed.ui.CoordinationInspectorViewModel
import com.humanoidai.embodiment.EmbodimentManager
import com.humanoidai.hearing.SpeechRecognizerManager
import com.humanoidai.memory.ConversationMemory
import com.humanoidai.memory.LongTermMemory
import com.humanoidai.ui.customization.AppearanceViewModel
import com.humanoidai.voice.VoiceEngine

@Composable
fun NavGraph(navController: NavHostController, startDestination: String = NavRoutes.LOGIN) {
    val context = LocalContext.current
    val appScope = rememberCoroutineScope()

    // Context & AI Foundation
    val contextEngine = remember { ContextEngine(context, appScope) }
    val conversationMemory = remember { ConversationMemory() }
    val voiceEngine = remember { VoiceEngine(context) }
    val authViewModel = remember { AuthViewModel() }
    
    // UI Appearance Engine
    val appearanceViewModel: AppearanceViewModel =
        viewModel(factory = AppearanceViewModel.Factory(context))

    // Shared ML managers
    val enrollmentManager  = remember { FaceEnrollmentManager(context) }
    val ownerManager       = remember { OwnerEnrollmentManager(context) }
    val recognitionManager = remember { FaceRecognitionManager() }

    val microphoneManager = remember { SpeechRecognizerManager(context) }
    
    // Embodiment & Behavior (Phase 6A/5D)

    val embodimentManager = remember { EmbodimentManager() }
    val behaviorEngine = remember {
        BehaviorEngine(voiceEngine, microphoneManager, embodimentManager)
    }

    // ---- Layout Customization Module (Phase 1B Centralized State) ----
    val layoutCustomizationRepository = remember { LayoutCustomizationRepository(context) }
    val layoutCustomizationValidator = remember { LayoutCustomizationValidator() }
    val layoutCustomizationResolver = remember { LayoutConstraintResolver() }
    val layoutCustomizationEngine = remember { LayoutCustomizationEngine(layoutCustomizationResolver) }
    val layoutCustomizationManager = remember { 
        LayoutCustomizationManager(
            context,
            layoutCustomizationRepository, 
            layoutCustomizationValidator,
            layoutCustomizationEngine,
            behaviorEngine = behaviorEngine,
            embodimentManager = embodimentManager
        )
    }
    
    val layoutCustomizationViewModel: LayoutCustomizationViewModel = viewModel(
        factory = LayoutCustomizationViewModel.Factory(layoutCustomizationManager)
    )

    // Masking API Key usage (Phase 7 Security)
    val apiKeys = mapOf(
        "gemini" to BuildConfig.GEMINI_API_KEY.ifBlank { "MOCK_KEY" },
        "groq" to BuildConfig.GROQ_API_KEY.ifBlank { "MOCK_KEY" },
        "openrouter" to BuildConfig.OPENROUTER_API_KEY.ifBlank { "MOCK_KEY" },
        "cerebras" to BuildConfig.CEREBRAS_API_KEY.ifBlank { "MOCK_KEY" },
        "mistral" to BuildConfig.MISTRAL_API_KEY.ifBlank { "MOCK_KEY" },
        "nvidia" to BuildConfig.NVIDIA_API_KEY.ifBlank { "MOCK_KEY" }
    )
    
    val commIntelEngine = remember { CommunicationIntelligenceEngine(context) }

    val aiManager = remember { AIManager(context, apiKeys, contextEngine, conversationMemory, voiceEngine, behaviorEngine, commIntelEngine, settings = appearanceViewModel.settings) }
    
    LaunchedEffect(aiManager) {
        commIntelEngine.setAIManager(aiManager)
    }

    val attentionManager = remember { AttentionManager() }

    // Proactive AI (CEA v1.4 Step 5)
    val proactiveTriggerEngine = remember {
        ProactiveTriggerEngine(contextEngine, voiceEngine, aiManager, appScope)
    }

    val alertEngine = remember {
        AlertEngine(
            ownerName = ownerManager.getOwnerName(),
            onNewAlert = { alert ->
                // Suppress standard notifications if we are currently looking at the HUD
                // Explicitly check route name to prevent messy overlaps
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (currentRoute != NavRoutes.ENVIRONMENT) {
                    NotificationHelper.sendAlert(context, alert)
                }

                // Path B Integration: Persist Alert to Long Term Memory
                appScope.launch {
                    try {
                        LongTermMemory.getInstance(context).logAlert(
                            alertType = alert.type.name,
                            priority = alert.priority.name,
                            contactName = alert.personName,
                            metadata = mapOf(
                                "title" to alert.title,
                                "description" to alert.description
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("NavGraph", "Alert persistence failure: ${e.message}")
                    }
                }
            }
        )
    }

    // Companion Engine Infrastructure (Milestone C1)
    val companionEngine = remember { CompanionEngine(context, voiceEngine, aiManager, contextEngine, microphoneManager, appScope) }

    DisposableEffect(Unit) {
        onDispose {
            microphoneManager.destroy()
            companionEngine.shutdown()
        }
    }



    // Load saved faces on startup
    LaunchedEffect(Unit) {
        NotificationHelper.createChannels(context)
        recognitionManager.setOwner(ownerManager.getOwnerName())
        enrollmentManager.loadAllInto(recognitionManager, ownerManager)
        companionEngine.wake()
        proactiveTriggerEngine.start()
        
        // Initialize Embodiment (Phase 6A)
        layoutCustomizationManager.embodimentManager.setEmbodiment(
            PhoneEmbodiment(context, voiceEngine)
        )
    }

    NavHost(navController = navController, startDestination = startDestination) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.i("NavGraph", "Navigated to: ${destination.route}")
        }

        composable(NavRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoutes.SPLASH) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
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

        composable(NavRoutes.BIOMETRIC_VERIFICATION) {
            BiometricVerificationScreen(
                navController = navController,
                ownerManager = ownerManager,
                recognitionManager = recognitionManager,
                authViewModel = authViewModel,
                onAccessGranted = {
                    val sessionManager = TrustFramework.getInstance(context).sessionManager
                    sessionManager.createSession(
                        userId = ownerManager.getOwnerName()
                    )
                    sessionManager.authenticateSession()
                    sessionManager.activateSession()

                    navController.navigate(NavRoutes.ENVIRONMENT) {
                        popUpTo(NavRoutes.BIOMETRIC_VERIFICATION) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.SPLASH) {
            SplashScreen(navController, ownerManager, authViewModel)
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
                attentionManager   = attentionManager,
                appearanceViewModel = appearanceViewModel,
                layoutViewModel = layoutCustomizationViewModel
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
                ownerManager       = ownerManager,
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

        composable(NavRoutes.ASSISTANT) {
            AssistantScreen(
                navController = navController,
                aiManager = aiManager,
                companionEngine = companionEngine,
                ownerName = ownerManager.getOwnerName()
            )
        }

        composable(NavRoutes.HISTORY)      { HistoryScreen(navController) }
        composable(NavRoutes.SETTINGS) { 
            SettingsScreen(
                navController = navController, 
                ownerManager = ownerManager,
                recognitionManager = recognitionManager,
                voiceEngine = voiceEngine,
                appearanceViewModel = appearanceViewModel,
                microphoneManager = microphoneManager,
                aiManager = aiManager
            ) 
        }

        composable(NavRoutes.APPEARANCE) {
            AppearanceScreen(navController, viewModel = appearanceViewModel)
        }

        composable(NavRoutes.LAYOUT_CUSTOMIZATION) {
            LayoutCustomizationScreen(navController, layoutCustomizationViewModel)
        }

        composable(NavRoutes.ANALYTICS)    { AnalyticsScreen(navController) }

        composable(NavRoutes.RUNTIME_INSPECTOR) {
            val runtimeViewModel: RuntimeInspectorViewModel = viewModel(
                factory = RuntimeInspectorViewModel.Factory(AIRuntimeManager.getInstance(context))
            )
            RuntimeInspectorScreen(runtimeViewModel)
        }

        composable(NavRoutes.PRIVACY_DASHBOARD) {
            val privacyViewModel: PrivacyDashboardViewModel = viewModel(
                factory = PrivacyDashboardViewModel.Factory(TrustFramework.getInstance(context))
            )
            PrivacyDashboardScreen(privacyViewModel)
        }

        composable(NavRoutes.SECURITY_INSPECTOR) {
            val privacyViewModel: PrivacyDashboardViewModel = viewModel(
                factory = PrivacyDashboardViewModel.Factory(TrustFramework.getInstance(context))
            )
            SecurityInspectorScreen(privacyViewModel)
        }

        composable(NavRoutes.GOAL_INSPECTOR) {
            val goalViewModel: GoalInspectorViewModel = viewModel(
                factory = GoalInspectorViewModel.Factory(layoutCustomizationManager.goalManager)
            )
            GoalInspectorScreen(goalViewModel)
        }

        composable(NavRoutes.BEHAVIOR_INSPECTOR) {
            val behaviorViewModel: BehaviorInspectorViewModel = viewModel(
                factory = BehaviorInspectorViewModel.Factory(layoutCustomizationManager.behaviorEngine!!)
            )
            BehaviorInspectorScreen(behaviorViewModel)
        }

        composable(NavRoutes.EMBODIMENT_INSPECTOR) {
            val embodimentViewModel: EmbodimentInspectorViewModel = viewModel(
                factory = EmbodimentInspectorViewModel.Factory(layoutCustomizationManager.embodimentManager)
            )
            EmbodimentInspectorScreen(embodimentViewModel)
        }

        composable(NavRoutes.COORDINATION_INSPECTOR) {
            val coordViewModel: CoordinationInspectorViewModel = viewModel(
                factory = CoordinationInspectorViewModel.Factory(layoutCustomizationManager.coordinationManager)
            )
            CoordinationInspectorScreen(coordViewModel)
        }

        composable(NavRoutes.COMMUNICATION_ACCESS) {
            CommunicationAccessScreen(navController)
        }

        composable(NavRoutes.COMMUNICATION_BRIEFING) {
            CommunicationBriefingScreen(navController, aiManager, voiceEngine)
        }
    }
}
