package com.humanoidai.behavior

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.PriorityQueue

/**
 * Prioritizes and throttles outgoing notifications.
 * Automatically processes the queue based on MIN_INTERVAL_MS.
 */
class NotificationCoordinator {

    companion object {
        private const val TAG = "NotificationCoord"
        private const val MIN_INTERVAL_MS = 500L // Reduced from 2000L for faster interaction
    }

    private val queue = PriorityQueue<InteractionResponse> { r1, r2 ->
        r2.priority.ordinal.compareTo(r1.priority.ordinal)
    }

    private var lastNotificationTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var isProcessing = false

    fun post(response: InteractionResponse, onReady: (InteractionResponse) -> Unit) {
        if (response.priority == InteractionPriority.CRITICAL) {
            onReady(response)
            lastNotificationTime = System.currentTimeMillis()
            return
        }

        queue.add(response)
        if (!isProcessing) {
            scheduleNext(onReady)
        }
    }

    private fun scheduleNext(onReady: (InteractionResponse) -> Unit) {
        val now = System.currentTimeMillis()
        val elapsed = now - lastNotificationTime
        val delay = (MIN_INTERVAL_MS - elapsed).coerceAtLeast(0)

        isProcessing = true
        handler.postDelayed({
            processQueue(onReady)
        }, delay)
    }

    private fun processQueue(onReady: (InteractionResponse) -> Unit) {
        val next = queue.poll()
        if (next != null) {
            onReady(next)
            lastNotificationTime = System.currentTimeMillis()
            
            if (queue.isNotEmpty()) {
                scheduleNext(onReady)
            } else {
                isProcessing = false
            }
        } else {
            isProcessing = false
        }
    }
}
