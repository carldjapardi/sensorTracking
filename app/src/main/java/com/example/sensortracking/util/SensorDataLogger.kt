package com.example.sensortracking.util

import android.content.Context
import com.example.sensortracking.data.*
import com.example.sensortracking.ui.screens.track.Area
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileWriter
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable

@Serializable
data class TrackingSessionJson(
    val metadata: SessionMetadataJson,
    val rawSensorData: RawSensorDataJson,
    val pdrData: PDRDataSeriesJson,
    val pathHistory: List<PositionJson>,
    val pathSegments: List<PathSegmentJson>
)

@Serializable
data class SessionMetadataJson(
    val sessionName: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val area: AreaJson,
    val warehouseMap: WarehouseMapJson?,
    val pdrConfig: PDRConfigJson
)

@Serializable
data class RawSensorDataJson(
    val timestamps: List<Long>,
    val accelerometerData: List<Float>,
    val rotationVectorData: List<Float>,
    val accelerometerAccuracy: List<Int>,
    val rotationVectorAccuracy: List<Int>
)

@Serializable
data class PDRDataSeriesJson(
    val timestamps: List<Long>,
    val positions: List<Float>,
    val stepCounts: List<Int>,
    val totalDistances: List<Float>,
    val headings: List<Float>,
    val headingConfidences: List<Float>,
    val overallConfidences: List<Float>,
    val stepData: StepDataSeriesJson?
)

@Serializable
data class StepDataSeriesJson(
    val stepTimestamps: List<Long>,
    val stepMagnitudes: List<Float>,
    val stepConfidences: List<Float>
)

@Serializable
data class PositionJson(val x: Float, val y: Float)

@Serializable
data class HeadingDataJson(val heading: Float, val confidence: Float)

@Serializable
data class StepDataJson(val timestamp: Long, val magnitude: Float, val confidence: Float)

@Serializable
data class AreaJson(val length: Float, val width: Float)

@Serializable
data class PDRConfigJson(
    val stepThreshold: Float,
    val stepCooldownMs: Long,
    val defaultStrideLength: Float,
    val headingTolerance: Float
)

@Serializable
data class WarehouseMapJson(
    val width: Int,
    val height: Int,
    val startPosition: PositionJson?,
    val endPosition: PositionJson?
)

@Serializable
sealed class PathSegmentJson {
    @Serializable
    data class Straight(
        val headingRangeStart: Float,
        val headingRangeEnd: Float,
        val distance: Float,
        val steps: Int
    ) : PathSegmentJson()
    
    @Serializable
    data class Turn(
        val direction: String,
        val angle: Float,
        val steps: Int
    ) : PathSegmentJson()
}

class SensorDataLogger {
    private val timestamps = mutableListOf<Long>()
    private val accelerometerData = mutableListOf<Float>()
    private val rotationVectorData = mutableListOf<Float>()
    private val accelerometerAccuracy = mutableListOf<Int>()
    private val rotationVectorAccuracy = mutableListOf<Int>()
    
    private val pdrTimestamps = mutableListOf<Long>()
    private val pdrPositions = mutableListOf<Float>()
    private val pdrStepCounts = mutableListOf<Int>()
    private val pdrTotalDistances = mutableListOf<Float>()
    private val pdrHeadings = mutableListOf<Float>()
    private val pdrHeadingConfidences = mutableListOf<Float>()
    private val pdrOverallConfidences = mutableListOf<Float>()
    
    private val stepTimestamps = mutableListOf<Long>()
    private val stepMagnitudes = mutableListOf<Float>()
    private val stepConfidences = mutableListOf<Float>()
    
    private var sessionStartTime: Long = 0
    private var lastLogTime: Long = 0
    private val samplingInterval = 100L
    
    private val _isLogging = MutableStateFlow(false)
    val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()
    
    fun startLogging() {
        timestamps.clear()
        accelerometerData.clear()
        rotationVectorData.clear()
        accelerometerAccuracy.clear()
        rotationVectorAccuracy.clear()
        
        pdrTimestamps.clear()
        pdrPositions.clear()
        pdrStepCounts.clear()
        pdrTotalDistances.clear()
        pdrHeadings.clear()
        pdrHeadingConfidences.clear()
        pdrOverallConfidences.clear()
        
        stepTimestamps.clear()
        stepMagnitudes.clear()
        stepConfidences.clear()
        
        sessionStartTime = System.currentTimeMillis()
        lastLogTime = 0
        _isLogging.value = true
    }
    
