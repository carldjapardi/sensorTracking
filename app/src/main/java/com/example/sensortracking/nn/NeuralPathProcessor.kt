package com.example.sensortracking.nn

import android.content.Context
import android.util.Log
import com.example.sensortracking.data.Position
import com.example.sensortracking.data.RawSensorData
import android.hardware.SensorManager
import java.nio.FloatBuffer
import ai.onnxruntime.*

/**
 * Loads an ONNX neural network model and predicts a refined position path from raw IMU data.
 *
 * This implementation is specifically designed for the RONIN model, which expects:
 * - Input: 6 features [gyro_x, gyro_y, gyro_z, accel_x, accel_y, accel_z] in world coordinates
 * - Output: 2 features [delta_x, delta_y] representing velocity in world coordinates
 * - Window size: 200 samples (fixed input size)
 */
class NeuralPathProcessor(context: Context, modelAssetPath: String) {
    private val session: OrtSession
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val windowSize = 200 // Ronin model expects 200 samples
    
    // Calibration parameters
    private var gyroBias: FloatArray? = null
    private var accelBias: FloatArray? = null
    private var isCalibrated = false

    init {
        Log.d("NeuralPathProcessor", "Loading ONNX model from: $modelAssetPath")
        val modelBytes = context.assets.open(modelAssetPath).use { it.readBytes() }
        val options = OrtSession.SessionOptions()
        session = env.createSession(modelBytes, options)
        Log.d("NeuralPathProcessor", "ONNX model loaded successfully")
    }

    /**
     * Perform dedicated calibration using provided sensor data.
     * This should be called with stationary sensor data for best results.
     * 
     * @param calibrationData Raw sensor data from stationary device
     * @param minSamples Minimum number of samples required for calibration (default: 200)
     * @return true if calibration was successful
     */
    fun calibrate(calibrationData: RawSensorData, minSamples: Int = 200): Boolean {
        val n = listOf(
            calibrationData.accelerometerSampleCount,
            calibrationData.gyroscopeSampleCount,
            calibrationData.rotationVectorSampleCount
        ).minOrNull() ?: 0
        
        if (n < minSamples) {
            Log.w("NeuralPathProcessor", "Insufficient samples for calibration: $n < $minSamples")
            return false
        }
        
        Log.d("NeuralPathProcessor", "Starting calibration with $n samples")
        
        // Calculate gyroscope bias (should be ~0 when stationary)
        gyroBias = calculateGyroBias(calibrationData, n)
        
        // Calculate accelerometer bias (should be ~[0, 0, -9.81] when stationary)
        accelBias = calculateAccelBias(calibrationData, n)
        
        // Validate calibration quality
        val gyroBiasMagnitude = kotlin.math.sqrt(gyroBias!![0] * gyroBias!![0] + gyroBias!![1] * gyroBias!![1] + gyroBias!![2] * gyroBias!![2])
        val accelBiasMagnitude = kotlin.math.sqrt(accelBias!![0] * accelBias!![0] + accelBias!![1] * accelBias!![1] + (accelBias!![2] + 9.81f) * (accelBias!![2] + 9.81f))
        
        Log.d("NeuralPathProcessor", "Calibration results:")
        Log.d("NeuralPathProcessor", "  Gyro bias: [${gyroBias!![0]}, ${gyroBias!![1]}, ${gyroBias!![2]}], magnitude: $gyroBiasMagnitude")
        Log.d("NeuralPathProcessor", "  Accel bias: [${accelBias!![0]}, ${accelBias!![1]}, ${accelBias!![2]}], magnitude: $accelBiasMagnitude")
        
        // Check if calibration is reasonable
        val isGyroReasonable = gyroBiasMagnitude < 0.1f // Less than 0.1 rad/s bias
        val isAccelReasonable = accelBiasMagnitude < 2.0f // Less than 2 m/s² bias from expected gravity
        
        if (isGyroReasonable && isAccelReasonable) {
            isCalibrated = true
            Log.d("NeuralPathProcessor", "Calibration successful")
            return true
        } else {
            Log.w("NeuralPathProcessor", "Calibration quality poor - gyro: $isGyroReasonable, accel: $isAccelReasonable")
            return false
        }
    }

