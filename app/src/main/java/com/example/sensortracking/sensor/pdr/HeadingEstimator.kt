package com.example.sensortracking.sensor.pdr

import com.example.sensortracking.data.HeadingData
import com.example.sensortracking.data.PDRConfig
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Heading estimation using Android's rotation vector sensor
 */
class HeadingEstimator(private val config: PDRConfig) {
    
    companion object {
        private const val TAG = "HeadingEstimator"
    }
    
    // Rotation vector data (quaternion)
    private var rotationVector: FloatArray = FloatArray(4)
    private var hasRotationVector = false
    
    fun updateRotationVector(rotationVector: FloatArray) {
        System.arraycopy(rotationVector, 0, this.rotationVector, 0, 4)
        hasRotationVector = true
    }
    
    fun estimateHeading(): HeadingData {
        if (!hasRotationVector) {
            return HeadingData(
                heading = 0f,
                confidence = 0f,
            )
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

    private fun quaternionToEulerAngles(quaternion: FloatArray): FloatArray {
        val qw = quaternion[3]
        val qx = quaternion[0]
        val qy = quaternion[1]
        val qz = quaternion[2]
        
        // Roll (x-axis rotation)
        val sinr_cosp = 2.0f * (qw * qx + qy * qz)
        val cosr_cosp = 1.0f - 2.0f * (qx * qx + qy * qy)
        val roll = atan2(sinr_cosp, cosr_cosp)
        
        // Pitch (y-axis rotation)
        val sinp = 2.0f * (qw * qy - qz * qx)
        val pitch = if (abs(sinp) >= 1.0f) {
            kotlin.math.PI.toFloat() / 2.0f * if (sinp >= 0.0f) 1.0f else -1.0f
        } else {
            asin(sinp)
        }
        
        // Yaw (z-axis rotation)
        val siny_cosp = 2.0f * (qw * qz + qx * qy)
        val cosy_cosp = 1.0f - 2.0f * (qy * qy + qz * qz)
        val yaw = atan2(siny_cosp, cosy_cosp)
        
        return floatArrayOf(roll, pitch, yaw)
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