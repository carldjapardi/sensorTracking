package com.example.sensortracking.ui.screens.track.trackScreenDialog

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sensortracking.ui.screens.track.TrackScreenViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveTrackingDialog(
    viewModel: TrackScreenViewModel,
    navController: NavController?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var sessionName by remember { 
        mutableStateOf(
            "tracking_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"
        ) 
    }
    var isSaving by remember { mutableStateOf(false) }
    var runNeural by remember { mutableStateOf(false) }
    
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = runNeural, onCheckedChange = { runNeural = it })
                    Text("Generate neural network path")
                }

            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (sessionName.isNotBlank() && !isSaving) {
                        isSaving = true
                        val success = viewModel.saveTracking(context, sessionName, runNeural)
                        isSaving = false
                        if (success) {
                            onDismiss()
                            navController?.navigate("home")
                        }
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