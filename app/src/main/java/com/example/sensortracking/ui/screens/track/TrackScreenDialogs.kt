package com.example.sensortracking.ui.screens.track

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InitialPositionDialog(area: Area, initialPosition: InitialPosition, onPositionSet: (Float, Float) -> Unit, onDismiss: () -> Unit) {
    var xPosition by remember { mutableStateOf(initialPosition.x.toString()) }
    var yPosition by remember { mutableStateOf(initialPosition.y.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Initial Position") },
        text = {
            Column {
                Text("Enter your starting position within the area (${area.length.toInt()}m x ${area.width.toInt()}m):")
                Spacer(Modifier.height(16.dp))
                Text("X Position (0-${area.length.toInt()}m):")
                OutlinedTextField(value = xPosition, onValueChange = { xPosition = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Y Position (0-${area.width.toInt()}m):")
                OutlinedTextField(value = yPosition, onValueChange = { yPosition = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val x = xPosition.toFloatOrNull() ?: 0f
                    val y = yPosition.toFloatOrNull() ?: 0f
                    onPositionSet(x, y)
                }
            ) {
                Text("Set Position")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NewTrackingConfirmDialog(viewModel: TrackScreenViewModel, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start New Tracking") },
        text = { Text("Do you want to save the current tracking before starting a new one?") },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                viewModel.saveTracking()
                viewModel.newTracking()
            }) { Text("Save and New Tracking") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun StartTrackingDialog(onSelectFloorPlan: () -> Unit, onUploadFloorPlan: () -> Unit, onNoFloorPlan: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Tracking") },
        text = {
            Column {
                Text("Choose how to start tracking:")
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    onDismiss()
                    onSelectFloorPlan()
                }) {
                    Text("Select Floor Plan")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    onDismiss()
                    onUploadFloorPlan()
                }) {
                    Text("Upload New Floor Plan")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    onDismiss()
                    onNoFloorPlan()
                }) {
                    Text("No Floor Plan")
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
fun AreaDimensionsDialog(tempLength: String, tempWidth: String, onLengthChange: (String) -> Unit, onWidthChange: (String) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Specify Area Dimensions") },
        text = {
            Column {
                Text("Enter Length (meters):")
                OutlinedTextField(
                    value = tempLength,
                    onValueChange = { v ->
                        if (v.all { it.isDigit() } && v.isNotEmpty()) onLengthChange(v)
                    },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text("Enter Width (meters):")
                OutlinedTextField(
                    value = tempWidth,
                    onValueChange = { v ->
                        if (v.all { it.isDigit() } && v.isNotEmpty()) onWidthChange(v)
                    },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("OK") }
        },
        dismissButton = {}
    )
}

@Composable
fun ShowSaveTrackingAlertDialog(viewModel: TrackScreenViewModel, onDismiss: () -> Unit) {
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
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}