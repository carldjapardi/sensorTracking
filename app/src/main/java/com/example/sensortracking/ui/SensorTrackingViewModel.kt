package com.example.sensortracking.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sensortracking.data.Position
import com.example.sensortracking.data.SensorData
import com.example.sensortracking.data.SensorTrackingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class SensorTrackingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SensorTrackingUiState(isTracking = true))
    val uiState: StateFlow<SensorTrackingUiState> = _uiState.asStateFlow()

    private var sensorManager: SensorManager? = null
    private var sensorEventListener: SensorEventListener? = null
    private var lastStepTime: Long = 0
    private val stepThreshold = 1.8f // Adjusted for filtered data
    private val stepLength = 0.7f // Average step length in meters

    // High-pass filter variables
    private val alpha = 0.8f
    private var gravity = floatArrayOf(0f, 0f, 0f)
    private var linearAcceleration = floatArrayOf(0f, 0f, 0f)

    private var accelerometerData = floatArrayOf(0f, 0f, 0f)
    private var magnetometerData = floatArrayOf(0f, 0f, 0f)

    fun initializeSensors(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorEventListener = createSensorEventListener()

        sensorManager?.let { manager ->
            manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
                manager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_GAME)
            }
            manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also {
                manager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    private fun createSensorEventListener() = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!_uiState.value.isTracking) return

            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    accelerometerData = event.values.clone()
                    // Isolate gravity
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

                    // Remove gravity to get linear acceleration
                    linearAcceleration[0] = event.values[0] - gravity[0]
                    linearAcceleration[1] = event.values[1] - gravity[1]
                    linearAcceleration[2] = event.values[2] - gravity[2]

                    _uiState.update {
                        it.copy(
                            xAcceleration = linearAcceleration[0],
                            yAcceleration = linearAcceleration[1],
                            zAcceleration = linearAcceleration[2],
                        )
                    }

                    detectStep(linearAcceleration[0], linearAcceleration[1], linearAcceleration[2])
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    magnetometerData = event.values.clone()
                }
            }
            calculateHeading()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun detectStep(accelX: Float, accelY: Float, accelZ: Float) {
        val magnitude = sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ)
        val currentTime = System.currentTimeMillis()

        if (magnitude > stepThreshold && currentTime - lastStepTime > 500) { // Cooldown to prevent false positives
            lastStepTime = currentTime
            updatePosition()
        }
    }

    private fun calculateHeading() {
        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)

        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerData, magnetometerData)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        val heading = (Math.toDegrees(orientationAngles[0].toDouble()) + 360) % 360

        _uiState.update { it.copy(direction = heading.toFloat()) }
    }

    private fun updatePosition() {
        _uiState.update { currentState ->
            val heading = currentState.direction
            val currentPos = currentState.currentPosition

            val deltaX = stepLength * sin(Math.toRadians(heading.toDouble())).toFloat()
            val deltaY = -stepLength * cos(Math.toRadians(heading.toDouble())).toFloat() // Negate for correct screen coordinates

            val newPosition = Position(
                x = (currentPos.x + deltaX).toInt(),
                y = (currentPos.y + deltaY).toInt()
            )

            val newStepCount = currentState.stepCount + 1
            val newDistance = newStepCount * stepLength

            currentState.copy(
                currentPosition = newPosition,
                pathHistory = currentState.pathHistory + newPosition,
                xPosition = newPosition.x.toFloat(),
                yPosition = newPosition.y.toFloat(),
                stepCount = newStepCount,
                distance = newDistance
            )
        }
    }

    fun startTracking() {
        _uiState.update { it.copy(isTracking = true) }
    }

    fun stopTracking() {
        _uiState.update { it.copy(isTracking = false) }
    }

    fun handleBarcodeScan(barcode: String, position: Position) {
        _uiState.update { currentState ->
            currentState.copy(
                lastBarcodeScan = barcode,
                lastBarcodePosition = position,
                currentPosition = position,
                pathHistory = currentState.pathHistory + position
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager?.unregisterListener(sensorEventListener)
        sensorEventListener = null
        sensorManager = null
    }
}
