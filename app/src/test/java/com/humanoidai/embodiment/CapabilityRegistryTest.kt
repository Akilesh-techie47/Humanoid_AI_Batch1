package com.humanoidai.embodiment

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityRegistryTest {

    private val registry = CapabilityRegistry()

    @Test
    fun testCapabilityUpdateAndQuery() {
        assertFalse(registry.hasCapability(Capability.VISION))
        
        registry.updateCapabilities(setOf(Capability.VISION, Capability.AUDIO_INPUT))
        
        assertTrue(registry.hasCapability(Capability.VISION))
        assertTrue(registry.hasCapability(Capability.AUDIO_INPUT))
        assertFalse(registry.hasCapability(Capability.LOCOMOTION))
    }

    @Test
    fun testMissingCapabilities() {
        registry.updateCapabilities(setOf(Capability.VISION))
        
        val required = setOf(Capability.VISION, Capability.LOCOMOTION)
        val missing = registry.getMissingCapabilities(required)
        
        assertEquals(1, missing.size)
        assertTrue(missing.contains(Capability.LOCOMOTION))
    }

    private fun assertEquals(expected: Any, actual: Any) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
