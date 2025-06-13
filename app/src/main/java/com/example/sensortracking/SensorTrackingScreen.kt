package com.example.sensortracking

import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sensortracking.ui.SensorTrackingViewModel

@Composable
fun SensorTrackingApp(
    sensorTrackingViewModel: SensorTrackingViewModel = viewModel<SensorTrackingViewModel>(),
){
    val gameUiState by sensorTrackingViewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            SensorTrackingTopAppBar()
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            Button(onClick = {}) {
                Text(text = "Start Tracking")
            }
            Button(onClick = {}) {
                Text(text = "Scan Barcode")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorTrackingTopAppBar(
    modifier: Modifier = Modifier
) {
    TopAppBar(
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier,
        title = {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null//stringResource(R.string.back_button)
            )
        }
    )
}