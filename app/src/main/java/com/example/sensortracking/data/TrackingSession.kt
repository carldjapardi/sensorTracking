package com.example.sensortracking.data

import com.example.sensortracking.ui.screens.track.Area

data class TrackingSession(
    val metadata: SessionMetadata,
    val rawSensorData: RawSensorData,
    val pdrData: PDRDataSeries,
    val pathHistory: List<Position>,
    val pathSegments: List<PathSegment>
)

data class SessionMetadata(
    val sessionName: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val area: Area,
    val warehouseMap: WarehouseMap?,
    val pdrConfig: PDRConfig
)

data class RawSensorData(
    val timestamps: LongArray,
    val accelerometerData: FloatArray,
    val rotationVectorData: FloatArray,
    val accelerometerAccuracy: IntArray,
    val rotationVectorAccuracy: IntArray
) {
    val accelerometerSampleCount: Int get() = accelerometerData.size / 3
    val rotationVectorSampleCount: Int get() = rotationVectorData.size / 4
    
    fun getAccelerometerSample(index: Int): FloatArray {
        val startIndex = index * 3
        return floatArrayOf(
            accelerometerData[startIndex],
            accelerometerData[startIndex + 1],
            accelerometerData[startIndex + 2]
        )
    }
    
    fun getRotationVectorSample(index: Int): FloatArray {
        val startIndex = index * 4
        return floatArrayOf(
            rotationVectorData[startIndex],
            rotationVectorData[startIndex + 1],
            rotationVectorData[startIndex + 2],
            rotationVectorData[startIndex + 3]
        )
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RawSensorData
        if (!timestamps.contentEquals(other.timestamps)) return false
        if (!accelerometerData.contentEquals(other.accelerometerData)) return false
        if (!rotationVectorData.contentEquals(other.rotationVectorData)) return false
        if (!accelerometerAccuracy.contentEquals(other.accelerometerAccuracy)) return false
        if (!rotationVectorAccuracy.contentEquals(other.rotationVectorAccuracy)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = timestamps.contentHashCode()
        result = 31 * result + accelerometerData.contentHashCode()
        result = 31 * result + rotationVectorData.contentHashCode()
        result = 31 * result + accelerometerAccuracy.contentHashCode()
        result = 31 * result + rotationVectorAccuracy.contentHashCode()
        return result
    }
}

data class PDRDataSeries(
    val timestamps: LongArray,
    val positions: FloatArray,
    val stepCounts: IntArray,
    val totalDistances: FloatArray,
    val headings: FloatArray,
    val headingConfidences: FloatArray,
    val overallConfidences: FloatArray,
    val stepData: StepDataSeries?
) {
    val sampleCount: Int get() = timestamps.size
    
    fun getPosition(index: Int): Position {
        val startIndex = index * 2
        return Position(positions[startIndex], positions[startIndex + 1])
    }
    
    fun getHeadingData(index: Int): HeadingData {
        return HeadingData(headings[index], headingConfidences[index])
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PDRDataSeries
        if (!timestamps.contentEquals(other.timestamps)) return false
        if (!positions.contentEquals(other.positions)) return false
        if (!stepCounts.contentEquals(other.stepCounts)) return false
        if (!totalDistances.contentEquals(other.totalDistances)) return false
        if (!headings.contentEquals(other.headings)) return false
        if (!headingConfidences.contentEquals(other.headingConfidences)) return false
        if (!overallConfidences.contentEquals(other.overallConfidences)) return false
        if (stepData != other.stepData) return false
        return true
    }

    override fun hashCode(): Int {
        var result = timestamps.contentHashCode()
        result = 31 * result + positions.contentHashCode()
        result = 31 * result + stepCounts.contentHashCode()
        result = 31 * result + totalDistances.contentHashCode()
        result = 31 * result + headings.contentHashCode()
        result = 31 * result + headingConfidences.contentHashCode()
        result = 31 * result + overallConfidences.contentHashCode()
        result = 31 * result + (stepData?.hashCode() ?: 0)
        return result
    }
}

data class StepDataSeries(
    val stepTimestamps: LongArray,
    val stepMagnitudes: FloatArray,
    val stepConfidences: FloatArray
) {
    val stepCount: Int get() = stepTimestamps.size
    
    fun getStepData(index: Int): StepData {
        return StepData(
            timestamp = stepTimestamps[index],
            magnitude = stepMagnitudes[index],
            confidence = stepConfidences[index]
        )
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as StepDataSeries
        if (!stepTimestamps.contentEquals(other.stepTimestamps)) return false
        if (!stepMagnitudes.contentEquals(other.stepMagnitudes)) return false
        if (!stepConfidences.contentEquals(other.stepConfidences)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = stepTimestamps.contentHashCode()
        result = 31 * result + stepMagnitudes.contentHashCode()
        result = 31 * result + stepConfidences.contentHashCode()
        return result
    }
}

data class PDRDataPoint(
    val timestamp: Long,
    val position: Position,
    val stepCount: Int,
    val totalDistance: Float,
    val currentHeading: HeadingData,
    val lastStep: StepData?,
    val confidence: Float
) 