package com.example.sensortracking.ui.screens.track

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sensortracking.data.PathSegment
import com.example.sensortracking.ui.screens.CustomTopAppBar
import com.example.sensortracking.ui.screens.track.trackScreenDialog.AreaDimensionsDialog
import com.example.sensortracking.ui.screens.track.trackScreenDialog.CalibratePositionDialog
import com.example.sensortracking.ui.screens.track.trackScreenDialog.EditPathSegmentDialog
import com.example.sensortracking.ui.screens.track.trackScreenDialog.InitialPositionDialog
import com.example.sensortracking.ui.screens.track.trackScreenDialog.NewTrackingConfirmDialog
import com.example.sensortracking.ui.screens.track.trackScreenDialog.SaveTrackingDialog
import com.example.sensortracking.ui.screens.track.trackScreenDialog.StartTrackingDialog
import com.example.sensortracking.util.SettingsManager
import kotlin.math.max
import kotlin.math.min

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun TrackScreen(
    navController: NavController? = null,
    viewModel: TrackScreenViewModel = viewModel(),
    showStartDialogOnNav: Int = 0,
    selectedFloorPlan: com.example.sensortracking.data.WarehouseMap? = null,
    settingsManager: SettingsManager
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    
    val neuralNetworkCalibration by settingsManager.neuralNetworkCalibration.collectAsState()

    var showCalibrateDialog by remember { mutableStateOf(false) }
    var showNewTrackingDialog by remember { mutableStateOf(false) }
    var showSaveTrackingDialog by remember { mutableStateOf(false) }
    var showStartDialog by remember { mutableStateOf(false) }
    var showAreaDialog by remember { mutableStateOf(false) }
    var showInitialPositionDialog by remember { mutableStateOf(false) }
    var showEditSegmentDialog by remember { mutableStateOf<PathSegment?>(null) }
    var lastDialogTrigger by remember { mutableStateOf(-1) }
    
    // Calibration state
    var isCalibrating by remember { mutableStateOf(false) }
    var calibrationTimeLeft by remember { mutableStateOf(10) }
    var hasUserInitiatedCalibration by remember { mutableStateOf(false) }
    
    var tempLength by remember { mutableStateOf(uiState.area.length.toInt().toString()) }
    var tempWidth by remember { mutableStateOf(uiState.area.width.toInt().toString()) }
    
    LaunchedEffect(selectedFloorPlan) {
        selectedFloorPlan?.let { floorPlan -> viewModel.loadWarehouseMap(floorPlan) }
    }

    LaunchedEffect(Unit) { viewModel.initializeSensors(context) }

    LaunchedEffect(uiState.isError, uiState.errorMessage) {
        if (uiState.isError && uiState.errorMessage != null) {
            snackbarHostState.showSnackbar(uiState.errorMessage!!)
            viewModel.clearError()
        }
    }

    LaunchedEffect(isCalibrating) {
        if (isCalibrating) {
            while (calibrationTimeLeft > 0) {
                kotlinx.coroutines.delay(1000)
                calibrationTimeLeft--
            }
            isCalibrating = false
            calibrationTimeLeft = 10
        }
    }

    LaunchedEffect(isCalibrating) {
        if (!isCalibrating && calibrationTimeLeft == 10 && uiState.canStartTracking && hasUserInitiatedCalibration) {
            // Small delay to ensure calibration dialog is dismissed
            kotlinx.coroutines.delay(500)
            viewModel.onStartTracking()
        }
    }

    // CONDITIONS FOR DIALOGS
    if (showStartDialogOnNav != lastDialogTrigger) {
        lastDialogTrigger = showStartDialogOnNav
        showStartDialog = true
    }

    if (showStartDialog) {
        StartTrackingDialog(
            onSelectFloorPlan = { navController?.navigate("upload") },
            onNoFloorPlan = {
                viewModel.clearWarehouseMap()
                showStartDialog = false
                showAreaDialog = true
            },
            onDismiss = { showStartDialog = false },
            hasSelectedFloorPlan = uiState.warehouseMap != null
        )
    }

    if (showAreaDialog) {
        AreaDimensionsDialog(
            tempLength = tempLength,
            tempWidth = tempWidth,
            onLengthChange = { tempLength = it },
            onWidthChange = { tempWidth = it },
            onConfirm = {
                val lengthInt = tempLength.toIntOrNull() ?: 1
                val widthInt = tempWidth.toIntOrNull() ?: 1
                viewModel.setArea(lengthInt.toFloat(), widthInt.toFloat())
                showAreaDialog = false
                showInitialPositionDialog = true
            },
            onDismiss = { showAreaDialog = false }
        )
    }

    if (showInitialPositionDialog) {
        InitialPositionDialog(
            viewModel = viewModel,
            onDismiss = { showInitialPositionDialog = false }
        )
    }

    if (showCalibrateDialog) {
        CalibratePositionDialog(
            viewModel = viewModel,
            onDismiss = {
                showCalibrateDialog = false
                if (uiState.isTracking) {
                    viewModel.resumeTracking()
                }
            }
        )
    }

    if (showNewTrackingDialog) {
        NewTrackingConfirmDialog(
            viewModel = viewModel,
            onDismiss = { showNewTrackingDialog = false },
            onNewTracking = {
                viewModel.onStartNewTracking()
                hasUserInitiatedCalibration = false
                showStartDialog = true
                showNewTrackingDialog = false
            }
        )
    }

    if (showSaveTrackingDialog) {
        SaveTrackingDialog(
            viewModel = viewModel,
            navController = navController,
            onDismiss = { showSaveTrackingDialog = false })
    }

    // Calibration dialog
    if (isCalibrating) {
        AlertDialog(
            onDismissRequest = { }, // Prevent dismissal during calibration
            title = { Text("Calibrating Sensors") },
            text = {
                Column {
                    Text("Please keep your device stationary for accurate calibration.")
                    Spacer(Modifier.height(16.dp))
                    Text("Time remaining: $calibrationTimeLeft seconds", 
                         style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("This ensures the neural network will work properly.")
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
    
    if (showEditSegmentDialog != null) {
        EditPathSegmentDialog(
            segment = showEditSegmentDialog!!,
            onConfirm = { newSegment ->
                val segments = viewModel.getPathSegments()
                val index = segments.indexOf(showEditSegmentDialog)
                if (index != -1) {
                    viewModel.updatePathSegment(index, newSegment)
                }
                showEditSegmentDialog = null
            },
            onDismiss = { showEditSegmentDialog = null }
        )
    }

    val minZoom = 0.5f
    val maxZoom = 3.0f

    // MAIN UI
    Scaffold(
        topBar = {
            CustomTopAppBar(title = "Tracking")
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        // 1. Grid / Map
        Column(modifier = Modifier.padding(top = 80.dp, bottom = 130.dp).verticalScroll(scrollState)) {
            Box(modifier = Modifier.fillMaxWidth().height(350.dp).padding(8.dp).clip(RoundedCornerShape(12.dp)).background(Color.LightGray)) {
                GridFloorPlanArea(
                    zoom = uiState.zoom,
                    onZoomChange = {
                        val clamped = it.coerceIn(minZoom, maxZoom)
                        viewModel.onZoomChange(clamped)
                    },
                    userPosition = uiState.currentPosition,
                    heading = uiState.currentHeading,
                    area = uiState.area,
                    pathHistory = viewModel.getPathHistory(),
                    warehouseMap = uiState.warehouseMap
                )
            }

            // 2. New/Save Tracking, Zoom Controls, Area Info
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                IconButton(
                    onClick = { showNewTrackingDialog = true },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Tracking", modifier = Modifier.size(28.dp))
                }
                IconButton(
                    onClick = { showSaveTrackingDialog = true },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save Tracking", modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Area: ${uiState.warehouseMap?.let { "${it.width}×${it.height}" } ?: "${uiState.area.length.toInt()}M × ${uiState.area.width.toInt()}M"}", 
                    fontSize = 16.sp
                )
                Row {
                    IconButton(
                        onClick = {
                            val newZoom = min(uiState.zoom * 1.2f, maxZoom)
                            viewModel.onZoomChange(newZoom)
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(30.dp))
                    }
                    IconButton(
                        onClick = {
                            val newZoom = max(uiState.zoom / 1.2f, minZoom)
                            viewModel.onZoomChange(newZoom)
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(30.dp))
                    }
                }
            }

            // 3. Start/Stop Tracking & Calibrate Position
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (!uiState.canStartTracking) {
                    Button(onClick = { viewModel.onStopTracking() }, modifier = Modifier.width(140.dp).height(50.dp)) {
                        Text("Stop Tracking")
                    }
                } else {
                    Button(
                        onClick = { 
                            if (!isCalibrating) {
                                if (neuralNetworkCalibration) {
                                    hasUserInitiatedCalibration = true
                                    isCalibrating = true
                                    calibrationTimeLeft = 10
                                } else {
                                    viewModel.onStartTracking()
                                }
                            }
                        }, 
                        enabled = uiState.canStartTracking && !isCalibrating, 
                        modifier = Modifier.width(140.dp).height(50.dp)
                    ) {
                        Text(if (isCalibrating) "Calibrating..." else "Start Tracking")
                    }
                }
                Button(
                    modifier = Modifier.width(140.dp).height(50.dp),
                    onClick = {
                        if (uiState.isTracking) { viewModel.pauseTracking() }
                        showCalibrateDialog = true
                    }
                ) {
                    Text("Calibrate Pos")
                }
            }

            // 4. PDR Data
            var pdrExpanded by remember { mutableStateOf(true) }
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp), shape = RoundedCornerShape(12.dp)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { pdrExpanded = !pdrExpanded }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PDR Data", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(
                            if (pdrExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                    if (pdrExpanded) { CombinedPDRDataDisplay(uiState = uiState) }
                }
            }

            // 5. Log History
            LogHistoryDisplay(
                segments = viewModel.getPathSegments(),
                onSegmentUpdate = { _, segment -> showEditSegmentDialog = segment }
            )
        }
    }
}