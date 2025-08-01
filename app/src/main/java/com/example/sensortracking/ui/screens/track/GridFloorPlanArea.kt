package com.example.sensortracking.ui.screens.track

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.example.sensortracking.data.CellType
import com.example.sensortracking.data.Position
import com.example.sensortracking.data.WarehouseMap
import kotlin.math.roundToInt

@Composable
fun GridFloorPlanArea(
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    userPosition: Position,
    heading: Float = 0f,
    area: Area,
    pathHistory: List<Position> = emptyList(),
    warehouseMap: WarehouseMap? = null
) {
    val horizontalBoxes = warehouseMap?.width ?: area.length.roundToInt().coerceAtLeast(1)
    val verticalBoxes = warehouseMap?.height ?: area.width.roundToInt().coerceAtLeast(1)

    val maxX = warehouseMap?.width?.toFloat() ?: area.length
    val maxY = warehouseMap?.height?.toFloat() ?: area.width

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(zoom) {
                detectTransformGestures { _, _, zoomChange, _ ->
                    val newZoom = (zoom * zoomChange).coerceIn(0.5f, 3.0f)
                    onZoomChange(newZoom)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                }
        ) {
            val cellSize = minOf(size.width / horizontalBoxes, size.height / verticalBoxes)
            val gridWidth = cellSize * horizontalBoxes
            val gridHeight = cellSize * verticalBoxes
            val startX = (size.width - gridWidth) / 2f
            val startY = (size.height - gridHeight) / 2f

            val userX = (userPosition.x / maxX) * gridWidth
            val userY = (userPosition.y / maxY) * gridHeight

            val centerX = size.width / 2f
            val centerY = size.height / 2f

            val offsetX = centerX - (startX + userX)
            val offsetY = centerY - (startY + userY)

            val clampedOffsetX = offsetX.coerceIn(
                -startX,
                size.width - (startX + gridWidth)
            )
            val clampedOffsetY = offsetY.coerceIn(
                -startY,
                size.height - (startY + gridHeight)
            )

            if (warehouseMap != null) {
                drawWarehouseMap(warehouseMap, startX + clampedOffsetX, startY + clampedOffsetY, cellSize)
            } else {
                for (i in 0..horizontalBoxes) {
                    val x = startX + clampedOffsetX + i * cellSize
                    drawLine(
                        color = Color.Black,
                        start = Offset(x, startY + clampedOffsetY),
                        end = Offset(x, startY + clampedOffsetY + gridHeight),
                        strokeWidth = 2f
                    )
                }
                for (j in 0..verticalBoxes) {
                    val y = startY + clampedOffsetY + j * cellSize
                    drawLine(
                        color = Color.Black,
                        start = Offset(startX + clampedOffsetX, y),
                        end = Offset(startX + clampedOffsetX + gridWidth, y),
                        strokeWidth = 2f
                    )
                }
            }

            if (pathHistory.size > 1) {
                for (i in 1 until pathHistory.size) {
                    val prev = pathHistory[i - 1]
                    val curr = pathHistory[i]

                    val prevX = startX + clampedOffsetX + (prev.x / maxX) * gridWidth
                    val prevY = startY + clampedOffsetY + (prev.y / maxY) * gridHeight
                    val currX = startX + clampedOffsetX + (curr.x / maxX) * gridWidth
                    val currY = startY + clampedOffsetY + (curr.y / maxY) * gridHeight

                    drawLine(
                        color = Color.Blue,
                        start = Offset(prevX, prevY),
                        end = Offset(currX, currY),
                        strokeWidth = 3f
                    )
                }
            }

            val posX = startX + clampedOffsetX + (userPosition.x / maxX) * gridWidth
            val posY = startY + clampedOffsetY + (userPosition.y / maxY) * gridHeight

            drawTriangleArrow(
                center = Offset(posX, posY),
                heading = heading,
                size = 20f,
                color = Color.Red
            )
        }
    }
}

private fun DrawScope.drawWarehouseMap(
    warehouseMap: WarehouseMap,
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
                CellType.STORAGE -> Color(0xFF4CAF50)
                CellType.AISLE -> Color(0xFF2196F3)
                CellType.WALL -> Color(0xFF9E9E9E)
                CellType.START -> Color(0xFF4CAF50)
                CellType.END -> Color(0xFFF44336)
            }
            
            drawRect(
                color = color,
                topLeft = Offset(cellX, cellY),
                size = Size(cellSize, cellSize)
            )
            
            drawRect(
                color = Color.Black,
                topLeft = Offset(cellX, cellY),
                size = Size(cellSize, cellSize),
                style = Stroke(width = 1f)
            )
        }
    }
}

private fun DrawScope.drawTriangleArrow(
    center: Offset,
    heading: Float,
    size: Float,
    color: Color
) {
    val angleRad = Math.toRadians(heading.toDouble())
    val cos = kotlin.math.cos(angleRad).toFloat()
    val sin = kotlin.math.sin(angleRad).toFloat()
    
    val arrowLength = size
    val arrowWidth = size * 0.6f
    
    val tip = Offset(
        center.x + sin * arrowLength,
        center.y - cos * arrowLength
    )
    
    val leftBase = Offset(
        center.x + sin * (-arrowLength * 0.3f) - cos * arrowWidth,
        center.y - cos * (-arrowLength * 0.3f) - sin * arrowWidth
    )
    
    val rightBase = Offset(
        center.x + sin * (-arrowLength * 0.3f) + cos * arrowWidth,
        center.y - cos * (-arrowLength * 0.3f) + sin * arrowWidth
    )
    
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(leftBase.x, leftBase.y)
        lineTo(rightBase.x, rightBase.y)
        close()
    }
    
    drawPath(path, color)
}