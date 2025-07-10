package com.example.sensortracking.ui.screens.track.trackScreenDialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AreaDimensionsDialog(
    tempLength: String,
    tempWidth: String,
    onLengthChange: (String) -> Unit,
    onWidthChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Specify Area Dimensions") },
        text = {
            Column {
                Text("Enter Length (meters):")
                OutlinedTextField(
                    value = tempLength,
                    onValueChange = { v -> if (v.all { it.isDigit() } && v.isNotEmpty()) onLengthChange(v) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text("Enter Width (meters):")
                OutlinedTextField(
                    value = tempWidth,
                    onValueChange = { v -> if (v.all { it.isDigit() } && v.isNotEmpty()) onWidthChange(v) },
                    singleLine = true
                )
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("OK") } },
        dismissButton = {}
    )
} 