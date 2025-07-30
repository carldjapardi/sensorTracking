package com.example.sensortracking.util

import android.content.Context
import android.net.Uri
import com.example.sensortracking.data.UploadedFloorPlan
import com.example.sensortracking.data.WarehouseMap
import com.example.sensortracking.sensor.pdr.WarehouseMapProcessor
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Date
import java.util.UUID

class UploadedFloorPlanManager(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("uploaded_floor_plans", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val warehouseMapProcessor = WarehouseMapProcessor()

    fun saveFloorPlan(name: String, description: String, uri: Uri, warehouseMap: WarehouseMap): UploadedFloorPlan {
        val id = UUID.randomUUID().toString()
        val uploadDate = Date()
        
        val floorPlan = UploadedFloorPlan(
            id = id,
            name = name,
            description = description,
            uri = uri,
            uploadDate = uploadDate,
            warehouseMap = warehouseMap
        )
        
        val floorPlans = getUploadedFloorPlans().toMutableList()
        floorPlans.add(floorPlan)
        saveFloorPlans(floorPlans)
        
        return floorPlan
    }
    
    fun getUploadedFloorPlans(): List<UploadedFloorPlan> {
        val jsonString = sharedPreferences.getString("floor_plans", "[]")
        val floorPlanDataList: List<UploadedFloorPlanData> = try {
            json.decodeFromString(jsonString ?: "[]")
        } catch (e: Exception) {
            emptyList()
        }
        
        return floorPlanDataList.mapNotNull { data ->
            try {
                val csvData = parseSerializedCSV(data.warehouseMapData)
                UploadedFloorPlan(
                    id = data.id,
                    name = data.name,
                    description = data.description,
                    uri = Uri.parse(data.uriString),
                    uploadDate = Date(data.uploadDate),
                    warehouseMap = warehouseMapProcessor.parseWarehouseMap(csvData)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
    
    fun deleteFloorPlan(id: String): Boolean {
        val floorPlans = getUploadedFloorPlans().toMutableList()
        val removed = floorPlans.removeAll { it.id == id }
        if (removed) {
            saveFloorPlans(floorPlans)
        }
        return removed
    }
    
    private fun saveFloorPlans(floorPlans: List<UploadedFloorPlan>) {
        val floorPlanDataList = floorPlans.map { floorPlan ->
            UploadedFloorPlanData(
                id = floorPlan.id,
                name = floorPlan.name,
                description = floorPlan.description,
                uriString = floorPlan.uri.toString(),
                uploadDate = floorPlan.uploadDate.time,
                warehouseMapData = warehouseMapProcessor.serializeWarehouseMap(floorPlan.warehouseMap)
            )
        }
        
        val jsonString = json.encodeToString(floorPlanDataList)
        sharedPreferences.edit().putString("floor_plans", jsonString).apply()
    }
    
    private fun parseSerializedCSV(serializedData: String): Array<Array<String>> {
        return serializedData.split("\n").map { line ->
            line.split(",").toTypedArray()
        }.toTypedArray()
    }
    
    @Serializable
    private data class UploadedFloorPlanData(
        val id: String,
        val name: String,
        val description: String,
        val uriString: String,
        val uploadDate: Long,
        val warehouseMapData: String
    )
} 