package com.example.anima.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDao {
    // 1. FIX: Return 'Long' (The Row ID), NOT Unit/Void
    @Insert
    suspend fun insertReading(reading: SensorReading): Long

    @Insert
    suspend fun insertSession(session: UserSession)

    @Query("SELECT * FROM sensor_readings WHERE timestamp >= :startTime ORDER BY timestamp ASC")
    fun getReadingsSince(startTime: Long): Flow<List<SensorReading>>

    @Query("SELECT AVG(hrv) FROM sensor_readings WHERE timestamp >= :startTime")
    suspend fun getAverageHrvSince(startTime: Long): Float?

    @Query("SELECT * FROM sensor_readings WHERE anomalyScore > :threshold ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentAnomalies(threshold: Float, limit: Int): List<SensorReading>

    // 2. FIX: Add this missing function for the feedback loop
    @Query("UPDATE sensor_readings SET userLabel = :isAccurate WHERE id = :readingId")
    suspend fun updateUserLabel(readingId: Long, isAccurate: Boolean)
}