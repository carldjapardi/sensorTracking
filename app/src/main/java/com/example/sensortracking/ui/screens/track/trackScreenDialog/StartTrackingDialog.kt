package com.example.sensortracking.ui.screens.track.trackScreenDialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StartTrackingDialog(
    onSelectFloorPlan: () -> Unit,
    onUploadFloorPlan: () -> Unit,
    onNoFloorPlan: () -> Unit,
    onDismiss: () -> Unit,
    hasSelectedFloorPlan: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Tracking") },
        text = {
            Column {
                Text("Choose how to start tracking:")
                Spacer(Modifier.height(8.dp))
                
                if (hasSelectedFloorPlan) {
                    Text(
                        "✓ Floor plan selected",
                        color = androidx.compose.ui.graphics.Color.Green,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                }
                
                Button(onClick = { onDismiss(); onSelectFloorPlan() }) { 
                    Text(if (hasSelectedFloorPlan) "Change Floor Plan" else "Select Floor Plan") 
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onDismiss(); onUploadFloorPlan() }) { Text("Upload New Floor Plan") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onDismiss(); onNoFloorPlan() }) { Text("No Floor Plan") }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
} 