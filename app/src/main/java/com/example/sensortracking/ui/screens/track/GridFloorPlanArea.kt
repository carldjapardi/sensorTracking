package com.example.sensortracking.ui.screens.track

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