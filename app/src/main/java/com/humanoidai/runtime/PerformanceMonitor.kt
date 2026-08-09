package com.humanoidai.runtime

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.RandomAccessFile

class PerformanceMonitor(private val context: Context) {

    private val _metrics = MutableStateFlow(PerformanceMetrics())
    val metrics: StateFlow<PerformanceMetrics> = _metrics.asStateFlow()

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    fun updateMetrics() {
        val currentMetrics = _metrics.value
        _metrics.value = currentMetrics.copy(
            cpu = getCpuMetrics(),
            memory = getMemoryMetrics(),
            battery = getBatteryMetrics(),
            thermal = getThermalMetrics(),
            timestamp = System.currentTimeMillis()
        )
    }

    private fun getCpuMetrics(): CpuMetrics {
        // Simple CPU usage estimation. Real-time CPU per-thread is complex on Android 8+
        // but we can at least count active threads in our process.
        val threadCount = Thread.activeCount()
        return CpuMetrics(
            utilizationPercent = estimateCpuUsage(),
            activeThreads = threadCount
        )
    }

    private fun estimateCpuUsage(): Int {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            reader.close()
            val toks = load.split(" +".toRegex())
            val idle1 = toks[4].toLong()
            val cpu1 = toks[1].toLong() + toks[2].toLong() + toks[3].toLong() + toks[6].toLong() + toks[7].toLong() + toks[8].toLong()
            
            Thread.sleep(100)
            
            val reader2 = RandomAccessFile("/proc/stat", "r")
            val load2 = reader2.readLine()
            reader2.close()
            val toks2 = load2.split(" +".toRegex())
            val idle2 = toks2[4].toLong()
            val cpu2 = toks2[1].toLong() + toks2[2].toLong() + toks2[3].toLong() + toks2[6].toLong() + toks2[7].toLong() + toks2[8].toLong()
            
            ((cpu2 - cpu1).toFloat() / ((cpu2 + idle2) - (cpu1 + idle1)) * 100).toInt()
        } catch (e: Exception) {
            0
        }
    }

    private fun getMemoryMetrics(): MemoryMetrics {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val runtime = Runtime.getRuntime()
        val heapUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val heapMax = runtime.maxMemory() / (1024 * 1024)
        
        return MemoryMetrics(
            heapUsedMb = heapUsed,
            heapMaxMb = heapMax,
            nativeUsedMb = android.os.Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
        )
    }

    private fun getBatteryMetrics(): BatteryMetrics {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, ifilter)
        
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL
        
        val batteryPct = (level / scale.toFloat() * 100).toInt()
        
        val currentNow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        } else 0

        return BatteryMetrics(
            percentage = batteryPct,
            isCharging = isCharging,
            currentNowMa = currentNow / 1000 // Convert microamps to milliamps
        )
    }

    private fun getThermalMetrics(): ThermalMetrics {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val status = powerManager.currentThermalStatus
            ThermalMetrics(
                status = status,
                isThrottling = status >= PowerManager.THERMAL_STATUS_MODERATE
            )
        } else {
            ThermalMetrics(status = 0, isThrottling = false)
        }
    }
}
