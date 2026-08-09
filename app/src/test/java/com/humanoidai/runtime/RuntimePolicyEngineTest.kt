package com.humanoidai.runtime

import android.os.PowerManager
import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimePolicyEngineTest {

    private val engine = RuntimePolicyEngine()

    @Test
    fun `test battery saver trigger`() {
        val lowBatteryMetrics = PerformanceMetrics(
            battery = BatteryMetrics(percentage = 10, isCharging = false)
        )
        engine.evaluate(lowBatteryMetrics)
        assertEquals(RuntimeProfile.BATTERY_SAVER, engine.currentProfile.value)
    }

    @Test
    fun `test performance trigger when charging`() {
        val chargingMetrics = PerformanceMetrics(
            battery = BatteryMetrics(percentage = 50, isCharging = true)
        )
        engine.evaluate(chargingMetrics)
        assertEquals(RuntimeProfile.PERFORMANCE, engine.currentProfile.value)
    }

    @Test
    fun `test thermal emergency override`() {
        // Even if charging, thermal emergency should force battery saver or balanced
        val emergencyThermalMetrics = PerformanceMetrics(
            battery = BatteryMetrics(percentage = 90, isCharging = true),
            thermal = ThermalMetrics(status = 5) // Map to PowerManager.THERMAL_STATUS_EMERGENCY if we had the actual constant
        )
        // PowerManager.THERMAL_STATUS_EMERGENCY is 5
        engine.evaluate(emergencyThermalMetrics)
        assertEquals(RuntimeProfile.BATTERY_SAVER, engine.currentProfile.value)
    }
}
