package com.example.sensortracking.sensor.pdr

import com.example.sensortracking.data.StrideData
import com.example.sensortracking.data.PDRConfig
import kotlin.math.pow
import kotlin.math.sqrt

class StrideEstimator(private var config: PDRConfig) {
    
    private var accelerationBuffer = mutableListOf<Float>()
    private var strideHistory = mutableListOf<Float>()
    private var lastStrideTime: Long = 0
    private val maxHistorySize = 10

    fun estimateStride(acceleration: FloatArray, timestamp: Long): StrideData {
        val magnitude = calculateMagnitude(acceleration)
        
        // Add to buffer
        accelerationBuffer.add(magnitude)
        if (accelerationBuffer.size > 50) {
            accelerationBuffer.removeAt(0)
        }
        
        val strideLength = estimateWeinbergStride()
        val confidence = calculateConfidence(accelerationBuffer)

        if (timestamp - lastStrideTime > 1000) { // update every second
            strideHistory.add(strideLength)
            if (strideHistory.size > maxHistorySize) {
                strideHistory.removeAt(0)
            }
            lastStrideTime = timestamp
        }
        
        return StrideData(
            length = strideLength,
            confidence = confidence
        )
    }
    
    /**
     * Weinberg's stride length estimation
     * Based on: Weinberg, H. "Using the ADXL202 in pedometer and personal navigation applications"
     */
    private fun estimateWeinbergStride(): Float {
        if (accelerationBuffer.size < 10) {
            return config.defaultStrideLength
        }
        
        val maxAccel = accelerationBuffer.maxOrNull() ?: 0f
        val minAccel = accelerationBuffer.minOrNull() ?: 0f
        val amplitude = maxAccel - minAccel
        
        // stride = K * (amplitude)^0.25
        // K is a constant that depends on user characteristics
        val k = 0.4f
        return k * amplitude.pow(0.25f)
    }

    private fun calculateMagnitude(acceleration: FloatArray): Float {
        return sqrt(acceleration[0].pow(2) + acceleration[1].pow(2) + acceleration[2].pow(2))
    }

    private fun calculateVariance(buffer: List<Float>): Float {
        if (buffer.isEmpty()) return 0f
        
        val mean = buffer.average().toFloat()
        val variance = buffer.map { (it - mean).pow(2) }.average().toFloat()
        return variance
    }

    private fun calculateAmplitude(buffer: List<Float>): Float {
        if (buffer.isEmpty()) return 0f
        
        val max = buffer.maxOrNull() ?: 0f
        val min = buffer.minOrNull() ?: 0f
        return max - min
    }

    private fun calculateConfidence(buffer: List<Float>): Float {
        if (buffer.size < 10) return 0.5f
        
        val variance = calculateVariance(buffer)
        val amplitude = calculateAmplitude(buffer)
        
        // Higher variance and amplitude = better signal quality
        var confidence = 0.5f
        
        if (variance > 0.5f) confidence += 0.2f
        if (variance > 1.0f) confidence += 0.2f
        
        if (amplitude > 2.0f) confidence += 0.1f
        if (amplitude > 4.0f) confidence += 0.1f
        
        return confidence.coerceIn(0f, 1f)
    }

    fun reset() {
        accelerationBuffer.clear()
        strideHistory.clear()
        lastStrideTime = 0
    }

    fun updateConfig(newConfig: PDRConfig) {
        config = newConfig
    }
} 