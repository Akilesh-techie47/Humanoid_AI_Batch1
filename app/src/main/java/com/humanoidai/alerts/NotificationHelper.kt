package com.humanoidai.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.scale
import com.humanoidai.MainActivity

// -----------------------------------------------------------------
// NotificationHelper
// -----------------------------------------------------------------
// Sends Android system notifications when an alert is generated
// by AlertEngine. Used when the app is in background or screen off.
//
// Channels:
//   CRITICAL — heads-up, full screen intent
//   HIGH     — heads-up notification
//   MEDIUM   — standard notification
//   LOW      — silent notification
// -----------------------------------------------------------------
object NotificationHelper {

    private const val CHANNEL_CRITICAL = "humanoid_critical"
    private const val CHANNEL_HIGH     = "humanoid_high"
    private const val CHANNEL_MEDIUM   = "humanoid_medium"
    private const val CHANNEL_LOW      = "humanoid_low"

    private var notificationId = 1000

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CRITICAL,
                "Critical Alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Critical security alerts — unknown persons, multiple intrusions"
                enableVibration(true)
                enableLights(true)
            }
        )

        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_HIGH,
            "Security Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High priority alerts — single unknown person detected"
            enableVibration(true)
        })

        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_MEDIUM,
            "Activity Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Medium priority — owner returned, known persons"
        })

        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_LOW,
            "Info Alerts",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Low priority informational alerts"
        })
    }

    fun sendAlert(context: Context, alert: AlertItem) {
        val channel = when (alert.priority) {
            AlertPriority.CRITICAL -> CHANNEL_CRITICAL
            AlertPriority.HIGH     -> CHANNEL_HIGH
            AlertPriority.MEDIUM   -> CHANNEL_MEDIUM
            AlertPriority.LOW      -> CHANNEL_LOW
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "alerts")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(alert.title)
            .setContentText(alert.description)
            .setPriority(when (alert.priority) {
                AlertPriority.CRITICAL -> NotificationCompat.PRIORITY_MAX
                AlertPriority.HIGH     -> NotificationCompat.PRIORITY_HIGH
                AlertPriority.MEDIUM   -> NotificationCompat.PRIORITY_DEFAULT
                AlertPriority.LOW      -> NotificationCompat.PRIORITY_LOW
            })
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Add face crop as large icon if available
        alert.faceBitmap?.let { bitmap ->
            builder.setLargeIcon(bitmap.scale(128, 128))
        }

        // Heads-up for HIGH/CRITICAL
        if ((alert.priority == AlertPriority.CRITICAL) || (alert.priority == AlertPriority.HIGH)) {
            builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE)
        }

        try {
            android.util.Log.i("NotificationHelper", "[NOTIFICATION_TRIGGER] priority=${alert.priority}, title=${alert.title}")
            NotificationManagerCompat.from(context).notify(notificationId++, builder.build())
        } catch (_: SecurityException) {
            // Notification permission not granted — silently ignore
        }

    }
}
