package com.example.sensortracking.util

import android.content.Context
import com.example.sensortracking.data.TrackingSession
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class TrackingSessionInfo(
    val fileName: String,
    val sessionName: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val stepCount: Int,
    val totalDistance: Float,
    val fileSize: Long
)

class TrackingSessionManager(private val context: Context) {
    
    fun getTrackingSessions(): List<TrackingSessionInfo> {
        val trackingDir = File(context.filesDir, "tracking_sessions")
        if (!trackingDir.exists()) {
            return emptyList()
        }
        
        return trackingDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    val json = Json { ignoreUnknownKeys = true }
                    val sessionJson = json.decodeFromString<TrackingSessionJson>(file.readText())
                    
                    TrackingSessionInfo(
                        fileName = file.name,
                        sessionName = sessionJson.metadata.sessionName,
                        startTime = sessionJson.metadata.startTime,
                        endTime = sessionJson.metadata.endTime,
                        duration = sessionJson.metadata.duration,
                        stepCount = sessionJson.pdrData.stepCounts.lastOrNull() ?: 0,
                        totalDistance = sessionJson.pdrData.totalDistances.lastOrNull() ?: 0f,
                        fileSize = file.length()
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            ?.sortedByDescending { it.startTime }
            ?: emptyList()
    }
    
    fun loadTrackingSession(fileName: String): TrackingSession? {
        return try {
            val trackingDir = File(context.filesDir, "tracking_sessions")
            val file = File(trackingDir, fileName)
            
            if (!file.exists()) {
                return null
            }
            
            val json = Json { ignoreUnknownKeys = true }
            val sessionJson = json.decodeFromString<TrackingSessionJson>(file.readText())
            
            val metadata = com.example.sensortracking.data.SessionMetadata(
                sessionName = sessionJson.metadata.sessionName,
                startTime = sessionJson.metadata.startTime,
                endTime = sessionJson.metadata.endTime,
                duration = sessionJson.metadata.duration,
                area = com.example.sensortracking.ui.screens.track.Area(
                    length = sessionJson.metadata.area.length,
                    width = sessionJson.metadata.area.width
                ),
                warehouseMap = null,
                pdrConfig = com.example.sensortracking.data.PDRConfig(
                    stepThreshold = sessionJson.metadata.pdrConfig.stepThreshold,
                    stepCooldownMs = sessionJson.metadata.pdrConfig.stepCooldownMs,
                    defaultStrideLength = sessionJson.metadata.pdrConfig.defaultStrideLength,
                    headingTolerance = sessionJson.metadata.pdrConfig.headingTolerance
                )
            )
            
            val rawSensorData = com.example.sensortracking.data.RawSensorData(
                timestamps = sessionJson.rawSensorData.timestamps.toLongArray(),
                accelerometerData = sessionJson.rawSensorData.accelerometerData.toFloatArray(),
                rotationVectorData = sessionJson.rawSensorData.rotationVectorData.toFloatArray(),
                accelerometerAccuracy = sessionJson.rawSensorData.accelerometerAccuracy.toIntArray(),
                rotationVectorAccuracy = sessionJson.rawSensorData.rotationVectorAccuracy.toIntArray()
            )
            
            val stepDataSeries = sessionJson.pdrData.stepData?.let { stepDataJson ->
                com.example.sensortracking.data.StepDataSeries(
                    stepTimestamps = stepDataJson.stepTimestamps.toLongArray(),
                    stepMagnitudes = stepDataJson.stepMagnitudes.toFloatArray(),
                    stepConfidences = stepDataJson.stepConfidences.toFloatArray()
                )
            }
            
            val pdrDataSeries = com.example.sensortracking.data.PDRDataSeries(
                timestamps = sessionJson.pdrData.timestamps.toLongArray(),
                positions = sessionJson.pdrData.positions.toFloatArray(),
                stepCounts = sessionJson.pdrData.stepCounts.toIntArray(),
                totalDistances = sessionJson.pdrData.totalDistances.toFloatArray(),
                headings = sessionJson.pdrData.headings.toFloatArray(),
                headingConfidences = sessionJson.pdrData.headingConfidences.toFloatArray(),
                overallConfidences = sessionJson.pdrData.overallConfidences.toFloatArray(),
                stepData = stepDataSeries
            )
            
            val pathHistory = sessionJson.pathHistory.map { positionJson ->
                com.example.sensortracking.data.Position(positionJson.x, positionJson.y)
            }
            
            val pathSegments = sessionJson.pathSegments.map { segmentJson ->
                when (segmentJson) {
                    is PathSegmentJson.Straight -> com.example.sensortracking.data.PathSegment.Straight(
                        headingRange = segmentJson.headingRangeStart..segmentJson.headingRangeEnd,
                        distance = segmentJson.distance,
                        steps = segmentJson.steps
                    )
                    is PathSegmentJson.Turn -> com.example.sensortracking.data.PathSegment.Turn(
                        direction = com.example.sensortracking.data.TurnDirection.valueOf(segmentJson.direction),
                        angle = segmentJson.angle,
                        steps = segmentJson.steps
                    )
                }
            }
            
            TrackingSession(
                metadata = metadata,
                rawSensorData = rawSensorData,
                pdrData = pdrDataSeries,
                pathHistory = pathHistory,
                pathSegments = pathSegments
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun deleteTrackingSession(fileName: String): Boolean {
        return try {
            val trackingDir = File(context.filesDir, "tracking_sessions")
            val file = File(trackingDir, fileName)
            
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
    
    fun formatDateTime(timestamp: Long): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return formatter.format(date)
    }
    
    fun formatDistance(distance: Float): String {
        return if (distance >= 1000) {
            "%.1f km".format(distance / 1000)
        } else {
            "%.0f m".format(distance)
        }
    }
} 