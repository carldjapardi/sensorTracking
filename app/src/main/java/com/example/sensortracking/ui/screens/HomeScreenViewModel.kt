package com.example.sensortracking.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sensortracking.util.TrackingSessionManager
import com.example.sensortracking.util.TrackingSessionInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class HomeScreenUiState(
    val trackingSessions: List<TrackingSessionInfo> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedSessionJson: String? = null,
    val showJsonDialog: Boolean = false
)

class HomeScreenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeScreenUiState())
    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()
    
    private var sessionManager: TrackingSessionManager? = null
    
    fun initialize(context: Context) {
        sessionManager = TrackingSessionManager(context)
        loadTrackingSessions()
    }
    
    fun loadTrackingSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val sessions = sessionManager?.getTrackingSessions() ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    trackingSessions = sessions,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load tracking sessions: ${e.message}"
                )
            }
        }
    }
    
    fun deleteTrackingSession(fileName: String) {
        viewModelScope.launch {
            try {
                val success = sessionManager?.deleteTrackingSession(fileName) ?: false
                if (success) {
                    loadTrackingSessions()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to delete tracking session"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error deleting tracking session: ${e.message}"
                )
            }
        }
    }
    
    fun loadSessionJson(context: Context, fileName: String) {
        viewModelScope.launch {
            try {
                val trackingDir = File(context.filesDir, "tracking_sessions")
                val file = File(trackingDir, fileName)
                
                if (file.exists()) {
                    val jsonContent = file.readText()
                    _uiState.value = _uiState.value.copy(
                        selectedSessionJson = jsonContent,
                        showJsonDialog = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Session file not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error loading session JSON: ${e.message}"
                )
            }
        }
    }
    
    fun hideJsonDialog() {
        _uiState.value = _uiState.value.copy(
            showJsonDialog = false,
            selectedSessionJson = null
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
} 