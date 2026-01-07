package com.example.anima.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// 1. Table for individual sensor readings
@Entity(tableName = "sensor_readings")
data class SensorReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val heartRate: Float?,
    val hrv: Float?,
    val eda: Float?,
    val skinTemperature: Float?,
    val pupilDiameter: Float?,
    val blinkRate: Float?,
    val motionActivity: Int,
    val stressLevel: Float?,
    val anomalyScore: Float,

    // 👇 THIS FIELD IS CRITICAL. DO NOT DELETE IT.
    val userLabel: Boolean? = null
)

// 2. Table for session summaries
@Entity(tableName = "user_sessions")
data class UserSession(
    @PrimaryKey val sessionId: String,
    val startTime: Long,
    val endTime: Long?,
    val sessionType: String,
    val averageStress: Float,
    val peakStress: Float,
    val environmentalContext: String
)