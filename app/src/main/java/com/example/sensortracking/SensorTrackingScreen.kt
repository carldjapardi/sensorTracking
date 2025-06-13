package com.example.sensortracking

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sensortracking.ui.SensorTrackingViewModel

@Composable
fun SensorTrackingApp(
    sensorTrackingViewModel: SensorTrackingViewModel = viewModel<SensorTrackingViewModel>(),
){
    val gameUiState by sensorTrackingViewModel.uiState.collectAsState()

}