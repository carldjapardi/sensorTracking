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
    private val gyroscopeData = mutableListOf<Float>()
    private val accelerometerAccuracy = mutableListOf<Int>()
    private val rotationVectorAccuracy = mutableListOf<Int>()
    private val gyroscopeAccuracy = mutableListOf<Int>()
    
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
    // Track the last logged timestamp separately for each sensor type so that
    // slower sensors do not get throttled by faster ones.
    private var lastAccelLogTime: Long = 0
    private var lastGyroLogTime: Long = 0
    private var lastRvLogTime: Long = 0
    private val samplingInterval = 20L // 50 Hz sampling rate (20ms interval)
    
    private val _isLogging = MutableStateFlow(false)
    val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()
    
    fun startLogging() {
        timestamps.clear()
        accelerometerData.clear()
        rotationVectorData.clear()
        gyroscopeData.clear()
        accelerometerAccuracy.clear()
        rotationVectorAccuracy.clear()
        gyroscopeAccuracy.clear()
        
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
        lastAccelLogTime = 0
        lastGyroLogTime = 0
        lastRvLogTime = 0
        _isLogging.value = true
    }
    
    fun stopLogging() {
        _isLogging.value = false
    }
    
    fun logSensorData(timestamp: Long, sensorType: SensorType, values: FloatArray, accuracy: Int) {
        if (!_isLogging.value) return

        // Select the correct timestamp gate for this sensor type
        val lastTime = when (sensorType) {
            SensorType.ACCELEROMETER -> lastAccelLogTime
            SensorType.GYROSCOPE -> lastGyroLogTime
            SensorType.ROTATION_VECTOR -> lastRvLogTime
        }

        if (timestamp - lastTime >= samplingInterval) {
            timestamps.add(timestamp)

            when (sensorType) {
                SensorType.ACCELEROMETER -> {
                    accelerometerData.addAll(values.toList())
                    accelerometerAccuracy.add(accuracy)
                    lastAccelLogTime = timestamp
                }
                SensorType.ROTATION_VECTOR -> {
                    rotationVectorData.addAll(values.toList())
                    rotationVectorAccuracy.add(accuracy)
                    lastRvLogTime = timestamp
                }
                SensorType.GYROSCOPE -> {
                    gyroscopeData.addAll(values.toList())
                    gyroscopeAccuracy.add(accuracy)
                    lastGyroLogTime = timestamp
                }
            }
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
    
    fun saveToFile(
        context: Context,
        sessionName: String,
        session: TrackingSession,
        neuralPath: List<Position>? = null
    ): Boolean {
        android.util.Log.d("SensorDataLogger", "Saving to file: $sessionName, neuralPath size: ${neuralPath?.size ?: 0}")
        
        return try {
            val filesDir = context.filesDir
            val trackingDir = File(filesDir, "tracking_sessions")
            if (!trackingDir.exists()) {
                trackingDir.mkdirs()
                android.util.Log.d("SensorDataLogger", "Created tracking directory: ${trackingDir.absolutePath}")
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

                writer.append("timestamp,ax,ay,az,gx,gy,gz,rv_w,rv_x,rv_y,rv_z,accel_acc,gyro_acc,rv_acc\n")
                val raw = session.rawSensorData
                val sampleCount = raw.timestamps.size
                for (i in 0 until sampleCount) {
                    val ts = raw.timestamps[i]
                    val acc = if (i < raw.accelerometerSampleCount) raw.getAccelerometerSample(i) else floatArrayOf(0f,0f,0f)
                    val rot = if (i < raw.rotationVectorSampleCount) raw.getRotationVectorSample(i) else floatArrayOf(0f,0f,0f,0f)
                    val gyro = if (i < raw.gyroscopeSampleCount) raw.getGyroscopeSample(i) else floatArrayOf(0f,0f,0f)
                    val accAcc = raw.accelerometerAccuracy.getOrNull(i) ?: 0
                    val gyroAcc = raw.gyroscopeAccuracy.getOrNull(i) ?: 0
                    val rotAcc = raw.rotationVectorAccuracy.getOrNull(i) ?: 0
                    writer.append("$ts,${acc[0]},${acc[1]},${acc[2]},${gyro[0]},${gyro[1]},${gyro[2]},${rot[0]},${rot[1]},${rot[2]},${rot[3]},$accAcc,$gyroAcc,$rotAcc\n")
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
            android.util.Log.d("SensorDataLogger", "Saving original PDR path to: ${imageFile.absolutePath}")
            val bitmap = generatePathBitmap(session.pathHistory, session.metadata.area, session.metadata.warehouseMap)
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(CompressFormat.PNG, 100, out)
            }

            neuralPath?.let { path ->
                val nnImage = generatePathBitmap(path, session.metadata.area, session.metadata.warehouseMap)
                val nnFile = File(trackingDir, "${sessionName}_nn.png")
                android.util.Log.d("SensorDataLogger", "Saving neural network path to: ${nnFile.absolutePath}")
                FileOutputStream(nnFile).use { out ->
                    nnImage.compress(CompressFormat.PNG, 100, out)
                }
                android.util.Log.d("SensorDataLogger", "Neural network path saved successfully")
            } ?: run {
                android.util.Log.d("SensorDataLogger", "No neural network path to save")
            }

            android.util.Log.d("SensorDataLogger", "All files saved successfully")
            true
        } catch (e: Exception) {
            android.util.Log.e("SensorDataLogger", "Error saving files", e)
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
        android.util.Log.d("SensorDataLogger", "Generating path bitmap with ${pathHistory.size} points")
        if (pathHistory.isNotEmpty()) {
            android.util.Log.d("SensorDataLogger", "Path bounds: x=[${pathHistory.minOf { it.x }}, ${pathHistory.maxOf { it.x }}], y=[${pathHistory.minOf { it.y }}, ${pathHistory.maxOf { it.y }}]")
        }
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        
        val horizontalBoxes = warehouseMap?.width ?: area.length.toInt().coerceAtLeast(1)
        val verticalBoxes = warehouseMap?.height ?: area.width.toInt().coerceAtLeast(1)
        val maxX = warehouseMap?.width?.toFloat() ?: area.length
        val maxY = warehouseMap?.height?.toFloat() ?: area.width
        
        android.util.Log.d("SensorDataLogger", "Area dimensions: ${maxX}x${maxY}, Grid: ${horizontalBoxes}x${verticalBoxes}")
        
        val cellSize = kotlin.math.min(width.toFloat() / horizontalBoxes, height.toFloat() / verticalBoxes)
        val gridWidth = cellSize * horizontalBoxes
        val gridHeight = cellSize * verticalBoxes
        val startX = (width - gridWidth) / 2f
        val startY = (height - gridHeight) / 2f
        
        // Draw grid
        val paint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE }
        for (i in 0..horizontalBoxes) {
            val x = startX + i * cellSize
            canvas.drawLine(x, startY, x, startY + gridHeight, paint)
        }
        for (j in 0..verticalBoxes) {
            val y = startY + j * cellSize
            canvas.drawLine(startX, y, startX + gridWidth, y, paint)
        }
        
        // Draw path
        if (pathHistory.size > 1) {
            paint.color = Color.BLUE
            paint.strokeWidth = 3f
            
            // Calculate path bounds for better scaling
            val minX = pathHistory.minOf { it.x }
            val maxXPath = pathHistory.maxOf { it.x }
            val minY = pathHistory.minOf { it.y }
            val maxYPath = pathHistory.maxOf { it.y }
            
            val pathWidth = maxXPath - minX
            val pathHeight = maxYPath - minY
            
            android.util.Log.d("SensorDataLogger", "Path dimensions: ${pathWidth}x${pathHeight}")
            
            // Use path bounds if they're reasonable, otherwise use area bounds
            val effectiveMaxX = if (pathWidth > 0.1f) maxXPath else maxX
            val effectiveMaxY = if (pathHeight > 0.1f) maxYPath else maxY
            val effectiveMinX = if (pathWidth > 0.1f) minX else 0f
            val effectiveMinY = if (pathHeight > 0.1f) minY else 0f
            
            for (i in 1 until pathHistory.size) {
                val prev = pathHistory[i-1]
                val curr = pathHistory[i]
                
                // Normalize coordinates to [0, 1] range
                val prevNormX = (prev.x - effectiveMinX) / (effectiveMaxX - effectiveMinX).coerceAtLeast(0.001f)
                val prevNormY = (prev.y - effectiveMinY) / (effectiveMaxY - effectiveMinY).coerceAtLeast(0.001f)
                val currNormX = (curr.x - effectiveMinX) / (effectiveMaxX - effectiveMinX).coerceAtLeast(0.001f)
                val currNormY = (curr.y - effectiveMinY) / (effectiveMaxY - effectiveMinY).coerceAtLeast(0.001f)
                
                // Map to canvas coordinates
                val prevX = startX + prevNormX * gridWidth
                val prevY = startY + prevNormY * gridHeight
                val currX = startX + currNormX * gridWidth
                val currY = startY + currNormY * gridHeight
                
                android.util.Log.d("SensorDataLogger", "Drawing line: (${prev.x}, ${prev.y}) -> (${curr.x}, ${curr.y}) -> (${prevX}, ${prevY}) -> (${currX}, ${currY})")
                
                canvas.drawLine(prevX, prevY, currX, currY, paint)
            }
            
            // Draw start and end points
            paint.color = Color.GREEN
            paint.strokeWidth = 8f
            val startPoint = pathHistory.first()
            val endPoint = pathHistory.last()
            
            val startNormX = (startPoint.x - effectiveMinX) / (effectiveMaxX - effectiveMinX).coerceAtLeast(0.001f)
            val startNormY = (startPoint.y - effectiveMinY) / (effectiveMaxY - effectiveMinY).coerceAtLeast(0.001f)
            val endNormX = (endPoint.x - effectiveMinX) / (effectiveMaxX - effectiveMinX).coerceAtLeast(0.001f)
            val endNormY = (endPoint.y - effectiveMinY) / (effectiveMaxY - effectiveMinY).coerceAtLeast(0.001f)
            
            val startCanvasX = startX + startNormX * gridWidth
            val startCanvasY = startY + startNormY * gridHeight
            val endCanvasX = startX + endNormX * gridWidth
            val endCanvasY = startY + endNormY * gridHeight
            
            canvas.drawPoint(startCanvasX, startCanvasY, paint)
            paint.color = Color.RED
            canvas.drawPoint(endCanvasX, endCanvasY, paint)
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
            gyroscopeData = gyroscopeData.toFloatArray(),
            accelerometerAccuracy = accelerometerAccuracy.toIntArray(),
            rotationVectorAccuracy = rotationVectorAccuracy.toIntArray(),
            gyroscopeAccuracy = gyroscopeAccuracy.toIntArray()
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

    /**
     * Get calibration data from the first part of the tracking session.
     * This data should be from when the device was stationary.
     * 
     * @return RawSensorData for calibration, or null if insufficient data
     */
    fun getCalibrationData(maxSamples: Int = 200): RawSensorData? {
        val sampleCount = minOf(maxSamples, timestamps.size)
        if (sampleCount < 50) {
            android.util.Log.d("SensorDataLogger", "Insufficient data for calibration: $sampleCount samples")
            return null
        }
        
        android.util.Log.d("SensorDataLogger", "Creating calibration data with $sampleCount samples")
        
        return RawSensorData(
            timestamps = timestamps.take(sampleCount).toLongArray(),
            accelerometerData = accelerometerData.take(sampleCount * 3).toFloatArray(),
            rotationVectorData = rotationVectorData.take(sampleCount * 4).toFloatArray(),
            gyroscopeData = gyroscopeData.take(sampleCount * 3).toFloatArray(),
            accelerometerAccuracy = accelerometerAccuracy.take(sampleCount).toIntArray(),
            rotationVectorAccuracy = rotationVectorAccuracy.take(sampleCount).toIntArray(),
            gyroscopeAccuracy = gyroscopeAccuracy.take(sampleCount).toIntArray()
        )
    }
} 