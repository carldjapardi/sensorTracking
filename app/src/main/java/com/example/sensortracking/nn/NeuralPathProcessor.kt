package com.example.sensortracking.nn

import android.content.Context
import com.example.sensortracking.data.Position
import com.example.sensortracking.data.RawSensorData
import android.hardware.SensorManager
import java.nio.FloatBuffer
import ai.onnxruntime.*

class NeuralPathProcessor(context: Context, modelAssetPath: String) {
    private val session: OrtSession
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val windowSize = 200
    
    private var gyroBias: FloatArray? = null
    private var accelBias: FloatArray? = null
    private var isCalibrated = false
    private var gyroScale: FloatArray = floatArrayOf(1f, 1f, 1f)
    private var accelScale: FloatArray = floatArrayOf(1f, 1f, 1f)

    init {
        val modelBytes = context.assets.open(modelAssetPath).use { it.readBytes() }
        val options = OrtSession.SessionOptions()
        session = env.createSession(modelBytes, options)
    }

    fun calibrate(calibrationData: RawSensorData, minSamples: Int = 200): Boolean {
        val n = listOf(
            calibrationData.accelerometerSampleCount,
            calibrationData.gyroscopeSampleCount,
            calibrationData.rotationVectorSampleCount
        ).minOrNull() ?: 0
        
        if (n < minSamples) return false
        
        gyroBias = calculateGyroBias(calibrationData, n)
        accelBias = calculateAccelBias(calibrationData, n)
        
        val gyroBiasMagnitude = kotlin.math.sqrt(gyroBias!![0] * gyroBias!![0] + gyroBias!![1] * gyroBias!![1] + gyroBias!![2] * gyroBias!![2])
        val accelBiasMagnitude = kotlin.math.sqrt(accelBias!![0] * accelBias!![0] + accelBias!![1] * accelBias!![1] + (accelBias!![2] + 9.81f) * (accelBias!![2] + 9.81f))
        
        val isGyroReasonable = gyroBiasMagnitude < 0.1f
        val isAccelReasonable = accelBiasMagnitude < 2.0f
        
        if (isGyroReasonable && isAccelReasonable) {
            isCalibrated = true
            return true
        }
        return false
    }

    fun setScaleFactors(accel: FloatArray, gyro: FloatArray) {
        if (accel.size >= 3 && gyro.size >= 3) {
            accelScale = accel.copyOf(3)
            gyroScale = gyro.copyOf(3)
        }
    }

