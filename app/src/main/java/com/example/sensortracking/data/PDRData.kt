package com.example.sensortracking.data

import kotlin.math.sqrt

/**
 * Represents a 2D position in the tracking area
 */
data class Position(
    val x: Float,
    val y: Float
) {
    fun distanceTo(other: Position): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }
}

/**
 * Represents step detection data
 */
data class StepData(
    val timestamp: Long,
    val magnitude: Float,
    val confidence: Float
)

/**
 * Represents stride length estimation
 */
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
    val source: HeadingSource
)

/**
 * Sources for heading estimation
 */
enum class HeadingSource {
    FUSED  // Sensor fusion (rotation vector)
}

/**
 * Complete PDR tracking data
 */
data class PDRData(
    val position: Position,
    val stepCount: Int,
    val totalDistance: Float,
    val currentHeading: HeadingData,
    val lastStep: StepData?,
    val isTracking: Boolean = false,
    val confidence: Float = 0f
)

/**
 * Configuration for PDR algorithms
 */
data class PDRConfig(
    val stepThreshold: Float = 1.8f,
    val stepCooldownMs: Long = 500,
    val defaultStrideLength: Float = 0.7f,
    val autoCorrelationWindowSize: Int = 50,
    val lowPassFilterAlpha: Float = 0.8f
) 