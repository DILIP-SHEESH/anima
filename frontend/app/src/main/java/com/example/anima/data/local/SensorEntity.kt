package com.example.anima.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// 1. Table for individual sensor readings (every second)
@Entity(tableName = "sensor_readings")
data class SensorReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val heartRate: Float?,        // BPM
    val hrv: Float?,             // ms (RMSSD)
    val eda: Float?,             // µS
    val skinTemperature: Float?, // °C
    val pupilDiameter: Float?,   // mm
    val blinkRate: Float?,       // blinks/min
    val motionActivity: Int,     // 0=Rest, 1=Walk, 2=Run
    val stressLevel: Float?,     // 0.0 to 1.0
    val anomalyScore: Float      // Output from your Anomaly Algo
)

// 2. Table for session summaries (e.g., "Afternoon Walk")
@Entity(tableName = "user_sessions")
data class UserSession(
    @PrimaryKey val sessionId: String,
    val startTime: Long,
    val endTime: Long?,
    val sessionType: String,       // "Work", "Relax", "Commute"
    val averageStress: Float,
    val peakStress: Float,
    val environmentalContext: String // "Office", "Home"
)