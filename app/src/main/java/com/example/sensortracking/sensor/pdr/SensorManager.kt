package com.example.sensortracking.sensor.pdr

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Android sensors and feeds data to PDR processor
 */
class PDRSensorManager(
    private val context: Context,
    private val pdrProcessor: PDRProcessor
) : SensorEventListener {
    
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    // Sensor state
    private var accelerometer: FloatArray = FloatArray(3)
    private var gyroscope: FloatArray = FloatArray(3)
    private var magnetometer: FloatArray = FloatArray(3)
    private var rotationVector: FloatArray = FloatArray(4)
    
    // Sensor availability
    private var hasAccelerometer = false
    private var hasGyroscope = false
    private var hasMagnetometer = false
    private var hasRotationVector = false
    
    // PDR data flow
    private val _pdrData = MutableStateFlow(pdrProcessor.getCurrentPDRData())
    val pdrData: StateFlow<com.example.sensortracking.data.PDRData> = _pdrData.asStateFlow()
    
    companion object {
        private const val TAG = "PDRSensorManager"
    }
    
    /**
     * Initialize and register sensors
     */
    fun initializeSensors(): Boolean {
        Log.d(TAG, "Initializing sensors...")
        
        // Get sensor instances
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        
        // Register sensors
        hasAccelerometer = accelerometerSensor != null
        hasGyroscope = gyroscopeSensor != null
        hasMagnetometer = magnetometerSensor != null
        hasRotationVector = rotationVectorSensor != null
        
        if (hasAccelerometer) {
            sensorManager.registerListener(
                this, 
                accelerometerSensor, 
                SensorManager.SENSOR_DELAY_GAME
            )
            Log.d(TAG, "Accelerometer registered")
        } else {
            Log.e(TAG, "Accelerometer not available")
        }
        
        if (hasGyroscope) {
            sensorManager.registerListener(
                this, 
                gyroscopeSensor, 
                SensorManager.SENSOR_DELAY_GAME
            )
            Log.d(TAG, "Gyroscope registered")
        } else {
            Log.e(TAG, "Gyroscope not available")
        }
        
        if (hasMagnetometer) {
            sensorManager.registerListener(
                this, 
                magnetometerSensor, 
                SensorManager.SENSOR_DELAY_GAME
            )
            Log.d(TAG, "Magnetometer registered")
        } else {
            Log.e(TAG, "Magnetometer not available")
        }
        
        if (hasRotationVector) {
            sensorManager.registerListener(
                this,
                rotationVectorSensor,
                SensorManager.SENSOR_DELAY_GAME
            )
            Log.d(TAG, "Rotation Vector sensor registered")
        } else {
            Log.e(TAG, "Rotation Vector sensor not available")
        }
        
        val allSensorsAvailable = hasAccelerometer && hasGyroscope && hasMagnetometer && hasRotationVector
        Log.d(TAG, "Sensor initialization complete. All sensors available: $allSensorsAvailable")
        
        return allSensorsAvailable
    }
    
    /**
     * Handle sensor events
     */
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometer, 0, 3)
                processSensorData()
            }
            Sensor.TYPE_GYROSCOPE -> {
                System.arraycopy(event.values, 0, gyroscope, 0, 3)
                processSensorData()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometer, 0, 3)
                processSensorData()
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                System.arraycopy(event.values, 0, rotationVector, 0, 4)
                pdrProcessor.updateRotationVector(rotationVector)
                processSensorData()
            }
        }
    }
    
    /**
     * Process sensor data and update PDR
     */
    private fun processSensorData() {
        val timestamp = System.currentTimeMillis()
        
        // Process with PDR
        val pdrResult = pdrProcessor.processSensorData(
            accelerometer, gyroscope, magnetometer, timestamp
        )
        
        // Update PDR data flow
        if (pdrResult != null) {
            _pdrData.value = pdrResult
        }
    }
    
    /**
     * Start PDR tracking
     */
    fun startTracking(initialPosition: com.example.sensortracking.data.Position) {
        Log.d(TAG, "Starting PDR tracking at position: $initialPosition")
        pdrProcessor.startTracking(initialPosition)
    }
    
    /**
     * Stop PDR tracking
     */
    fun stopTracking() {
        Log.d(TAG, "Stopping PDR tracking")
        pdrProcessor.stopTracking()
    }
    
    /**
     * Set initial position
     */
    fun setInitialPosition(position: com.example.sensortracking.data.Position) {
        pdrProcessor.setInitialPosition(position)
        Log.d(TAG, "Initial position set to: $position")
    }
    
    /**
     * Get current PDR data
     */
    fun getCurrentPDRData(): com.example.sensortracking.data.PDRData {
        return pdrProcessor.getCurrentPDRData()
    }
    
    /**
     * Get path history
     */
    fun getPathHistory(): List<com.example.sensortracking.data.Position> {
        return pdrProcessor.getPathHistory()
    }
    
    /**
     * Check if sensors are available
     */
    fun getSensorAvailability(): SensorAvailability {
        return SensorAvailability(
            hasAccelerometer = hasAccelerometer,
            hasGyroscope = hasGyroscope,
            hasMagnetometer = hasMagnetometer,
            hasRotationVector = hasRotationVector
        )
    }
    
    /**
     * Sensor availability data class
     */
    data class SensorAvailability(
        val hasAccelerometer: Boolean,
        val hasGyroscope: Boolean,
        val hasMagnetometer: Boolean,
        val hasRotationVector: Boolean
    ) {
        val allSensorsAvailable: Boolean
            get() = hasAccelerometer && hasGyroscope && hasMagnetometer && hasRotationVector
    }
    
    /**
     * Handle sensor accuracy changes
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        when (accuracy) {
            SensorManager.SENSOR_STATUS_UNRELIABLE -> {
                Log.w(TAG, "Sensor accuracy unreliable: ${sensor?.name}")
            }
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up sensor manager...")
        sensorManager.unregisterListener(this)
        pdrProcessor.reset()
    }
} 