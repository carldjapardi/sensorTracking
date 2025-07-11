package com.example.sensortracking.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sensortracking.data.WarehouseMap
import com.example.sensortracking.sensor.pdr.WarehouseMapProcessor
import com.example.sensortracking.util.CSVParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    navController: NavController,
    onFloorPlanSelected: (WarehouseMap) -> Unit = {}
) {
    var showFloorPlanDialog by remember { mutableStateOf(false) }
    var selectedFloorPlan by remember { mutableStateOf<WarehouseMap?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Floor Plans") },
                actions = {
                    IconButton(onClick = { /* TODO: More options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Available Floor Plans",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            
            item {
                FloorPlanCard(
                    title = "Example Warehouse (CSV)",
                    description = "Sample warehouse layout with storage locations and aisles",
                    onSelect = {
                        showFloorPlanDialog = true
                    }
                )
            }
            
            item {
                FloorPlanCard(
                    title = "Upload Custom Floor Plan",
                    description = "Upload your own Excel floor plan (Coming Soon)",
                    onSelect = {
                        // TODO: Implement file upload
                    },
                    enabled = false
                )
            }
        }
    }
    
    // Floor Plan Selection Dialog
    if (showFloorPlanDialog) {
        FloorPlanSelectionDialog(
            onConfirm = {
                selectedFloorPlan?.let { onFloorPlanSelected(it) }
                showFloorPlanDialog = false
            },
            onDismiss = { showFloorPlanDialog = false },
            onFloorPlanLoaded = { warehouseMap ->
                selectedFloorPlan = warehouseMap
            }
        )
    }
}

@Composable
fun FloorPlanCard(
    title: String,
    description: String,
    onSelect: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (enabled) onSelect() },
        enabled = enabled
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Select",
                    tint = if (enabled) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FloorPlanSelectionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onFloorPlanLoaded: (WarehouseMap) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading floor plan...")
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text("Floor plan loaded successfully!")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isLoading && errorMessage == null
            ) {
                Text("Use This Floor Plan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 