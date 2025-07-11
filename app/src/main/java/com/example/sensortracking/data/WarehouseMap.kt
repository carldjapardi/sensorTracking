package com.example.sensortracking.data

/**
 * Represents a warehouse map cell with its properties
 */
data class WarehouseCell(
    val x: Int,
    val y: Int,
    val cellType: CellType,
    val storageLocation: String? = null
)

enum class CellType {
    STORAGE,      // Storage location (B02, C80, etc.)
    AISLE,        // Walkable aisle space
    WALL,         // Non-walkable wall/obstacle
    START,        // Starting point
    END           // Ending point
}

/**
 * Warehouse map representation with collision detection
 */
data class WarehouseMap(
    val width: Int,
    val height: Int,
    val cells: Array<Array<WarehouseCell>>,
    val startPosition: Position? = null,
    val endPosition: Position? = null
) {
    fun isValidPosition(position: Position): Boolean {
        val x = position.x.toInt().coerceIn(0, width - 1)
        val y = position.y.toInt().coerceIn(0, height - 1)
        return cells[y][x].cellType != CellType.WALL
    }
    
    fun getCellAt(position: Position): WarehouseCell? {
        val x = position.x.toInt().coerceIn(0, width - 1)
        val y = position.y.toInt().coerceIn(0, height - 1)
        return cells[y][x]
    }
    
    fun getStorageLocations(): List<String> {
        return cells.flatMap { row ->
            row.filter { it.cellType == CellType.STORAGE }
                .mapNotNull { it.storageLocation }
        }
    }
    
    fun getAisleCells(): List<WarehouseCell> {
        return cells.flatMap { row ->
            row.filter { it.cellType == CellType.AISLE }
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as WarehouseMap
        return width == other.width && 
               height == other.height && 
               cells.contentDeepEquals(other.cells)
    }
    
    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + cells.contentDeepHashCode()
        return result
    }
} 