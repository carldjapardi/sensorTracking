package com.example.sensortracking.ui.screens.upload

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import com.example.sensortracking.util.CSVParser
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.sensortracking.data.WarehouseMap
import com.example.sensortracking.ui.screens.upload.uploadScreenDialog.FloorPlanSelectionDialog
import com.example.sensortracking.ui.screens.upload.uploadScreenDialog.CustomFloorPlanDialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.example.sensortracking.ui.screens.upload.CustomFloorPlanInfo
import com.example.sensortracking.ui.screens.upload.UploadScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    onFloorPlanSelected: (WarehouseMap) -> Unit = {},
    viewModel: UploadScreenViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showFloorPlanDialog by remember { mutableStateOf(false) }
    var selectedFloorPlan by remember { mutableStateOf<WarehouseMap?>(null) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var customCsvUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingCustomPlan by remember { mutableStateOf<CustomFloorPlanInfo?>(null) }

    val exampleMetadata = remember(context) {
        val csvData = CSVParser.parseCSVFile(context, "example-wh-map.csv")
        val rows = csvData?.size ?: 0
        val cols = csvData?.firstOrNull()?.size ?: 0
        val size = try { context.assets.openFd("example-wh-map.csv").length } catch (e: Exception) { 0L }
        Triple(rows, cols, size)
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            customCsvUri = uri
            showCustomDialog = true
        }
    }
    
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
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Available Floor Plans",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            item {
                val (rows, cols, size) = exampleMetadata
                FloorPlanCard(
                    title = "Example Warehouse (CSV)",
                    description = "$rows rows, $cols columns, ${size} bytes",
                    onSelect = { showFloorPlanDialog = true }
                )
            }

            items(uiState.customFloorPlans.size) { index ->
                val plan = uiState.customFloorPlans[index]
                FloorPlanCard(
                    title = plan.title,
                    description = "${plan.rows} rows, ${plan.columns} columns, ${plan.sizeBytes} bytes",
                    onSelect = { onFloorPlanSelected(plan.map) }
                )
            }

            item {
                FloorPlanCard(
                    title = "Upload Custom Floor Plan",
                    description = "Upload your own CSV floor plan",
                    onSelect = {
                        openDocumentLauncher.launch(
                            arrayOf(
                                "text/csv",
                                "text/comma-separated-values",
                                "text/plain",
                                "application/vnd.ms-excel"
                            )
                        )
                    },
                    enabled = true
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

    if (showCustomDialog && customCsvUri != null) {
        CustomFloorPlanDialog(
            csvUri = customCsvUri!!,
            onConfirm = {
                pendingCustomPlan?.let {
                    viewModel.addCustomFloorPlan(it)
                    onFloorPlanSelected(it.map)
                }
                showCustomDialog = false
                customCsvUri = null
                pendingCustomPlan = null
            },
            onDismiss = {
                showCustomDialog = false
                customCsvUri = null
                pendingCustomPlan = null
            },
            onFloorPlanLoaded = { warehouseMap, sizeBytes ->
                val name = DocumentFile.fromSingleUri(context, customCsvUri!!)?.name ?: "Custom Floor Plan"
                pendingCustomPlan = CustomFloorPlanInfo(
                    title = name,
                    rows = warehouseMap.height,
                    columns = warehouseMap.width,
                    sizeBytes = sizeBytes,
                    map = warehouseMap
                )
            }
        )
    }
}

@Composable
fun FloorPlanCard(title: String, description: String, onSelect: () -> Unit, enabled: Boolean = true) {
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