    fun stopLogging() {
        _isLogging.value = false
    }
    
    fun logSensorData(timestamp: Long, sensorType: SensorType, values: FloatArray, accuracy: Int) {
        if (!_isLogging.value) return
        
        if (timestamp - lastLogTime >= samplingInterval) {
            timestamps.add(timestamp)
            
            when (sensorType) {
                SensorType.ACCELEROMETER -> {
                    accelerometerData.addAll(values.toList())
                    accelerometerAccuracy.add(accuracy)
                }
                SensorType.ROTATION_VECTOR -> {
                    rotationVectorData.addAll(values.toList())
                    rotationVectorAccuracy.add(accuracy)
                }
            }
            
            lastLogTime = timestamp
        }
    }
    
    fun logPDRData(pdrDataPoint: PDRDataPoint) {
        if (!_isLogging.value) return
        
        pdrTimestamps.add(pdrDataPoint.timestamp)
        pdrPositions.add(pdrDataPoint.position.x)
        pdrPositions.add(pdrDataPoint.position.y)
        pdrStepCounts.add(pdrDataPoint.stepCount)
        pdrTotalDistances.add(pdrDataPoint.totalDistance)
        pdrHeadings.add(pdrDataPoint.currentHeading.heading)
        pdrHeadingConfidences.add(pdrDataPoint.currentHeading.confidence)
        pdrOverallConfidences.add(pdrDataPoint.confidence)
        
        pdrDataPoint.lastStep?.let { step ->
            stepTimestamps.add(step.timestamp)
            stepMagnitudes.add(step.magnitude)
            stepConfidences.add(step.confidence)
        }
    }
    
