package com.example.sensortracking.ui.screens

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
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
    val selectedSessionCsv: String? = null,
    val showCsvDialog: Boolean = false
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
    
    fun loadSessionCsv(context: Context, fileName: String) {
        viewModelScope.launch {
            try {
                val trackingDir = File(context.filesDir, "tracking_sessions")
                val file = File(trackingDir, fileName)
                
                if (file.exists()) {
                    val csvContent = file.readText()
                    _uiState.value = _uiState.value.copy(
                        selectedSessionCsv = csvContent,
                        showCsvDialog = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Session file not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error loading session CSV: ${e.message}"
                )
            }
        }
    }
    
    fun hideCsvDialog() {
        _uiState.value = _uiState.value.copy(
            showCsvDialog = false,
            selectedSessionCsv = null
        )
    }

    fun shareSessionCsv(context: Context, fileName: String) {
        viewModelScope.launch {
            try {
                val trackingDir = File(context.filesDir, "tracking_sessions")
                val file = File(trackingDir, fileName)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        context.packageName + ".fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share CSV"))
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Session file not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error sharing session: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
} 