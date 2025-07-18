package com.example.sensortracking.sensor.pdr

import com.example.sensortracking.data.HeadingData
import com.example.sensortracking.data.PDRConfig
import com.example.sensortracking.util.math.quaternionToEulerAngles
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Heading estimation using Android's rotation vector sensor
 */
class HeadingEstimator(private val config: PDRConfig) {
    // Rotation vector data (quaternion)
    private var rotationVector: FloatArray = FloatArray(4)
    private var hasRotationVector = false
    
    fun updateRotationVector(rotationVector: FloatArray) {
        System.arraycopy(rotationVector, 0, this.rotationVector, 0, 4)
        hasRotationVector = true
    }
    
    fun estimateHeading(): HeadingData {
        if (!hasRotationVector) {
            return HeadingData(heading = 0f, confidence = 0f,)
        }
        return estimateHeadingFromRotationVector()
    }
    
    private fun estimateHeadingFromRotationVector(): HeadingData {
        val eulerAngles = quaternionToEulerAngles(rotationVector)
        // Extract yaw (heading) from Euler angles
        val heading = (Math.toDegrees(eulerAngles[2].toDouble()) + 360) % 360
        
        // Calculate confidence based on quaternion magnitude
        val quaternionMagnitude = sqrt(
            rotationVector[0] * rotationVector[0] +
            rotationVector[1] * rotationVector[1] +
            rotationVector[2] * rotationVector[2] +
            rotationVector[3] * rotationVector[3]
        )
        
        val confidence = if (abs(quaternionMagnitude - 1.0f) < 0.1f) {
            0.9f
        } else {
            0.6f
        }
        
        return HeadingData(
            heading = heading.toFloat(),
            confidence = confidence,
        )
    }
    
    fun reset() {
        hasRotationVector = false
        rotationVector = FloatArray(4)
    }
    
    fun updateConfig(newConfig: PDRConfig) {
        // Currently no config parameters needed for rotation vector
        // Could be extended for filtering or calibration parameters
    }
}