package com.example.sensortracking.ui.screens.upload.uploadScreenDialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.sensortracking.data.WarehouseMap
import com.example.sensortracking.sensor.pdr.WarehouseMapProcessor
import com.example.sensortracking.util.CSVParser

@Composable
fun FloorPlanSelectionDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, onFloorPlanLoaded: (WarehouseMap) -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(context) {
        try {
            val csvData = CSVParser.parseCSVFile(context, "example-wh-map.csv")
            if (csvData != null) {
                val warehouseMapProcessor = WarehouseMapProcessor()
                val warehouseMap = warehouseMapProcessor.parseWarehouseMap(csvData)
                onFloorPlanLoaded(warehouseMap)
            } else {
                errorMessage = "Failed to load floor plan"
            }
        } catch (e: Exception) {
            errorMessage = "Error loading floor plan: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Load Floor Plan") },
        text = {
            Column {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading floor plan...")
                } else if (errorMessage != null) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Floor plan loaded successfully!")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isLoading && errorMessage == null) {
                Text("Use This Floor Plan")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
} 