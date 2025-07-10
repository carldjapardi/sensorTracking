package com.example.sensortracking.ui.screens.track.trackScreenDialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
    var xPos by remember { mutableStateOf(uiState.currentPosition.x.toString()) }
    var yPos by remember { mutableStateOf(uiState.currentPosition.y.toString()) }

    val areaLength = uiState.area.length.toInt()
    val areaWidth = uiState.area.width.toInt()
    val currentXPos = uiState.currentPosition.x
    val currentYPos = uiState.currentPosition.y

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calibrate Position") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Set your current position within the area (${areaLength}m x ${areaWidth}m):")
                Text("Current Position: (${currentXPos.toFixed(1)}, ${currentYPos.toFixed(1)})")

                Text("X Position (0-${areaLength}m):")
                OutlinedTextField(value = xPos, onValueChange = { xPos = it }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Text("Y Position (0-${areaWidth}m):")
                OutlinedTextField(value = yPos, onValueChange = { yPos = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val x = xPos.toFloatOrNull() ?: currentXPos
                val y = yPos.toFloatOrNull() ?: currentYPos
                viewModel.setCalibratedPosition(x, y)
                onDismiss()
            }) { Text("Calibrate") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun Float.toFixed(digits: Int): String = "%.${digits}f".format(this) 