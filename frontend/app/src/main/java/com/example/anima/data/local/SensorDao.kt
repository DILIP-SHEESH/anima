package com.example.anima.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDao {
    // SAVE Data
    @Insert
    suspend fun insertReading(reading: SensorReading)

    @Insert
    suspend fun insertSession(session: UserSession)

    // READ Data for Graphs (Last N minutes)
    @Query("SELECT * FROM sensor_readings WHERE timestamp >= :startTime ORDER BY timestamp ASC")
    fun getReadingsSince(startTime: Long): Flow<List<SensorReading>>

    // READ Data for "History" Card (Last 24 hours stats)
    @Query("SELECT AVG(hrv) FROM sensor_readings WHERE timestamp >= :startTime")
    suspend fun getAverageHrvSince(startTime: Long): Float?

    // READ Anomalies for Alerts
    @Query("SELECT * FROM sensor_readings WHERE anomalyScore > :threshold ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentAnomalies(threshold: Float, limit: Int): List<SensorReading>
}