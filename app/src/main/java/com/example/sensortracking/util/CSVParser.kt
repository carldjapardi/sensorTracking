package com.example.sensortracking.util

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object CSVParser {
    fun parseCSVFile(context: Context, fileName: String): Array<Array<String>>? {
        return try {
            val inputStream = context.assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = mutableListOf<Array<String>>()
            
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val values = parseCSVLine(line!!)
                lines.add(values)
            }
            
            reader.close()
            inputStream.close()
            
            lines.toTypedArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun parseCSVLine(line: String): Array<String> {
        val result = mutableListOf<String>()
        val chars = line.toCharArray()
        var i = 0
        var current = StringBuilder()
        var inQuotes = false
        
        while (i < chars.size) {
            val char = chars[i]
            
            when {
                char == '"' -> {
                    inQuotes = !inQuotes
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> {
                    current.append(char)
                }
            }
            i++
        }
        
        // Add the last value
        result.add(current.toString().trim())
        
        return result.toTypedArray()
    }
} 