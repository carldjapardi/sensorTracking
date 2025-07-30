package com.example.sensortracking.ui.screens.upload

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.sensortracking.data.WarehouseMap

/** Information about a custom uploaded floor plan. */
data class CustomFloorPlanInfo(
    val title: String,
    val rows: Int,
    val columns: Int,
    val sizeBytes: Long,
    val map: WarehouseMap
)

/** UI state for [UploadScreen]. */
data class UploadScreenUiState(
    val customFloorPlans: List<CustomFloorPlanInfo> = emptyList()
)

class UploadScreenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UploadScreenUiState())
    val uiState: StateFlow<UploadScreenUiState> = _uiState.asStateFlow()

    /** Add a new custom floor plan to the list. */
    fun addCustomFloorPlan(plan: CustomFloorPlanInfo) {
        _uiState.update { it.copy(customFloorPlans = it.customFloorPlans + plan) }
    }
}
