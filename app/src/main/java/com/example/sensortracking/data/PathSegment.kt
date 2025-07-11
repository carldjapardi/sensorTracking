package com.example.sensortracking.data

sealed class PathSegment {
    data class Straight(
        val headingRange: ClosedRange<Float>,
        val distance: Float,
        val steps: Int
    ) : PathSegment()
    
    data class Turn(
        val direction: TurnDirection,
        val angle: Float,
        val steps: Int
    ) : PathSegment()
}

enum class TurnDirection {
    LEFT, RIGHT
} 