package com.example.sensortracking.ui.screens.track.trackScreenDialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.sensortracking.ui.screens.track.TrackScreenViewModel

@Composable
fun SaveTrackingDialog(viewModel: TrackScreenViewModel, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Tracking") },
        text = { Text("Do you want to stop tracking and save?") },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                viewModel.saveTracking()
                viewModel.onStopTracking()
            }) { Text("Stop Tracking and Save") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
} 