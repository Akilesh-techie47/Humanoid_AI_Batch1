package com.humanoidai.planning

import com.humanoidai.ui.layoutcustomization.domain.skill.SkillManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class PlannerTest {

    private val skillManager = mock(SkillManager::class.java)
    private val planner = Planner(skillManager)

    @Test
    fun testVisualAnalysisPlanDecomposition() {
        val goal = Goal(title = "Visual Analysis", type = GoalType.KNOWLEDGE_ACQUISITION, priority = 50)
        planner.createPlan(goal)
        
        assertEquals(GoalStatus.READY, goal.status)
        assertEquals(3, goal.steps.size)
        assertEquals("Capture ROI", goal.steps[0].name)
        assertEquals("Run Vision Skill", goal.steps[1].name)
        assertEquals("Surface Insights", goal.steps[2].name)
    }

    @Test
    fun testOptimizationPlanDecomposition() {
        val goal = Goal(title = "System Optimization", type = GoalType.SYSTEM_OPTIMIZATION, priority = 30)
        planner.createPlan(goal)
        
        assertEquals(GoalStatus.READY, goal.status)
        assertEquals(2, goal.steps.size)
        assertEquals("Clear Cache", goal.steps[0].name)
    }

    @Test
    fun testDefaultPlanDecomposition() {
        val goal = Goal(title = "Unknown Goal", type = GoalType.USER_REQUEST, priority = 10)
        planner.createPlan(goal)
        
        assertEquals(GoalStatus.READY, goal.status)
        assertEquals(1, goal.steps.size)
    }
}
