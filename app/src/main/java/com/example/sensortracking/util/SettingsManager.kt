package com.example.sensortracking.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sensor_tracking_settings", Context.MODE_PRIVATE)
    
    private val _neuralNetworkCalibration = MutableStateFlow(
        prefs.getBoolean(KEY_NEURAL_NETWORK_CALIBRATION, true)
    )
    val neuralNetworkCalibration: StateFlow<Boolean> = _neuralNetworkCalibration.asStateFlow()
    
    fun setNeuralNetworkCalibration(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NEURAL_NETWORK_CALIBRATION, enabled).apply()
        _neuralNetworkCalibration.value = enabled
    }
    
    companion object {
        private const val KEY_NEURAL_NETWORK_CALIBRATION = "neural_network_calibration"
    }
} 