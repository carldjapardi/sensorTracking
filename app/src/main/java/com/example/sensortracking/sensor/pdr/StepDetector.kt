package com.example.sensortracking.sensor.pdr

import android.util.Log
import com.example.sensortracking.data.StepData
import com.example.sensortracking.data.PDRConfig
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Simplified step detection using threshold-based approach
 */
class StepDetector(private val config: PDRConfig) {
    
    companion object {
        private const val TAG = "StepDetector"
    }
    
    private var lastStepTime: Long = 0
    private var lastMagnitude: Float = 0f
    private var stepCount = 0
    
    // Threshold-based detection
    private val stepThreshold = 12.0f // m/s²
    private val stepCooldownMs = 300L // Minimum time between steps
    
    /**
     * Process acceleration data and detect steps
     */
    fun detectStep(acceleration: FloatArray, timestamp: Long): StepData? {
        val magnitude = calculateMagnitude(acceleration)
        
        // Check cooldown
        if (timestamp - lastStepTime < stepCooldownMs) {
            return null
        }
        
        // Simple threshold-based step detection
        if (magnitude > stepThreshold && lastMagnitude <= stepThreshold) {
            stepCount++
            lastStepTime = timestamp
            
            return StepData(
                timestamp = timestamp,
                magnitude = magnitude,
                confidence = 0.8f // High confidence for threshold-based detection
            )
        }
        
        lastMagnitude = magnitude
        return null
    }
    
    /**
     * Calculate acceleration magnitude
     */
    private fun calculateMagnitude(acceleration: FloatArray): Float {
        return sqrt(acceleration[0] * acceleration[0] + 
                   acceleration[1] * acceleration[1] + 
                   acceleration[2] * acceleration[2])
    }
    
    /**
     * Reset detector state
     */
    fun reset() {
        lastStepTime = 0
        lastMagnitude = 0f
        stepCount = 0
    }
    
    /**
     * Update configuration
     */
    fun updateConfig(newConfig: PDRConfig) {
        // Could update thresholds based on config
    }
} 