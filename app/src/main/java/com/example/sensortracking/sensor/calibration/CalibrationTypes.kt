package com.example.sensortracking.sensor.calibration

sealed class CalibrationType {
    data object AddLine : CalibrationType()
    data object SetPosition : CalibrationType()
    data object ShiftPath : CalibrationType()
} 