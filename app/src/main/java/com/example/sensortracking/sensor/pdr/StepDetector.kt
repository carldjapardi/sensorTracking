package com.example.sensortracking.sensor.pdr

import com.example.sensortracking.data.StepData
import com.example.sensortracking.data.PDRConfig
import kotlin.math.sqrt

class StepDetector(private var config: PDRConfig) {
    private var lastStepTime: Long = 0
    private var lastMagnitude: Float = 0f
    private var stepCount = 0

    // Threshold-based detection
    private val stepThreshold = config.stepThreshold
    private val stepCooldownMs = config.stepCooldownMs

    fun detectStep(acceleration: FloatArray, timestamp: Long): StepData? {
        val magnitude = calculateMagnitude(acceleration)
        // Check cooldown
        if (timestamp - lastStepTime < stepCooldownMs) {
            return null
        }
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

    private fun calculateMagnitude(acceleration: FloatArray): Float {
        return sqrt(acceleration[0] * acceleration[0] + 
                   acceleration[1] * acceleration[1] + 
                   acceleration[2] * acceleration[2])
    }

    fun reset() {
        lastStepTime = 0
        lastMagnitude = 0f
        stepCount = 0
    }

    fun updateConfig(newConfig: PDRConfig) {
        // Config is already used in detectStep method
    }
} 