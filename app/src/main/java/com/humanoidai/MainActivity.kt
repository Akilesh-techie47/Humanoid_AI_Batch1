package com.humanoidai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.humanoidai.ml.FisheyeCorrector
import com.humanoidai.navigation.NavGraph
import com.humanoidai.navigation.NavRoutes
import com.humanoidai.ui.screens.AuthViewModel
import com.humanoidai.ui.theme.HumanoidAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FisheyeCorrector.initOpenCV()
        enableEdgeToEdge()
        setContent {
            HumanoidAITheme {
                val navController = rememberNavController()
                val authViewModel = remember { AuthViewModel() }

                // Skip login if already signed in
                val startDestination = if (authViewModel.isUserLoggedIn()) {
                    NavRoutes.SPLASH
                } else {
                    NavRoutes.LOGIN
                }

                NavGraph(navController = navController, startDestination = startDestination)
            }
        }
    }
}