package com.humanoidai.runtime

import android.app.ActivityManager
import android.content.Context
import android.os.Build

data class DeviceCapabilityProfile(
    val cpuCores: Int,
    val totalRamMb: Long,
    val isLowRamDevice: Boolean,
    val apiLevel: Int,
    val manufacturer: String,
    val model: String
)

class DeviceCapabilityDetector(private val context: Context) {

    fun detect(): DeviceCapabilityProfile {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        return DeviceCapabilityProfile(
            cpuCores = Runtime.getRuntime().availableProcessors(),
            totalRamMb = memoryInfo.totalMem / (1024 * 1024),
            isLowRamDevice = activityManager.isLowRamDevice,
            apiLevel = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL
        )
    }
}
