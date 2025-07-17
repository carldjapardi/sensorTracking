package com.example.sensortracking.sensor.log

import com.example.sensortracking.data.PathSegment
import com.example.sensortracking.data.Position
import com.example.sensortracking.data.TurnDirection
import kotlin.math.cos
import kotlin.math.sin

class PathReconstructor {
    fun reconstructPath(segments: List<PathSegment>, startPosition: Position): List<Position> {
        val path = mutableListOf<Position>()
        path.add(startPosition)
        
        var currentPosition = startPosition
        var currentHeading = 0f
        
        segments.forEach { segment ->
            when (segment) {
                is PathSegment.Straight -> {
                    val stepDistance = segment.distance / segment.steps
                    val headingStep = (segment.headingRange.endInclusive - segment.headingRange.start) / (segment.steps - 1)
                    
                    for (i in 0 until segment.steps) {
                        val heading = segment.headingRange.start + (headingStep * i)
                        currentPosition = calculateNextPosition(currentPosition, heading, stepDistance)
                        path.add(currentPosition)
                    }
                }
                is PathSegment.Turn -> {
                    val turnDirection = if (segment.direction == TurnDirection.RIGHT) 1 else -1
                    currentHeading += segment.angle * turnDirection
                    currentHeading = (currentHeading + 360) % 360
                }
            }
        }
        
        return path
    }
    
    private fun calculateNextPosition(current: Position, heading: Float, distance: Float): Position {
        val headingRadians = Math.toRadians(heading.toDouble())
        val deltaX = distance * sin(headingRadians).toFloat()
        val deltaY = -distance * cos(headingRadians).toFloat()
        
        return Position(
            x = current.x + deltaX,
            y = current.y + deltaY
        )
    }
} 