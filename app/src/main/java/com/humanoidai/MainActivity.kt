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
import com.humanoidai.recovery.model.GapEventEntity
import com.humanoidai.recovery.model.RecoveryTier
import com.humanoidai.runtime.AIRuntimeManager
import com.humanoidai.security.TrustFramework
import com.humanoidai.security.IntegrityChecker
import com.humanoidai.security.SecurityCategory
import com.humanoidai.security.SecurityStatus
import com.humanoidai.ui.screens.AuthViewModel
import com.humanoidai.ui.theme.HumanoidAITheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.i("HumanoidMain", "MainActivity: onCreate")

        val trust = TrustFramework.getInstance(this)
        trust.initialize()

        // Security Integrity Check
        if (!IntegrityChecker.isDeviceSecure(this)) {
            trust.auditLogger.log(
                SecurityCategory.INTEGRITY,
                "Device Compromised",
                SecurityStatus.VIOLATION,
                "Root or Emulator detected"
            )
            // In a real app, we might block access here
        }
        
        // Initialize Runtime Manager
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val trust = com.humanoidai.security.TrustFramework.getInstance(this@MainActivity)
            trust.initialize()
            com.humanoidai.runtime.SystemReadiness.markReady("TRUST")

            com.humanoidai.runtime.AIRuntimeManager.getInstance(this@MainActivity).initialize()
            com.humanoidai.runtime.SystemReadiness.markReady("RUNTIME")

            try {
                com.humanoidai.ml.FisheyeCorrector.initOpenCV()
            } catch (e: Exception) {
                android.util.Log.e("HumanoidMain", "OpenCV Load Error: ${e.message}")
            } finally {
                com.humanoidai.runtime.SystemReadiness.markReady("OPENCV")
            }

            try {
                net.sqlcipher.database.SQLiteDatabase.loadLibs(this@MainActivity)
            } catch (e: Exception) {
                android.util.Log.e("HumanoidMain", "SQLCipher Load Error: ${e.message}")
            } finally {
                com.humanoidai.runtime.SystemReadiness.markReady("SQLITE")
            }

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
            if (detectedGap != null) {
                android.util.Log.d(
                    "GapDetection",
                    "Gap detected: ${detectedGap.cause}, ${(detectedGap.gapEnd - detectedGap.gapStart) / 60000} min"
                )
                
                // Path B: Persist the gap event
                try {
                    val entity = GapEventEntity(
                        gapId = detectedGap.gapId,
                        gapStart = detectedGap.gapStart,
                        gapEnd = detectedGap.gapEnd,
                        cause = detectedGap.cause.name,
                        lastKnownContextJson = null,
                        eventsDuringGapJson = "[]",
                        reconstructedSummary = "System was offline for ${(detectedGap.gapEnd - detectedGap.gapStart) / 60000} minutes due to ${detectedGap.cause}.",
                        tier = RecoveryTier.PARTIAL_DATA.name
                    )
                    db.gapEventDao().insert(entity)
                    android.util.Log.i("GapDetection", "Gap event persisted: ${detectedGap.gapId}")
                } catch (e: Exception) {
                    android.util.Log.e("GapDetection", "Failed to persist gap: ${e.message}")
                }
            } else {
                android.util.Log.d("GapDetection", "No gap detected on resume.")
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
