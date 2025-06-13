package com.example.sensortracking.data

data class SensorTrackingUiState (
    val currentXPos: Int = 0,
    val currentYPos: Int = 0,
    val currentZPos: Int = 0,
    val currentSpeed: Int = 0,
    val directionInDegrees: Int = 0,
)