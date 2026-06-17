package com.humanoidai.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.humanoidai.ui.screens.AlertsScreen
import com.humanoidai.ui.screens.AnalyticsScreen
import com.humanoidai.ui.screens.AssistantScreen
import com.humanoidai.ui.screens.DashboardScreen
import com.humanoidai.ui.screens.EnvironmentScreen
import com.humanoidai.ui.screens.HistoryScreen
import com.humanoidai.ui.screens.LoginScreen
import com.humanoidai.ui.screens.RecognitionScreen
import com.humanoidai.ui.screens.SettingsScreen
import com.humanoidai.ui.screens.SplashScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = "login"
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable("login") {
            LoginScreen(onLoginSuccess = {
                navController.navigate("splash") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }

        composable("splash") {
            SplashScreen(navController)
        }

        composable("dashboard") {
            DashboardScreen(navController)
        }
        // ... your other existing routes
        composable(NavRoutes.ENVIRONMENT)  { EnvironmentScreen(navController) }
        composable(NavRoutes.RECOGNITION)  { RecognitionScreen(navController) }
        composable(NavRoutes.ALERTS)       { AlertsScreen(navController) }
        composable(NavRoutes.ASSISTANT)    { AssistantScreen(navController) }
        composable(NavRoutes.HISTORY)      { HistoryScreen(navController) }
        composable(NavRoutes.SETTINGS)     { SettingsScreen(navController) }
        composable(NavRoutes.ANALYTICS)    { AnalyticsScreen(navController) }
        composable("alerts") { AlertsScreen(navController) }
        composable("recognition") { RecognitionScreen(navController) }
        composable("history") { HistoryScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
        composable("analytics") { AnalyticsScreen(navController) }
    }
}