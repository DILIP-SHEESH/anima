package com.example.anima.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DashboardViewModel : ViewModel() {
    private val _steps = MutableStateFlow(8230)
    val steps: StateFlow<Int> = _steps
}
