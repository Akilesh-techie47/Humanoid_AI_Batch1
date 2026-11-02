package com.humanoidai.navigation

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import com.humanoidai.platform.manager.AuraCapabilityManager
import com.humanoidai.platform.manager.AuraCapabilityManagerImpl
import com.humanoidai.ui.apps.AppHubScreen
import com.humanoidai.ui.apps.AppDetailsScreen
import com.humanoidai.ui.apps.AuraAppPlaceholderScreen
import com.humanoidai.ui.apps.UniversalSearchScreen
import com.humanoidai.ui.apps.education.AuraEducationHomeScreen
import com.humanoidai.ui.apps.education.TeacherConversationScreen
import com.humanoidai.ui.apps.education.SubjectListScreen
import com.humanoidai.ui.apps.education.SubjectDetailScreen
import com.humanoidai.ui.apps.education.LessonDetailScreen
import com.humanoidai.ui.apps.education.viewmodel.EducationViewModel
import com.humanoidai.ui.apps.finance.*
import com.humanoidai.platform.model.finance.FinanceRepository
import com.humanoidai.platform.model.finance.FinanceAssistantEngine
import com.humanoidai.memory.database.Aura360Database
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
import com.humanoidai.platform.bridge.LocalAuraCapabilityBridge
import com.humanoidai.platform.capability.RealCapabilityProvider
import com.humanoidai.platform.capability.SearchResult
import com.humanoidai.platform.manager.SearchManager
import com.humanoidai.platform.model.education.EducationRepository
import com.humanoidai.platform.model.education.TeacherEngineImpl
import com.humanoidai.platform.orchestration.AuraMultimodalOrchestrator
import com.humanoidai.platform.orchestration.AuraTeacherOrchestratorImpl
import com.humanoidai.platform.orchestration.MLKitVisionContextProvider
import com.humanoidai.platform.orchestration.SpeechToTextProviderImpl
import com.humanoidai.platform.orchestration.VisionContextProviderImpl
import com.humanoidai.ui.customization.AppearanceViewModel
import com.humanoidai.voice.VoiceEngine

