package com.example.sensortracking.sensor.pdr

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.example.sensortracking.data.SensorData
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
    
    // Sensor availability
    private var hasAccelerometer = false
    private var hasGyroscope = false
    private var hasMagnetometer = false
    
    // Sensor data flow
    private val _sensorData = MutableStateFlow<SensorData?>(null)
    val sensorData: StateFlow<SensorData?> = _sensorData.asStateFlow()
    
    // PDR data flow
    private val _pdrData = MutableStateFlow(pdrProcessor.getCurrentPDRData())
    val pdrData: StateFlow<com.example.sensortracking.data.PDRData> = _pdrData.asStateFlow()
    
    // Calibration state
    private var isCalibrating = false
    private var calibrationProgress = 0f
    
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
        
        // Register sensors
        hasAccelerometer = accelerometerSensor != null
        hasGyroscope = gyroscopeSensor != null
        hasMagnetometer = magnetometerSensor != null
        
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
        
        val allSensorsAvailable = hasAccelerometer && hasGyroscope && hasMagnetometer
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
        }
    }
    
    /**
     * Process sensor data and update PDR
     */
    private fun processSensorData() {
        val timestamp = System.currentTimeMillis()
        
        // Create sensor data object with current values
        val sensorData = SensorData(
            accelerometer = accelerometer.clone(),
            gyroscope = gyroscope.clone(),
            magnetometer = magnetometer.clone(),
            timestamp = timestamp
        )
        
        // Update sensor data flow
        _sensorData.value = sensorData
        
        // Always process with PDR for calibration
        val pdrResult = pdrProcessor.processSensorData(
            accelerometer, gyroscope, magnetometer, timestamp
        )
        
        // Update PDR data flow
        if (pdrResult != null) {
            _pdrData.value = pdrResult
            Log.d(TAG, "PDR data updated - Position: ${pdrResult.position}, Steps: ${pdrResult.stepCount}, Distance: ${pdrResult.totalDistance}")
        }
        
        // Update calibration progress
        updateCalibrationProgress()
    }
    
    /**
     * Update calibration progress
     */
    private fun updateCalibrationProgress() {
        if (isCalibrating) {
            calibrationProgress = pdrProcessor.getMagnetometerCalibrationProgress()
            
            if (calibrationProgress >= 1.0f) {
                isCalibrating = false
                Log.d(TAG, "Magnetometer calibration complete")
            }
        }
    }
    
    /**
     * Start magnetometer calibration
     */
    fun startCalibration() {
        isCalibrating = true
        calibrationProgress = 0f
        Log.d(TAG, "Starting magnetometer calibration...")
    }
    
    /**
     * Get calibration progress
     */
    fun getCalibrationProgress(): Float = calibrationProgress
    
    /**
     * Check if calibration is complete
     */
    fun isCalibrationComplete(): Boolean = pdrProcessor.isMagnetometerCalibrated()
    
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
            hasMagnetometer = hasMagnetometer
        )
    }
    
    /**
     * Sensor availability data class
     */
    data class SensorAvailability(
        val hasAccelerometer: Boolean,
        val hasGyroscope: Boolean,
        val hasMagnetometer: Boolean
    ) {
        val allSensorsAvailable: Boolean
            get() = hasAccelerometer && hasGyroscope && hasMagnetometer
    }
    
    /**
     * Handle sensor accuracy changes
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        when (accuracy) {
            SensorManager.SENSOR_STATUS_UNRELIABLE -> {
                Log.w(TAG, "Sensor accuracy unreliable: ${sensor?.name}")
                // Could trigger recalibration here
            }
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> {
                Log.w(TAG, "Sensor accuracy low: ${sensor?.name}")
            }
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> {
                Log.d(TAG, "Sensor accuracy medium: ${sensor?.name}")
            }
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> {
                Log.d(TAG, "Sensor accuracy high: ${sensor?.name}")
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