package com.example.anima.presentation.screens.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.anima.data.local.AppDatabase
import com.example.anima.data.local.SensorReading
import com.example.anima.domain.ml.StressModelExecutor // Import your new Brain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale

// Base URL for your Python HTTP server
private const val HTTP_SENSOR_URL = "http://10.0.2.2:8080/sensor"

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    // 1. Initialize Database & ML Engine
    private val sensorDao = AppDatabase.getDatabase(application).sensorDataDao()
    private val mlExecutor = StressModelExecutor(application) // The AI Brain

    private val _httpSensorData = MutableStateFlow("Loading...")
    val httpSensorData: StateFlow<String> = _httpSensorData

    fun fetchHttpSensorData() {
        viewModelScope.launch {
            _httpSensorData.value = try {
                fetchDataFromHttp()
            } catch (e: Exception) {
                "Connection failed: ${e.message}"
            }
        }
    }

    private suspend fun fetchDataFromHttp(): String {
        return withContext(Dispatchers.IO) {
            try {
                // --- A. Network Request ---
                val url = URL(HTTP_SENSOR_URL)
                val connection = url.openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val response = connection.getInputStream().bufferedReader().use { it.readText() }

                val json = JSONObject(response)

                // --- B. Parse Raw Data ---
                val heartRate = json.optInt("heartRate", 0)
                val pulseRaw = json.optInt("pulseRaw", 0)
                val temp = json.optDouble("temperature", 0.0).toFloat()
                val touchStatus = json.optInt("touch", 0)
                val irDetected = json.optInt("irDetected", 0)
                val hrv = json.optDouble("hrv", 0.0).toFloat()

                // --- C. Personalization (The "Memory") ---
                // 1. Get user's history from DB to establish a baseline
                // Look back 24 hours (86400000 ms)
                val yesterday = System.currentTimeMillis() - 86400000

                // Fetch average HRV from DB. If no history exists (first run), use default 40.0
                val baselineHRV = sensorDao.getAverageHrvSince(yesterday) ?: 40.0f
                val baselineHR = 70.0f // You can add getAverageHrSince() to DAO later for full personalization

                // --- D. ML Inference (The "Brain") ---
                // 2. Feed live data + baseline history into the model
                val prediction = mlExecutor.predictStress(
                    currentHR = heartRate.toFloat(),
                    currentHRV = hrv,
                    currentTemp = temp,
                    baselineHR = baselineHR,
                    baselineHRV = baselineHRV
                )

                if (prediction.isStress) {
                    Log.w("AnimaAI", "High Stress Detected! Confidence: ${prediction.confidence}")
                }

                // --- E. Save to Database ---
                // 3. Store the result so we can learn from it later
                val reading = SensorReading(
                    timestamp = System.currentTimeMillis(),
                    heartRate = heartRate.toFloat(),
                    hrv = hrv,
                    eda = null, // Placeholder for future EDA sensor
                    skinTemperature = temp,
                    pupilDiameter = null, // Placeholder for Eye Tracking
                    blinkRate = null,
                    motionActivity = if (touchStatus == 1) 1 else 0,
                    stressLevel = prediction.confidence, // Save the calculated AI confidence (0.0 - 1.0)
                    anomalyScore = if (prediction.isStress) 1.0f else 0.0f
                )
                sensorDao.insertReading(reading)
                Log.d("DashboardViewModel", "Saved Reading -> Stress: ${(prediction.confidence * 100).toInt()}%")

                // --- F. Format Output for UI ---
                val hrOutput = if (heartRate > 0) heartRate.toString() else "—"
                val stressStatus = if (prediction.isStress) "⚠️ HIGH" else "Normal"

                // Create a status string that shows real data + AI analysis
                "HR:$hrOutput | Stress:${(prediction.confidence * 100).toInt()}% ($stressStatus) | T:${
                    String.format(Locale.getDefault(), "%.1f", temp)
                }°C"

            } catch (e: Exception) {
                Log.e("DashboardViewModel", "HTTP fetch failed", e)
                throw e
            }
        }
    }
}