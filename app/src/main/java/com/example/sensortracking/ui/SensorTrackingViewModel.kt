package com.example.sensortracking.ui

import androidx.lifecycle.ViewModel
import com.example.sensortracking.data.SensorTrackingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SensorTrackingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SensorTrackingUiState())
    val uiState: StateFlow<SensorTrackingUiState> = _uiState.asStateFlow()


}