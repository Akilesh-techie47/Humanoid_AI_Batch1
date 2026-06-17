package com.humanoidai.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.humanoidai.ml.FaceEnrollmentManager
import com.humanoidai.ml.FaceRecognitionManager
import com.humanoidai.ui.screens.AlertsScreen
import com.humanoidai.ui.screens.AnalyticsScreen
import com.humanoidai.ui.screens.AssistantScreen
import com.humanoidai.ui.screens.DashboardScreen
import com.humanoidai.ui.screens.EnrollmentScreen
import com.humanoidai.ui.screens.EnvironmentScreen
import com.humanoidai.ui.screens.HistoryScreen
import com.humanoidai.ui.screens.LoginScreen
import com.humanoidai.ui.screens.RecognitionScreen
import com.humanoidai.ui.screens.SettingsScreen
import com.humanoidai.ui.screens.SplashScreen

@Composable
fun NavGraph(navController: NavHostController, startDestination: String = NavRoutes.LOGIN) {
    val context = LocalContext.current
    val enrollmentManager  = remember { FaceEnrollmentManager(context) }
    val recognitionManager = remember { FaceRecognitionManager() }

    // Load saved faces on startup
    LaunchedEffect(Unit) {
        enrollmentManager.loadAllInto(recognitionManager)
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(NavRoutes.LOGIN) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(NavRoutes.SPLASH) {
                    popUpTo(NavRoutes.LOGIN) { inclusive = true }
                }
            })
        }

        composable(NavRoutes.SPLASH) {
            SplashScreen(navController)
        }

        composable(NavRoutes.DASHBOARD) {
            DashboardScreen(navController)
        }
        composable(NavRoutes.ENVIRONMENT) {
            EnvironmentScreen(
                navController      = navController,
                recognitionManager = recognitionManager
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
                recognitionManager = recognitionManager
            )
        }
        composable(NavRoutes.ALERTS)       { AlertsScreen(navController) }
        composable(NavRoutes.ASSISTANT)    { AssistantScreen(navController) }
        composable(NavRoutes.HISTORY)      { HistoryScreen(navController) }
        composable(NavRoutes.SETTINGS)     { SettingsScreen(navController) }
        composable(NavRoutes.ANALYTICS)    { AnalyticsScreen(navController) }
    }
}