package com.example.sensortracking.sensor.pdr

import android.util.Log
import com.example.sensortracking.data.HeadingData
import com.example.sensortracking.data.HeadingSource
import com.example.sensortracking.data.PDRConfig
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Heading estimation using sensor fusion
 * Implements complementary filter and magnetometer calibration
 */
class HeadingEstimator(private val config: PDRConfig) {
    
    private var lastGyroUpdate: Long = 0
    private var gyroAngle: FloatArray = FloatArray(3)
    private var filteredHeading: Float = 0f
    
    // Magnetometer calibration
    private var magnetometerBias: FloatArray = FloatArray(3)
    private var magnetometerScale: FloatArray = FloatArray(3) { 1f }
    private var isMagnetometerCalibrated = false
    private var calibrationSamples = 0
    private val calibrationRequired = 50
    
    // Complementary filter variables
    private var alpha: Float = config.headingSmoothingFactor
    private var lastAccelHeading: Float = 0f
    private var lastMagHeading: Float = 0f
    
    // Magnetic field strength tracking
    private var magneticFieldStrength = mutableListOf<Float>()
    private var lastMagneticStrength: Float = 0f
    
    /**
     * Estimate heading using sensor fusion
     */
    fun estimateHeading(
        accelerometer: FloatArray,
        gyroscope: FloatArray,
        magnetometer: FloatArray,
        timestamp: Long
    ): HeadingData {
        
        // Calibrate magnetometer if needed
        if (!isMagnetometerCalibrated) {
            calibrateMagnetometer(magnetometer)
            return HeadingData(
                heading = 0f,
                confidence = 0f,
                source = HeadingSource.MAGNETOMETER
            )
        }
        
        // Apply magnetometer calibration
        val calibratedMagnetometer = applyMagnetometerCalibration(magnetometer)
        
        // Calculate heading from different sources
        val accelHeading = calculateAccelerometerHeading(accelerometer)
        val magHeading = calculateMagnetometerHeading(accelerometer, calibratedMagnetometer)
        val gyroHeading = calculateGyroscopeHeading(gyroscope, timestamp)
        
        // Fuse headings using complementary filter
        val fusedHeading = fuseHeadings(accelHeading, magHeading, gyroHeading)
        
        // Calculate confidence based on magnetic field quality
        val confidence = calculateHeadingConfidence(calibratedMagnetometer, accelerometer)
        
        // Determine best source
        val source = determineHeadingSource(confidence, calibratedMagnetometer)
        
        return HeadingData(
            heading = fusedHeading,
            confidence = confidence,
            source = source
        )
    }
    
    /**
     * Calculate heading from accelerometer (pitch and roll only)
     */
    private fun calculateAccelerometerHeading(accelerometer: FloatArray): Float {
        val magnitude = sqrt(accelerometer[0].pow(2) + accelerometer[1].pow(2) + accelerometer[2].pow(2))
        
        if (magnitude < 0.1f) return lastAccelHeading
        
        val pitch = atan2(-accelerometer[0], sqrt(accelerometer[1].pow(2) + accelerometer[2].pow(2)))
        val roll = atan2(accelerometer[1], accelerometer[2])
        
        // Accelerometer can only provide pitch and roll, not yaw
        // We use the previous heading as a base
        lastAccelHeading = lastAccelHeading
        return lastAccelHeading
    }
    
    /**
     * Calculate heading from magnetometer
     */
    private fun calculateMagnetometerHeading(accelerometer: FloatArray, magnetometer: FloatArray): Float {
        val rotationMatrix = FloatArray(9)
        val inclinationMatrix = FloatArray(9)
        
        // Get rotation matrix from accelerometer and magnetometer
        if (android.hardware.SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, accelerometer, magnetometer)) {
            val orientation = FloatArray(3)
            android.hardware.SensorManager.getOrientation(rotationMatrix, orientation)
            
            // Convert radians to degrees and normalize to 0-360
            val heading = (Math.toDegrees(orientation[0].toDouble()) + 360) % 360
            lastMagHeading = heading.toFloat()
            return heading.toFloat()
        }
        
