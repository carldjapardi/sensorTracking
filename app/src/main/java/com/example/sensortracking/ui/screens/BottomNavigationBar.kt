package com.example.sensortracking.ui.screens

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun BottomNavigationBar(
    navController: NavController,
    selected: String,
    onTrackTabTapped: () -> Unit = {},
    onTabSelected: (String) -> Unit = {}
) {
    NavigationBar(
        modifier = Modifier.height(130.dp).fillMaxHeight()
    ) {
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