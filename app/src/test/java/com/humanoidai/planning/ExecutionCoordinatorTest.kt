package com.humanoidai.planning

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class ExecutionCoordinatorTest {

    @Test
    fun testGoalExecutionFlow() = runTest {
        val coordinator = ExecutionCoordinator(this)
        val goal = Goal(title = "Test Goal", type = GoalType.USER_REQUEST, priority = 50)
        
        val results = mutableListOf<String>()
        val latch = CountDownLatch(2)

        goal.steps.add(PlanStep(name = "Step 1", description = "") {
            results.add("Step 1 Done")
            latch.countDown()
            Result.success(Unit)
        })

        goal.steps.add(PlanStep(name = "Step 2", description = "") {
            results.add("Step 2 Done")
            latch.countDown()
            Result.success(Unit)
        })

        coordinator.execute(goal) { }
        
        latch.await(2, TimeUnit.SECONDS)
        
        assertEquals(GoalStatus.COMPLETED, goal.status)
        assertEquals(2, results.size)
        assertEquals(1.0f, goal.progress)
    }

    @Test
    fun testGoalFailureBailout() = runTest {
        val coordinator = ExecutionCoordinator(this)
        val goal = Goal(title = "Failure Goal", type = GoalType.USER_REQUEST, priority = 50)
        
        goal.steps.add(PlanStep(name = "Step 1", description = "") {
            Result.failure(Exception("Oops"))
        })

        goal.steps.add(PlanStep(name = "Step 2", description = "") {
            Result.success(Unit) // Should not run
        })

        coordinator.execute(goal) { }
        
        // In real test we'd wait for completion properly
        kotlinx.coroutines.delay(100)
        
        assertEquals(GoalStatus.FAILED, goal.status)
        assertEquals(StepStatus.FAILED, goal.steps[0].status)
        assertEquals(StepStatus.PENDING, goal.steps[1].status)
    }
}