        return lastMagHeading
    }
    
    /**
     * Calculate heading from gyroscope integration
     */
    private fun calculateGyroscopeHeading(gyroscope: FloatArray, timestamp: Long): Float {
        if (lastGyroUpdate != 0L) {
            val dt = (timestamp - lastGyroUpdate) / 1000.0f
            
            // Apply threshold to reduce drift
            for (i in 0..2) {
                if (abs(gyroscope[i]) > 0.1f) {
                    gyroAngle[i] += gyroscope[i] * dt
                }
            }
        }
        
        lastGyroUpdate = timestamp
        
        // Convert gyro angles to heading (simplified)
        val heading = (Math.toDegrees(gyroAngle[2].toDouble()) + 360) % 360
        return heading.toFloat()
    }
    
    /**
     * Fuse headings using complementary filter
     */
    private fun fuseHeadings(accelHeading: Float, magHeading: Float, gyroHeading: Float): Float {
        // Complementary filter: gyroHeading = alpha * gyroHeading + (1 - alpha) * magHeading
        val filteredGyroHeading = alpha * gyroHeading + (1 - alpha) * magHeading
        
        // Apply low-pass filter to reduce noise
        filteredHeading = alpha * filteredHeading + (1 - alpha) * filteredGyroHeading
        
        return filteredHeading
    }
    
    /**
     * Calibrate magnetometer using ellipsoid fitting
     */
    private fun calibrateMagnetometer(magnetometer: FloatArray) {
        calibrationSamples++
        
        // Collect magnetometer samples - accumulate bias
        magnetometerBias[0] += magnetometer[0]
        magnetometerBias[1] += magnetometer[1]
        magnetometerBias[2] += magnetometer[2]
        
        // Track magnetic field strength
        val strength = sqrt(magnetometer[0].pow(2) + magnetometer[1].pow(2) + magnetometer[2].pow(2))
        magneticFieldStrength.add(strength)
        
        // Debug logging every 10 samples
        if (calibrationSamples % 10 == 0) {
            Log.d("HeadingEstimator", "Calibration progress: $calibrationSamples/$calibrationRequired (${(calibrationSamples.toFloat() / calibrationRequired * 100).toInt()}%)")
        }
        
        if (calibrationSamples >= calibrationRequired) {
            // Calculate bias (mean of all samples)
            magnetometerBias[0] /= calibrationRequired.toFloat()
            magnetometerBias[1] /= calibrationRequired.toFloat()
            magnetometerBias[2] /= calibrationRequired.toFloat()
            
            // Calculate scale factors (simplified ellipsoid fitting)
            val avgStrength = magneticFieldStrength.average().toFloat()
            magnetometerScale[0] = avgStrength / 50f // Normalize to typical magnetic field strength
            magnetometerScale[1] = avgStrength / 50f
            magnetometerScale[2] = avgStrength / 50f
            
            isMagnetometerCalibrated = true
            lastMagneticStrength = avgStrength
            
            Log.d("HeadingEstimator", "Calibration complete! Bias: [${magnetometerBias[0]}, ${magnetometerBias[1]}, ${magnetometerBias[2]}], Avg Strength: $avgStrength")
        }
    }
    
    /**
     * Apply magnetometer calibration
     */
    private fun applyMagnetometerCalibration(magnetometer: FloatArray): FloatArray {
        val calibrated = FloatArray(3)
        calibrated[0] = (magnetometer[0] - magnetometerBias[0]) * magnetometerScale[0]
        calibrated[1] = (magnetometer[1] - magnetometerBias[1]) * magnetometerScale[1]
        calibrated[2] = (magnetometer[2] - magnetometerBias[2]) * magnetometerScale[2]
        return calibrated
    }
    
    /**
     * Calculate heading confidence based on signal quality
     */
    private fun calculateHeadingConfidence(magnetometer: FloatArray, accelerometer: FloatArray): Float {
        var confidence = 0.5f
        
        // Check magnetic field strength
        val currentStrength = sqrt(magnetometer[0].pow(2) + magnetometer[1].pow(2) + magnetometer[2].pow(2))
        val strengthRatio = currentStrength / lastMagneticStrength
        
        if (strengthRatio in 0.8f..1.2f) {
            confidence += 0.3f // Good magnetic field
        } else if (strengthRatio in 0.5f..1.5f) {
            confidence += 0.1f // Acceptable magnetic field
        }
        
        // Check accelerometer stability
        val accelMagnitude = sqrt(accelerometer[0].pow(2) + accelerometer[1].pow(2) + accelerometer[2].pow(2))
        if (abs(accelMagnitude - 9.8f) < 2f) {
            confidence += 0.2f // Stable accelerometer reading
        }
        
        return confidence.coerceIn(0f, 1f)
    }
    
    /**
     * Determine best heading source
     */
    private fun determineHeadingSource(confidence: Float, magnetometer: FloatArray): HeadingSource {
        val strength = sqrt(magnetometer[0].pow(2) + magnetometer[1].pow(2) + magnetometer[2].pow(2))
        
        return when {
            confidence > 0.8f && strength > 20f -> HeadingSource.FUSED
            confidence > 0.6f -> HeadingSource.MAGNETOMETER
            else -> HeadingSource.GYROSCOPE
        }
    }
    
    /**
     * Reset estimator state
     */
    fun reset() {
        lastGyroUpdate = 0
        gyroAngle = FloatArray(3)
        filteredHeading = 0f
        lastAccelHeading = 0f
        lastMagHeading = 0f
        magnetometerBias = FloatArray(3)
        magnetometerScale = FloatArray(3) { 1f }
        isMagnetometerCalibrated = false
        calibrationSamples = 0
        magneticFieldStrength.clear()
        lastMagneticStrength = 0f
    }
    
    /**
     * Update configuration
     */
    fun updateConfig(newConfig: PDRConfig) {
        alpha = newConfig.headingSmoothingFactor
    }
    
    /**
     * Check if magnetometer is calibrated
     */
    fun isCalibrated(): Boolean = isMagnetometerCalibrated
    
    /**
     * Get calibration progress (0.0 to 1.0)
     */
    fun getCalibrationProgress(): Float {
        return (calibrationSamples.toFloat() / calibrationRequired).coerceIn(0f, 1f)
    }
} 