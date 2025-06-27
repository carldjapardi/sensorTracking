package com.example.sensortracking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.sensortracking.ui.theme.SensorTrackingTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sensortracking.ui.screens.HomeScreen
import com.example.sensortracking.ui.screens.TrackScreen
import com.example.sensortracking.ui.screens.UploadScreen
import com.example.sensortracking.ui.screens.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SensorTrackingTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") { HomeScreen(navController) }
                    composable("track") { TrackScreen(navController) }
                    composable("upload") { UploadScreen(navController) }
                    composable("settings") { SettingsScreen(navController) }
                }
            }
        }
    }
}