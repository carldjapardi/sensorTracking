package com.example.sensortracking.ui.screens.track

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(
    navController: NavController,
    viewModel: TrackScreenViewModel = viewModel(),
    showStartDialogOnNav: Int = 0
) {
    val uiState by viewModel.uiState.collectAsState()
    var showNewTrackingDialog by remember { mutableStateOf(false) }
    var showNewTrackingConfirmDialog by remember { mutableStateOf(false) }
    var showSaveTrackingDialog by remember { mutableStateOf(false) }
    var showAreaDialog by remember { mutableStateOf(false) }
    var tempLength by remember { mutableStateOf(uiState.area.length.toInt().toString()) }
    var tempWidth by remember { mutableStateOf(uiState.area.width.toInt().toString()) }

    // Show start dialog every time showStartDialogOnNav changes
    var lastDialogTrigger by remember { mutableStateOf(-1) }
    var showStartDialog by remember { mutableStateOf(false) }
    if (showStartDialogOnNav != lastDialogTrigger) {
        lastDialogTrigger = showStartDialogOnNav
        showStartDialog = true
    }

    // Confirmation dialog for new tracking
    if (showNewTrackingConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showNewTrackingConfirmDialog = false },
            title = { Text("Start New Tracking") },
            text = { Text("Do you want to save the current tracking before starting a new one?") },
            confirmButton = {
                Button(onClick = {
                    showNewTrackingConfirmDialog = false
                    // TODO: Save current tracking (future: show name/desc dialog)
                    showStartDialog = true
                }) { Text("Save and New Tracking") }
            },
            dismissButton = {
                Button(onClick = { showNewTrackingConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog for saving current tracking
    if (showSaveTrackingDialog) {
        AlertDialog(
            onDismissRequest = { showSaveTrackingDialog = false },
            title = { Text("Save Tracking") },
            text = { Text("Do you want to stop tracking and save?") },
            confirmButton = {
                Button(onClick = {
                    showSaveTrackingDialog = false
                    // TODO: Save current tracking (future: show name/desc dialog)
                }) { Text("Stop Tracking and Save") }
            },
            dismissButton = {
                Button(onClick = { showSaveTrackingDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog for selecting floor plan or no floor plan
    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Start Tracking") },
            text = {
                Column {
                    Text("Choose how to start tracking:")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { /* TODO: Select floor plan */ }) {
                        Text("Select Floor Plan")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { /* TODO: Upload new floor plan */ }) {
                        Text("Upload New Floor Plan")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        showStartDialog = false
                        showAreaDialog = true
                    }) {
                        Text("No Floor Plan")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    // Dialog for entering area if no floor plan (integers only)
    if (showAreaDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Specify Area Dimensions") },
            text = {
                Column {
                    Text("Enter Length (meters):")
                    OutlinedTextField(
                        value = tempLength,
                        onValueChange = { v ->
                            if (v.all { it.isDigit() } && v.isNotEmpty()) tempLength = v
                        },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Enter Width (meters):")
                    OutlinedTextField(
                        value = tempWidth,
                        onValueChange = { v ->
                            if (v.all { it.isDigit() } && v.isNotEmpty()) tempWidth = v
                        },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val lengthInt = tempLength.toIntOrNull() ?: 1
                    val widthInt = tempWidth.toIntOrNull() ?: 1
                    viewModel.setArea(lengthInt.toFloat(), widthInt.toFloat())
                    showAreaDialog = false
                }) { Text("OK") }
            },
            dismissButton = {}
        )
    }

    // Zoom Set to 0% and 300%.
    val minZoom = 0.0f
    val maxZoom = 3.0f
    val zoomPercent = (uiState.zoom * 100).roundToInt().coerceIn(0, 300)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Intellinum") },
                actions = {
                    IconButton(onClick = { /* TODO: More options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Info Row: Save and New Tracking icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                IconButton(
                    onClick = { showNewTrackingConfirmDialog = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Tracking", modifier = Modifier.size(28.dp))
                }
                IconButton(
                    onClick = { showSaveTrackingDialog = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save Tracking", modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.weight(1f))
                Column {
                    Text("Zoom: $zoomPercent%", fontSize = 16.sp)
                    Text("Area: ${uiState.area.length.toInt()}M X ${uiState.area.width.toInt()}M", fontSize = 16.sp)
                }
                Column {
                    IconButton(
                        onClick = {
                            val newZoom = min(uiState.zoom * 1.2f, maxZoom)
                            viewModel.onZoomChange(newZoom)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(30.dp))
                    }
                    IconButton(
                        onClick = {
                            val newZoom = max(uiState.zoom / 1.2f, minZoom)
                            viewModel.onZoomChange(newZoom)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(30.dp))
                    }
                }
            }
            // 2. Grid/Floor Plan Area (no panning)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray) // Navy blue
            ) {
                GridFloorPlanArea(
                    zoom = uiState.zoom,
                    onZoomChange = {
                        val clamped = it.coerceIn(minZoom, maxZoom)
                        viewModel.onZoomChange(clamped)
                    },
                    userPosition = uiState.userPosition,
                    floorPlan = uiState.floorPlan,
                    area = uiState.area
                )
            }
            // 3. Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = viewModel::onStartTracking) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Tracking")
                }
                Button(onClick = viewModel::onScanBarcode) {
                    Icon(Icons.Default.Star, contentDescription = null) // TODO: Replace with barcode icon
                    Spacer(Modifier.width(8.dp))
                    Text("Scan Barcode")
                }
            }
            // 4. IMU Sensor Details (collapsible)
            var imuExpanded by remember { mutableStateOf(true) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { imuExpanded = !imuExpanded }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("IMU Sensor Details", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(
                            if (imuExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                    if (imuExpanded) {
                        IMUSensorDetails(uiState = uiState)
                    }
                }
            }
        }
    }
}

@Composable
fun GridFloorPlanArea(
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    userPosition: Pair<Float, Float>,
    floorPlan: Any?, // Placeholder for floor plan image or grid data
    area: Area
) {
    val horizontalBoxes = area.length.roundToInt().coerceAtLeast(1)
    val verticalBoxes = area.width.roundToInt().coerceAtLeast(1)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(zoom) {
                detectTransformGestures { _, _, zoomChange, _ ->
                    val newZoom = (zoom * zoomChange).coerceIn(0.0f, 3.0f)
                    onZoomChange(newZoom)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Draw grid using Canvas, apply zoom only
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                }
        ) {
            // Calculate square cell size
            val cellSize = minOf(size.width / horizontalBoxes, size.height / verticalBoxes)
            val gridWidth = cellSize * horizontalBoxes
            val gridHeight = cellSize * verticalBoxes
            val startX = (size.width - gridWidth) / 2f
            val startY = (size.height - gridHeight) / 2f
            for (i in 0..horizontalBoxes) {
                val x = startX + i * cellSize
                drawLine(
                    color = Color.Black,
                    start = Offset(x, startY),
                    end = Offset(x, startY + gridHeight),
                    strokeWidth = 2f
                )
            }
            for (j in 0..verticalBoxes) {
                val y = startY + j * cellSize
                drawLine(
                    color = Color.Black,
                    start = Offset(startX, y),
                    end = Offset(startX + gridWidth, y),
                    strokeWidth = 2f
                )
            }
        }
        // Placeholder for user position
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(Color.Red, shape = RoundedCornerShape(8.dp))
        )
    }
}

@Composable
fun IMUSensorDetails(uiState: TrackScreenUiState) {
    // TODO: Show actual IMU sensor data
    Column(modifier = Modifier.padding(12.dp)) {
        Text("Accelerometer: ${uiState.imuData.accelerometer}")
        Text("Gyroscope: ${uiState.imuData.gyroscope}")
        Text("Magnetometer: ${uiState.imuData.magnetometer}")
        // Add more sensor details as needed
    }
}

