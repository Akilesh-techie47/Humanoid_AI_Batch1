package com.humanoidai.runtime

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskSchedulerTest {

    @Test
    fun testTaskPriorityOrdering() = runTest {
        val scheduler = TaskScheduler(this)
        val results = mutableListOf<String>()

        val lowTask = AITask(
            name = "Low",
            priority = TaskPriority.LOW,
            category = TaskCategory.SYSTEM,
            execution = {
                results.add("Low")
            }
        )

        val criticalTask = AITask(
            name = "Critical",
            priority = TaskPriority.CRITICAL,
            category = TaskCategory.VISION,
            execution = {
                results.add("Critical")
            }
        )

        val highTask = AITask(
            name = "High",
            priority = TaskPriority.HIGH,
            category = TaskCategory.AUDIO,
            execution = {
                results.add("High")
            }
        )

        // Submit in reverse order
        scheduler.submit(lowTask)
        scheduler.submit(highTask)
        scheduler.submit(criticalTask)

        scheduler.start()
        
        // Advance virtual time to allow processing
        advanceUntilIdle()
        
        assertEquals("Critical", results[0])
        assertEquals("High", results[1])
        assertEquals("Low", results[2])
        
        scheduler.stop()
    }
}
