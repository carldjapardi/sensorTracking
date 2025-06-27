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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    
    var showNewTrackingDialog by remember { mutableStateOf(false) }
    var showNewTrackingConfirmDialog by remember { mutableStateOf(false) }
    var showSaveTrackingDialog by remember { mutableStateOf(false) }
    var showAreaDialog by remember { mutableStateOf(false) }
    var tempLength by remember { mutableStateOf(uiState.area.length.toInt().toString()) }
    var tempWidth by remember { mutableStateOf(uiState.area.width.toInt().toString()) }
    
    // Initialize sensors when screen is first loaded
    LaunchedEffect(Unit) {
        viewModel.initializeSensors(context)
    }
    
    // Show start dialog every time showStartDialogOnNav changes
    var lastDialogTrigger by remember { mutableStateOf(-1) }
    var showStartDialog by remember { mutableStateOf(false) }
    if (showStartDialogOnNav != lastDialogTrigger) {
        lastDialogTrigger = showStartDialogOnNav
        showStartDialog = true
    }
    
    // Handle errors
    LaunchedEffect(uiState.isError) {
        if (uiState.isError && uiState.errorMessage != null) {
            snackbarHostState.showSnackbar(uiState.errorMessage!!)
            viewModel.clearError()
        }
    }
    
    // Show initial position dialog when needed
    if (uiState.showInitialPositionDialog) {
        InitialPositionDialog(
            area = uiState.area,
            initialPosition = uiState.initialPosition,
            onPositionSet = { x, y -> viewModel.setInitialPosition(x, y) },
            onDismiss = { viewModel.hideInitialPositionDialog() }
        )
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
                    viewModel.saveTracking()
                    viewModel.newTracking()
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
                    viewModel.saveTracking()
                    viewModel.onStopTracking()
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
                    viewModel.showInitialPositionDialog()
                }) { Text("OK") }
            },
            dismissButton = {}
        )
    }
    
    // Zoom Set to 0% and 300%..
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
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
            
            // 2. Calibration Progress (if calibrating)
            if (uiState.isCalibrating) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Calibrating sensors...", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = uiState.calibrationProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "${(uiState.calibrationProgress * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
            
            // 3. Grid/Floor Plan Area (larger container)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp) // Fixed height for grid
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray)
            ) {
                GridFloorPlanArea(
                    zoom = uiState.zoom,
                    onZoomChange = {
                        val clamped = it.coerceIn(minZoom, maxZoom)
                        viewModel.onZoomChange(clamped)
                    },
                    userPosition = uiState.currentPosition,
                    floorPlan = uiState.floorPlan,
                    area = uiState.area,
                    pathHistory = viewModel.getPathHistory()
                )
            }
            
            // 4. Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (uiState.isTracking) {
                    Button(
                        onClick = viewModel::onStopTracking,
                        modifier = Modifier.width(140.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Stop Tracking")
                    }
                } else {
                    Button(
                        onClick = viewModel::onStartTracking,
                        enabled = uiState.canStartTracking,
                        modifier = Modifier.width(140.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start Tracking")
                    }
                }
                Button(
                    onClick = viewModel::onScanBarcode,
                    modifier = Modifier.width(140.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan Barcode")
                }
            }
            
            // 5. Combined PDR Data Display (collapsible)
            var pdrExpanded by remember { mutableStateOf(true) }
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
                            .clickable { pdrExpanded = !pdrExpanded }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PDR Data", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(
                            if (pdrExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                    if (pdrExpanded) {
                        CombinedPDRDataDisplay(uiState = uiState, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun InitialPositionDialog(
    area: Area,
    initialPosition: InitialPosition,
    onPositionSet: (Float, Float) -> Unit,
    onDismiss: () -> Unit
) {
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
                OutlinedTextField(
                    value = xPosition,
                    onValueChange = { xPosition = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("Y Position (0-${area.width.toInt()}m):")
                OutlinedTextField(
                    value = yPosition,
                    onValueChange = { yPosition = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
fun CombinedPDRDataDisplay(uiState: TrackScreenUiState, viewModel: TrackScreenViewModel) {
    Column(modifier = Modifier.padding(12.dp)) {
        // Position and Heading
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Steps:", style = MaterialTheme.typography.bodyMedium)
                Text("${uiState.stepCount}", style = MaterialTheme.typography.bodySmall)
            }
            Column {
                Text("Distance:", style = MaterialTheme.typography.bodyMedium)
                Text("${uiState.totalDistance.toFixed(2)}m", style = MaterialTheme.typography.bodySmall)
            }
            Column {
                Text("Confidence:", style = MaterialTheme.typography.bodyMedium)
                Text("${(uiState.confidence * 100).toFixed(0)}%", style = MaterialTheme.typography.bodySmall)
            }
        }
        
        // Algorithm Selection
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Stride Algorithm:", style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = { viewModel.cycleStrideAlgorithm() },
                modifier = Modifier.height(32.dp)
            ) {
                Text(uiState.strideAlgorithm, style = MaterialTheme.typography.bodySmall)
            }
        }
        
        // Sensor availability
        Spacer(Modifier.height(8.dp))
        Text("Sensors:", style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("Acc: ${if (uiState.hasAccelerometer) "✓" else "✗"}", style = MaterialTheme.typography.bodySmall)
            Text("Gyro: ${if (uiState.hasGyroscope) "✓" else "✗"}", style = MaterialTheme.typography.bodySmall)
            Text("Mag: ${if (uiState.hasMagnetometer) "✓" else "✗"}", style = MaterialTheme.typography.bodySmall)
        }
        
        // IMU Data (if available)
        if (uiState.pdrData != null) {
            Spacer(Modifier.height(8.dp))
            Text("Sensor Values:", style = MaterialTheme.typography.bodyMedium)
            Text("Accelerometer: ${uiState.imuData.accelerometer}", style = MaterialTheme.typography.bodySmall)
            Text("Gyroscope: ${uiState.imuData.gyroscope}", style = MaterialTheme.typography.bodySmall)
            Text("Magnetometer: ${uiState.imuData.magnetometer}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun GridFloorPlanArea(
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    userPosition: com.example.sensortracking.data.Position,
    floorPlan: Any?,
    area: Area,
    pathHistory: List<com.example.sensortracking.data.Position> = emptyList()
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
            
            // Draw grid lines
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
            
            // Draw path history
            if (pathHistory.size > 1) {
                for (i in 1 until pathHistory.size) {
                    val prev = pathHistory[i - 1]
                    val curr = pathHistory[i]
                    
                    val prevX = startX + (prev.x / area.length) * gridWidth
                    val prevY = startY + (prev.y / area.width) * gridHeight
                    val currX = startX + (curr.x / area.length) * gridWidth
                    val currY = startY + (curr.y / area.width) * gridHeight
                    
                    drawLine(
                        color = Color.Blue,
                        start = Offset(prevX, prevY),
                        end = Offset(currX, currY),
                        strokeWidth = 3f
                    )
                }
            }
            
            // Draw current position
            val posX = startX + (userPosition.x / area.length) * gridWidth
            val posY = startY + (userPosition.y / area.width) * gridHeight
            
            // Draw position indicator
            drawCircle(
                color = Color.Red,
                radius = 8f,
                center = Offset(posX, posY)
            )
        }
    }
}

private fun Float.toFixed(digits: Int): String {
    return "%.${digits}f".format(this)
}

