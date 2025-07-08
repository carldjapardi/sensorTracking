package com.example.sensortracking.sensor.math

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2

fun quaternionToEulerAngles(quaternion: FloatArray): FloatArray {
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