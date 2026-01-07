package com.example.anima.presentation.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.anima.data.local.AppDatabase
import com.example.anima.data.local.SensorReading
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorDao = AppDatabase.getDatabase(application).sensorDataDao()

    // 1. Get ALL readings from the last 24 hours (for graphs)
    // We filter for "non-zero" values so the graph doesn't drop to 0
    private val historyFlow = sensorDao.getReadingsSince(System.currentTimeMillis() - 86400000)

    // 2. Extract specific data streams for the UI

    // A. The Latest Single Reading (For the top Grid)
    val latestReading: StateFlow<SensorReading?> = historyFlow
        .map { list -> list.lastOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // B. Heart Rate History (For the Graph)
    // Take last 20 points, map to Float, default to 0f if null
    val heartRateHistory: StateFlow<List<Float>> = historyFlow
        .map { list ->
            list.takeLast(30).map { it.heartRate ?: 0f }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // C. Stress Level History (For the Graph)
    val stressHistory: StateFlow<List<Float>> = historyFlow
        .map { list ->
            list.takeLast(30).map { (it.stressLevel ?: 0f) * 100 } // Convert 0.0-1.0 to 0-100%
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // D. Temperature History
    val tempHistory: StateFlow<List<Float>> = historyFlow
        .map { list ->
            list.takeLast(30).map { it.skinTemperature ?: 0f }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}