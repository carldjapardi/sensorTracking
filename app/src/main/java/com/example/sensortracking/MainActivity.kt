package com.example.sensortracking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.sensortracking.ui.theme.SensorTrackingTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sensortracking.ui.screens.HomeScreen
import com.example.sensortracking.ui.screens.track.TrackScreen
import com.example.sensortracking.ui.screens.upload.UploadScreen
import com.example.sensortracking.ui.screens.SettingsScreen
import com.example.sensortracking.ui.screens.BottomNavigationBar
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sensortracking.ui.AppViewModel
import com.example.sensortracking.ui.NavigationEvent
import com.example.sensortracking.data.WarehouseMap

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
                var selectedFloorPlan by remember { mutableStateOf<WarehouseMap?>(null) }

                val appViewModel: AppViewModel = viewModel()
                val navigationEvent by appViewModel.navigationEvent.collectAsState()
                val isOnTrackScreen = currentRoute == "track"

                Scaffold(
                    bottomBar = {
                        BottomNavigationBar(
                            navController = navController,
                            selected = currentRoute,
                            onTrackTabTapped = { trackTabTrigger++ },
                            onTabSelected = { route ->
                                appViewModel.requestNavigation(route, isOnTrackScreen)
                            }
                        )
                    }
                ) { innerPadding ->
                    when (val event = navigationEvent) {
                        is NavigationEvent.ConfirmLeaveTrack -> {
                            AlertDialog(
                                onDismissRequest = { appViewModel.clearNavigationEvent() },
                                title = { Text("Stop Tracking?") },
                                text = { Text("Do you want to stop the current tracking and save, or cancel?") },
                                confirmButton = {
                                    Button(onClick = {
                                        appViewModel.confirmLeaveTrack(event.route)
                                    }) { Text("Save and Leave") }
                                },
                                dismissButton = {
                                    Button(onClick = {
                                        appViewModel.clearNavigationEvent()
                                    }) { Text("Cancel") }
                                }
                            )
                        }
                        is NavigationEvent.NavigateTo -> {
                            LaunchedEffect(event.route) {
                                navController.navigate(event.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                }
                                appViewModel.clearNavigationEvent()
                            }
                        }
                        else -> {}
                    }
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") { HomeScreen(navController) }
                        composable("track") { 
                            TrackScreen(
                                navController = navController,
                                showStartDialogOnNav = trackTabTrigger,
                                selectedFloorPlan = selectedFloorPlan
                            ) 
                        }
                        composable("upload") { 
                            UploadScreen(
                                navController = navController,
                                onFloorPlanSelected = { floorPlan ->
                                    selectedFloorPlan = floorPlan
                                    navController.navigate("track")
                                }
                            ) 
                        }
                        composable("settings") { SettingsScreen(navController) }
                    }
                }
            }
        }
    }
}