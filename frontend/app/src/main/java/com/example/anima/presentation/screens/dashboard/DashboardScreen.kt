package com.example.anima.presentation.screens.dashboard


import androidx.health.connect.client.PermissionController
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
import androidx.activity.compose.rememberLauncherForActivityResult
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
import com.example.anima.data.digitalwellbeing.HealthConnectManager
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

    // --- State: Usage Stats ---
    var hasUsagePermission by remember { mutableStateOf(hasUsagePermission(context)) }
    var topApps by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }
    var totalScreenTime by remember { mutableStateOf(0L) }
    var refreshing by remember { mutableStateOf(false) }

    // --- State: Health Connect (NEW) ---
    val healthManager = remember { HealthConnectManager(context) }
    var stepsToday by remember { mutableStateOf(0L) }
    var sleepDuration by remember { mutableStateOf(0L) }
    var hasHealthPermission by remember { mutableStateOf(false) }

    // --- State: Sensor Data ---
    val httpSensorData by viewModel.httpSensorData.collectAsState()

    // --- Health Connect Permission Launcher ---
// --- Health Connect Permission Launcher ---
    // Fix: Use PermissionController.createRequestPermissionResultContract() directly
    // Fix: Explicitly type 'granted: Set<String>' to solve inference errors
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted: Set<String> ->
        if (granted.containsAll(healthManager.permissions)) {
            hasHealthPermission = true
            // Permission granted, fetch data immediately
            coroutineScope.launch {
                stepsToday = healthManager.getStepsToday()
                sleepDuration = healthManager.getSleepDurationMinutes()
            }
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        // 1. Check Usage Stats
        hasUsagePermission = hasUsagePermission(context)
        if (hasUsagePermission) {
            val stats = withContext(Dispatchers.IO) { getAppUsageStats(context) }
            topApps = getTopUsedApps(stats)
            totalScreenTime = getRealScreenTime(context)
        }

        // 2. Check Health Connect
        hasHealthPermission = healthManager.hasPermissions()
        if (hasHealthPermission) {
            stepsToday = healthManager.getStepsToday()
            sleepDuration = healthManager.getSleepDurationMinutes()
        }

        // 3. Fetch Server Data
        viewModel.fetchHttpSensorData()
    }

    fun refreshData() {
        refreshing = true
        coroutineScope.launch {
            try {
                // Refresh Usage Stats
                if (hasUsagePermission) {
                    val stats = withContext(Dispatchers.IO) { getAppUsageStats(context) }
                    topApps = getTopUsedApps(stats)
                    totalScreenTime = getRealScreenTime(context)
                }

                // Refresh Health Data
                if (hasHealthPermission) {
                    stepsToday = healthManager.getStepsToday()
                    sleepDuration = healthManager.getSleepDurationMinutes()
                }

                // Refresh Server Data
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
            text = "Anima Dashboard",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        // REFRESH BUTTON
        Button(
            onClick = { refreshData() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !refreshing
        ) {
            Text(if (refreshing) "Refreshing..." else "Refresh Data")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ==================== SECTION 1: PHYSICAL WELLBEING (HEALTH CONNECT) ====================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Physical Wellbeing (Google Fit)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (hasHealthPermission) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Steps Today", style = MaterialTheme.typography.labelMedium)
                            Text("$stepsToday", style = MaterialTheme.typography.headlineMedium)
                        }
                        Column {
                            Text("Sleep", style = MaterialTheme.typography.labelMedium)
                            Text("${sleepDuration / 60}h ${sleepDuration % 60}m", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                } else {
                    Text(
                        text = "Connect to see Steps & Sleep data.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { healthPermissionLauncher.launch(healthManager.permissions) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Connect Health Data")
                    }
                }
            }
        }

        // ==================== SECTION 2: DIGITAL WELLBEING (USAGE STATS) ====================
        if (!hasUsagePermission) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Digital Wellbeing Permission Needed", fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { openUsageAccessSettings(context) },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Grant Usage Access")
                    }
                }
            }
        } else {
            Text(
                text = "Top Used Apps (Screen Time: ${formatTime(totalScreenTime)})",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                if (topApps.isEmpty()) {
                    item { Text("No usage data yet today.") }
                } else {
                    items(topApps) { (packageName, time) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = getAppName(context, packageName),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = formatTime(time),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==================== SECTION 3: SENSOR DATA & THRESHOLDS ====================
        // Scrollable container for the rest so it doesn't get cut off on small screens
        LazyColumn(modifier = Modifier.weight(1f)) {

            // HTTP Data
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Live Sensor Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = httpSensorData, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // Thresholds
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Physiological Thresholds", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        physiologicalThresholds.forEach { threshold ->
                            Text(
                                text = "${threshold.name}: ${threshold.anomalyThreshold}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
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