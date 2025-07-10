package com.example.sensortracking.ui.screens.track

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sensortracking.data.PDRConfig
import com.example.sensortracking.data.Position
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

    private var pdrProcessor: PDRProcessor? = null
    private var pdrSensorManager: PDRSensorManager? = null
    
    private var isInitialized = false
    
    fun onZoomChange(newZoom: Float) {
        _uiState.update { it.copy(zoom = newZoom.coerceIn(0.5f, 5f)) }
    }
    
    fun setArea(length: Float, width: Float) {
        _uiState.update { it.copy(area = Area(length, width)) }
        initializePDRProcessor()
    }
    
    fun setInitialPosition(x: Float, y: Float) {
        val clampedX = x.coerceIn(0f, _uiState.value.area.length)
        val clampedY = y.coerceIn(0f, _uiState.value.area.width)
        pdrSensorManager?.setInitialPosition(Position(clampedX, clampedY))
        
        pdrProcessor?.getCurrentPDRData()?.let { pdrData ->
            _uiState.update { it.copy(pdrData = pdrData) }
        }
    }

    fun onCalibratePosition() {
        val currentPos = _uiState.value.pdrData?.position ?: Position(0f, 0f)
        pdrSensorManager?.calibratePosition(currentPos)
    }

    fun setCalibratedPosition(x: Float, y: Float) {
        val clampedX = x.coerceIn(0f, _uiState.value.area.length)
        val clampedY = y.coerceIn(0f, _uiState.value.area.width)
        pdrSensorManager?.calibratePosition(Position(clampedX, clampedY))
    }
    
    fun initializeSensors(context: Context) {
        if (isInitialized) return
        
        viewModelScope.launch {
            try {
                initializePDRProcessor()
                pdrSensorManager = PDRSensorManager(context, pdrProcessor!!)
                val sensorsAvailable = pdrSensorManager!!.initializeSensors()
                if (sensorsAvailable) {
                    val sensorAvailability = pdrSensorManager!!.getSensorAvailability()
                    _uiState.update {
                        it.copy(
                            hasAccelerometer = sensorAvailability.hasAccelerometer,
                            hasGyroscope = sensorAvailability.hasGyroscope,
                            hasMagnetometer = sensorAvailability.hasMagnetometer,
                            hasRotationVector = sensorAvailability.hasRotationVector,
                        )
                    }
                    observePDRData()
                    isInitialized = true
                } else {
                    _uiState.update { it.copy(isError = true, errorMessage = "Required sensors not available") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isError = true, errorMessage = "Failed to initialize sensors: ${e.message}") }
            }
        }
    }
    
    private fun initializePDRProcessor() {
        val area = _uiState.value.area
        val bounds = PDRProcessor.AreaBounds(minX = 0f, maxX = area.length, minY = 0f, maxY = area.width)
        pdrProcessor = PDRProcessor(config = _uiState.value.pdrConfig, areaBounds = bounds)
    }
    
    private fun observePDRData() {
        viewModelScope.launch {
            pdrSensorManager?.pdrData?.collect { pdrData -> _uiState.update { it.copy(pdrData = pdrData, isTracking = pdrData.isTracking) } }
        }
    }
    
    fun onStartTracking() {
        if (!_uiState.value.canStartTracking) return
        
        // If have existing PDR data, resume tracking
        if (_uiState.value.pdrData != null) {
            pdrSensorManager?.resumeTracking()
        } else {
            val currentPos = _uiState.value.pdrData?.position ?: Position(0f, 0f)
            pdrSensorManager?.startTracking(currentPos)
        }
        
        _uiState.update { it.copy(isTracking = true) }
    }
    
    fun onStopTracking() {
        pdrSensorManager?.stopTracking()
        _uiState.update { it.copy(isTracking = false) }
    }
    
    fun onStartNewTracking() {
        onStopTracking()
        pdrProcessor?.reset()
        _uiState.update { 
            it.copy(
                pdrData = null,
                isTracking = false
            )
        }
    }
    
    fun saveTracking() {
        // TODO: Implement save logic (future: show name/desc dialog)
        // Save current tracking data to storage
    }
    
    fun updatePDRConfig(newConfig: PDRConfig) {
        _uiState.update { it.copy(pdrConfig = newConfig) }
        pdrSensorManager?.updateConfig(newConfig)
        if (isInitialized) { initializePDRProcessor() }
    }
    
    fun clearError() {
        _uiState.update { it.copy(isError = false, errorMessage = null) }
    }
    
    fun getPathHistory(): List<Position> {
        return pdrSensorManager?.getPathHistory() ?: emptyList()
    }
    
    override fun onCleared() {
        super.onCleared()
        pdrSensorManager?.cleanup()
        pdrSensorManager = null
        pdrProcessor = null
    }
}