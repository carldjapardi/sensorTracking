package com.example.sensortracking.ui.screens.upload

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.sensortracking.data.WarehouseMap
import kotlinx.serialization.Serializable
import com.example.sensortracking.util.CustomFloorPlanStorage

/** Information about a custom uploaded floor plan. */
@Serializable
data class CustomFloorPlanInfo(
    val title: String,
    val rows: Int,
    val columns: Int,
    val sizeBytes: Long,
    val map: WarehouseMap,
    val fileName: String? = null
)

/** UI state for [UploadScreen]. */
data class UploadScreenUiState(
    val customFloorPlans: List<CustomFloorPlanInfo> = emptyList()
)

class UploadScreenViewModel : ViewModel() {
    private var storage: CustomFloorPlanStorage? = null
    private val _uiState = MutableStateFlow(UploadScreenUiState())
    val uiState: StateFlow<UploadScreenUiState> = _uiState.asStateFlow()

    fun initialize(context: Context) {
        storage = CustomFloorPlanStorage(context)
        loadPlans()
    }

    private fun loadPlans() {
        val plans = storage?.loadPlans() ?: emptyList()
        _uiState.value = UploadScreenUiState(plans)
    }

    /** Add a new custom floor plan to the list. */
    fun addCustomFloorPlan(plan: CustomFloorPlanInfo) {
        val stored = storage?.savePlan(plan) ?: plan
        _uiState.update { it.copy(customFloorPlans = it.customFloorPlans + stored) }
    }

    fun deleteCustomFloorPlan(plan: CustomFloorPlanInfo) {
        plan.fileName?.let { storage?.deletePlan(it) }
        _uiState.update { it.copy(customFloorPlans = it.customFloorPlans - plan) }
    }
}
