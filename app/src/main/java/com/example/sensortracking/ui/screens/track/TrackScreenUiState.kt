package com.example.sensortracking.ui.screens.track

import com.example.sensortracking.data.PDRData
import com.example.sensortracking.data.Position
import com.example.sensortracking.data.PDRConfig

data class Area(val length: Float = 10f, val width: Float = 10f)

data class InitialPosition(
    val x: Float = 0f,
    val y: Float = 0f
)

data class TrackScreenUiState(
    val zoom: Float = 1f,
    val area: Area = Area(),
    val floorPlan: Any? = null, // Placeholder for floor plan image or grid data
    
    // PDR Data
    val pdrData: PDRData? = null,
    val isTracking: Boolean = false,
    
    // PDR Configuration
    val pdrConfig: PDRConfig = PDRConfig(),
    
    // Sensor availability
    val hasAccelerometer: Boolean = false,
    val hasGyroscope: Boolean = false,
    val hasMagnetometer: Boolean = false,
    val hasRotationVector: Boolean = false,
    
    // Error states
    val errorMessage: String? = null,
    val isError: Boolean = false
) {
    val currentPosition: Position
        get() = pdrData?.position ?: Position(0f, 0f)
    
    val currentHeading: Float
        get() = pdrData?.currentHeading?.heading ?: 0f
    
    val stepCount: Int
        get() = pdrData?.stepCount ?: 0
    
    val totalDistance: Float
        get() = pdrData?.totalDistance ?: 0f
    
    val confidence: Float
        get() = pdrData?.confidence ?: 0f
    
    val canStartTracking: Boolean
        get() = !isTracking
} 