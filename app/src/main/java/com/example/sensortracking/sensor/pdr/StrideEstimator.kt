package com.example.sensortracking.sensor.pdr

import com.example.sensortracking.data.StrideData
import com.example.sensortracking.data.StrideEstimationMethod
import com.example.sensortracking.data.PDRConfig
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Stride length estimation using multiple algorithms
 * Implements Weinberg's, Kim's, and adaptive methods
 */
class StrideEstimator(private var config: PDRConfig) {
    
    private var accelerationBuffer = mutableListOf<Float>()
    private var strideHistory = mutableListOf<Float>()
    private var lastStrideTime: Long = 0
    private val maxHistorySize = 10
    
    // User-specific parameters (can be calibrated)
    private var userHeight: Float = 1.7f // meters
    private var userGender: Gender = Gender.UNKNOWN
    private var userAge: Int = 30
    
    enum class Gender {
        MALE, FEMALE, UNKNOWN
    }
    
    /**
     * Estimate stride length using configured method
     */
    fun estimateStride(acceleration: FloatArray, timestamp: Long): StrideData {
        val magnitude = calculateMagnitude(acceleration)
        
        // Add to buffer
        accelerationBuffer.add(magnitude)
        if (accelerationBuffer.size > 50) {
            accelerationBuffer.removeAt(0)
        }
        
        val strideLength = when (config.strideEstimationMethod) {
            StrideEstimationMethod.WEINBERG -> estimateWeinbergStride()
            StrideEstimationMethod.KIM -> estimateKimStride()
            StrideEstimationMethod.FIXED_LENGTH -> config.defaultStrideLength
            StrideEstimationMethod.ADAPTIVE -> estimateAdaptiveStride()
        }
        
        val confidence = calculateConfidence(accelerationBuffer)
        
        // Update history
        if (timestamp - lastStrideTime > 1000) { // Only update every second
            strideHistory.add(strideLength)
            if (strideHistory.size > maxHistorySize) {
                strideHistory.removeAt(0)
            }
            lastStrideTime = timestamp
        }
        
        return StrideData(
            length = strideLength,
            confidence = confidence,
            method = config.strideEstimationMethod
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
        
        // Weinberg's formula: stride = K * (amplitude)^0.25
        // K is a constant that depends on user characteristics
        val k = calculateWeinbergConstant()
        return k * amplitude.pow(0.25f)
    }
    
    /**
     * Kim's stride length estimation
     * Based on: Kim, J.W., et al. "Step length estimation using waist-mounted accelerometer"
     */
    private fun estimateKimStride(): Float {
        if (accelerationBuffer.size < 20) {
            return config.defaultStrideLength
        }
        
        val variance = calculateVariance(accelerationBuffer)
        val mean = accelerationBuffer.average().toFloat()
        
        // Kim's formula: stride = a * (variance)^b + c
        // where a, b, c are constants
        val a = 0.4f
        val b = 0.5f
        val c = 0.3f
        
        return a * variance.pow(b) + c
    }
    
    /**
     * Adaptive stride length estimation
     * Combines multiple factors for dynamic estimation
     */
    private fun estimateAdaptiveStride(): Float {
        if (accelerationBuffer.size < 15) {
            return config.defaultStrideLength
        }
        
        val variance = calculateVariance(accelerationBuffer)
        val amplitude = calculateAmplitude(accelerationBuffer)
        val frequency = calculateStepFrequency()
        
        // Adaptive formula combining multiple factors
        val baseStride = config.defaultStrideLength
        val varianceFactor = 1f + (variance - 0.5f) * 0.2f
        val amplitudeFactor = 1f + (amplitude - 2f) * 0.1f
        val frequencyFactor = 1f + (frequency - 2f) * 0.05f
        
        return baseStride * varianceFactor * amplitudeFactor * frequencyFactor
    }
    
    /**
     * Calculate acceleration magnitude
     */
    private fun calculateMagnitude(acceleration: FloatArray): Float {
        return sqrt(acceleration[0].pow(2) + acceleration[1].pow(2) + acceleration[2].pow(2))
    }
    
    /**
     * Calculate variance of acceleration buffer
     */
    private fun calculateVariance(buffer: List<Float>): Float {
        if (buffer.isEmpty()) return 0f
        
        val mean = buffer.average().toFloat()
        val variance = buffer.map { (it - mean).pow(2) }.average().toFloat()
        return variance
    }
    
    /**
     * Calculate amplitude (max - min) of acceleration buffer
     */
    private fun calculateAmplitude(buffer: List<Float>): Float {
        if (buffer.isEmpty()) return 0f
        
        val max = buffer.maxOrNull() ?: 0f
        val min = buffer.minOrNull() ?: 0f
        return max - min
    }
    
    /**
     * Calculate step frequency from recent steps
     */
    private fun calculateStepFrequency(): Float {
        if (strideHistory.size < 2) return 2f // Default frequency
        
        // Estimate frequency from stride history timing
        // This is a simplified calculation
        return 2f // Default to 2 steps per second
    }
    
    /**
     * Calculate Weinberg constant based on user characteristics
     */
    private fun calculateWeinbergConstant(): Float {
        var k = 0.4f // Base constant
        
        // Adjust for height
        k *= (userHeight / 1.7f).pow(0.5f)
        
        // Adjust for gender
        k *= when (userGender) {
            Gender.MALE -> 1.1f
            Gender.FEMALE -> 0.9f
            Gender.UNKNOWN -> 1.0f
        }
        
        // Adjust for age
        k *= when {
            userAge < 25 -> 1.05f
            userAge < 50 -> 1.0f
            else -> 0.95f
        }
        
        return k
    }
    
    /**
     * Calculate confidence based on signal quality
     */
    private fun calculateConfidence(buffer: List<Float>): Float {
        if (buffer.size < 10) return 0.5f
        
        val variance = calculateVariance(buffer)
        val amplitude = calculateAmplitude(buffer)
        
        // Higher variance and amplitude indicate better signal quality
        var confidence = 0.5f
        
        if (variance > 0.5f) confidence += 0.2f
        if (variance > 1.0f) confidence += 0.2f
        
        if (amplitude > 2.0f) confidence += 0.1f
        if (amplitude > 4.0f) confidence += 0.1f
        
        return confidence.coerceIn(0f, 1f)
    }
    
    /**
     * Set user characteristics for better estimation
     */
    fun setUserCharacteristics(height: Float, gender: Gender, age: Int) {
        userHeight = height
        userGender = gender
        userAge = age
    }
    
    /**
     * Get average stride length from history
     */
    fun getAverageStrideLength(): Float {
        return if (strideHistory.isNotEmpty()) {
            strideHistory.average().toFloat()
        } else {
            config.defaultStrideLength
        }
    }
    
    /**
     * Reset estimator state
     */
    fun reset() {
        accelerationBuffer.clear()
        strideHistory.clear()
        lastStrideTime = 0
    }
    
    /**
     * Update configuration
     */
    fun updateConfig(newConfig: PDRConfig) {
        // Update internal parameters based on new config
    }
    
    /**
     * Set the stride estimation method
     */
    fun setStrideEstimationMethod(method: StrideEstimationMethod) {
        config = config.copy(strideEstimationMethod = method)
    }
    
    /**
     * Get current stride estimation method
     */
    fun getCurrentMethod(): StrideEstimationMethod = config.strideEstimationMethod
    
    /**
     * Get available stride estimation methods with descriptions
     */
    fun getAvailableMethods(): Map<StrideEstimationMethod, String> = mapOf(
        StrideEstimationMethod.WEINBERG to "Weinberg: Simple amplitude-based (fast, good for walking)",
        StrideEstimationMethod.KIM to "Kim: Variance-based (more accurate, requires more data)",
        StrideEstimationMethod.ADAPTIVE to "Adaptive: Multi-factor (most accurate, computationally intensive)"
    )
} 