    fun saveToFile(context: Context, sessionName: String, session: TrackingSession): Boolean {
        return try {
            val json = Json { 
                ignoreUnknownKeys = true
            }
            
            val trackingSessionJson = TrackingSessionJson(
                metadata = SessionMetadataJson(
                    sessionName = session.metadata.sessionName,
                    startTime = session.metadata.startTime,
                    endTime = session.metadata.endTime,
                    duration = session.metadata.duration,
                    area = AreaJson(session.metadata.area.length, session.metadata.area.width),
                    warehouseMap = session.metadata.warehouseMap?.let { map ->
                        WarehouseMapJson(
                            width = map.width,
                            height = map.height,
                            startPosition = map.startPosition?.let { PositionJson(it.x, it.y) },
                            endPosition = map.endPosition?.let { PositionJson(it.x, it.y) }
                        )
                    },
                    pdrConfig = PDRConfigJson(
                        stepThreshold = session.metadata.pdrConfig.stepThreshold,
                        stepCooldownMs = session.metadata.pdrConfig.stepCooldownMs,
                        defaultStrideLength = session.metadata.pdrConfig.defaultStrideLength,
                        headingTolerance = session.metadata.pdrConfig.headingTolerance
                    )
                ),
                rawSensorData = RawSensorDataJson(
                    timestamps = session.rawSensorData.timestamps.toList(),
                    accelerometerData = session.rawSensorData.accelerometerData.toList(),
                    rotationVectorData = session.rawSensorData.rotationVectorData.toList(),
                    accelerometerAccuracy = session.rawSensorData.accelerometerAccuracy.toList(),
                    rotationVectorAccuracy = session.rawSensorData.rotationVectorAccuracy.toList()
                ),
                pdrData = PDRDataSeriesJson(
                    timestamps = session.pdrData.timestamps.toList(),
                    positions = session.pdrData.positions.toList(),
                    stepCounts = session.pdrData.stepCounts.toList(),
                    totalDistances = session.pdrData.totalDistances.toList(),
                    headings = session.pdrData.headings.toList(),
                    headingConfidences = session.pdrData.headingConfidences.toList(),
                    overallConfidences = session.pdrData.overallConfidences.toList(),
                    stepData = session.pdrData.stepData?.let { stepData ->
                        StepDataSeriesJson(
                            stepTimestamps = stepData.stepTimestamps.toList(),
                            stepMagnitudes = stepData.stepMagnitudes.toList(),
                            stepConfidences = stepData.stepConfidences.toList()
                        )
                    }
                ),
                pathHistory = session.pathHistory.map { position -> PositionJson(position.x, position.y) },
                pathSegments = session.pathSegments.map { segment ->
                    when (segment) {
                        is PathSegment.Straight -> PathSegmentJson.Straight(
                            headingRangeStart = segment.headingRange.start,
                            headingRangeEnd = segment.headingRange.endInclusive,
                            distance = segment.distance,
                            steps = segment.steps
                        )
                        is PathSegment.Turn -> PathSegmentJson.Turn(
                            direction = segment.direction.name,
                            angle = segment.angle,
                            steps = segment.steps
                        )
                    }
                }
            )
            
            val compactJson = json.encodeToString(trackingSessionJson)
            val formattedJson = formatJsonCompact(compactJson)
            
            val filesDir = context.filesDir
            val trackingDir = File(filesDir, "tracking_sessions")
            if (!trackingDir.exists()) {
                trackingDir.mkdirs()
            }
            
            val file = File(trackingDir, "${sessionName}.json")
            
            FileWriter(file).use { writer ->
                writer.write(formattedJson)
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun formatJsonCompact(jsonString: String): String {
        val result = StringBuilder()
        var indentLevel = 0
        val indentSize = 2
        
        var i = 0
        while (i < jsonString.length) {
            val char = jsonString[i]
            
            when (char) {
                '{' -> {
                    result.append(" ".repeat(indentLevel * indentSize))
                    result.append(char)
                    result.append("\n")
                    indentLevel++
                }
                '}' -> {
                    indentLevel--
                    result.append(" ".repeat(indentLevel * indentSize))
                    result.append(char)
                }
                '[' -> {
                    result.append(" ".repeat(indentLevel * indentSize))
                    result.append(char)
                    var bracketCount = 1
                    var arrayContent = StringBuilder()
                    i++
                    
                    while (i < jsonString.length && bracketCount > 0) {
                        val nextChar = jsonString[i]
                        when (nextChar) {
                            '[' -> bracketCount++
                            ']' -> bracketCount--
                        }
                        if (bracketCount > 0) {
                            arrayContent.append(nextChar)
                        }
                        i++
                    }
                    result.append(arrayContent.toString())
                    result.append("]")
                    i--
                }
                ',' -> {
                    result.append(char)
                    if (i + 1 < jsonString.length && jsonString[i + 1] != '{' && jsonString[i + 1] != '[') {
                        result.append("\n")
                    }
                }
                ':' -> {
                    result.append(char)
                    result.append(" ")
                }
                else -> {
                    if (!char.isWhitespace()) {
                        result.append(char)
                    }
                }
            }
            i++
        }
        
        return result.toString()
    }
    
    fun getCurrentSession(
        sessionName: String,
        area: Area,
        warehouseMap: WarehouseMap?,
        pdrConfig: PDRConfig,
        pathHistory: List<Position>,
        pathSegments: List<PathSegment>
    ): TrackingSession {
        val endTime = System.currentTimeMillis()
        val duration = endTime - sessionStartTime
        
        val rawSensorData = RawSensorData(
            timestamps = timestamps.toLongArray(),
            accelerometerData = accelerometerData.toFloatArray(),
            rotationVectorData = rotationVectorData.toFloatArray(),
            accelerometerAccuracy = accelerometerAccuracy.toIntArray(),
            rotationVectorAccuracy = rotationVectorAccuracy.toIntArray()
        )
        
        val stepDataSeries = if (stepTimestamps.isNotEmpty()) {
            StepDataSeries(
                stepTimestamps = stepTimestamps.toLongArray(),
                stepMagnitudes = stepMagnitudes.toFloatArray(),
                stepConfidences = stepConfidences.toFloatArray()
            )
        } else null
        
        val pdrDataSeries = PDRDataSeries(
            timestamps = pdrTimestamps.toLongArray(),
            positions = pdrPositions.toFloatArray(),
            stepCounts = pdrStepCounts.toIntArray(),
            totalDistances = pdrTotalDistances.toFloatArray(),
            headings = pdrHeadings.toFloatArray(),
            headingConfidences = pdrHeadingConfidences.toFloatArray(),
            overallConfidences = pdrOverallConfidences.toFloatArray(),
            stepData = stepDataSeries
        )
        
        return TrackingSession(
            metadata = SessionMetadata(
                sessionName = sessionName,
                startTime = sessionStartTime,
                endTime = endTime,
                duration = duration,
                area = area,
                warehouseMap = warehouseMap,
                pdrConfig = pdrConfig
            ),
            rawSensorData = rawSensorData,
            pdrData = pdrDataSeries,
            pathHistory = pathHistory,
            pathSegments = pathSegments
        )
    }
} 