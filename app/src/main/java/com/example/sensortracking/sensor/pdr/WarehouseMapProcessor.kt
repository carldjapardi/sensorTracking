package com.example.sensortracking.sensor.pdr

import com.example.sensortracking.data.*
import kotlin.math.roundToInt

/**
 * Processes Excel warehouse maps and provides collision detection
 */
class WarehouseMapProcessor {
    
    /**
     * Parse Excel data into WarehouseMap
     */
    fun parseWarehouseMap(excelData: Array<Array<String>>): WarehouseMap {
        val height = excelData.size
        val width = excelData[0].size
        
        val cells = Array(height) { y ->
            Array(width) { x ->
                parseCell(x, y, excelData[y][x])
            }
        }
        
        val startPos = findStartPosition(cells)
        val endPos = findEndPosition(cells)
        
        return WarehouseMap(
            width = width,
            height = height,
            cells = cells,
            startPosition = startPos,
            endPosition = endPos
        )
    }
    
    private fun parseCell(x: Int, y: Int, value: String): WarehouseCell {
        return when {
            value == "START" -> WarehouseCell(x, y, CellType.START)
            value == "END" -> WarehouseCell(x, y, CellType.END)
            value.startsWith("A:") -> parseAisleCell(x, y, value)
            value == "NP" -> WarehouseCell(x, y, CellType.WALL)
            value.matches(Regex("[A-Z]\\d+")) -> WarehouseCell(x, y, CellType.STORAGE, value)
            value.matches(Regex("PRB\\d+")) -> WarehouseCell(x, y, CellType.STORAGE, value)
            else -> WarehouseCell(x, y, CellType.WALL)
        }
    }
    
    private fun parseAisleCell(x: Int, y: Int, value: String): WarehouseCell {
        val restrictions = mutableSetOf<AisleRestriction>()
        
        if (value.contains(";L")) restrictions.add(AisleRestriction.LEFT)
        if (value.contains(";R")) restrictions.add(AisleRestriction.RIGHT)
        if (value.contains(";U")) restrictions.add(AisleRestriction.UP)
        if (value.contains(";D")) restrictions.add(AisleRestriction.DOWN)
        
        return WarehouseCell(x, y, CellType.AISLE, aisleRestrictions = restrictions)
    }
    
    private fun findStartPosition(cells: Array<Array<WarehouseCell>>): Position? {
        for (y in cells.indices) {
            for (x in cells[y].indices) {
                if (cells[y][x].cellType == CellType.START) {
                    return Position(x.toFloat(), y.toFloat())
                }
            }
        }
        return null
    }
    
    private fun findEndPosition(cells: Array<Array<WarehouseCell>>): Position? {
        for (y in cells.indices) {
            for (x in cells[y].indices) {
                if (cells[y][x].cellType == CellType.END) {
                    return Position(x.toFloat(), y.toFloat())
                }
            }
        }
        return null
    }
    
    /**
     * Validate movement from current position to new position
     */
    fun validateMovement(
        warehouseMap: WarehouseMap,
        currentPos: Position,
        newPos: Position
    ): Position {
        val clampedPos = Position(
            x = newPos.x.coerceIn(0f, warehouseMap.width - 1f),
            y = newPos.y.coerceIn(0f, warehouseMap.height - 1f)
        )
        
        val cell = warehouseMap.getCellAt(clampedPos)
        if (cell?.cellType == CellType.STORAGE || cell?.cellType == CellType.WALL) {
            return currentPos
        }
        
        val currentCell = warehouseMap.getCellAt(currentPos)
        if (currentCell?.cellType == CellType.AISLE) {
            val direction = calculateDirection(currentPos, clampedPos)
            if (direction in currentCell.aisleRestrictions) {
                return currentPos
            }
        }
        
        return clampedPos
    }
    
    private fun calculateDirection(from: Position, to: Position): AisleRestriction? {
        val deltaX = to.x - from.x
        val deltaY = to.y - from.y
        
        return when {
            deltaX > 0 -> AisleRestriction.RIGHT
            deltaX < 0 -> AisleRestriction.LEFT
            deltaY > 0 -> AisleRestriction.DOWN
            deltaY < 0 -> AisleRestriction.UP
            else -> null
        }
    }
} 