package com.example.sensortracking

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sensortracking.data.Position
import com.example.sensortracking.data.SensorTrackingUiState
import com.example.sensortracking.ui.SensorTrackingViewModel
import com.example.sensortracking.ui.theme.*
import kotlin.math.sqrt

@Composable
fun SensorTrackingScreen(
    viewModel: SensorTrackingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initializeSensors(context)
    }

    Scaffold(
        topBar = { TopBar() },
        bottomBar = { BottomNavBar() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(LightGray)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(PrimaryBlue)
            ) {
                TrackingCanvas(uiState = uiState)
                Compass()
            }
            DetailsSection(uiState = uiState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar() {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Placeholder for logo
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(AccentOrange, shape = MaterialTheme.shapes.medium)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Intellinum",
                    color = PrimaryBlue,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        actions = {
            IconButton(onClick = { /* TODO */ }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = PrimaryBlue
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = LightGray)
    )
}

@Composable
fun TrackingCanvas(uiState: SensorTrackingUiState) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridCount = 50
        val cellWidth = size.width / gridCount
        val cellHeight = cellWidth // Make cells square-like

        // Draw vertical grid lines
        for (i in 0..gridCount) {
            val x = i * cellWidth
            drawLine(
                color = GridLineColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
        }

        // Draw horizontal grid lines
        val numHorizontalLines = (size.height / cellHeight).toInt()
        for (i in 0..numHorizontalLines) {
            val y = i * cellHeight
            drawLine(
                color = GridLineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        // Center of the canvas
        val centerX = size.width / 2
        val centerY = size.height / 2
        val scalingFactor = cellWidth // Scale according to cell size

        // Draw path history
        if (uiState.pathHistory.size > 1) {
            val path = Path()
            val firstPos = uiState.pathHistory.first()
            path.moveTo(
                centerX + firstPos.x * scalingFactor,
                centerY + firstPos.y * scalingFactor
            )

            uiState.pathHistory.drop(1).forEach { position ->
                path.lineTo(
                    centerX + position.x * scalingFactor,
                    centerY + position.y * scalingFactor
                )
            }

            drawPath(
                path = path,
                color = AccentRed,
                style = Stroke(width = 8f)
            )
        }

        // Current position coordinates
        val currentX = centerX + uiState.xPosition * scalingFactor
        val currentY = centerY + uiState.yPosition * scalingFactor

        // Draw current position dot
        drawCircle(
            color = AccentOrange,
            radius = 20f,
            center = Offset(currentX, currentY)
        )
    }
}

@Composable
fun BoxScope.Compass() {
    Canvas(
        modifier = Modifier
            .size(80.dp)
            .align(Alignment.TopEnd)
            .padding(16.dp)
    ) {
        drawCircle(color = AccentOrange, style = Stroke(width = 8f))
        // Draw compass needle
        // This is a simplified needle
        drawLine(
            color = AccentOrange,
            start = Offset(size.width / 2, size.height / 2 + 15),
            end = Offset(size.width / 2, size.height / 2 - 15),
            strokeWidth = 8f
        )
    }
}

@Composable
fun DetailsSection(uiState: SensorTrackingUiState) {
    val totalAcceleration = sqrt(
        (uiState.xAcceleration * uiState.xAcceleration) +
                (uiState.yAcceleration * uiState.yAcceleration) +
                (uiState.zAcceleration * uiState.zAcceleration)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        DetailRow("Position", "(${uiState.xPosition.toInt()}, ${uiState.yPosition.toInt()})")
        DetailRow("Acceleration", String.format("%.2f", totalAcceleration))
        DetailRow("Direction", "${uiState.direction.toInt()}°")
        DetailRow("Step Count", uiState.stepCount.toString())
        DetailRow("Distance", uiState.distance.toInt().toString())
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun BottomNavBar() {
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf("Track", "Scan", "Settings")
    val icons = listOf(Icons.Filled.PlayArrow, Icons.Filled.Star, Icons.Filled.Settings)

    NavigationBar(
        containerColor = DarkBlue,
        contentColor = Color.White
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = item) },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = { selectedItem = index },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentOrange,
                    selectedTextColor = AccentOrange,
                    unselectedIconColor = Color.White,
                    unselectedTextColor = Color.White,
                    indicatorColor = DarkBlue
                )
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SensorScreenPreview() {
    SensorTrackingTheme {
        SensorTrackingScreen()
    }
}