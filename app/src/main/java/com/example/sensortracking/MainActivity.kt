package com.example.sensortracking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.sensortracking.ui.theme.SensorTrackingTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sensortracking.ui.screens.HomeScreen
import com.example.sensortracking.ui.screens.track.TrackScreen
import com.example.sensortracking.ui.screens.UploadScreen
import com.example.sensortracking.ui.screens.SettingsScreen
import com.example.sensortracking.ui.screens.BottomNavigationBar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SensorTrackingTheme {
                val navController = rememberNavController()
                var trackTabTrigger by remember { mutableStateOf(0) }
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "home"

                Scaffold(
                    bottomBar = {
                        BottomNavigationBar(
                            navController = navController,
                            selected = currentRoute,
                            onTrackTabTapped = { trackTabTrigger++ }
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") { HomeScreen(navController) }
                        composable("track") {
                            TrackScreen(
                                navController,
                                showStartDialogOnNav = trackTabTrigger
                            )
                        }
                        composable("upload") { UploadScreen(navController) }
                        composable("settings") { SettingsScreen(navController) }
                    }
                }
            }
        }
    }
}