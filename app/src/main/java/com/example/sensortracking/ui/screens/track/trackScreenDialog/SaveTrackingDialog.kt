package com.example.sensortracking.ui.screens.track.trackScreenDialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.sensortracking.ui.screens.track.TrackScreenViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveTrackingDialog(viewModel: TrackScreenViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var sessionName by remember { 
        mutableStateOf(
            "tracking_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"
        ) 
    }
    var isSaving by remember { mutableStateOf(false) }
    var saveResult by remember { mutableStateOf<Boolean?>(null) }
    
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Save Tracking Session") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Enter a name for your tracking session:")
                OutlinedTextField(
                    value = sessionName,
                    onValueChange = { sessionName = it },
                    label = { Text("Session Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                
                if (saveResult != null) {
                    Text(
                        text = if (saveResult == true) {
                            "Session saved successfully!"
                        } else {
                            "Failed to save session. Please try again."
                        },
                        color = if (saveResult == true) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (sessionName.isNotBlank() && !isSaving) {
                        isSaving = true
                        saveResult = viewModel.saveTracking(context, sessionName)
                        isSaving = false
                    }
                },
                enabled = sessionName.isNotBlank() && !isSaving
            ) { 
                Text(if (isSaving) "Saving..." else "Save") 
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                enabled = !isSaving
            ) { 
                Text("Cancel") 
            }
        }
    )
} 