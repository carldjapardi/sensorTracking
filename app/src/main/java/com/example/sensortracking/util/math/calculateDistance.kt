package com.example.sensortracking.util.math
import com.example.sensortracking.data.Position

fun calculateDistance(from: Position, to: Position): Float {
    val dx = to.x - from.x
    val dy = to.y - from.y
    return kotlin.math.sqrt(dx * dx + dy * dy)
}