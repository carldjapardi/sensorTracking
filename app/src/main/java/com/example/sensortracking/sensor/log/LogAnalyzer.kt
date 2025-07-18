package com.example.sensortracking.sensor.log

import com.example.sensortracking.data.PathSegment
import com.example.sensortracking.data.Position
import com.example.sensortracking.data.TurnDirection
import com.example.sensortracking.util.math.calculateDistance
import com.example.sensortracking.util.math.calculateHeading

class LogAnalyzer(private val headingTolerance: Float = 30f) {
    private val segments = mutableListOf<PathSegment>()
    private var currentHeading: Float = 0f
    private var currentDistance: Float = 0f
    private var currentSteps: Int = 0
    private var headingRange: ClosedRange<Float> = 0f..0f
    
    fun analyzePath(pathHistory: List<Position>): List<PathSegment> {
        if (pathHistory.size < 2) return emptyList()
        
        segments.clear()
        currentDistance = 0f
        currentSteps = 0
        headingRange = 0f..0f
        
        for (i in 1 until pathHistory.size) {
            val prev = pathHistory[i - 1]
            val curr = pathHistory[i]
            
            val heading = calculateHeading(prev, curr)
            val distance = calculateDistance(prev, curr)
            
            if (isSignificantTurn(heading)) {
                addCurrentSegment()
                addTurnSegment(heading)
                resetCurrentSegment(heading)
            } else {
                updateCurrentSegment(heading, distance)
            }
        }
        
        addCurrentSegment()
        return segments.toList()
    }
    
    private fun isSignificantTurn(newHeading: Float): Boolean {
        val headingDiff = kotlin.math.abs(newHeading - currentHeading)
        val normalizedDiff = if (headingDiff > 180) 360 - headingDiff else headingDiff
        return normalizedDiff > headingTolerance
    }
    
    private fun updateCurrentSegment(heading: Float, distance: Float) {
        currentHeading = heading
        currentDistance += distance
        currentSteps++
        
        if (currentSteps == 1) {
            headingRange = heading..heading
        } else {
            headingRange = minOf(headingRange.start, heading)..maxOf(headingRange.endInclusive, heading)
        }
    }
    
    private fun addCurrentSegment() {
        if (currentSteps > 0) {
            segments.add(PathSegment.Straight(headingRange, currentDistance, currentSteps))
        }
    }
    
    private fun addTurnSegment(newHeading: Float) {
        val headingDiff = newHeading - currentHeading
        val normalizedDiff = when {
            headingDiff > 180 -> headingDiff - 360
            headingDiff < -180 -> headingDiff + 360
            else -> headingDiff
        }
        
        val direction = if (normalizedDiff > 0) TurnDirection.RIGHT else TurnDirection.LEFT
        val angle = kotlin.math.abs(normalizedDiff)
        
        segments.add(PathSegment.Turn(direction, angle, 1))
    }
    
    private fun resetCurrentSegment(heading: Float) {
        currentHeading = heading
        currentDistance = 0f
        currentSteps = 0
        headingRange = heading..heading
    }
} 