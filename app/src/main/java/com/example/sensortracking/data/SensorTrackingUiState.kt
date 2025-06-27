package com.example.sensortracking.data

import androidx.compose.ui.geometry.Offset

data class SensorTrackingUiState(
    val isTracking: Boolean = false,
    val currentPosition: Position = Position(0, 0),
    val pathHistory: List<Position> = emptyList(),
    val lastBarcodeScan: String? = null,
    val lastBarcodePosition: Position? = null,
    val driftError: Float = 0f,
    val sensorData: SensorData = SensorData(),
    val xPosition: Float = 0f,
    val yPosition: Float = 0f,
    val xAcceleration: Float = 0f,
    val yAcceleration: Float = 0f,
    val zAcceleration: Float = 0f,
    val direction: Float = 0f,
    val distance: Float = 0f,
    val stepCount: Int = 0
)

data class Position(
    val x: Int,
    val y: Int
) {
    fun toOffset(): Offset = Offset(x.toFloat(), y.toFloat())
}

data class SensorData(
    val accelerometer: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val gyroscope: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val magnetometer: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val heading: Float = 0f,
    val stepCount: Int = 0
)