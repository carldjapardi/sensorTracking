package com.example.sensortracking.nn

import android.content.Context
import com.example.sensortracking.data.Position
import com.example.sensortracking.data.RawSensorData
import android.hardware.SensorManager
import java.nio.FloatBuffer
import ai.onnxruntime.*

/**
 * Loads an ONNX neural network model and predicts a refined position path from raw IMU data.
 *
 * The actual preprocessing steps depend on the selected model (e.g. L-IONet or RONIN).
 * This class currently acts as a placeholder for integrating such models.
 */
class NeuralPathProcessor(context: Context, modelAssetPath: String) {
    private val session: OrtSession
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()

    init {
        val modelBytes = context.assets.open(modelAssetPath).use { it.readBytes() }
        val options = SessionOptions()
        session = env.createSession(modelBytes, options)
    }

    /**
     * Run inference on the provided raw sensor data using the loaded ONNX model.
     * This implementation assumes the model takes a tensor of shape
     * `[1, N, 6]` containing world-frame linear acceleration and angular
     * velocity and outputs `[N, 2]` delta translations in meters.
     */
    fun predictPath(rawData: RawSensorData): List<Position> {
        val n = listOf(
            rawData.accelerometerSampleCount,
            rawData.gyroscopeSampleCount,
            rawData.rotationVectorSampleCount
        ).minOrNull() ?: 0
        if (n == 0) return emptyList()

        val input = FloatArray(n * 6)
        val rotMatrix = FloatArray(9)
        for (i in 0 until n) {
            val acc = rawData.getAccelerometerSample(i)
            val gyro = rawData.getGyroscopeSample(i)
            val rv = rawData.getRotationVectorSample(i)
            SensorManager.getRotationMatrixFromVector(rotMatrix, rv)
            val accWorld = rotate(acc, rotMatrix)
            val gyroWorld = rotate(gyro, rotMatrix)
            val base = i * 6
            input[base] = accWorld[0]
            input[base + 1] = accWorld[1]
            input[base + 2] = accWorld[2] - 9.81f
            input[base + 3] = gyroWorld[0]
            input[base + 4] = gyroWorld[1]
            input[base + 5] = gyroWorld[2]
        }

        val shape = longArrayOf(1, n.toLong(), 6)
        val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(input), shape)
        val result = session.run(mapOf(session.inputNames.first() to tensor))
        @Suppress("UNCHECKED_CAST")
        val output = result[0].value as Array<FloatArray>

        val path = mutableListOf<Position>()
        var x = 0f
        var y = 0f
        for (i in output.indices) {
            x += output[i][0]
            y += output[i][1]
            path.add(Position(x, y))
        }
        return path
    }

    private fun rotate(v: FloatArray, m: FloatArray): FloatArray {
        return floatArrayOf(
            m[0] * v[0] + m[1] * v[1] + m[2] * v[2],
            m[3] * v[0] + m[4] * v[1] + m[5] * v[2],
            m[6] * v[0] + m[7] * v[1] + m[8] * v[2]
        )
    }
}
