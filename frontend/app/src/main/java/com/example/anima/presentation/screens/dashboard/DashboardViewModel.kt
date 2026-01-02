package com.example.anima.presentation.screens.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class DashboardViewModel : ViewModel() {

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
                val url = URL(HTTP_SENSOR_URL)
                val connection = url.openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val response = connection.getInputStream().bufferedReader().use { it.readText() }

                val json = JSONObject(response)

                // 🟢 UPDATED: Capture all relevant keys based on your system output
                val heartRate = json.optInt("heartRate", 0)
                val pulseRaw = json.optInt("pulseRaw", 0)
                val temp = json.optDouble("temperature", 0.0)
                val touchStatus = json.optInt("touch", 0)
                val irDetected = json.optInt("irDetected", 0)

                // 🟢 UPDATED: Format the output to display Pulse Raw and Touch Status
                val hrOutput = if (heartRate > 0) heartRate.toString() else "—"
                val touchOutput = if (touchStatus == 1) "1 (Touched)" else "0 (Clear)"
                val irOutput = if (irDetected == 1) "1 (Detected)" else "0 (Clear)"


                "HR:$hrOutput | P:$pulseRaw | T:${
                    String.format(
                        Locale.getDefault(),
                        "%.1f",
                        temp
                    )
                }°C | Touch:$touchOutput | IR:$irOutput"

            } catch (e: Exception) {
                Log.e("DashboardViewModel", "HTTP fetch failed", e)
                throw e
            }
        }
    }
}