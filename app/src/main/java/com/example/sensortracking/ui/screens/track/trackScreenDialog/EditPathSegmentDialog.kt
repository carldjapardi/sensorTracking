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
fun EditPathSegmentDialog(
    segment: PathSegment,
    onConfirm: (PathSegment) -> Unit,
    onDismiss: () -> Unit
) {
    when (segment) {
        is PathSegment.Straight -> EditStraightSegmentDialog(segment, onConfirm, onDismiss)
        is PathSegment.Turn -> EditTurnSegmentDialog(segment, onConfirm, onDismiss)
    }
}

@Composable
private fun EditStraightSegmentDialog(
    segment: PathSegment.Straight,
    onConfirm: (PathSegment) -> Unit,
    onDismiss: () -> Unit
) {
    var distance by remember { mutableStateOf(segment.distance.toString()) }
    var headingStart by remember { mutableStateOf(segment.headingRange.start.toString()) }
    var headingEnd by remember { mutableStateOf(segment.headingRange.endInclusive.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Path Segment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Distance (meters):")
                OutlinedTextField(
                    value = distance,
                    onValueChange = { distance = it },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Heading Start (degrees):")
                OutlinedTextField(
                    value = headingStart,
                    onValueChange = { headingStart = it },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Heading End (degrees):")
                OutlinedTextField(
                    value = headingEnd,
                    onValueChange = { headingEnd = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val newDistance = distance.toFloatOrNull() ?: segment.distance
                val newStart = headingStart.toFloatOrNull() ?: segment.headingRange.start
                val newEnd = headingEnd.toFloatOrNull() ?: segment.headingRange.endInclusive
                
                val newSegment = PathSegment.Straight(
                    headingRange = newStart..newEnd,
                    distance = newDistance,
                    steps = segment.steps
                )
                onConfirm(newSegment)
            }) { Text("Save") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EditTurnSegmentDialog(
    segment: PathSegment.Turn,
    onConfirm: (PathSegment) -> Unit,
    onDismiss: () -> Unit
) {
    var angle by remember { mutableStateOf(segment.angle.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Path Segment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Turn Angle (degrees):")
                OutlinedTextField(
                    value = angle,
                    onValueChange = { angle = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val newAngle = angle.toFloatOrNull() ?: segment.angle
                
                val newSegment = PathSegment.Turn(
                    direction = segment.direction,
                    angle = newAngle,
                    steps = segment.steps
                )
                onConfirm(newSegment)
            }) { Text("Save") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
} 