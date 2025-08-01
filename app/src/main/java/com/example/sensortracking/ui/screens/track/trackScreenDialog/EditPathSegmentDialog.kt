package com.example.sensortracking.ui.screens.track.trackScreenDialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.sensortracking.data.PathSegment
import com.example.sensortracking.data.TurnDirection

@Composable
fun EditPathSegmentDialog(segment: PathSegment, onConfirm: (PathSegment) -> Unit, onDismiss: () -> Unit) {
    when (segment) {
        is PathSegment.Straight -> EditStraightSegmentDialog(segment, onConfirm, onDismiss)
        is PathSegment.Turn -> {
            // Turn segments are not editable
            onDismiss()
        }
    }
}

@Composable
private fun EditStraightSegmentDialog(segment: PathSegment.Straight, onConfirm: (PathSegment) -> Unit, onDismiss: () -> Unit) {
    var distance by remember { mutableStateOf(segment.distance.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Distance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Distance (meters):")
                OutlinedTextField(value = distance, onValueChange = { distance = it }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val newDistance = distance.toFloatOrNull() ?: segment.distance
                
                val newSegment = PathSegment.Straight(
                    headingRange = segment.headingRange, 
                    distance = newDistance, 
                    steps = segment.steps
                )
                onConfirm(newSegment)
            }) { Text("Save") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
} 