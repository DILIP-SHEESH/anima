package com.example.anima.data.digitalwellbeing

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.temporal.ChronoUnit

class HealthConnectManager(private val context: Context) {

    // 1. Check if Health Connect is available
    val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    // 2. Define permissions we need
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class)
    )

    // 3. Check if permissions are granted
    suspend fun hasPermissions(): Boolean {
        return healthConnectClient.permissionController.getGrantedPermissions()
            .containsAll(permissions)
    }

    // 4. GET STEPS TODAY
    suspend fun getStepsToday(): Long {
        if (!hasPermissions()) return 0L

        val todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS)
        val now = Instant.now()

        return try {
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(todayStart, now)
                )
            )
            response[StepsRecord.COUNT_TOTAL] ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    // 5. GET SLEEP DURATION (Minutes) - This was missing!
    suspend fun getSleepDurationMinutes(): Long {
        if (!hasPermissions()) return 0L

        // Look back 24 hours
        val yesterday = Instant.now().minus(1, ChronoUnit.DAYS)
        val now = Instant.now()

        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(yesterday, now)
                )
            )
            // Sum up duration of all sleep sessions found
            val totalSeconds = response.records.sumOf { record ->
                java.time.Duration.between(record.startTime, record.endTime).seconds
            }
            totalSeconds / 60 // Convert to minutes
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }
}