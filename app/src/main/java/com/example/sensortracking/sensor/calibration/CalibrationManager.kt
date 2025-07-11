package com.example.sensortracking.sensor.calibration

import com.example.sensortracking.data.Position

class CalibrationManager {
    fun calibrate(
        currentPosition: Position,
        newPosition: Position,
        pathHistory: MutableList<Position>,
        calibrationType: CalibrationType
    ) {
        when (calibrationType) {
            is CalibrationType.AddLine -> {
                pathHistory.add(newPosition)
            }
            is CalibrationType.SetPosition -> {
                if (pathHistory.isNotEmpty()) {
                    pathHistory[pathHistory.size - 1] = newPosition
                }
            }
            is CalibrationType.ShiftPath -> {
                val offsetX = newPosition.x - currentPosition.x
                val offsetY = newPosition.y - currentPosition.y
                
                for (i in pathHistory.indices) {
                    val oldPos = pathHistory[i]
                    pathHistory[i] = Position(oldPos.x + offsetX, oldPos.y + offsetY)
                }
            }
        }
    }
} 