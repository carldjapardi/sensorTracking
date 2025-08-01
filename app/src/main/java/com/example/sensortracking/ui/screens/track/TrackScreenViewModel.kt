package com.example.sensortracking.ui.screens.track

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sensortracking.data.PDRConfig
import com.example.sensortracking.data.Position
import com.example.sensortracking.data.PathSegment
import com.example.sensortracking.data.WarehouseMap
import com.example.sensortracking.sensor.pdr.PDRProcessor
import com.example.sensortracking.sensor.pdr.PDRSensorManager
import com.example.sensortracking.sensor.calibration.CalibrationType
import com.example.sensortracking.sensor.log.LogAnalyzer
import com.example.sensortracking.sensor.log.PathReconstructor
import com.example.sensortracking.sensor.pdr.WarehouseMapProcessor
import com.example.sensortracking.util.SensorDataLogger
import com.example.sensortracking.nn.NeuralPathProcessor
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
    private var logAnalyzer: LogAnalyzer? = null
    private var pathReconstructor: PathReconstructor? = null
    private var warehouseMapProcessor: WarehouseMapProcessor? = null
    
    private var isInitialized = false
    
    fun onZoomChange(newZoom: Float) {
        _uiState.update { it.copy(zoom = newZoom.coerceIn(0.5f, 3.0f)) }
    }
    
    fun setArea(length: Float, width: Float) {
        _uiState.update { it.copy(area = Area(length, width)) }
        initializePDRProcessor()
    }
    
    fun setInitialPosition(x: Float, y: Float) {
        val warehouseMap = _uiState.value.warehouseMap
        val maxX = warehouseMap?.width?.toFloat() ?: _uiState.value.area.length
        val maxY = warehouseMap?.height?.toFloat() ?: _uiState.value.area.width
        
        val clampedX = x.coerceIn(0f, maxX)
        val clampedY = y.coerceIn(0f, maxY)
        pdrSensorManager?.setInitialPosition(Position(clampedX, clampedY))
        
        pdrProcessor?.getCurrentPDRData()?.let { pdrData ->
            _uiState.update { it.copy(pdrData = pdrData) }
        }
    }

    fun setCalibratedPosition(x: Float, y: Float, calibrationType: CalibrationType) {
        val warehouseMap = _uiState.value.warehouseMap
        val maxX = warehouseMap?.width?.toFloat() ?: _uiState.value.area.length
        val maxY = warehouseMap?.height?.toFloat() ?: _uiState.value.area.width
        
        val clampedX = x.coerceIn(0f, maxX)
        val clampedY = y.coerceIn(0f, maxY)
        pdrSensorManager?.calibratePosition(Position(clampedX, clampedY), calibrationType)
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
        val warehouseMap = _uiState.value.warehouseMap
        
        // Check for floor plan if selected, else normal grid walls
        val bounds = if (warehouseMap != null) {
            PDRProcessor.AreaBounds(
                minX = 0f, maxX = warehouseMap.width.toFloat(),
                minY = 0f, maxY = warehouseMap.height.toFloat()
            )
        } else {
            PDRProcessor.AreaBounds(
                minX = 0f, maxX = area.length,
                minY = 0f, maxY = area.width
            )
        }
        
        pdrProcessor = PDRProcessor(
            config = _uiState.value.pdrConfig, 
            areaBounds = bounds,
            warehouseMap = warehouseMap
        )
        logAnalyzer = LogAnalyzer(_uiState.value.pdrConfig.headingTolerance)
        pathReconstructor = PathReconstructor()
        warehouseMapProcessor = WarehouseMapProcessor()
    }
    
    private fun observePDRData() {
        viewModelScope.launch {
            pdrSensorManager?.pdrData?.collect { pdrData -> _uiState.update { it.copy(pdrData = pdrData, isTracking = pdrData.isTracking) } }
        }
    }
    
    fun onStartTracking() {
        if (!_uiState.value.canStartTracking) return
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
    
    fun pauseTracking() {
        pdrSensorManager?.stopTracking()
    }
    
    fun resumeTracking() {
        pdrSensorManager?.resumeTracking()
        _uiState.update { it.copy(isTracking = true) }
    }
    
    fun onStartNewTracking() {
        onStopTracking()
        pdrProcessor?.reset()
        _uiState.update { it.copy(pdrData = null, isTracking = false) }
    }
    
    fun saveTracking(context: Context, sessionName: String, runNeural: Boolean): Boolean {
        android.util.Log.d("TrackScreenViewModel", "Saving tracking session: $sessionName, runNeural: $runNeural")
        
        val sensorDataLogger = pdrSensorManager?.getSensorDataLogger()
        if (sensorDataLogger == null) {
            android.util.Log.e("TrackScreenViewModel", "SensorDataLogger is null")
            return false
        }
        
        val pathHistory = getPathHistory()
        val pathSegments = getPathSegments()
        
        android.util.Log.d("TrackScreenViewModel", "Path history size: ${pathHistory.size}, Path segments: ${pathSegments.size}")
        
        val session = sensorDataLogger.getCurrentSession(
            sessionName = sessionName,
            area = _uiState.value.area,
            warehouseMap = _uiState.value.warehouseMap,
            pdrConfig = _uiState.value.pdrConfig,
            pathHistory = pathHistory,
            pathSegments = pathSegments
        )

        val nnPath = if (runNeural) {
            android.util.Log.d("TrackScreenViewModel", "Starting neural network processing")
            try {
                val modelAssetPath = "path_model.onnx"
                val assetManager = context.assets
                val modelExists = try {
                    assetManager.open(modelAssetPath).use { true }
                } catch (e: Exception) {
                    android.util.Log.e("TrackScreenViewModel", "Model file not found: $modelAssetPath")
                    false
                }
                
                if (!modelExists) {
                    android.util.Log.e("TrackScreenViewModel", "Neural network model not found")
                    _uiState.update { it.copy(
                        isError = true,
                        errorMessage = "Neural network model not found. Please ensure 'path_model.onnx' is in the assets folder."
                    ) }
                    return false
                }
                
                android.util.Log.d("TrackScreenViewModel", "Model file found, creating NeuralPathProcessor")
                val processor = NeuralPathProcessor(context, modelAssetPath)
                
                // Try to calibrate if we have enough stationary data
                val calibrationData = sensorDataLogger.getCalibrationData()
                if (calibrationData != null) {
                    android.util.Log.d("TrackScreenViewModel", "Attempting calibration with ${calibrationData.accelerometerSampleCount} samples")
                    val calibrationSuccess = processor.calibrate(calibrationData, minSamples = 50) // Reduced from 100 to 50
                    android.util.Log.d("TrackScreenViewModel", "Calibration result: $calibrationSuccess")
                } else {
                    android.util.Log.d("TrackScreenViewModel", "No calibration data available, using auto-calibration")
                }
                
                val result = processor.predictPath(session.rawSensorData)
                android.util.Log.d("TrackScreenViewModel", "Neural network processing completed, generated ${result.size} path points")
                result
            } catch (e: Exception) {
                android.util.Log.e("TrackScreenViewModel", "Neural network processing failed", e)
                _uiState.update { it.copy(
                    isError = true,
                    errorMessage = "Neural network processing failed: ${e.message}"
                ) }
                null
            }
        } else {
            android.util.Log.d("TrackScreenViewModel", "Neural network processing skipped")
            null
        }

        android.util.Log.d("TrackScreenViewModel", "Calling saveToFile with nnPath size: ${nnPath?.size ?: 0}")
        val saveResult = sensorDataLogger.saveToFile(context, sessionName, session, nnPath)
        android.util.Log.d("TrackScreenViewModel", "Save result: $saveResult")
        return saveResult
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
    
    fun getPathSegments(): List<PathSegment> {
        val pathHistory = getPathHistory()
        return logAnalyzer?.analyzePath(pathHistory) ?: emptyList()
    }

    fun loadWarehouseMap(warehouseMap: WarehouseMap) {
        _uiState.update { it.copy(warehouseMap = warehouseMap) }
        pdrProcessor?.setWarehouseMap(warehouseMap)
    }
    
    fun clearWarehouseMap() {
        _uiState.update { it.copy(warehouseMap = null) }
        pdrProcessor?.setWarehouseMap(null)
    }
    
    fun updatePathSegment(index: Int, newSegment: PathSegment) {
        val segments = getPathSegments().toMutableList()
        if (index in segments.indices) {
            segments[index] = newSegment
            val newPath = pathReconstructor?.reconstructPath(segments, Position(0f, 0f)) ?: emptyList()
            pdrSensorManager?.updatePathHistory(newPath)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        pdrSensorManager?.cleanup()
        pdrSensorManager = null
        pdrProcessor = null
    }
}