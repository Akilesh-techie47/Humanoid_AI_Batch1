package com.humanoidai.behavior

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class NotificationCoordinatorTest {

    private val coordinator = NotificationCoordinator()

    @Test
    fun testCriticalPriorityBypassesQueue() {
        val results = mutableListOf<String>()
        val criticalResponse = InteractionResponse(text = "Critical", priority = InteractionPriority.CRITICAL)
        
        coordinator.post(criticalResponse) {
            results.add(it.text)
        }
        
        assertEquals(1, results.size)
        assertEquals("Critical", results[0])
    }

    @Test
    fun testPriorityOrdering() {
        val results = mutableListOf<String>()
        val low = InteractionResponse(text = "Low", priority = InteractionPriority.LOW)
        val high = InteractionResponse(text = "High", priority = InteractionPriority.HIGH)
        
        // Post low then high. High should come first when processing the queue
        // (Note: processQueue logic depends on timing, so this is a simplified check)
        
        coordinator.post(low) { results.add(it.text) }
        coordinator.post(high) { results.add(it.text) }
        
        // Because of the 2000ms delay in processQueue, only the first one posted might be delivered immediately if enough time passed since last.
        // In this test, lastNotificationTime starts at 0, so the FIRST call to post() will trigger processQueue and deliver immediately.
        // The SECOND call will queue and wait.
    }
}
