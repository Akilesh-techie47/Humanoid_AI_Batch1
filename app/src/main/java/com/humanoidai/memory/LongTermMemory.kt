package com.humanoidai.memory

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists owner habits, preferences, and recurring events.
 */
class LongTermMemory(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("humanoid_lt_memory", Context.MODE_PRIVATE)

    fun storePreference(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getPreference(key: String, defaultValue: String = ""): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    fun recordEvent(eventName: String) {
        val count = prefs.getInt(eventName, 0)
        prefs.edit().putInt(eventName, count + 1).apply()
    }

    fun getEventFrequency(eventName: String): Int {
        return prefs.getInt(eventName, 0)
    }
}
