package com.example.sensortracking.data

import android.net.Uri
import java.util.Date

data class UploadedFloorPlan(
    val id: String,
    val name: String,
    val description: String,
    val uri: Uri,
    val uploadDate: Date,
    val warehouseMap: WarehouseMap
) 