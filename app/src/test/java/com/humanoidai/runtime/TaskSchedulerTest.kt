package com.humanoidai.runtime

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class TaskSchedulerTest {

    @Test
    fun testTaskPriorityOrdering() = runTest {
        val testScope = this
        val scheduler = TaskScheduler(testScope)
        val results = mutableListOf<String>()
        val latch = CountDownLatch(3)

        val lowTask = AITask(
            name = "Low",
            priority = TaskPriority.LOW,
            category = TaskCategory.SYSTEM,
            execution = {
                results.add("Low")
                latch.countDown()
            }
        )

        val criticalTask = AITask(
            name = "Critical",
            priority = TaskPriority.CRITICAL,
            category = TaskCategory.VISION,
            execution = {
                results.add("Critical")
                latch.countDown()
            }
        )

        val highTask = AITask(
            name = "High",
            priority = TaskPriority.HIGH,
            category = TaskCategory.AUDIO,
            execution = {
                results.add("High")
                latch.countDown()
            }
        )

        // Submit in reverse order
        scheduler.submit(lowTask)
        scheduler.submit(highTask)
        scheduler.submit(criticalTask)

        scheduler.start()
        
        // Give some time for processing
        latch.await(2, TimeUnit.SECONDS)
        
        assertEquals("Critical", results[0])
        assertEquals("High", results[1])
        assertEquals("Low", results[2])
        
        scheduler.stop()
    }
}