@Composable
fun NavGraph(navController: NavHostController, startDestination: String = NavRoutes.LOGIN) {
    val context = LocalContext.current
    val appScope = rememberCoroutineScope()

    // UI Appearance Engine
    val appearanceViewModel: AppearanceViewModel =
        viewModel(factory = AppearanceViewModel.Factory(context))

    // Context & AI Foundation
    val contextEngine = remember { ContextEngine(context, appScope) }
    val conversationMemory = remember { ConversationMemory() }
    val voiceEngine = remember { VoiceEngine(context) }
    val authViewModel = remember { AuthViewModel() }
    
    // Shared ML managers
    val enrollmentManager  = remember { FaceEnrollmentManager(context) }
    val ownerManager       = remember { OwnerEnrollmentManager(context) }
    val recognitionManager = remember { FaceRecognitionManager() }

    val microphoneManager = remember { SpeechRecognizerManager(context) }
    
    // Embodiment & Behavior
    val embodimentManager = remember { EmbodimentManager() }
    val behaviorEngine = remember {
        BehaviorEngine(voiceEngine, microphoneManager, embodimentManager)
    }

    // ---- Layout Customization Module ----
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
            embodimentManager = embodimentManager,
        )
    }
    
    val layoutCustomizationViewModel: LayoutCustomizationViewModel = viewModel(
        factory = LayoutCustomizationViewModel.Factory(layoutCustomizationManager)
    )

    // Masking API Key usage
    // SECURITY NOTE: These keys are currently sourced from local.properties/BuildConfig.
    // This is a DEVELOPMENT level configuration. In PRODUCTION, keys should be managed
    // via a secure vault or remote config with narrow scoping.
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
    
    // Platform Capabilities (Unified)
    val capabilityManager = remember { 
        AuraCapabilityManagerImpl(
            context,
            RealCapabilityProvider(context, aiManager, microphoneManager, voiceEngine, recognitionManager)
        ) 
    }

    val visionContextProvider = remember { MLKitVisionContextProvider() }

    // Teacher Orchestration (Phase 4 Real Pipeline)
    val teacherOrchestrator = remember {
        AuraTeacherOrchestratorImpl(
            capabilityManager = capabilityManager,
            teacherEngine = TeacherEngineImpl(aiManager),
            sttProvider = SpeechToTextProviderImpl(microphoneManager),
            visionProvider = visionContextProvider
        )
    }

    // Aura Finance Module
    val database = remember { Aura360Database.getInstance(context) }
    val financeRepository = remember {
        FinanceRepository(
            database.transactionDao(),
            database.budgetDao(),
            database.savingsGoalDao()
        )
    }

    LaunchedEffect(Unit) {
        SearchManager.registerProvider { query ->
            val results = mutableListOf<SearchResult>()
            val memory = LongTermMemory.getInstance(context)
            
            // 1. System Search
            memory.searchInteractions(query).forEach {
                results.add(
                    SearchResult(
                        title = it.eventType.replace("_", " "),
                        description = it.aiResponse,
                        sourceAppId = "aura.system",
                        timestamp = it.timestamp
                    )
                )
            }
            memory.searchAlerts(query).forEach {
                results.add(
                    SearchResult(
                        title = it.alertType.replace("_", " "),
                        description = "Alert: ${it.contactName}",
                        sourceAppId = "aura.system",
                        timestamp = it.triggeredAt
                    )
                )
            }

            // 2. Finance Search
            memory.searchFinance(query).forEach {
                results.add(
                    SearchResult(
                        title = "Transaction: ${it.merchant ?: it.category}",
                        description = "${it.type} of ${it.amount/100.0} ${it.currency} - ${it.description}",
                        sourceAppId = "aura.finance",
                        timestamp = it.timestamp
                    )
                )
            }
            results
        }
    }

    val financeViewModel: FinanceViewModel = viewModel(
        factory = FinanceViewModel.Factory(financeRepository, visionContextProvider)
    )
    val financeAssistantEngine = remember {
        FinanceAssistantEngine(aiManager, financeRepository)
    }

    val educationViewModel: EducationViewModel = viewModel(
        factory = EducationViewModel.Factory(capabilityManager, teacherOrchestrator)
    )

    LaunchedEffect(Unit) {
        SearchManager.registerProvider { query ->
            val results = mutableListOf<SearchResult>()
            val repo = EducationRepository() // Ideally shared instance
            
            repo.getSubjects().first().forEach { subject ->
                if (subject.title.contains(query, ignoreCase = true) || 
                    subject.description.contains(query, ignoreCase = true)) {
                    results.add(
                        SearchResult(
                            title = "Education: ${subject.title}",
                            description = subject.description,
                            sourceAppId = "aura.education",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                subject.lessons.forEach { lesson ->
                    if (lesson.title.contains(query, ignoreCase = true) || 
                        lesson.content.contains(query, ignoreCase = true)) {
                        results.add(
                            SearchResult(
                                title = "Lesson: ${lesson.title}",
                                description = "Subject: ${subject.title}",
                                sourceAppId = "aura.education",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
            results
        }
    }
    
    LaunchedEffect(aiManager) {
        commIntelEngine.setAIManager(aiManager)
    }

    val attentionManager = remember { AttentionManager() }

    // Proactive AI
    val proactiveTriggerEngine = remember {
        ProactiveTriggerEngine(contextEngine, voiceEngine, aiManager, appScope)
    }

    val alertEngine = remember {
        AlertEngine(initialOwnerName = "Owner") { alert ->
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (currentRoute != NavRoutes.ENVIRONMENT) {
                    NotificationHelper.sendAlert(context, alert)
                }

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
    }

    // Companion Engine
    val companionEngine = remember { CompanionEngine(context, voiceEngine, aiManager, contextEngine, microphoneManager, appScope) }

    var ownerNameState by remember { mutableStateOf("Owner") }
    LaunchedEffect(Unit) {
        ownerNameState = ownerManager.getOwnerName()
    }

    DisposableEffect(Unit) {
        onDispose {
            microphoneManager.destroy()
            companionEngine.shutdown()
        }
    }

    // Load saved faces on startup
    LaunchedEffect(Unit) {
        TrustFramework.getInstance(context).initialize()
        NotificationHelper.createChannels(context)
        
        val currentOwner = ownerManager.getOwnerName()
        recognitionManager.setOwner(currentOwner)
        alertEngine.setOwnerName(currentOwner)
        
        enrollmentManager.loadAllInto(recognitionManager, ownerManager)
        companionEngine.wake()
        proactiveTriggerEngine.start()
        
        layoutCustomizationManager.embodimentManager.setEmbodiment(
            PhoneEmbodiment(context, voiceEngine)
        )
    }

    val bridge = LocalAuraCapabilityBridge.current
    val multimodalOrchestrator = remember(aiManager, bridge) {
        AuraMultimodalOrchestrator(aiManager, bridge)
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally(initialOffsetX = { 60 }) },
        exitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally(targetOffsetX = { -60 }) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally(initialOffsetX = { -60 }) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally(targetOffsetX = { 60 }) }
    ) {
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

        composable(
            route = "${NavRoutes.OWNER_ENROLLMENT}?isUpdateMode={isUpdateMode}&updateType={updateType}",
            arguments = listOf(
                navArgument("isUpdateMode") { type = NavType.BoolType; defaultValue = false },
                navArgument("updateType") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val isUpdateMode = backStackEntry.arguments?.getBoolean("isUpdateMode") ?: false
            val updateType = backStackEntry.arguments?.getString("updateType")
            
            OwnerEnrollmentScreen(
                navController      = navController,
                ownerManager       = ownerManager,
                recognitionManager = recognitionManager,
                capabilityManager  = capabilityManager,
                microphoneManager  = microphoneManager,
                voiceEngine        = voiceEngine,
                isUpdateMode       = isUpdateMode,
                updateType         = updateType,
                onEnrollmentComplete = { userId ->
                    val sessionManager = TrustFramework.getInstance(context).sessionManager
                    sessionManager.createSession(userId)
                    sessionManager.authenticateSession()
                    sessionManager.activateSession()
                }
            )
        }

        composable(NavRoutes.BIOMETRIC_VERIFICATION) {
            BiometricVerificationScreen(
                navController = navController,
                ownerManager = ownerManager,
                recognitionManager = recognitionManager,
                authViewModel = authViewModel,
                onAccessGranted = {
                    appScope.launch {
                        val name = ownerManager.getOwnerName()
                        ownerNameState = name
                        val sessionManager = TrustFramework.getInstance(context).sessionManager
                        sessionManager.createSession(userId = name)
                        sessionManager.authenticateSession()
                        sessionManager.activateSession()
                        withContext(Dispatchers.Main) {
                            navController.navigate(NavRoutes.ENVIRONMENT) {
                                popUpTo(NavRoutes.BIOMETRIC_VERIFICATION) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        composable(NavRoutes.SPLASH) {
            SplashScreen(navController, ownerManager, authViewModel) { userId ->
                val sessionManager = TrustFramework.getInstance(context).sessionManager
                sessionManager.createSession(userId)
                sessionManager.authenticateSession()
                sessionManager.activateSession()
            }
        }

        composable(NavRoutes.DASHBOARD) { DashboardScreen(navController) }

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
                layoutViewModel = layoutCustomizationViewModel,
                capabilityManager = capabilityManager,
                multimodalOrchestrator = multimodalOrchestrator
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
            AlertsScreen(navController = navController, alertEngine = alertEngine)
        }

        composable(NavRoutes.ASSISTANT) {
            AssistantScreen(
                navController = navController,
                aiManager = aiManager,
                companionEngine = companionEngine,
                ownerName = ownerNameState
            )
        }

        composable(NavRoutes.HISTORY) { HistoryScreen(navController) }
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

        composable(NavRoutes.ANALYTICS) { AnalyticsScreen(navController) }

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

        composable(NavRoutes.COMMUNICATION_ACCESS) { CommunicationAccessScreen(navController) }

        composable(NavRoutes.COMMUNICATION_BRIEFING) {
            CommunicationBriefingScreen(navController, aiManager, voiceEngine)
        }

        composable(NavRoutes.APPS_HUB) { AppHubScreen(navController, capabilityManager) }

        composable(NavRoutes.UNIVERSAL_SEARCH) { 
            val settings by appearanceViewModel.settings.collectAsState()
            UniversalSearchScreen(navController, settings) 
        }

        composable(
            route = NavRoutes.APP_DETAILS,
            arguments = listOf(navArgument("appId") { type = NavType.StringType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getString("appId") ?: ""
            AppDetailsScreen(navController, appId, capabilityManager)
        }

        composable(NavRoutes.AURA_FINANCE) {
            FinanceHomeScreen(navController, financeViewModel, capabilityManager)
        }

        composable(NavRoutes.FINANCE_TRANSACTIONS) {
            TransactionListScreen(navController, financeViewModel)
        }

        composable(NavRoutes.FINANCE_ADD_TRANSACTION) {
            AddTransactionScreen(navController, financeViewModel)
        }

        composable(
            route = NavRoutes.TRANSACTION_DETAILS,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
            TransactionDetailsScreen(navController, transactionId, financeViewModel)
        }

        composable(NavRoutes.FINANCE_BUDGETS) {
            BudgetsScreen(navController, financeViewModel)
        }

        composable(NavRoutes.FINANCE_ADD_BUDGET) {
            AddBudgetScreen(navController, financeViewModel)
        }

        composable(NavRoutes.FINANCE_GOALS) {
            SavingsGoalsScreen(navController, financeViewModel)
        }

        composable(NavRoutes.FINANCE_ADD_GOAL) {
            AddGoalScreen(navController, financeViewModel)
        }

        composable(
            route = NavRoutes.GOAL_DETAILS,
            arguments = listOf(navArgument("goalId") { type = NavType.StringType })
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getString("goalId") ?: ""
            GoalDetailsScreen(navController, goalId, financeViewModel)
        }

        composable(NavRoutes.FINANCE_ANALYSIS) {
            FinancialAnalysisScreen(navController, financeViewModel)
        }

        composable(NavRoutes.FINANCE_AI) {
            FinanceAiScreen(navController, financeAssistantEngine, capabilityManager, financeViewModel)
        }

        composable(NavRoutes.FINANCE_SCAN_RECEIPT) {
            ReceiptScannerScreen(navController, capabilityManager) { bitmap ->
                financeViewModel.setCapturedBitmap(bitmap)
                navController.navigate(NavRoutes.FINANCE_RECEIPT_REVIEW)
            }
        }

        composable(NavRoutes.FINANCE_RECEIPT_REVIEW) {
            val bitmap by financeViewModel.capturedBitmap.collectAsState()
            if (bitmap != null) {
                ReceiptReviewScreen(navController, financeViewModel, bitmap!!)
            } else {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }

        composable(NavRoutes.AURA_MAPS) {
            AuraAppPlaceholderScreen(navController, "aura.maps")
        }

        composable(NavRoutes.AURA_LIFE) {
            AuraAppPlaceholderScreen(navController, "aura.life")
        }

        composable(NavRoutes.AURA_BUSINESS) {
            AuraAppPlaceholderScreen(navController, "aura.business")
        }

        composable(NavRoutes.AURA_EDUCATION) {
            AuraEducationHomeScreen(navController, capabilityManager, educationViewModel)
        }

        composable(
            route = NavRoutes.EDUCATION_TEACHER,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("lessonId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId")
            val lessonId = backStackEntry.arguments?.getString("lessonId")
            TeacherConversationScreen(navController, capabilityManager, educationViewModel, subjectId, lessonId)
        }

        composable(NavRoutes.EDUCATION_SUBJECTS) {
            SubjectListScreen(navController, educationViewModel)
        }

        composable(
            route = NavRoutes.EDUCATION_SUBJECT_DETAIL,
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            SubjectDetailScreen(navController, subjectId, educationViewModel)
        }

        composable(
            route = NavRoutes.EDUCATION_LESSON,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("lessonId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            LessonDetailScreen(navController, subjectId, lessonId, educationViewModel)
        }
    }
}
