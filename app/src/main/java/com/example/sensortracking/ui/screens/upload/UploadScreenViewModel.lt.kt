package com.example.sensortracking.ui.screens.upload

import androidx.lifecycle.ViewModel
import com.example.sensortracking.data.WarehouseMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UploadScreenUiState(
    val floorPlans: List<WarehouseMap> = emptyList(),
    val selectedFloorPlan: WarehouseMap? = null
)

class UploadScreenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UploadScreenUiState())
    val uiState: StateFlow<UploadScreenUiState> = _uiState.asStateFlow()

}