package com.humanoidai.navigation

import androidx.compose.runtime.*
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
import com.humanoidai.embodiment.phone.PhoneEmbodiment
import com.humanoidai.embodiment.ui.EmbodimentInspectorScreen
import com.humanoidai.embodiment.ui.EmbodimentInspectorViewModel
import com.humanoidai.distributed.ui.CoordinationInspectorScreen
import com.humanoidai.distributed.ui.CoordinationInspectorViewModel

@Composable
fun NavGraph(navController: NavHostController, startDestination: String = NavRoutes.LOGIN) {
    val context = LocalContext.current
    val appScope = rememberCoroutineScope()

    // Context & AI Foundation
    val contextEngine = remember { com.humanoidai.context.ContextEngine(context, appScope) }
    val conversationMemory = remember { com.humanoidai.memory.ConversationMemory() }
    val voiceEngine = remember { com.humanoidai.voice.VoiceEngine(context) }
    val authViewModel = remember { AuthViewModel() }
    
    // Embodiment & Behavior (Phase 6A/5D)
    val embodimentManager = remember { com.humanoidai.embodiment.EmbodimentManager() }
    val behaviorEngine = remember { com.humanoidai.behavior.BehaviorEngine(voiceEngine, embodimentManager) }

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
    
    val layoutCustomizationViewModel: LayoutCustomizationViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = LayoutCustomizationViewModel.Factory(layoutCustomizationManager)
    )

    // Shared ML managers
    val enrollmentManager  = remember { FaceEnrollmentManager(context) }
    val ownerManager       = remember { OwnerEnrollmentManager(context) }
    val recognitionManager = remember { FaceRecognitionManager() }

    val aiManager = remember { AIManager(context, BuildConfig.GEMINI_API_KEY, contextEngine, conversationMemory, voiceEngine, behaviorEngine) }
    val attentionManager = remember { com.humanoidai.attention.AttentionManager() }
    val microphoneManager = remember { com.humanoidai.hearing.SpeechRecognizerManager(context) }

    // Proactive AI (CEA v1.4 Step 5)
    val proactiveTriggerEngine = remember { 
        com.humanoidai.context.proactive.ProactiveTriggerEngine(contextEngine, voiceEngine, appScope) 
    }

    // Alert engine — shared across EnvironmentScreen and AlertsScreen
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
            }
        )
    }

    // Companion Engine Infrastructure (Milestone C1)
    val ttsManager = remember { com.humanoidai.voice.TTSManager(context) }
    val companionEngine = remember { com.humanoidai.companion.CompanionEngine(context, ttsManager, contextEngine, microphoneManager, appScope) }

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

    // UI Appearance Engine
    val appearanceViewModel: com.humanoidai.ui.customization.AppearanceViewModel = 
        androidx.lifecycle.viewmodel.compose.viewModel(factory = com.humanoidai.ui.customization.AppearanceViewModel.Factory(context))

    NavHost(navController = navController, startDestination = startDestination) {

        composable(NavRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoutes.SPLASH) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                },
                authViewModel = authViewModel,
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

        composable(NavRoutes.BIOMETRIC_VERIFICATION) {
            BiometricVerificationScreen(
                navController = navController,
                ownerManager = ownerManager,
                recognitionManager = recognitionManager,
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

        composable(NavRoutes.ASSISTANT)    { 
            navController.navigate(NavRoutes.ENVIRONMENT)
        }
        composable(NavRoutes.HISTORY)      { HistoryScreen(navController) }
        composable(NavRoutes.SETTINGS) { 
            SettingsScreen(
                navController = navController, 
                ownerManager = ownerManager,
                recognitionManager = recognitionManager,
                voiceEngine = voiceEngine,
                appearanceViewModel = appearanceViewModel,
                microphoneManager = microphoneManager
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
            val runtimeViewModel: RuntimeInspectorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return RuntimeInspectorViewModel(AIRuntimeManager.getInstance(context)) as T
                    }
                }
            )
            RuntimeInspectorScreen(runtimeViewModel)
        }

        composable(NavRoutes.PRIVACY_DASHBOARD) {
            val privacyViewModel: PrivacyDashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return PrivacyDashboardViewModel(TrustFramework.getInstance(context)) as T
                    }
                }
            )
            PrivacyDashboardScreen(privacyViewModel)
        }

        composable(NavRoutes.SECURITY_INSPECTOR) {
            val privacyViewModel: PrivacyDashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return PrivacyDashboardViewModel(TrustFramework.getInstance(context)) as T
                    }
                }
            )
            SecurityInspectorScreen(privacyViewModel)
        }

        composable(NavRoutes.GOAL_INSPECTOR) {
            val goalViewModel: GoalInspectorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return GoalInspectorViewModel(layoutCustomizationManager.goalManager) as T
                    }
                }
            )
            GoalInspectorScreen(goalViewModel)
        }

        composable(NavRoutes.BEHAVIOR_INSPECTOR) {
            val behaviorViewModel: BehaviorInspectorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return BehaviorInspectorViewModel(layoutCustomizationManager.behaviorEngine!!) as T
                    }
                }
            )
            BehaviorInspectorScreen(behaviorViewModel)
        }

        composable(NavRoutes.EMBODIMENT_INSPECTOR) {
            val embodimentViewModel: EmbodimentInspectorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return EmbodimentInspectorViewModel(layoutCustomizationManager.embodimentManager) as T
                    }
                }
            )
            EmbodimentInspectorScreen(embodimentViewModel)
        }

        composable(NavRoutes.COORDINATION_INSPECTOR) {
            val coordViewModel: CoordinationInspectorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return CoordinationInspectorViewModel(layoutCustomizationManager.coordinationManager) as T
                    }
                }
            )
            CoordinationInspectorScreen(coordViewModel)
        }
    }
}
