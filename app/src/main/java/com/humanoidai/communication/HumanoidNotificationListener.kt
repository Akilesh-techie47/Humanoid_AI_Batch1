package com.humanoidai.communication

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HumanoidNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "HumanoidNotif"
        
        private val _notifications = MutableStateFlow<List<CommunicationItem>>(emptyList())
        val notifications: StateFlow<List<CommunicationItem>> = _notifications.asStateFlow()

        fun clearNotifications() {
            _notifications.value = emptyList()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val notification = sbn?.notification ?: return
        val extras = notification.extras
        
        val packageName = sbn.packageName
        val id = sbn.key
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val sender = extras.getString("android.title") ?: "Unknown sender" // Usually title is sender for IMs
        
        val type = when {
            packageName.contains("whatsapp") -> CommunicationType.WHATSAPP
            packageName.contains("telegram") -> CommunicationType.TELEGRAM
            packageName.contains("messaging") || packageName.contains("sms") -> CommunicationType.SMS
            packageName.contains("gmail") -> CommunicationType.EMAIL
            else -> CommunicationType.GENERIC
        }

        // Avoid adding system or app's own notifications
        if (packageName == applicationContext.packageName) return

        val item = CommunicationItem(
            id = id,
            sourcePackage = packageName,
            sourceApp = getAppName(packageName),
            sender = sender,
            title = title,
            contentPreview = text,
            timestamp = sbn.postTime,
            type = type
        )

        updateNotifications(item)
        Log.d(TAG, "Notification posted: $packageName, $title, $text")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        val id = sbn?.key ?: return
        _notifications.value = _notifications.value.filter { it.id != id }
    }

    private fun updateNotifications(item: CommunicationItem) {
        val current = _notifications.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index != -1) {
            current[index] = item
        } else {
            current.add(0, item)
        }
        _notifications.value = current.take(50) // Keep last 50
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
