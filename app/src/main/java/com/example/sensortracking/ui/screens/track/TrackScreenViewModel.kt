package com.example.sensortracking.ui.screens.track

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.sensortracking.ui.screens.track.TrackScreenUiState

class TrackScreenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TrackScreenUiState())
    val uiState: StateFlow<TrackScreenUiState> = _uiState.asStateFlow()

    fun onZoomChange(newZoom: Float) {
        _uiState.value = _uiState.value.copy(zoom = newZoom.coerceIn(0.5f, 5f))
    }

    fun onStartTracking() {
        // TODO: Implement start tracking logic
    }

    fun onScanBarcode() {
        // TODO: Implement scan barcode logic
    }

    fun setArea(length: Float, width: Float) {
        _uiState.value = _uiState.value.copy(area = Area(length, width))
    }

    fun saveTracking() {
        // TODO: Implement save logic (future: show name/desc dialog)
    }

    fun newTracking() {
        // TODO: Implement new tracking logic (future: show name/desc dialog)
    }
}