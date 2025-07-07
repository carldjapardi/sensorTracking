package com.example.sensortracking.ui.screens.track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CombinedPDRDataDisplay(uiState: TrackScreenUiState, viewModel: TrackScreenViewModel) {
    Column(modifier = Modifier.padding(12.dp)) {
        // Position and Heading
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Position:", style = MaterialTheme.typography.bodyMedium)
                Text("X: ${uiState.currentPosition.x.toFixed(2)}m", style = MaterialTheme.typography.bodySmall)
                Text("Y: ${uiState.currentPosition.y.toFixed(2)}m", style = MaterialTheme.typography.bodySmall)
            }
            Column {
                Text("Heading:", style = MaterialTheme.typography.bodyMedium)
                Text("${uiState.currentHeading.toFixed(1)}°", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Steps, Distance, and Confidence
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Steps:", style = MaterialTheme.typography.bodyMedium)
                Text("${uiState.stepCount}", style = MaterialTheme.typography.bodySmall)
            }
            Column {
                Text("Distance:", style = MaterialTheme.typography.bodyMedium)
                Text("${uiState.totalDistance.toFixed(2)}m", style = MaterialTheme.typography.bodySmall)
            }
            Column {
                Text("Confidence:", style = MaterialTheme.typography.bodySmall)
                Text("${(uiState.confidence * 100).toFixed(0)}%", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Sensor availability
        Spacer(Modifier.height(8.dp))
        Text("Sensors:", style = MaterialTheme.typography.bodyMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("Acc: ${if (uiState.hasAccelerometer) "✓" else "✗"}", style = MaterialTheme.typography.bodySmall)
            Text("Gyro: ${if (uiState.hasGyroscope) "✓" else "✗"}", style = MaterialTheme.typography.bodySmall)
            Text("Mag: ${if (uiState.hasMagnetometer) "✓" else "✗"}", style = MaterialTheme.typography.bodySmall)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("RotVec: ${if (uiState.hasRotationVector) "✓" else "✗"}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun Float.toFixed(digits: Int): String {
    return "%.${digits}f".format(this)
}
