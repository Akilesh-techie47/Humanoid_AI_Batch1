package com.humanoidai.behavior

import android.util.Log
import java.util.PriorityQueue

/**
 * Prioritizes and throttles outgoing notifications.
 */
class NotificationCoordinator {

    companion object {
        private const val TAG = "NotificationCoord"
        private const val MIN_INTERVAL_MS = 2000L
    }

    private val queue = PriorityQueue<InteractionResponse> { r1, r2 ->
        r2.priority.ordinal.compareTo(r1.priority.ordinal)
    }

    private var lastNotificationTime = 0L

    fun post(response: InteractionResponse, onReady: (InteractionResponse) -> Unit) {
        if (response.priority == InteractionPriority.CRITICAL) {
            onReady(response)
            lastNotificationTime = System.currentTimeMillis()
            return
        }

        queue.add(response)
        processQueue(onReady)
    }

    private fun processQueue(onReady: (InteractionResponse) -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastNotificationTime < MIN_INTERVAL_MS) return

        val next = queue.poll()
        if (next != null) {
            onReady(next)
            lastNotificationTime = now
        }
    }
}
