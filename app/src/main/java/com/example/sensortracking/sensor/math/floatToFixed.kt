package com.example.sensortracking.sensor.math

fun Float.toFixed(digits: Int): String {
    return "%.${digits}f".format(this)
}