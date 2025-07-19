package com.example.sensortracking.ui.screens.track.trackScreenDialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.sensortracking.ui.screens.track.TrackScreenViewModel

@Composable
fun NewTrackingConfirmDialog(viewModel: TrackScreenViewModel, onDismiss: () -> Unit, onNewTracking: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start New Tracking") },
        text = { Text("Do you want to start a new tracking session? The current session will be lost.") },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                onNewTracking()
            }) { Text("Start New Tracking") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
} 