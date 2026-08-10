package com.humanoidai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.humanoidai.memory.database.HumanoidDatabase
import com.humanoidai.ml.FisheyeCorrector
import com.humanoidai.navigation.NavGraph
import com.humanoidai.navigation.NavRoutes
import com.humanoidai.recovery.engine.ContextLogTimestampAdapter
import com.humanoidai.recovery.engine.GapDetector
import com.humanoidai.runtime.AIRuntimeManager
import com.humanoidai.security.TrustFramework
import com.humanoidai.ui.screens.AuthViewModel
import com.humanoidai.ui.theme.HumanoidAITheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.i("HumanoidMain", "MainActivity: onCreate")
        
        // Initialize Runtime Manager
        AIRuntimeManager.getInstance(this).initialize()

        // Initialize Trust Framework
        TrustFramework.getInstance(this).initialize()

        try {
            FisheyeCorrector.initOpenCV()
            net.sqlcipher.database.SQLiteDatabase.loadLibs(this)
            android.util.Log.d("HumanoidMain", "Libraries loaded successfully")
        } catch (e: Exception) {
            android.util.Log.e("HumanoidMain", "Library Load Error: ${e.message}")
        }

        enableEdgeToEdge()

        // Path B: Step D - Check for Crash (Dirty Shutdown)
        val prefs = getSharedPreferences("humanoid_recovery", MODE_PRIVATE)
        val wasCleanShutdown = prefs.getBoolean("clean_shutdown", true)
        if (!wasCleanShutdown) {
            android.util.Log.w("GapDetection", "System detected a dirty shutdown (likely CRASH)")
        }
        prefs.edit { putBoolean("clean_shutdown", false) } // Reset for this session

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

    override fun onResume() {
        super.onResume()
        TrustFramework.getInstance(this).sessionManager.activateSession()

        // Path B: Step C - Gap Detection on Resume
        lifecycleScope.launch {
            val db = HumanoidDatabase.getInstance(this@MainActivity)
            val adapter = ContextLogTimestampAdapter(db.contextLogDao())
            val gapDetector = GapDetector(adapter)
            
            val detectedGap = gapDetector.checkForGap()
            detectedGap?.let { gap ->
                android.util.Log.d(
                    "GapDetection",
                    "Gap detected: ${gap.cause}, ${(gap.gapEnd - gap.gapStart) / 60000} min"
                )
                // TODO: Store GapEventEntity and hand off to RecoveryEngine UI
            }
        }
    }

    override fun onPause() {
        super.onPause()
        TrustFramework.getInstance(this).sessionManager.lockSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        TrustFramework.getInstance(this).sessionManager.closeSession()
        
        // Path B: Step D - Clean Shutdown Marker
        getSharedPreferences("humanoid_recovery", MODE_PRIVATE).edit {
            putBoolean("clean_shutdown", true)
        }
    }
}
