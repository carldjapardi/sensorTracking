package com.example.sensortracking.sensor.pdr

import com.example.sensortracking.data.CellType
import com.example.sensortracking.data.Position
import com.example.sensortracking.data.WarehouseCell
import com.example.sensortracking.data.WarehouseMap

/**
 * Parses specified csv data format and validates movements within a warehouse map.
 */
class WarehouseMapProcessor {
    fun parseWarehouseMap(excelData: Array<Array<String>>): WarehouseMap {
        val height = excelData.size
        val width = excelData[0].size
        
        val cells = Array(height) { y ->
            Array(width) { x ->
                parseCell(x, y, excelData[y][x])
            }
        }
        
        val startPos = findStartOrEndPosition(CellType.START, cells) ?: throw IllegalArgumentException("No START cell found in the map")
        val endPos = findStartOrEndPosition(CellType.END, cells) ?: throw IllegalArgumentException("No END cell found in the map")
        
        return WarehouseMap(width = width, height = height, cells = cells, startPosition = startPos, endPosition = endPos)
    }
    
    private fun parseCell(x: Int, y: Int, value: String): WarehouseCell {
        return when {
            value == "START" -> WarehouseCell(x, y, CellType.START)
            value == "END" -> WarehouseCell(x, y, CellType.END)
            value.startsWith("A:") -> WarehouseCell(x, y, CellType.AISLE)
            value == "NP" -> WarehouseCell(x, y, CellType.WALL)
            value.matches(Regex("[A-Z]\\d+")) -> WarehouseCell(x, y, CellType.STORAGE, value)
            value.matches(Regex("PRB\\d+")) -> WarehouseCell(x, y, CellType.STORAGE, value)
            else -> WarehouseCell(x, y, CellType.WALL)
        }
    }
    
    private fun findStartOrEndPosition(cellTypeData: CellType, cells: Array<Array<WarehouseCell>>): Position? {
        for (y in cells.indices) {
            for (x in cells[y].indices) {
                if (cells[y][x].cellType == cellTypeData) {
                    return Position(x.toFloat(), y.toFloat())
                }
            }
        }
        return null
    }

    // Walkable areas are aisles / start / stop ; Non walkable areas are walls and storage cells
    // Ignores aisle specific restrictions LRUD
    fun validateMovement(warehouseMap: WarehouseMap, currPos: Position, newPos: Position): Position {
        val clampedPos = Position(
            x = newPos.x.coerceIn(0f, warehouseMap.width - 1f),
            y = newPos.y.coerceIn(0f, warehouseMap.height - 1f)
        )
        val cell = warehouseMap.getCellAt(clampedPos)
        if (cell.cellType == CellType.STORAGE || cell.cellType == CellType.WALL) {
            return currPos
        }
        return clampedPos
    }
} 