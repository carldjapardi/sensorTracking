package com.example.sensortracking.sensor.pdr

import com.example.sensortracking.data.HeadingData
import com.example.sensortracking.data.PDRConfig
import com.example.sensortracking.data.PDRData
import com.example.sensortracking.data.Position
import com.example.sensortracking.data.StepData
import com.example.sensortracking.sensor.calibration.CalibrationManager
import com.example.sensortracking.sensor.calibration.CalibrationType
import kotlin.math.cos
import kotlin.math.sin

/**
 * Main PDR processor manages step detection, stride estimation, heading estimation, and position tracking
 */
class PDRProcessor(private val config: PDRConfig = PDRConfig(), private val areaBounds: AreaBounds) {
    // initializing components for PDR
    private val stepDetector = StepDetector(config)
    private val strideEstimator = StrideEstimator(config)
    private val headingEstimator = HeadingEstimator(config)
    
    // Current state
    private var currentPosition = Position(0f, 0f)
    private var currentHeading = HeadingData(0f, 0f)
    private var stepCount = 0
    private var totalDistance = 0f
    private var isTracking = false
    private var lastStepData: StepData? = null
    
    private val pathHistory = ArrayList<Position>()
    private val calibrationManager = CalibrationManager()
    
    private var lastStrideConfidence = 0f
    private var lastStrideTimestamp = 0L
    
    // Area bounds for position validation
    data class AreaBounds(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float) {
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
    
    fun processSensorData(accelerometer: FloatArray, timestamp: Long): PDRData {
        currentHeading = headingEstimator.estimateHeading()
        
        if (!isTracking) {
            return getCurrentPDRData()
        }
        
        val stepData = stepDetector.detectStep(accelerometer, timestamp)
        
        if (stepData != null) {
            val strideData = strideEstimator.estimateStride(accelerometer, timestamp)
            
            // Cache stride confidence for later use
            lastStrideConfidence = strideData.confidence
            lastStrideTimestamp = timestamp
            
            updatePosition(strideData.length)
            
            stepCount++
            totalDistance += strideData.length
            lastStepData = stepData
            
            addToPathHistory(currentPosition)
        }

        return getCurrentPDRData()
    }
    
    // Update position based on stride length and heading
    private fun updatePosition(strideLength: Float) {
        val headingRadians = Math.toRadians(currentHeading.heading.toDouble())
        
        // Calculate position delta
        val deltaX = strideLength * sin(headingRadians).toFloat()
        val deltaY = -strideLength * cos(headingRadians).toFloat() // Negative for correct coordinate system
        
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
    
    private fun addToPathHistory(position: Position) {
        pathHistory.add(position)
    }

    fun getCurrentPDRData(): PDRData {
        return PDRData(
            position = currentPosition, stepCount = stepCount, totalDistance = totalDistance,
            currentHeading = currentHeading, lastStep = lastStepData, isTracking = isTracking,
            confidence = calculateOverallConfidence(), config = config
        )
    }

    private fun calculateOverallConfidence(): Float {
        var confidence = 0f
        var count = 0
        
        lastStepData?.let {
            confidence += it.confidence
            count += 1
        }
        
        confidence += currentHeading.confidence
        count += 1
        
        if (lastStrideTimestamp > 0) {
            confidence += lastStrideConfidence
            count += 1
        }

        return confidence / count
    }

    fun startTracking(initialPosition: Position) {
        currentPosition = if (areaBounds.contains(initialPosition)) {
            initialPosition
        } else {
            areaBounds.clamp(initialPosition)
        }
        
        stepDetector.reset()
        strideEstimator.reset()
        headingEstimator.reset()
        
        // Reset state
        stepCount = 0
        totalDistance = 0f
        pathHistory.clear()
        pathHistory.add(currentPosition)
        
        // Reset cached values
        lastStrideConfidence = 0f
        lastStrideTimestamp = 0L
        
        isTracking = true
    }

    fun resumeTracking() { isTracking = true } // Resume tracking without clearing path history

    fun stopTracking() { isTracking = false }

    fun setInitialPosition(position: Position) {
        currentPosition = if (areaBounds.contains(position)) {
            position
        } else {
            areaBounds.clamp(position)
        }
        pathHistory.clear()
        pathHistory.add(currentPosition)
    }

    fun calibratePosition(position: Position, calibrationType: CalibrationType) {
        val previousPosition = currentPosition
        currentPosition = if (areaBounds.contains(position)) {
            position
        } else {
            areaBounds.clamp(position)
        }
        calibrationManager.calibrate(previousPosition, currentPosition, pathHistory, calibrationType)
    }

    fun getPathHistory(): List<Position> = pathHistory.toList()
    
    fun updatePathHistory(newPath: List<Position>) {
        pathHistory.clear()
        pathHistory.addAll(newPath)
        if (newPath.isNotEmpty()) {
            currentPosition = newPath.last()
        }
    }

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
        currentHeading = HeadingData(0f, 0f)
        stepCount = 0
        totalDistance = 0f
        isTracking = false
        lastStepData = null
        pathHistory.clear()
        
        lastStrideConfidence = 0f
        lastStrideTimestamp = 0L
    }
} 