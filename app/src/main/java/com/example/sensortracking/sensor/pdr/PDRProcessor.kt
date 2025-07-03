package com.example.sensortracking.sensor.pdr

import com.example.sensortracking.data.HeadingData
import com.example.sensortracking.data.HeadingSource
import com.example.sensortracking.data.PDRConfig
import com.example.sensortracking.data.PDRData
import com.example.sensortracking.data.Position
import com.example.sensortracking.data.StepData
import kotlin.math.cos
import kotlin.math.sin

/**
 * Main PDR processor manages step detection, stride estimation, heading estimation, and position tracking
 */
class PDRProcessor(
    private val config: PDRConfig = PDRConfig(),
    private val areaBounds: AreaBounds
) {
    
    companion object {
        private const val TAG = "PDRProcessor"
    }
    
    private val stepDetector = StepDetector(config)
    private val strideEstimator = StrideEstimator(config)
    private val headingEstimator = HeadingEstimator(config)
    
    // Current state
    private var currentPosition = Position(0f, 0f)
    private var currentHeading = HeadingData(0f, 0f, HeadingSource.FUSED)
    private var stepCount = 0
    private var totalDistance = 0f
    private var isTracking = false
    private var lastStepData: StepData? = null
    
    // Path history
    private val pathHistory = mutableListOf<Position>()
    private val maxHistorySize = 1000
    
    // Sensor data buffers
    private var lastAccelerometer: FloatArray? = null
    private var lastGyroscope: FloatArray? = null
    private var lastMagnetometer: FloatArray? = null
    private var lastTimestamp: Long = 0
    
    /**
     * Area bounds for position validation
     */
    data class AreaBounds(
        val minX: Float,
        val maxX: Float,
        val minY: Float,
        val maxY: Float
    ) {
        fun contains(position: Position): Boolean {
            return position.x in minX..maxX && position.y in minY..maxY
        }
        
        fun clamp(position: Position): Position {
            return Position(
                x = position.x.coerceIn(minX, maxX),
                y = position.y.coerceIn(minY, maxY)
            )
        }
    }
    
    /**
     * Process sensor data and update PDR state
     */
    fun processSensorData(
        accelerometer: FloatArray,
        gyroscope: FloatArray,
        magnetometer: FloatArray,
        timestamp: Long
    ): PDRData? {
        
        // Always store sensor data for calibration
        lastAccelerometer = accelerometer.clone()
        lastGyroscope = gyroscope.clone()
        lastMagnetometer = magnetometer.clone()
        lastTimestamp = timestamp
        
        // Always update heading estimation for calibration
        currentHeading = headingEstimator.estimateHeading(
            accelerometer, gyroscope, magnetometer, timestamp
        )
        
        // Only process steps and update position if tracking
        if (!isTracking) {
            return getCurrentPDRData()
        }
        
        // Detect step
        val stepData = stepDetector.detectStep(accelerometer, timestamp)
        
        if (stepData != null) {
            // Estimate stride length
            val strideData = strideEstimator.estimateStride(accelerometer, timestamp)
            
            // Update position
            updatePosition(strideData.length)
            
            // Update state
            stepCount++
            totalDistance += strideData.length
            lastStepData = stepData
            
            // Add to path history
            addToPathHistory(currentPosition)
        }
        
        return getCurrentPDRData()
    }
    
    /**
     * Update position based on stride length and heading
     */
    private fun updatePosition(strideLength: Float) {
        val headingRadians = Math.toRadians(currentHeading.heading.toDouble())
        
        // Calculate position delta
        val deltaX = strideLength * sin(headingRadians).toFloat()
        val deltaY = -strideLength * cos(headingRadians).toFloat() // Negative for correct coordinate system
        
        // Update position
        val newPosition = Position(
            x = currentPosition.x + deltaX,
            y = currentPosition.y + deltaY
        )
        
        // Check bounds and clamp if necessary
        currentPosition = if (areaBounds.contains(newPosition)) {
            newPosition
        } else {
            areaBounds.clamp(newPosition)
        }
    }
    
    /**
     * Add position to path history
     */
    private fun addToPathHistory(position: Position) {
        pathHistory.add(position)
        if (pathHistory.size > maxHistorySize) {
            pathHistory.removeAt(0)
        }
    }
    
    /**
     * Get current PDR data
     */
    fun getCurrentPDRData(): PDRData {
        return PDRData(
            position = currentPosition,
            stepCount = stepCount,
            totalDistance = totalDistance,
            currentHeading = currentHeading,
            lastStep = lastStepData,
            isTracking = isTracking,
            confidence = calculateOverallConfidence()
        )
    }

    private fun calculateOverallConfidence(): Float {
        var confidence = 0f
        var count = 0
        
        // Step detection confidence
        lastStepData?.let {
            confidence += it.confidence
            count++
        }
        
        // Heading confidence
        confidence += currentHeading.confidence
        count++
        
        // Stride estimation confidence (if available)
        lastAccelerometer?.let {
            val strideData = strideEstimator.estimateStride(it, lastTimestamp)
            confidence += strideData.confidence
            count++
        }
        
        return if (count > 0) confidence / count else 0f
    }
    
    /**
     * Start tracking from a specific position
     */
    fun startTracking(initialPosition: Position) {
        // Validate initial position
        currentPosition = if (areaBounds.contains(initialPosition)) {
            initialPosition
        } else {
            areaBounds.clamp(initialPosition)
        }
        
        // Reset all components
        stepDetector.reset()
        strideEstimator.reset()
        headingEstimator.reset()
        
        // Reset state
        stepCount = 0
        totalDistance = 0f
        pathHistory.clear()
        pathHistory.add(currentPosition)
        
        isTracking = true
    }

    fun stopTracking() {
        isTracking = false
    }
    
    /**
     * Set initial position (without starting tracking)
     */
    fun setInitialPosition(position: Position) {
        currentPosition = if (areaBounds.contains(position)) {
            position
        } else {
            areaBounds.clamp(position)
        }
        pathHistory.clear()
        pathHistory.add(currentPosition)
    }

    fun getPathHistory(): List<Position> = pathHistory.toList()

    /**
     * Update rotation vector data in heading estimator
     */
    fun updateRotationVector(rotationVector: FloatArray) {
        headingEstimator.updateRotationVector(rotationVector)
    }

    fun updateConfig(newConfig: PDRConfig) {
        stepDetector.updateConfig(newConfig)
        strideEstimator.updateConfig(newConfig)
        headingEstimator.updateConfig(newConfig)
    }

    fun reset() {
        stepDetector.reset()
        strideEstimator.reset()
        headingEstimator.reset()
        
        currentPosition = Position(0f, 0f)
        currentHeading = HeadingData(0f, 0f, HeadingSource.FUSED)
        stepCount = 0
        totalDistance = 0f
        isTracking = false
        lastStepData = null
        pathHistory.clear()
        
        lastAccelerometer = null
        lastGyroscope = null
        lastMagnetometer = null
        lastTimestamp = 0
    }
    
    /**
     * Get current position
     */
    fun getCurrentPosition(): Position = currentPosition
    
    /**
     * Get current heading
     */
    fun getCurrentHeading(): HeadingData = currentHeading
    
    /**
     * Get step count
     */
    fun getStepCount(): Int = stepCount
    
    /**
     * Get total distance
     */
    fun getTotalDistance(): Float = totalDistance
    
    /**
     * Check if currently tracking
     */
    fun isCurrentlyTracking(): Boolean = isTracking
} 