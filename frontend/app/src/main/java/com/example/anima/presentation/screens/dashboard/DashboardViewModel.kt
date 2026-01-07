package com.example.anima.presentation.screens.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.anima.data.local.AppDatabase
import com.example.anima.data.local.SensorReading
import com.example.anima.domain.ml.StressModelExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale

// URL Note: If using Emulator use "http://10.0.2.2:8080/sensor"
// If using Real Phone + ADB Reverse, use "http://127.0.0.1:8080/sensor"
private const val HTTP_SENSOR_URL = "http://10.0.2.2:8080/sensor"

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    // 1. Initialize Components
    private val sensorDao = AppDatabase.getDatabase(application).sensorDataDao()
    private val mlExecutor = StressModelExecutor(application)
    // NotificationHelper removed

    // 2. UI States
    private val _httpSensorData = MutableStateFlow("Waiting for sensor data...")
    val httpSensorData: StateFlow<String> = _httpSensorData

    // Feedback Dialog State (Reinforcement Learning)
    private val _showFeedbackDialog = MutableStateFlow(false)
    val showFeedbackDialog: StateFlow<Boolean> = _showFeedbackDialog
    private var lastStressReadingId: Long = -1

    fun fetchHttpSensorData() {
        viewModelScope.launch {
            _httpSensorData.value = try {
                fetchDataFromHttp()
            } catch (e: Exception) {
                "Error: Check Server Connection\n${e.message}"
            }
        }
    }

    // Function to handle User Feedback (Yes/No)
    fun submitFeedback(isAccurate: Boolean) {
        viewModelScope.launch {
            if (lastStressReadingId != -1L) {
                sensorDao.updateUserLabel(lastStressReadingId, isAccurate)
                Log.d("AnimaRL", "User Feedback Saved: $isAccurate")
            }
            _showFeedbackDialog.value = false // Close dialog
        }
    }

    private suspend fun fetchDataFromHttp(): String {
        return withContext(Dispatchers.IO) {
            try {
                // --- A. Network Request ---
                val url = URL(HTTP_SENSOR_URL)
                val connection = url.openConnection().apply {
                    connectTimeout = 3000
                    readTimeout = 3000
                }
                val response = connection.getInputStream().bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                // --- B. Parse ALL Sensors (Matches your Python Server) ---
                val heartRate = json.optInt("heartRate", 0)
                val pulseRaw = json.optInt("pulseRaw", 0)
                val temp = json.optDouble("temperature", 0.0).toFloat()

                // Hardware Sensors
                val touch = json.optInt("touch", 0)
                val ir = json.optInt("irDetected", 0)
                val radar = json.optInt("radar", 0)

                // Gyroscope & Motion
                val gx = json.optInt("gyroX", 0)
                val gy = json.optInt("gyroY", 0)
                val gz = json.optInt("gyroZ", 0)
                val motionState = json.optString("motionState", "Unknown")

                // --- C. Personalization (The "Memory") ---
                // Get baseline from DB (Last 24h)
                val yesterday = System.currentTimeMillis() - 86400000
                val baselineHRV = sensorDao.getAverageHrvSince(yesterday) ?: 40.0f
                val baselineHR = 70.0f

                // --- D. ML Inference (The "Brain") ---
                val prediction = mlExecutor.predictStress(
                    currentHR = heartRate.toFloat(),
                    currentHRV = 0.0f, // Pulse sensor doesn't give HRV yet
                    currentTemp = temp,
                    baselineHR = baselineHR,
                    baselineHRV = baselineHRV
                )

                // --- E. Safety & Alert Logic ---
                // Check if Server reported a FALL or ML reported High Stress
                val isFall = motionState.contains("FALL", ignoreCase = true)
                val isHighStress = prediction.isStress && prediction.confidence > 0.8f && !isFall

                if (isFall || isHighStress) {
                    // Notification Logic Removed

                    // 2. Trigger Feedback Dialog (Only for Stress, not Falls)
                    if (isHighStress) {
                        _showFeedbackDialog.value = true
                    }
                }

                // --- F. Save to Database ---
                val reading = SensorReading(
                    timestamp = System.currentTimeMillis(),
                    heartRate = heartRate.toFloat(),
                    hrv = 0.0f,
                    eda = null,
                    skinTemperature = temp,
                    pupilDiameter = null,
                    blinkRate = null,
                    // Use Radar or MotionState to determine activity (1=Active, 0=Rest)
                    motionActivity = if (radar == 1 || motionState == "Walking" || motionState == "Running") 1 else 0,
                    stressLevel = prediction.confidence,
                    anomalyScore = if (isFall) 1.0f else if (prediction.isStress) 0.8f else 0.0f,
                    userLabel = null
                )

                val newId = sensorDao.insertReading(reading)
                if (isHighStress) lastStressReadingId = newId

                // --- G. Format Output for UI ---
                // This string is what appears on your Dashboard Card
                """
                Status: $motionState
                HR: $heartRate BPM | Pulse: $pulseRaw | T: ${String.format("%.1f", temp)}°C
                Gyro: X:$gx Y:$gy Z:$gz
                Touch: $touch | IR: $ir | Radar: $radar
                """.trimIndent()

            } catch (e: Exception) {
                Log.e("DashboardViewModel", "HTTP fetch failed", e)
                throw e
            }
        }
    }
}