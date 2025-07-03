# Rotation Vector Sensor Refactoring

## Overview

This refactoring replaces the custom heading estimation implementation with Android's built-in rotation vector sensor, which provides superior sensor fusion for orientation tracking.

## What Changed

### 1. HeadingEstimator.kt
- **Primary Change**: Now uses Android's `TYPE_ROTATION_VECTOR` sensor for heading estimation
- **Fallback Mode**: Maintains the original manual sensor fusion as a fallback when rotation vector is unavailable
- **Quaternion Processing**: Added quaternion-to-Euler angle conversion for rotation vector data
- **Automatic Calibration**: Rotation vector sensor handles magnetometer calibration internally

### 2. SensorManager.kt
- **New Sensor**: Added registration and handling for `TYPE_ROTATION_VECTOR` sensor
- **Sensor Availability**: Enhanced to track rotation vector sensor availability
- **Data Flow**: Rotation vector data is passed directly to the heading estimator

### 3. PDRProcessor.kt
- **New Methods**: Added `updateRotationVector()` and `enableFallbackMode()` methods
- **Integration**: Seamlessly integrates rotation vector data into the PDR processing pipeline

### 4. UI Updates
- **Sensor Status**: Added rotation vector sensor status display
- **Optimal Sensors**: Shows when all optimal sensors (including rotation vector) are available
- **Enhanced Monitoring**: Better visibility into sensor fusion capabilities

## Benefits

### 1. **Improved Accuracy**
- Android's rotation vector sensor uses advanced sensor fusion algorithms
- Better handling of sensor noise and drift
- More stable heading estimation

### 2. **Reduced Complexity**
- Eliminates the need for manual magnetometer calibration when rotation vector is available
- Removes complex complementary filter implementation
- Leverages Android's optimized sensor processing

### 3. **Better Performance**
- Hardware-accelerated sensor fusion
- Reduced computational overhead
- More efficient battery usage

### 4. **Robustness**
- Automatic fallback to manual fusion when rotation vector is unavailable
- Maintains compatibility with older devices
- Graceful degradation of functionality

## Technical Details

### Rotation Vector Sensor
- **Type**: `Sensor.TYPE_ROTATION_VECTOR`
- **Data Format**: Quaternion (4 values: x, y, z, w)
- **Update Rate**: `SENSOR_DELAY_GAME` for optimal performance
- **Fusion**: Combines accelerometer, gyroscope, and magnetometer data

### Quaternion to Euler Conversion
```kotlin
private fun quaternionToEulerAngles(quaternion: FloatArray): FloatArray {
    val qw = quaternion[3]
    val qx = quaternion[0]
    val qy = quaternion[1]
    val qz = quaternion[2]
    
    // Roll (x-axis rotation)
    val sinr_cosp = 2.0f * (qw * qx + qy * qz)
    val cosr_cosp = 1.0f - 2.0f * (qx * qx + qy * qy)
    val roll = atan2(sinr_cosp, cosr_cosp)
    
    // Pitch (y-axis rotation)
    val sinp = 2.0f * (qw * qy - qz * qx)
    val pitch = if (abs(sinp) >= 1.0f) {
        PI.toFloat() / 2.0f * if (sinp >= 0.0f) 1.0f else -1.0f
    } else {
        asin(sinp)
    }
    
    // Yaw (z-axis rotation)
    val siny_cosp = 2.0f * (qw * qz + qx * qy)
    val cosy_cosp = 1.0f - 2.0f * (qy * qy + qz * qz)
    val yaw = atan2(siny_cosp, cosy_cosp)
    
    return floatArrayOf(roll, pitch, yaw)
}
```

### Fallback Mode
When rotation vector sensor is unavailable:
1. Enables manual sensor fusion
2. Uses original magnetometer calibration
3. Applies complementary filter
4. Maintains full functionality

## Usage

### Automatic Detection
The system automatically detects rotation vector sensor availability and switches between modes:

```kotlin
// In SensorManager.kt
if (hasRotationVector) {
    // Use rotation vector sensor
    sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME)
} else {
    // Enable fallback mode
    pdrProcessor.enableFallbackMode()
}
```

### UI Indicators
The UI now shows:
- **RotVec**: Rotation vector sensor availability
- **Optimal**: Whether all optimal sensors are available
- **Individual sensors**: Status of each sensor type

## Compatibility

### Devices with Rotation Vector
- **Modern Android devices** (API 18+)
- **High-end smartphones** with advanced sensor fusion
- **Tablets** with comprehensive sensor suites

### Devices without Rotation Vector
- **Older Android devices** (pre-API 18)
- **Budget devices** with limited sensor capabilities
- **Fallback mode** ensures continued functionality

## Testing

### Verification Steps
1. **Sensor Detection**: Check logcat for rotation vector sensor registration
2. **Data Flow**: Monitor quaternion data updates
3. **Heading Accuracy**: Compare heading stability with previous implementation
4. **Fallback Mode**: Test on devices without rotation vector sensor

### Expected Logs
```
D/PDRSensorManager: Rotation Vector sensor registered
D/HeadingEstimator: Rotation vector updated: [0.1, 0.2, 0.3, 0.9]
```

## Future Enhancements

### Potential Improvements
1. **Game Rotation Vector**: Use `TYPE_GAME_ROTATION_VECTOR` for even better performance
2. **Geomagnetic Rotation Vector**: Alternative for devices without gyroscope
3. **Sensor Quality Assessment**: Dynamic sensor selection based on quality metrics
4. **Calibration Optimization**: Enhanced calibration for fallback mode

### Performance Monitoring
- Track heading stability over time
- Monitor sensor fusion quality
- Compare accuracy between rotation vector and fallback modes
- Optimize update rates based on device capabilities

## Conclusion

This refactoring significantly improves the heading estimation accuracy and reliability by leveraging Android's built-in sensor fusion capabilities while maintaining backward compatibility through a robust fallback system. The implementation provides a more professional and reliable PDR tracking experience. 