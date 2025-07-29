package com.example.sensortracking.util

import android.content.Context
import com.example.sensortracking.data.TrackingSession
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
    val fileSize: Long,
    val imageFileName: String?
)

class TrackingSessionManager(private val context: Context) {
    
    fun getTrackingSessions(): List<TrackingSessionInfo> {
        val trackingDir = File(context.filesDir, "tracking_sessions")
        if (!trackingDir.exists()) {
            return emptyList()
        }
        
        return trackingDir.listFiles()
            ?.filter { it.extension == "csv" }
            ?.mapNotNull { file ->
                try {
                    val lines = file.readLines()
                    val meta = mutableMapOf<String, String>()
                    var idx = 0
                    while (idx < lines.size && lines[idx].isNotBlank()) {
                        val parts = lines[idx].split(",", limit = 2)
                        if (parts.size == 2) meta[parts[0]] = parts[1]
                        idx++
                    }

                    val sessionName = meta["sessionName"] ?: file.nameWithoutExtension
                    val startTime = meta["startTime"]?.toLong() ?: 0L
                    val endTime = meta["endTime"]?.toLong() ?: 0L
                    val duration = meta["duration"]?.toLong() ?: 0L

                    val pdrHeader = "timestamp,pos_x,pos_y,step_count,total_distance,heading,heading_confidence,overall_confidence"
                    val pdrIndex = lines.indexOf(pdrHeader)
                    var stepCount = 0
                    var totalDistance = 0f
                    if (pdrIndex != -1) {
                        for (i in pdrIndex + 1 until lines.size) {
                            val l = lines[i]
                            if (l.isBlank()) break
                            val parts = l.split(',')
                            if (parts.size >= 8) {
                                stepCount = parts[3].toIntOrNull() ?: stepCount
                                totalDistance = parts[4].toFloatOrNull() ?: totalDistance
                            }
                        }
                    }

                    TrackingSessionInfo(
                        fileName = file.name,
                        sessionName = sessionName,
                        startTime = startTime,
                        endTime = endTime,
                        duration = duration,
                        stepCount = stepCount,
                        totalDistance = totalDistance,
                        fileSize = file.length(),
                        imageFileName = file.nameWithoutExtension + ".png"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            ?.sortedByDescending { it.startTime }
            ?: emptyList()
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