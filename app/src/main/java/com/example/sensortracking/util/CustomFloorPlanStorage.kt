package com.example.sensortracking.util

import android.content.Context
import com.example.sensortracking.ui.screens.upload.CustomFloorPlanInfo
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class CustomFloorPlanStorage(private val context: Context) {
    private val dir = File(context.filesDir, "floor_plans")
    private val json = Json { prettyPrint = true }

    init {
        if (!dir.exists()) dir.mkdirs()
    }

    fun loadPlans(): List<CustomFloorPlanInfo> {
        return dir.listFiles()?.mapNotNull { file ->
            runCatching { json.decodeFromString<CustomFloorPlanInfo>(file.readText()) }.getOrNull()
        } ?: emptyList()
    }

    fun savePlan(plan: CustomFloorPlanInfo): CustomFloorPlanInfo {
        if (!dir.exists()) dir.mkdirs()
        val fileName = plan.fileName ?: sanitize(plan.title) + "_" + System.currentTimeMillis() + ".json"
        val file = File(dir, fileName)
        val withFile = plan.copy(fileName = fileName)
        file.writeText(json.encodeToString(withFile))
        return withFile
    }

    fun deletePlan(fileName: String) {
        val file = File(dir, fileName)
        if (file.exists()) file.delete()
    }

    private fun sanitize(name: String): String = name.replace(Regex("[^A-Za-z0-9_]"), "_")
}
