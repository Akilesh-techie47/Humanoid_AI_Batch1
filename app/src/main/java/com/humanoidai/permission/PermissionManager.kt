package com.humanoidai.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Handles permission checks and provides information for the UI to request them.
 */
object PermissionManager {

    val RECORD_AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO

    fun hasRecordAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            RECORD_AUDIO_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Additional permissions can be added here
}
