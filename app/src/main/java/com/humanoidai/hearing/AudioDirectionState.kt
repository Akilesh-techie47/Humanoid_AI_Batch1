package com.humanoidai.hearing

enum class CompassSector { N, NE, E, SE, S, SW, W, NW, UNKNOWN }

data class AudioDirectionState(
    val sector: CompassSector = CompassSector.UNKNOWN,
    val confidence: Float = 0f,
    val amplitudeDb: Float = 0f
)
