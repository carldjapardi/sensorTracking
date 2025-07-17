package com.example.sensortracking.sensor.math
import com.example.sensortracking.data.Position

fun calculateHeading(from: Position, to: Position): Float {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val result = Math.toDegrees(kotlin.math.atan2(dx.toDouble(), -dy.toDouble()))
    return result.toFloat().let { if (it < 0) it + 360 else it }
}