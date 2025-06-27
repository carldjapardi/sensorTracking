package com.example.sensortracking.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

@Composable
fun BottomNavigationBar(
    navController: NavController,
    selected: String,
    onTrackTabTapped: () -> Unit = {},
    onTabSelected: (String) -> Unit = {}
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = selected == "home",
            onClick = {
                onTabSelected("home")
                if (selected != "home") navController.navigate("home")
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Track") },
            label = { Text("Track") },
            selected = selected == "track",
            onClick = {
                onTabSelected("track")
                if (selected != "track") {
                    navController.navigate("track")
                }
                onTrackTabTapped()
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Create, contentDescription = "Upload") },
            label = { Text("Uploads") },
            selected = selected == "upload",
            onClick = {
                onTabSelected("upload")
                if (selected != "upload") navController.navigate("upload")
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = selected == "settings",
            onClick = {
                onTabSelected("settings")
                if (selected != "settings") navController.navigate("settings")
            }
        )
    }
} 