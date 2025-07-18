package com.example.sensortracking.sensor.pdr

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.example.sensortracking.data.PDRConfig
import com.example.sensortracking.data.PDRData
import com.example.sensortracking.data.Position
import com.example.sensortracking.data.SensorType
import com.example.sensortracking.sensor.calibration.CalibrationType
import com.example.sensortracking.util.SensorDataLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Android sensors and feeds data to PDR processor
 */
class PDRSensorManager(private val context: Context, private val pdrProcessor: PDRProcessor) : SensorEventListener {
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    // Only using accelerometer and rotation vector for PDR, rotation vector is used for heading and will initialize gyroscope and magnetometer
    private var accelerometer: FloatArray = FloatArray(3)
    private var rotationVector: FloatArray = FloatArray(4)

    private var hasAccelerometer = false
    private var hasGyroscope = false
    private var hasMagnetometer = false
    private var hasRotationVector = false
    
    private val _pdrData = MutableStateFlow(pdrProcessor.getCurrentPDRData())
    val pdrData: StateFlow<PDRData> = _pdrData.asStateFlow()
    
    private val sensorDataLogger = SensorDataLogger()

    fun initializeSensors(): Boolean {
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        
        hasAccelerometer = accelerometerSensor != null
        hasGyroscope = gyroscopeSensor != null
        hasMagnetometer = magnetometerSensor != null
        hasRotationVector = rotationVectorSensor != null

        registerSensorIfAvailable(hasAccelerometer, accelerometerSensor)
        registerSensorIfAvailable(hasGyroscope, gyroscopeSensor)
        registerSensorIfAvailable(hasMagnetometer, magnetometerSensor)
        registerSensorIfAvailable(hasRotationVector,  rotationVectorSensor)

        val allSensorsAvailable = hasAccelerometer && hasGyroscope && hasMagnetometer && hasRotationVector
        return allSensorsAvailable
    }

    private fun registerSensorIfAvailable(sensorAvailable: Boolean, sensorType: Sensor?) {
        if (sensorAvailable) {
            sensorManager.registerListener(this, sensorType, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometer, 0, 3)
                // Log raw accelerometer data
                sensorDataLogger.logSensorData(
                    event.timestamp / 1000000, // Convert nanoseconds to milliseconds
                    SensorType.ACCELEROMETER,
                    event.values,
                    event.accuracy
                )
                processSensorData()
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                System.arraycopy(event.values, 0, rotationVector, 0, 4)
                pdrProcessor.updateRotationVector(rotationVector)
                // Log raw rotation vector data
                sensorDataLogger.logSensorData(
                    event.timestamp / 1000000, // Convert nanoseconds to milliseconds
                    SensorType.ROTATION_VECTOR,
                    event.values,
                    event.accuracy
                )
                processSensorData()
            }
        }
    }

    private fun processSensorData() {
        val timestamp = System.currentTimeMillis()
        val pdrResult = pdrProcessor.processSensorData(
            accelerometer, timestamp
        )
        _pdrData.value = pdrResult
        
        // Log processed PDR data
        sensorDataLogger.logPDRData(
            com.example.sensortracking.data.PDRDataPoint(
                timestamp = timestamp,
                position = pdrResult.position,
                stepCount = pdrResult.stepCount,
                totalDistance = pdrResult.totalDistance,
                currentHeading = pdrResult.currentHeading,
                lastStep = pdrResult.lastStep,
                confidence = pdrResult.confidence
            )
        )
    }

    fun startTracking(initialPosition: Position) { 
        pdrProcessor.startTracking(initialPosition)
        sensorDataLogger.startLogging()
    }

    fun resumeTracking() { 
        pdrProcessor.resumeTracking()
        sensorDataLogger.startLogging()
    }

    fun stopTracking() { 
        pdrProcessor.stopTracking()
        sensorDataLogger.stopLogging()
    }

    fun setInitialPosition(position: Position) { pdrProcessor.setInitialPosition(position) }

    fun calibratePosition(position: Position, calibrationType: CalibrationType) { pdrProcessor.calibratePosition(position, calibrationType) }

    fun updateConfig(newConfig: PDRConfig) { pdrProcessor.updateConfig(newConfig) }

    fun getPathHistory(): List<Position> { return pdrProcessor.getPathHistory() }
    
    fun updatePathHistory(newPath: List<Position>) { pdrProcessor.updatePathHistory(newPath) }
    
    fun getSensorDataLogger(): SensorDataLogger = sensorDataLogger

    fun getSensorAvailability(): SensorAvailability {
        return SensorAvailability(
            hasAccelerometer = hasAccelerometer,
            hasGyroscope = hasGyroscope,
            hasMagnetometer = hasMagnetometer,
            hasRotationVector = hasRotationVector
        )
    }

    data class SensorAvailability(
        val hasAccelerometer: Boolean,
        val hasGyroscope: Boolean,
        val hasMagnetometer: Boolean,
        val hasRotationVector: Boolean
    )

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        when (accuracy) {
            SensorManager.SENSOR_STATUS_UNRELIABLE -> {
                Log.w("PDRSensorManager","Sensor accuracy unreliable: ${sensor?.name}")
            }
        }
    }

    fun cleanup() {
        sensorManager.unregisterListener(this)
        pdrProcessor.reset()
    }
} 