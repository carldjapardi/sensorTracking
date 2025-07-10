package com.example.sensortracking.ui.screens.track.trackScreenDialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun StartTrackingDialog(
    onSelectFloorPlan: () -> Unit,
    onUploadFloorPlan: () -> Unit,
    onNoFloorPlan: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Tracking") },
        text = {
            Column {
                Text("Choose how to start tracking:")
                Spacer(androidx.compose.ui.Modifier.height(8.dp))
                Button(onClick = { onDismiss(); onSelectFloorPlan() }) { Text("Select Floor Plan") }
                Spacer(androidx.compose.ui.Modifier.height(8.dp))
                Button(onClick = { onDismiss(); onUploadFloorPlan() }) { Text("Upload New Floor Plan") }
                Spacer(androidx.compose.ui.Modifier.height(8.dp))
                Button(onClick = { onDismiss(); onNoFloorPlan() }) { Text("No Floor Plan") }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
} 