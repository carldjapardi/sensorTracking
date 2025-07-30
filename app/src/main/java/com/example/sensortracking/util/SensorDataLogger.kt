package com.example.sensortracking.util

import android.content.Context
import com.example.sensortracking.data.*
import com.example.sensortracking.ui.screens.track.Area
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileWriter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Bitmap.CompressFormat
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import java.io.FileOutputStream


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
            val filesDir = context.filesDir
            val trackingDir = File(filesDir, "tracking_sessions")
            if (!trackingDir.exists()) {
                trackingDir.mkdirs()
            }

            val csvFile = File(trackingDir, "$sessionName.csv")
            FileWriter(csvFile).use { writer ->
                writer.append("sessionName,${session.metadata.sessionName}\n")
                writer.append("startTime,${session.metadata.startTime}\n")
                writer.append("endTime,${session.metadata.endTime}\n")
                writer.append("duration,${session.metadata.duration}\n")
                writer.append("areaLength,${session.metadata.area.length}\n")
                writer.append("areaWidth,${session.metadata.area.width}\n")
                writer.append("\n")

                writer.append("timestamp,ax,ay,az,rv_w,rv_x,rv_y,rv_z,accel_acc,rv_acc\n")
                val raw = session.rawSensorData
                val sampleCount = raw.timestamps.size
                for (i in 0 until sampleCount) {
                    val ts = raw.timestamps[i]
                    val acc = if (i < raw.accelerometerSampleCount) raw.getAccelerometerSample(i) else floatArrayOf(0f,0f,0f)
                    val rot = if (i < raw.rotationVectorSampleCount) raw.getRotationVectorSample(i) else floatArrayOf(0f,0f,0f,0f)
                    val accAcc = raw.accelerometerAccuracy.getOrNull(i) ?: 0
                    val rotAcc = raw.rotationVectorAccuracy.getOrNull(i) ?: 0
                    writer.append("$ts,${acc[0]},${acc[1]},${acc[2]},${rot[0]},${rot[1]},${rot[2]},${rot[3]},$accAcc,$rotAcc\n")
                }

                writer.append("\n")
                writer.append("timestamp,pos_x,pos_y,step_count,total_distance,heading,heading_confidence,overall_confidence\n")
                val pdr = session.pdrData
                for (i in 0 until pdr.sampleCount) {
                    val ts = pdr.timestamps[i]
                    val pos = pdr.getPosition(i)
                    val stepCount = pdr.stepCounts[i]
                    val totalDist = pdr.totalDistances[i]
                    val heading = pdr.headings[i]
                    val headingConf = pdr.headingConfidences[i]
                    val overallConf = pdr.overallConfidences[i]
                    writer.append("$ts,${pos.x},${pos.y},$stepCount,$totalDist,$heading,$headingConf,$overallConf\n")
                }

                pdr.stepData?.let { stepData ->
                    writer.append("\nstep_timestamp,step_magnitude,step_confidence\n")
                    for (i in 0 until stepData.stepCount) {
                        writer.append("${stepData.stepTimestamps[i]},${stepData.stepMagnitudes[i]},${stepData.stepConfidences[i]}\n")
                    }
                }
            }

            val imageFile = File(trackingDir, "$sessionName.png")
            val bitmap = generatePathBitmap(session.pathHistory, session.metadata.area, session.metadata.warehouseMap)
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(CompressFormat.PNG, 100, out)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun generatePathBitmap(
        pathHistory: List<Position>,
        area: Area,
        warehouseMap: WarehouseMap?,
        width: Int = 400,
        height: Int = 400
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val horizontalBoxes = warehouseMap?.width ?: area.length.toInt().coerceAtLeast(1)
        val verticalBoxes = warehouseMap?.height ?: area.width.toInt().coerceAtLeast(1)
        val maxX = warehouseMap?.width?.toFloat() ?: area.length
        val maxY = warehouseMap?.height?.toFloat() ?: area.width
        val cellSize = kotlin.math.min(width.toFloat() / horizontalBoxes, height.toFloat() / verticalBoxes)
        val gridWidth = cellSize * horizontalBoxes
        val gridHeight = cellSize * verticalBoxes
        val startX = (width - gridWidth) / 2f
        val startY = (height - gridHeight) / 2f
        val paint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE }
        for (i in 0..horizontalBoxes) {
            val x = startX + i * cellSize
            canvas.drawLine(x, startY, x, startY + gridHeight, paint)
        }
        for (j in 0..verticalBoxes) {
            val y = startY + j * cellSize
            canvas.drawLine(startX, y, startX + gridWidth, y, paint)
        }
        paint.color = Color.BLUE
        paint.strokeWidth = 3f
        for (i in 1 until pathHistory.size) {
            val prev = pathHistory[i-1]
            val curr = pathHistory[i]
            val prevX = startX + (prev.x / maxX) * gridWidth
            val prevY = startY + (prev.y / maxY) * gridHeight
            val currX = startX + (curr.x / maxX) * gridWidth
            val currY = startY + (curr.y / maxY) * gridHeight
            canvas.drawLine(prevX, prevY, currX, currY, paint)
        }
        return bitmap
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
        for (j in 0..verticalBoxes) {
            val y = startY + j * cellSize
            canvas.drawLine(startX, y, startX + gridWidth, y, paint)
        }
        paint.color = Color.BLUE
        paint.strokeWidth = 3f
        for (i in 1 until pathHistory.size) {
            val prev = pathHistory[i-1]
            val curr = pathHistory[i]
            val prevX = startX + (prev.x / maxX) * gridWidth
            val prevY = startY + (prev.y / maxY) * gridHeight
            val currX = startX + (curr.x / maxX) * gridWidth
            val currY = startY + (curr.y / maxY) * gridHeight
            canvas.drawLine(prevX, prevY, currX, currY, paint)
        }
        return bitmap
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