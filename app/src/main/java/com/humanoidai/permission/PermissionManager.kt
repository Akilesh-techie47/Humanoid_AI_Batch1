package com.humanoidai.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Handles permission checks and provides information for the UI to request them.
 */
object PermissionManager {

    val RECORD_AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO
    val READ_CONTACTS_PERMISSION = Manifest.permission.READ_CONTACTS
    val READ_CALL_LOG_PERMISSION = Manifest.permission.READ_CALL_LOG
    val READ_SMS_PERMISSION = Manifest.permission.READ_SMS

    fun hasRecordAudioPermission(context: Context) = hasPermission(context, RECORD_AUDIO_PERMISSION)
    fun hasReadContactsPermission(context: Context) = hasPermission(context, READ_CONTACTS_PERMISSION)
    fun hasReadCallLogPermission(context: Context) = hasPermission(context, READ_CALL_LOG_PERMISSION)
    fun hasReadSmsPermission(context: Context) = hasPermission(context, READ_SMS_PERMISSION)

    private fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isNotificationServiceEnabled(context: Context): Boolean {
        val pkgName = context.packageName
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return flat != null && flat.contains(pkgName)
    }
}
