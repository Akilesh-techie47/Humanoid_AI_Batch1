package com.humanoidai.distributed

import com.humanoidai.embodiment.Capability
import com.humanoidai.embodiment.EmbodimentType
import com.humanoidai.embodiment.HardwareStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CapabilityNegotiatorTest {

    private val registry = InstanceRegistry()
    private val negotiator = CapabilityNegotiator(registry)

    @Test
    fun testNegotiationPrefersLocal() {
        val localId = "phone_local"
        registry.registerInstance(AIInstance(id = localId, type = EmbodimentType.PHONE, capabilities = setOf(Capability.VISION), workloadPercent = 10))
        registry.registerInstance(AIInstance(id = "remote_robot", type = EmbodimentType.ROBOT, capabilities = setOf(Capability.VISION), workloadPercent = 5))
        
        val winner = negotiator.negotiate(Capability.VISION, localId)
        assertEquals(localId, winner)
    }

    @Test
    fun testNegotiationDelegatesToRemoteWhenLocalLacksCapability() {
        val localId = "phone_local"
        registry.registerInstance(AIInstance(id = localId, type = EmbodimentType.PHONE, capabilities = setOf(Capability.VISION)))
        registry.registerInstance(AIInstance(id = "remote_robot", type = EmbodimentType.ROBOT, capabilities = setOf(Capability.LOCOMOTION)))
        
        val winner = negotiator.negotiate(Capability.LOCOMOTION, localId)
        assertEquals("remote_robot", winner)
    }

    @Test
    fun testNegotiationSelectsBestRemoteByWorkload() {
        val localId = "phone_local"
        registry.registerInstance(AIInstance(id = "remote_1", type = EmbodimentType.DESKTOP, capabilities = setOf(Capability.VISION), workloadPercent = 70))
        registry.registerInstance(AIInstance(id = "remote_2", type = EmbodimentType.DESKTOP, capabilities = setOf(Capability.VISION), workloadPercent = 20))
        
        val winner = negotiator.negotiate(Capability.VISION, localId)
        assertEquals("remote_2", winner)
    }
}
