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
    val confidence: Float,
    val method: StrideEstimationMethod
)

/**
 * Methods for stride length estimation
 */
enum class StrideEstimationMethod {
    WEINBERG,      // Based on Weinberg's algorithm
    KIM,           // Based on Kim's algorithm
    FIXED_LENGTH,  // Fixed step length
    ADAPTIVE       // Adaptive based on acceleration variance
}

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
    MAGNETOMETER,  // Pure magnetometer
    GYROSCOPE,     // Pure gyroscope
    FUSED          // Sensor fusion
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
 * Sensor data for PDR processing
 */
data class SensorData(
    val accelerometer: FloatArray,
    val gyroscope: FloatArray,
    val magnetometer: FloatArray,
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorData

        if (!accelerometer.contentEquals(other.accelerometer)) return false
        if (!gyroscope.contentEquals(other.gyroscope)) return false
        if (!magnetometer.contentEquals(other.magnetometer)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = accelerometer.contentHashCode()
        result = 31 * result + gyroscope.contentHashCode()
        result = 31 * result + magnetometer.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Configuration for PDR algorithms
 */
data class PDRConfig(
    val stepThreshold: Float = 1.8f,
    val stepCooldownMs: Long = 500,
    val defaultStrideLength: Float = 0.7f,
    val strideEstimationMethod: StrideEstimationMethod = StrideEstimationMethod.WEINBERG,
    val headingSmoothingFactor: Float = 0.8f,
    val autoCorrelationWindowSize: Int = 50,
    val lowPassFilterAlpha: Float = 0.8f
) 