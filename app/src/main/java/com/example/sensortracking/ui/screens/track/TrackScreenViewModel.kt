package com.example.sensortracking.ui.screens.track

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sensortracking.data.PDRConfig
import com.example.sensortracking.data.Position
import com.example.sensortracking.data.StrideEstimationMethod
import com.example.sensortracking.sensor.pdr.PDRProcessor
import com.example.sensortracking.sensor.pdr.PDRSensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TrackScreenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TrackScreenUiState())
    val uiState: StateFlow<TrackScreenUiState> = _uiState.asStateFlow()
    
    // PDR Components
    private var pdrProcessor: PDRProcessor? = null
    private var pdrSensorManager: PDRSensorManager? = null
    
    // Initialization state
    private var isInitialized = false
    
    fun onZoomChange(newZoom: Float) {
        _uiState.update { it.copy(zoom = newZoom.coerceIn(0.5f, 5f)) }
    }
    
    fun setArea(length: Float, width: Float) {
        _uiState.update { it.copy(area = Area(length, width)) }
        
        // Reinitialize PDR processor with new area bounds
        initializePDRProcessor()
    }
    
    fun showInitialPositionDialog() {
        _uiState.update { it.copy(showInitialPositionDialog = true) }
    }
    
    fun hideInitialPositionDialog() {
        _uiState.update { it.copy(showInitialPositionDialog = false) }
    }
    
    fun setInitialPosition(x: Float, y: Float) {
        val clampedX = x.coerceIn(0f, _uiState.value.area.length)
        val clampedY = y.coerceIn(0f, _uiState.value.area.width)
        
        _uiState.update { 
            it.copy(
                initialPosition = InitialPosition(clampedX, clampedY),
                showInitialPositionDialog = false
            )
        }
        
        // Set initial position in PDR processor
        pdrProcessor?.setInitialPosition(Position(clampedX, clampedY))
        pdrSensorManager?.setInitialPosition(Position(clampedX, clampedY))
    }
    
    fun initializeSensors(context: Context) {
        if (isInitialized) return
        
        viewModelScope.launch {
            try {
                // Initialize PDR processor
                initializePDRProcessor()
                
                // Initialize sensor manager
                pdrSensorManager = PDRSensorManager(context, pdrProcessor!!)
                
                // Initialize sensors
                val sensorsAvailable = pdrSensorManager!!.initializeSensors()
                
                if (sensorsAvailable) {
                    // Start calibration
                    pdrSensorManager!!.startCalibration()
                    
                    // Update UI state
                    _uiState.update { 
                        it.copy(
                            hasAccelerometer = true,
                            hasGyroscope = true,
                            hasMagnetometer = true,
                            isCalibrating = true
                        )
                    }
                    
                    // Observe PDR data
                    observePDRData()
                    
                    isInitialized = true
                } else {
                    _uiState.update { 
                        it.copy(
                            isError = true,
                            errorMessage = "Required sensors not available"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isError = true,
                        errorMessage = "Failed to initialize sensors: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun initializePDRProcessor() {
        val area = _uiState.value.area
        val bounds = PDRProcessor.AreaBounds(
            minX = 0f,
            maxX = area.length,
            minY = 0f,
            maxY = area.width
        )
        
        pdrProcessor = PDRProcessor(
            config = _uiState.value.pdrConfig,
            areaBounds = bounds
        )
    }
    
    private fun observePDRData() {
        viewModelScope.launch {
            pdrSensorManager?.pdrData?.collect { pdrData ->
                val strideAlgorithm = pdrProcessor?.getCurrentStrideMethod() ?: "Weinberg"
                val isCalibrationComplete = pdrSensorManager?.isCalibrationComplete() ?: false
                val calibrationProgress = pdrSensorManager?.getCalibrationProgress() ?: 0f
                
                _uiState.update { 
                    it.copy(
                        pdrData = pdrData,
                        isTracking = pdrData.isTracking,
                        isCalibrating = !isCalibrationComplete,
                        calibrationProgress = calibrationProgress,
                        strideAlgorithm = strideAlgorithm
                    )
                }
            }
        }
        
        // Observe sensor data for UI display
        viewModelScope.launch {
            pdrSensorManager?.sensorData?.collect { sensorData ->
                if (sensorData != null) {
                    _uiState.update { 
                        it.copy(
                            imuData = IMUData(
                                accelerometer = "[${sensorData.accelerometer[0].toFixed(2)}, ${sensorData.accelerometer[1].toFixed(2)}, ${sensorData.accelerometer[2].toFixed(2)}]",
                                gyroscope = "[${sensorData.gyroscope[0].toFixed(2)}, ${sensorData.gyroscope[1].toFixed(2)}, ${sensorData.gyroscope[2].toFixed(2)}]",
                                magnetometer = "[${sensorData.magnetometer[0].toFixed(2)}, ${sensorData.magnetometer[1].toFixed(2)}, ${sensorData.magnetometer[2].toFixed(2)}]"
                            )
                        )
                    }
                }
            }
        }
    }
    
    fun onStartTracking() {
        if (!_uiState.value.canStartTracking) return
        
        val initialPos = _uiState.value.initialPosition
        val position = Position(initialPos.x, initialPos.y)
        
        pdrSensorManager?.startTracking(position)
        
        _uiState.update { 
            it.copy(
                isTracking = true,
                showInitialPositionDialog = false
            )
        }
    }
    
    fun onStopTracking() {
        pdrSensorManager?.stopTracking()
        
        _uiState.update { 
            it.copy(isTracking = false)
        }
    }
    
    fun onScanBarcode() {
        // TODO: Implement scan barcode logic
        // This could update the current position based on barcode location
    }
    
    fun saveTracking() {
        // TODO: Implement save logic (future: show name/desc dialog)
        // Save current tracking data to storage
    }
    
    fun newTracking() {
        // Stop current tracking
        onStopTracking()
        
        // Reset PDR processor
        pdrProcessor?.reset()
        
        // Reset UI state
        _uiState.update { 
            it.copy(
                pdrData = null,
                isTracking = false,
                showInitialPositionDialog = true
            )
        }
    }
    
    fun updatePDRConfig(newConfig: PDRConfig) {
        _uiState.update { it.copy(pdrConfig = newConfig) }
        pdrProcessor?.updateConfig(newConfig)
    }
    
    fun clearError() {
        _uiState.update { 
            it.copy(
                isError = false,
                errorMessage = null
            )
        }
    }
    
    fun getPathHistory(): List<Position> {
        return pdrSensorManager?.getPathHistory() ?: emptyList()
    }
    
    fun cycleStrideAlgorithm() {
        val currentMethod = pdrProcessor?.getCurrentStrideMethod() ?: "Weinberg"
        val nextMethod = when (currentMethod) {
            "Weinberg" -> StrideEstimationMethod.KIM
            "Kim" -> StrideEstimationMethod.ADAPTIVE
            "Adaptive" -> StrideEstimationMethod.WEINBERG
            else -> StrideEstimationMethod.WEINBERG
        }
        
        pdrProcessor?.setStrideEstimationMethod(nextMethod)
        
        // Update UI state immediately
        _uiState.update { 
            it.copy(strideAlgorithm = when (nextMethod) {
                StrideEstimationMethod.WEINBERG -> "Weinberg"
                StrideEstimationMethod.KIM -> "Kim"
                StrideEstimationMethod.FIXED_LENGTH -> "Fixed Length"
                StrideEstimationMethod.ADAPTIVE -> "Adaptive"
            })
        }
    }
    
    fun isCalibrationComplete(): Boolean {
        return pdrSensorManager?.isCalibrationComplete() ?: false
    }
    
    override fun onCleared() {
        super.onCleared()
        pdrSensorManager?.cleanup()
        pdrSensorManager = null
        pdrProcessor = null
    }
}

private fun Float.toFixed(digits: Int): String {
    return "%.${digits}f".format(this)
}