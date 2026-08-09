package com.humanoidai.hearing

import kotlin.math.log10

/**
 * Coarse sound source localization for built-in smartphone mics.
 */
class DirectionOfArrivalEstimator {

    fun estimate(channelLeft: FloatArray, channelRight: FloatArray): AudioDirectionState {
        if (channelLeft.isEmpty() || channelRight.isEmpty()) return AudioDirectionState()

        val leftEnergy = calculateEnergy(channelLeft)
        val rightEnergy = calculateEnergy(channelRight)
        
        val ratio = leftEnergy / (rightEnergy + 1e-6f)
        val amplitude = 20 * log10((leftEnergy + rightEnergy) / 2 + 1e-6)

        val sector = when {
            ratio > 1.5 -> CompassSector.W
            ratio < 0.6 -> CompassSector.E
            else -> CompassSector.N
        }

        return AudioDirectionState(
            sector = sector,
            confidence = (if (ratio > 1f) ratio - 1f else 1f / ratio - 1f).coerceIn(0f, 1f),
            amplitudeDb = amplitude.toFloat()
        )
    }

    private fun calculateEnergy(buffer: FloatArray): Float {
        var sum = 0f
        for (sample in buffer) {
            sum += sample * sample
        }
        return sum / buffer.size
    }
}
