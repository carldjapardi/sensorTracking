package com.example.sensortracking.ui.screens.track.trackScreenDialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sensortracking.ui.screens.track.TrackScreenViewModel

@Composable
fun CalibratePositionDialog(viewModel: TrackScreenViewModel, onDismiss: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var xPosition by remember { mutableStateOf(uiState.currentPosition.x.toString()) }
    var yPosition by remember { mutableStateOf(uiState.currentPosition.y.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calibrate Position") },
        text = {
            Column {
                Text("Set your current position within the area (${uiState.area.length.toInt()}m x ${uiState.area.width.toInt()}m):")
                Spacer(Modifier.height(16.dp))
                Text("Current Position: (${uiState.currentPosition.x.toFixed(1)}, ${uiState.currentPosition.y.toFixed(1)})")
                Spacer(Modifier.height(8.dp))
                Text("X Position (0-${uiState.area.length.toInt()}m):")
                OutlinedTextField(value = xPosition, onValueChange = { xPosition = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Y Position (0-${uiState.area.width.toInt()}m):")
                OutlinedTextField(value = yPosition, onValueChange = { yPosition = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val x = xPosition.toFloatOrNull() ?: uiState.currentPosition.x
                val y = yPosition.toFloatOrNull() ?: uiState.currentPosition.y
                viewModel.setCalibratedPosition(x, y)
            }) { Text("Calibrate") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun Float.toFixed(digits: Int): String = "%.${digits}f".format(this) 