package com.example.sensortracking.ui.screens.track

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt

@Composable
fun GridFloorPlanArea(
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    userPosition: com.example.sensortracking.data.Position,
    floorPlan: Any?,
    area: Area,
    pathHistory: List<com.example.sensortracking.data.Position> = emptyList(),
    warehouseMap: com.example.sensortracking.data.WarehouseMap? = null
) {
    // Use warehouse map dimensions if available, otherwise use area dimensions
    val horizontalBoxes = if (warehouseMap != null) warehouseMap.width else area.length.roundToInt().coerceAtLeast(1)
    val verticalBoxes = if (warehouseMap != null) warehouseMap.height else area.width.roundToInt().coerceAtLeast(1)

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
        Canvas(
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

            // Draw warehouse map if available, otherwise draw grid
            if (warehouseMap != null) {
                drawWarehouseMap(warehouseMap, startX, startY, cellSize)
            } else {
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
            }

            // Draw path history
            if (pathHistory.size > 1) {
                for (i in 1 until pathHistory.size) {
                    val prev = pathHistory[i - 1]
                    val curr = pathHistory[i]

                    val maxX = if (warehouseMap != null) warehouseMap.width.toFloat() else area.length
                    val maxY = if (warehouseMap != null) warehouseMap.height.toFloat() else area.width

                    val prevX = startX + (prev.x / maxX) * gridWidth
                    val prevY = startY + (prev.y / maxY) * gridHeight
                    val currX = startX + (curr.x / maxX) * gridWidth
                    val currY = startY + (curr.y / maxY) * gridHeight

                    drawLine(
                        color = Color.Blue,
                        start = Offset(prevX, prevY),
                        end = Offset(currX, currY),
                        strokeWidth = 3f
                    )
                }
            }

            // current position
            val maxX = if (warehouseMap != null) warehouseMap.width.toFloat() else area.length
            val maxY = if (warehouseMap != null) warehouseMap.height.toFloat() else area.width
            val posX = startX + (userPosition.x / maxX) * gridWidth
            val posY = startY + (userPosition.y / maxY) * gridHeight

            // position indicator
            drawCircle(color = Color.Red, radius = 8f, center = Offset(posX, posY))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWarehouseMap(
    warehouseMap: com.example.sensortracking.data.WarehouseMap,
    startX: Float,
    startY: Float,
    cellSize: Float
) {
    for (y in 0 until warehouseMap.height) {
        for (x in 0 until warehouseMap.width) {
            val cell = warehouseMap.cells[y][x]
            val cellX = startX + x * cellSize
            val cellY = startY + y * cellSize
            
            val color = when (cell.cellType) {
                com.example.sensortracking.data.CellType.STORAGE -> Color(0xFF4CAF50) // Green
                com.example.sensortracking.data.CellType.AISLE -> Color(0xFF2196F3) // Blue
                com.example.sensortracking.data.CellType.WALL -> Color(0xFF9E9E9E) // Gray
                com.example.sensortracking.data.CellType.START -> Color(0xFF4CAF50) // Green
                com.example.sensortracking.data.CellType.END -> Color(0xFFF44336) // Red
            }
            
            // Draw cell background
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(cellX, cellY),
                size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
            )
            
            // Draw cell border
            drawRect(
                color = Color.Black,
                topLeft = androidx.compose.ui.geometry.Offset(cellX, cellY),
                size = androidx.compose.ui.geometry.Size(cellSize, cellSize),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
            )
            
            // Draw storage location labels
            if (cell.storageLocation != null) {
                // Note: Text drawing would require additional setup
                // For now, we'll just use colored rectangles
            }
        }
    }
}