package com.example.sensortracking.util.math

fun Float.toFixed(digits: Int): String {
    return "%.${digits}f".format(this)
}
