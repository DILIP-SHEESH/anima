package com.example.anima.presentation.screens.dashboard

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/* -------------------- Data Structures for Hardcoded Thresholds -------------------- */
data class SensorThreshold(
    val name: String,
    val normalRange: String,
    val anomalyThreshold: String,
    val interpretation: String
)

val physiologicalThresholds = listOf(
    SensorThreshold(
        name = "Heart Rate (BPM)",
        normalRange = "Resting adult: 60–100 bpm",
        anomalyThreshold = ">100 bpm (tachycardia) or <60 bpm (bradycardia) at rest; persistent >120 bpm is critical",
        interpretation = "High resting HR (tachycardia) often correlates with anxiety or stress. Persistent high is critical."
    ),
    SensorThreshold(
        name = "HRV (RMSSD)",
        normalRange = "20–89 ms (typically 30–60 ms)",
        anomalyThreshold = "<20 ms or sudden drop ≥20–30% from baseline",
        interpretation = "Low RMSSD correlates strongly with high stress, anxiety, or poor recovery."
    ),
    SensorThreshold(
        name = "EDA—SCL (μS)",
        normalRange = "Resting SCL: 1–5 μS typical",
        anomalyThreshold = "Significant, sustained increase from baseline; SCL >5 μS may be marked arousal",
        interpretation = "SCL rises with stress/arousal. Spikes/bursts indicate acute events."
    ),
    SensorThreshold(
        name = "EDA—SCRs",
        normalRange = "NS.SCRs: 1–5/min at rest",
        anomalyThreshold = ">10 SCRs/min (sustained)",
        interpretation = "Higher frequency/amplitude reflects stronger acute sympathetic nervous system stress."
    ),
    SensorThreshold(
        name = "Skin Temperature (°C)",
        normalRange = "34.0–36.0°C on skin",
        anomalyThreshold = "Rapid drop ≥1°C from baseline during stress; chronic low = vasoconstriction",
        interpretation = "Stress causes short-term skin temperature drop (vasoconstriction). Use change from user's baseline."
    ),
    SensorThreshold(
        name = "Pupil Diameter (mm)",
        normalRange = "2–8 mm range overall",
        anomalyThreshold = "Task-evoked dilation ≥0.5 mm above baseline for similar light",
        interpretation = "Dilation tracks cognitive load and emotional stress. Relative increase matters most."
    ),
    SensorThreshold(
        name = "IMU (Motion/Sleep)",
        normalRange = "Contextual; low variance = rest/sleep",
        anomalyThreshold = "High activity (spikes) during expected rest/sleep periods",
        interpretation = "Used for artefact suppression and contextual analysis, especially with sleep/activity schedules."
    )
)
/* ---------------------------------------------------------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(hasUsagePermission(context)) }
    var topApps by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }
    var totalScreenTime by remember { mutableStateOf(0L) }
    var refreshing by remember { mutableStateOf(false) }

    val httpSensorData by viewModel.httpSensorData.collectAsState()

    // Initial load
    LaunchedEffect(Unit) {
        hasPermission = hasUsagePermission(context)
        viewModel.fetchHttpSensorData()
    }

    fun refreshData() {
        refreshing = true
        coroutineScope.launch {
            try {
                if (hasPermission) {
                    val stats = withContext(Dispatchers.IO) { getAppUsageStats(context) }
                    topApps = getTopUsedApps(stats)
                    totalScreenTime = getRealScreenTime(context)
                }
                viewModel.fetchHttpSensorData()
            } catch (e: Exception) {
                Log.e("DashboardScreen", "Refresh failed", e)
            } finally {
                refreshing = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Digital Wellbeing Dashboard",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (!hasPermission) {
            Text(
                text = "To see your screen time and app usage, please grant Usage Access permission.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { openUsageAccessSettings(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Usage Access")
            }
        } else {
            Button(
                onClick = { refreshData() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !refreshing
            ) {
                Text(if (refreshing) "Refreshing..." else "Refresh Now")
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (topApps.isEmpty()) {
                Text(
                    text = if (refreshing) "Loading app usage data..." else "No app usage data available today.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "Top 5 Most Used Apps",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(topApps) { (packageName, time) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = getAppName(context, packageName),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Used for ${formatTime(time)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- HTTP Sensor Card (Existing) ----
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Sensor Data from Server",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = httpSensorData,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Physiological Anomaly Thresholds Card (NEW STUFF) ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Physiological Anomaly Thresholds (Reference)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(physiologicalThresholds) { threshold ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = threshold.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Normal: ${threshold.normalRange}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = "Anomaly: ${threshold.anomalyThreshold}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Note: ${threshold.interpretation}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Divider(modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/* -------------------- Helper Functions -------------------- */
// (All existing helper functions remain unchanged)

fun hasUsagePermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

fun openUsageAccessSettings(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    context.startActivity(intent)
}

fun getAppUsageStats(context: Context): List<UsageStats> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startTime = calendar.timeInMillis
    val endTime = System.currentTimeMillis()

    return usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        startTime,
        endTime
    ) ?: emptyList()
}

fun getTopUsedApps(stats: List<UsageStats>): List<Pair<String, Long>> {
    return stats
        .filter { it.totalTimeInForeground > 0 }
        .sortedByDescending { it.totalTimeInForeground }
        .take(5)
        .map { it.packageName to it.totalTimeInForeground }
}

fun getAppName(context: Context, packageName: String): String {
    return try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        packageName
    }
}

fun getRealScreenTime(context: Context): Long {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startTime = calendar.timeInMillis
    val endTime = System.currentTimeMillis()

    val events = usageStatsManager.queryEvents(startTime, endTime)
    var totalTime = 0L
    var lastResumeTime = 0L

    while (events.hasNextEvent()) {
        val event = UsageEvents.Event()
        events.getNextEvent(event)

        when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> lastResumeTime = event.timeStamp
            UsageEvents.Event.ACTIVITY_PAUSED -> {
                if (lastResumeTime > 0) {
                    totalTime += (event.timeStamp - lastResumeTime)
                    lastResumeTime = 0
                }
            }
        }
    }

    if (lastResumeTime > 0) {
        totalTime += (endTime - lastResumeTime)
    }

    return totalTime
}

fun formatTime(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))

    return when {
        hours > 0 -> String.format(Locale.getDefault(), "%dh %dm", hours, minutes)
        minutes > 0 -> String.format(Locale.getDefault(), "%dm %ds", minutes, seconds)
        else -> String.format(Locale.getDefault(), "%ds", seconds)
    }
}