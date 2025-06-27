package com.example.sensortracking.ui.screens.track

// Data class for area
data class Area(val length: Float = 10f, val width: Float = 10f)

// Data class for IMU sensor data
data class IMUData(
    val accelerometer: String = "-",
    val gyroscope: String = "-",
    val magnetometer: String = "-"
)

// UI State for TrackScreen
data class TrackScreenUiState(
    val zoom: Float = 1f,
    val area: Area = Area(),
    val floorPlan: Any? = null, // Placeholder for floor plan image or grid data
    val userPosition: Pair<Float, Float> = 0f to 0f,
    val imuData: IMUData = IMUData()
) 