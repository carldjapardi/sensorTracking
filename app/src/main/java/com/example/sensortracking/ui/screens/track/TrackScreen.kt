package com.example.sensortracking.ui.screens.track

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sensortracking.ui.screens.track.trackScreenDialog.*
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(
    viewModel: TrackScreenViewModel = viewModel(),
    showStartDialogOnNav: Int = 0
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    var showCalibrateDialog by remember { mutableStateOf(false) }
    var showNewTrackingDialog by remember { mutableStateOf(false) }
    var showSaveTrackingDialog by remember { mutableStateOf(false) }
    var showStartDialog by remember { mutableStateOf(false) }
    var showAreaDialog by remember { mutableStateOf(false) }
    var showInitialPositionDialog by remember { mutableStateOf(false) }
    var lastDialogTrigger by remember { mutableStateOf(-1) }
    
    // Temporary state for dialogs
    var tempLength by remember { mutableStateOf(uiState.area.length.toInt().toString()) }
    var tempWidth by remember { mutableStateOf(uiState.area.width.toInt().toString()) }

    LaunchedEffect(Unit) { viewModel.initializeSensors(context) }

    if (showStartDialogOnNav != lastDialogTrigger) {
        lastDialogTrigger = showStartDialogOnNav
        showStartDialog = true
    }

    LaunchedEffect(uiState.isError, uiState.errorMessage) {
        if (uiState.isError && uiState.errorMessage != null) {
            snackbarHostState.showSnackbar(uiState.errorMessage!!)
            viewModel.clearError()
        }
    }

    // ===== DIALOGS =====
    if (showStartDialog) {
        StartTrackingDialog(
            onSelectFloorPlan = { /* TODO: Select floor plan */ },
            onUploadFloorPlan = { /* TODO: Upload new floor plan */ },
            onNoFloorPlan = {
                showStartDialog = false
                showAreaDialog = true
            },
            onDismiss = { showStartDialog = false }
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
                showInitialPositionDialog = true
                showNewTrackingDialog = false
            }
        )
    }

    if (showSaveTrackingDialog) {
        SaveTrackingDialog(
            viewModel = viewModel,
            onDismiss = { showSaveTrackingDialog = false })
    }

    // ===== MAIN UI =====
    val minZoom = 0.0f
    val maxZoom = 3.0f
    
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
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(scrollState)) {
            // 1. Grid/Floor Plan Canvas
            Box(modifier = Modifier.fillMaxWidth().height(350.dp).padding(8.dp).clip(RoundedCornerShape(12.dp)).background(Color.LightGray)) {
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

            // 2. Info Row: New Tracking, Save Tracking, Zoom, Area
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
                Text("Area: ${uiState.area.length.toInt()}M X ${uiState.area.width.toInt()}M", fontSize = 16.sp)
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

            // 3. Action Buttons: Start Tracking, Calibrate Position
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (!uiState.canStartTracking) {
                    Button(onClick = viewModel::onStopTracking, modifier = Modifier.width(140.dp).height(50.dp)) {
                        Text("Stop Tracking")
                    }
                } else {
                    Button(onClick = viewModel::onStartTracking, enabled = uiState.canStartTracking, modifier = Modifier.width(140.dp).height(50.dp)) {
                        Text("Start Tracking")
                    }
                }
                Button(
                    modifier = Modifier.width(140.dp).height(50.dp),
                    onClick = {
                    if (uiState.isTracking) { viewModel.pauseTracking() }
                    showCalibrateDialog = true }
                ) {
                    Text("Calibrate Pos")
                }
            }
            
            // 4. PDR Data Display
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
        }
    }
}