    /**
     * Run inference on the provided raw sensor data using the loaded ONNX model.
     * The model expects input tensor of shape [1, 200, 6] containing world-frame
     * angular velocity and linear acceleration, and outputs [200, 2] velocity deltas.
     */
    fun predictPath(rawData: RawSensorData): List<Position> {
        Log.d("NeuralPathProcessor", "Starting neural network prediction")
        
        val n = minOf(rawData.timestamps.size, rawData.accelerometerData.size / 3, rawData.gyroscopeData.size / 3)
        Log.d("NeuralPathProcessor", "Raw data samples - Acc: ${rawData.accelerometerData.size / 3}, Gyro: ${rawData.gyroscopeData.size / 3}, RV: ${rawData.rotationVectorData.size / 4}")
        Log.d("NeuralPathProcessor", "Processing $n samples")
        
        if (n < 10) {
            Log.w("NeuralPathProcessor", "Insufficient samples for prediction: $n")
            return listOf(Position(0f, 0f))
        }
        
        // Use calibration if available, otherwise auto-calibrate
        val gyroBias = if (isCalibrated && this.gyroBias != null) {
            Log.d("NeuralPathProcessor", "Using calibrated gyro bias")
            this.gyroBias!!
        } else {
            Log.d("NeuralPathProcessor", "Using auto-calibrated gyro bias")
            calculateGyroBias(rawData, minOf(100, n))
        }
        
        val accelBias = if (isCalibrated && this.accelBias != null) {
            Log.d("NeuralPathProcessor", "Using calibrated accel bias")
            this.accelBias!!
        } else {
            Log.d("NeuralPathProcessor", "Using auto-calibrated accel bias")
            calculateAccelBias(rawData, minOf(100, n))
        }
        
        Log.d("NeuralPathProcessor", "Final calibration - Gyro bias: ${gyroBias.contentToString()}, Accel bias: ${accelBias.contentToString()}")
        
        // For low sampling rates, use a smaller window size and process all available data
        val effectiveWindowSize = minOf(windowSize, n)
        val stepSize = maxOf(1, effectiveWindowSize / 4) // Use smaller step size for low sample counts
        val numWindows = maxOf(1, (n - effectiveWindowSize) / stepSize + 1)
        
        Log.d("NeuralPathProcessor", "Using effective window size: $effectiveWindowSize, step size: $stepSize, num windows: $numWindows")
        
        var x = 0f
        var y = 0f
        val path = mutableListOf<Position>()
        path.add(Position(x, y))
        
        for (windowIdx in 0 until numWindows) {
            val startIdx = windowIdx * stepSize
            val endIdx = minOf(startIdx + effectiveWindowSize, n)
            
            // Prepare input for this window
            val input = FloatArray(6 * windowSize) // Still use full window size for model input
            
            // Fill the input array with available data
            for (i in 0 until windowSize) {
                val dataIdx = minOf(startIdx + i, n - 1)
                
                // Get sensor data
                val accIdx = dataIdx * 3
                val gyroIdx = dataIdx * 3
                val rvIdx = dataIdx * 4
                
                val accWorld = FloatArray(3)
                val gyroWorld = FloatArray(3)
                
                // Apply bias correction
                for (j in 0..2) {
                    accWorld[j] = rawData.accelerometerData[accIdx + j] - accelBias[j]
                    gyroWorld[j] = rawData.gyroscopeData[gyroIdx + j] - gyroBias[j]
                }
                
                // Transform to world coordinates using rotation vector
                if (rvIdx < rawData.rotationVectorData.size - 3) {
                    val rv = FloatArray(4)
                    for (j in 0..3) {
                        rv[j] = rawData.rotationVectorData[rvIdx + j]
                    }
                    
                    // Convert rotation vector to rotation matrix and apply
                    val rotationMatrix = rotationVectorToMatrix(rv)
                    val accTemp = FloatArray(3)
                    val gyroTemp = FloatArray(3)
                    
                    // Apply rotation
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
                
                // Store in [channels, time] format: [6, windowSize]
                input[i] = gyroWorld[0]
                input[i + windowSize] = gyroWorld[1]
                input[i + 2 * windowSize] = gyroWorld[2]
                input[i + 3 * windowSize] = accWorld[0]
                input[i + 4 * windowSize] = accWorld[1]
                input[i + 5 * windowSize] = accWorld[2]
            }
            
            // Run inference
            val shape = longArrayOf(1, 6, windowSize.toLong())
            val inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(input), shape)
            
            Log.d("NeuralPathProcessor", "Running ONNX inference for window $windowIdx with input shape: ${shape.contentToString()}")
            
            val output = session.run(mapOf("imu" to inputTensor))
            val outputValue = output["delta_xy"]
            val outputTensor = when {
                outputValue is OnnxTensor -> outputValue
                outputValue is java.util.Optional<*> -> {
                    val unwrapped = outputValue.orElse(null)
                    if (unwrapped is OnnxTensor) {
                        unwrapped
                    } else {
                        Log.e("NeuralPathProcessor", "Optional contains unexpected type: ${unwrapped?.javaClass}")
                        continue
                    }
                }
                else -> {
                    Log.e("NeuralPathProcessor", "Unexpected output type: ${outputValue?.javaClass}")
                    continue
                }
            }
            val outputShape = outputTensor.info.shape
            val outputData = outputTensor.floatBuffer.array()
            
            Log.d("NeuralPathProcessor", "ONNX inference completed for window $windowIdx, output shape: ${outputShape.contentToString()}")
            Log.d("NeuralPathProcessor", "Output details: ${outputData.size} values")
            
            // Interpret output as velocity prediction
            if (outputData.size >= 2) {
                val deltaX = outputData[0]
                val deltaY = outputData[1]
                
                // Calculate time step for this window
                val timeStep = if (windowIdx < numWindows - 1) {
                    val nextStartIdx = (windowIdx + 1) * stepSize
                    val currentTime = rawData.timestamps[minOf(startIdx, rawData.timestamps.size - 1)]
                    val nextTime = rawData.timestamps[minOf(nextStartIdx, rawData.timestamps.size - 1)]
                    (nextTime - currentTime) / 1000f // Convert to seconds
                } else {
                    0.1f // Default time step for last window
                }
                
                // Scale the velocity prediction based on the actual number of samples processed
                val scaleFactor = effectiveWindowSize.toFloat() / windowSize
                val scaledDeltaX = deltaX * scaleFactor
                val scaledDeltaY = deltaY * scaleFactor
                
                // Apply additional scaling to make the path more visible
                // The model might be outputting velocity in a different scale than expected
                val outputScale = 10.0f // Scale factor to make path more visible
                val finalDeltaX = scaledDeltaX * outputScale
                val finalDeltaY = scaledDeltaY * outputScale
                
                // Integrate velocity to get position change
                val posDeltaX = finalDeltaX * timeStep
                val posDeltaY = finalDeltaY * timeStep
                
                x += posDeltaX
                y += posDeltaY
                
                Log.d("NeuralPathProcessor", "Window $windowIdx: velocity=($deltaX, $deltaY), scaled=($scaledDeltaX, $scaledDeltaY), final=($finalDeltaX, $finalDeltaY), timeStep=$timeStep, posDelta=($posDeltaX, $posDeltaY), pos=($x, $y)")
                
                path.add(Position(x, y))
            }
        }
        
        Log.d("NeuralPathProcessor", "Generated neural network path with ${path.size} points, final position: ($x, $y)")
        Log.d("NeuralPathProcessor", "Path bounds: x=[${path.minOf { it.x }}, ${path.maxOf { it.x }}], y=[${path.minOf { it.y }}, ${path.maxOf { it.y }}]")
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
