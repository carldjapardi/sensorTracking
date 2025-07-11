package com.example.sensortracking.data

/**
 * Represents a 2D position in the tracking area
 */
data class Position(
    val x: Float,
    val y: Float
)

data class StepData(
    val timestamp: Long,
    val magnitude: Float,
    val confidence: Float
)

data class StrideData(
    val length: Float,
    val confidence: Float
)

/**
 * Represents heading/orientation data
 */
data class HeadingData(
    val heading: Float, // in degrees, 0-360
    val confidence: Float,
)

data class PDRData(
    val position: Position,
    val stepCount: Int,
    val totalDistance: Float,
    val currentHeading: HeadingData,
    val lastStep: StepData?,
    val isTracking: Boolean = false,
    val confidence: Float = 0f,
    val config: PDRConfig = PDRConfig()
)

data class PDRConfig(
    val stepThreshold: Float = 12.0f,
    val stepCooldownMs: Long = 450L,
    val defaultStrideLength: Float = 0.7f,
    val headingTolerance: Float = 30f
) 