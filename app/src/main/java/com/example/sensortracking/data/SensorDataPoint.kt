package com.example.sensortracking.data

data class SensorDataPoint(
    val timestamp: Long,
    val sensorType: SensorType,
    val values: FloatArray,
    val accuracy: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SensorDataPoint
        if (timestamp != other.timestamp) return false
        if (sensorType != other.sensorType) return false
        if (!values.contentEquals(other.values)) return false
        if (accuracy != other.accuracy) return false
        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + sensorType.hashCode()
        result = 31 * result + values.contentHashCode()
        result = 31 * result + accuracy
        return result
    }
}

enum class SensorType {
    ACCELEROMETER,
    ROTATION_VECTOR
} 