    fun predictPath(rawData: RawSensorData, initialPosition: Position = Position(0f, 0f)): List<Position> {
        val n = minOf(rawData.timestamps.size, rawData.accelerometerData.size / 3, rawData.gyroscopeData.size / 3)
        
        if (n < 10) return listOf(initialPosition)
        
        val gyroBias = if (isCalibrated && this.gyroBias != null) {
            this.gyroBias!!
        } else {
            calculateGyroBias(rawData, minOf(100, n))
        }
        
        val accelBias = if (isCalibrated && this.accelBias != null) {
            this.accelBias!!
        } else {
            calculateAccelBias(rawData, minOf(100, n))
        }
        
        val effectiveWindowSize = minOf(windowSize, n)
        val stepSize = maxOf(1, effectiveWindowSize / 4)
        val numWindows = maxOf(1, (n - effectiveWindowSize) / stepSize + 1)
        
        var x = initialPosition.x
        var y = initialPosition.y
        val path = mutableListOf<Position>()
        path.add(Position(x, y))
        
        for (windowIdx in 0 until numWindows) {
            val startIdx = windowIdx * stepSize
            
            val input = FloatArray(6 * windowSize)
            
            for (i in 0 until windowSize) {
                val dataIdx = minOf(startIdx + i, n - 1)
                
                val accIdx = dataIdx * 3
                val gyroIdx = dataIdx * 3
                val rvIdx = dataIdx * 4
                
                val accWorld = FloatArray(3)
                val gyroWorld = FloatArray(3)
                
                for (j in 0..2) {
                    accWorld[j] = (rawData.accelerometerData[accIdx + j] - accelBias[j]) * accelScale[j]
                    gyroWorld[j] = (rawData.gyroscopeData[gyroIdx + j] - gyroBias[j]) * gyroScale[j]
                }
                
                if (rvIdx < rawData.rotationVectorData.size - 3) {
                    val rv = FloatArray(4)
                    for (j in 0..3) {
                        rv[j] = rawData.rotationVectorData[rvIdx + j]
                    }
                    
                    val rotationMatrix = rotationVectorToMatrix(rv)
                    val accTemp = FloatArray(3)
                    val gyroTemp = FloatArray(3)
                    
                    for (row in 0..2) {
                        accTemp[row] = 0f
                        gyroTemp[row] = 0f
                        for (col in 0..2) {
                            accTemp[row] += rotationMatrix[row * 3 + col] * accWorld[col]
                            gyroTemp[row] += rotationMatrix[row * 3 + col] * gyroWorld[col]
                        }
                    }
                    
                    accWorld[0] = accTemp[0]
                    accWorld[1] = accTemp[1]
                    accWorld[2] = accTemp[2]
                    gyroWorld[0] = gyroTemp[0]
                    gyroWorld[1] = gyroTemp[1]
                    gyroWorld[2] = gyroTemp[2]
                }
                
                input[i] = gyroWorld[0]
                input[i + windowSize] = gyroWorld[1]
                input[i + 2 * windowSize] = gyroWorld[2]
                input[i + 3 * windowSize] = accWorld[0]
                input[i + 4 * windowSize] = accWorld[1]
                input[i + 5 * windowSize] = accWorld[2]
            }
            
            val shape = longArrayOf(1, 6, windowSize.toLong())
            val inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(input), shape)
            
            val output = session.run(mapOf("imu" to inputTensor))
            val outputValue = output["delta_xy"]
            val outputTensor = when {
                outputValue is OnnxTensor -> outputValue
                outputValue is java.util.Optional<*> -> {
                    val unwrapped = outputValue.orElse(null)
                    if (unwrapped is OnnxTensor) {
                        unwrapped
                    } else {
                        continue
                    }
                }
                else -> continue
            }
            val outputData = outputTensor.floatBuffer.array()
            
            if (outputData.size >= 2) {
                val deltaX = outputData[0]
                val deltaY = outputData[1]
                
                val timeStep = if (windowIdx < numWindows - 1) {
                    val nextStartIdx = (windowIdx + 1) * stepSize
                    val currentTime = rawData.timestamps[minOf(startIdx, rawData.timestamps.size - 1)]
                    val nextTime = rawData.timestamps[minOf(nextStartIdx, rawData.timestamps.size - 1)]
                    (nextTime - currentTime) / 1000f
                } else {
                    0.1f
                }
                
                val scaleFactor = effectiveWindowSize.toFloat() / windowSize
                val scaledDeltaX = deltaX * scaleFactor
                val scaledDeltaY = deltaY * scaleFactor
                
                val posDeltaX = scaledDeltaX * timeStep
                val posDeltaY = scaledDeltaY * timeStep
                
                x += posDeltaX
                y += posDeltaY
                
                path.add(Position(x, y))
            }
        }
        
        return path
    }

    private fun calculateGyroBias(rawData: RawSensorData, samples: Int): FloatArray {
        val bias = FloatArray(3) { 0f }
        for (i in 0 until samples) {
            if (i < rawData.gyroscopeSampleCount) {
                val gyro = rawData.getGyroscopeSample(i)
                bias[0] += gyro[0]
                bias[1] += gyro[1]
                bias[2] += gyro[2]
            }
        }
        return floatArrayOf(bias[0] / samples, bias[1] / samples, bias[2] / samples)
    }

    private fun calculateAccelBias(rawData: RawSensorData, samples: Int): FloatArray {
        val bias = FloatArray(3) { 0f }
        val count = minOf(samples, rawData.accelerometerData.size / 3)
        
        for (i in 0 until count) {
            val idx = i * 3
            bias[0] += rawData.accelerometerData[idx]
            bias[1] += rawData.accelerometerData[idx + 1]
            bias[2] += rawData.accelerometerData[idx + 2]
        }
        
        bias[0] /= count
        bias[1] /= count
        bias[2] /= count
        
        return bias
    }
    
    private fun rotationVectorToMatrix(rv: FloatArray): FloatArray {
        val matrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(matrix, rv)
        return matrix
    }
}
