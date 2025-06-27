package com.example.sensortracking.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class SensorFusion(
    private val sensorManager: SensorManager,
    private val alpha: Float = 0.8f // Complementary filter coefficient
) : SensorEventListener {
    private var accelerometer: FloatArray = FloatArray(3)
    private var magnetometer: FloatArray = FloatArray(3)
    private var gyroscope: FloatArray = FloatArray(3)

    private var orientation: FloatArray = FloatArray(3) // [azimuth, pitch, roll]
    private var linearAcceleration: FloatArray = FloatArray(3)

    private var lastUpdate: Long = 0
    private var lastGyroUpdate: Long = 0
    private var gyroAngle: FloatArray = FloatArray(3)

    private var listener: ((FloatArray, FloatArray) -> Unit)? = null

    // Calibration values
    private var accelerometerBias: FloatArray = FloatArray(3)
    private var magnetometerBias: FloatArray = FloatArray(3)
    private var isCalibrated = false
    private var calibrationSamples = 0
    private val calibrationRequired = 50 // Number of samples for calibration

    init {
        // Initialize sensors
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        if (accelerometerSensor == null) {
            Log.e("SensorFusion", "Accelerometer sensor not available")
        } else {
            Log.d("SensorFusion", "Registering accelerometer sensor")
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        if (magnetometerSensor == null) {
            Log.e("SensorFusion", "Magnetometer sensor not available")
        } else {
            Log.d("SensorFusion", "Registering magnetometer sensor")
            sensorManager.registerListener(this, magnetometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        if (gyroscopeSensor == null) {
            Log.e("SensorFusion", "Gyroscope sensor not available")
        } else {
            Log.d("SensorFusion", "Registering gyroscope sensor")
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun setListener(listener: (FloatArray, FloatArray) -> Unit) {
        this.listener = listener
        Log.d("SensorFusion", "Listener set")
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // Apply calibration
                if (!isCalibrated) {
                    calibrateAccelerometer(event.values)
                    return
                }
                System.arraycopy(event.values, 0, accelerometer, 0, 3)
                for (i in 0..2) {
                    accelerometer[i] -= accelerometerBias[i]
                }
                updateOrientation()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                // Apply calibration
                if (!isCalibrated) {
                    calibrateMagnetometer(event.values)
                    return
                }
                System.arraycopy(event.values, 0, magnetometer, 0, 3)
                for (i in 0..2) {
                    magnetometer[i] -= magnetometerBias[i]
                }
                updateOrientation()
            }
            Sensor.TYPE_GYROSCOPE -> {
                val currentTime = System.currentTimeMillis()
                if (lastGyroUpdate != 0L) {
                    val dt = (currentTime - lastGyroUpdate) / 1000.0f
                    // Apply threshold to reduce drift
                    for (i in 0..2) {
                        if (abs(event.values[i]) > 0.1f) {
                            gyroAngle[i] += event.values[i] * dt
                        }
                    }
                }
                lastGyroUpdate = currentTime
                System.arraycopy(event.values, 0, gyroscope, 0, 3)
            }
        }
    }

    private fun calibrateAccelerometer(values: FloatArray) {
        // Create a new array to store the sum
        val newBias = FloatArray(3)
        for (i in 0..2) {
            newBias[i] = accelerometerBias[i] + values[i]
        }
        accelerometerBias = newBias

        calibrationSamples++
        if (calibrationSamples >= calibrationRequired) {
            // Calculate final bias
            val finalBias = FloatArray(3)
            for (i in 0..2) {
                finalBias[i] = accelerometerBias[i] / calibrationRequired
            }
            accelerometerBias = finalBias
            isCalibrated = true
            Log.d("SensorFusion", "Accelerometer calibration complete: bias=$accelerometerBias")
        }
    }

    private fun calibrateMagnetometer(values: FloatArray) {
        // Create a new array to store the sum
        val newBias = FloatArray(3)
        for (i in 0..2) {
            newBias[i] = magnetometerBias[i] + values[i]
        }
        magnetometerBias = newBias
    }

    private fun updateOrientation() {
        if (!isCalibrated) return
        if (accelerometer[0] == 0f && accelerometer[1] == 0f && accelerometer[2] == 0f) return
        if (magnetometer[0] == 0f && magnetometer[1] == 0f && magnetometer[2] == 0f) return

        val rotationMatrix = FloatArray(9)
        val inclinationMatrix = FloatArray(9)

        // Get rotation matrix
        if (SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, accelerometer, magnetometer)) {
            // Get orientation angles
            SensorManager.getOrientation(rotationMatrix, orientation)

            // Calculate linear acceleration (remove gravity)
            val gravity = FloatArray(3)
            gravity[0] = sin(orientation[1]) * cos(orientation[2])
            gravity[1] = -sin(orientation[2])
            gravity[2] = cos(orientation[1]) * cos(orientation[2])

            // Apply complementary filter with drift compensation
            val currentTime = System.currentTimeMillis()
            if (lastUpdate != 0L) {
                val dt = (currentTime - lastUpdate) / 1000.0f
                // Create new array for filtered acceleration
                val newLinearAccel = FloatArray(3)
                for (i in 0..2) {
                    // Apply low-pass filter to reduce noise
                    val filteredAccel = alpha * (accelerometer[i] - gravity[i]) + (1 - alpha) * linearAcceleration[i]
                    // Apply drift compensation
                    newLinearAccel[i] = if (abs(filteredAccel) < 0.1f) 0f else filteredAccel
                }
                linearAcceleration = newLinearAccel
            }
            lastUpdate = currentTime

            // Notify listener
            listener?.invoke(linearAcceleration, orientation)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Reset calibration if accuracy changes
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            Log.w("SensorFusion", "Sensor accuracy unreliable: ${sensor?.name}")
            isCalibrated = false
            calibrationSamples = 0
            accelerometerBias = FloatArray(3)
            magnetometerBias = FloatArray(3)
        }
    }

    fun unregisterListener() {
        Log.d("SensorFusion", "Unregistering sensor listeners")
        sensorManager.unregisterListener(this)
    }
}