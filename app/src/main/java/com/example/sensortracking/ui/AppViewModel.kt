package com.example.sensortracking.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class NavigationEvent {
    data object None : NavigationEvent()
    data class NavigateTo(val route: String) : NavigationEvent()
}

class AppViewModel : ViewModel() {
    private val _navigationEvent = MutableStateFlow<NavigationEvent>(NavigationEvent.None)
    val navigationEvent: StateFlow<NavigationEvent> = _navigationEvent.asStateFlow()

    fun requestNavigation(route: String, isOnTrackScreen: Boolean) {
        _navigationEvent.value = NavigationEvent.NavigateTo(route)
    }

    fun clearNavigationEvent() {
        _navigationEvent.value = NavigationEvent.None
    }
} 