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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.anima.data.digitalwellbeing.HealthConnectManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/* -------------------- Data Structures -------------------- */

data class SensorState(
    val status: String = "Syncing...",
    val heartRate: Int = 0,
    val pulseRaw: Int = 0,
    val temp: Float = 0f,
    val gyroX: Int = 0,
    val gyroY: Int = 0,
    val gyroZ: Int = 0,
    val touch: Int = 0,
    val ir: Int = 0,
    val radar: Int = 0
)

data class SensorThreshold(
    val title: String,
    val value: String,
    val desc: String
)

/* -------------------- Knowledge Base -------------------- */
val sensorGuideData = listOf(
    SensorThreshold("Heart Rate (PPG)", "60-100 BPM", "Resting rate. >100 stationary suggests stress."),
    SensorThreshold("Pulse Raw", "Signal Amp", "Raw photoplethysmogram amplitude. <1000 means loose sensor."),
    SensorThreshold("Gyroscope (IMU)", "Deg/Sec", "Measures angular velocity. Spikes >30k indicate sudden impact."),
    SensorThreshold("Radar (Doppler)", "0 (Still) / 1 (Move)", "Detects micro-movements like breathing through clothes."),
    SensorThreshold("IR Proximity", "0 (Off) / 1 (On)", "Safety check: ensures glasses are worn before alerting.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- State ---
    val httpSensorData by viewModel.httpSensorData.collectAsState()
    var sensorState by remember { mutableStateOf(SensorState()) }

    // Usage & Health States
    val healthManager = remember { HealthConnectManager(context) }
    var hasUsagePermission by remember { mutableStateOf(hasUsagePermission(context)) }
    var hasHealthPermission by remember { mutableStateOf(false) }
    var stepsToday by remember { mutableStateOf(0L) }
    var sleepDuration by remember { mutableStateOf(0L) }
    var topApps by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }
    var totalScreenTime by remember { mutableStateOf(0L) }
    var refreshing by remember { mutableStateOf(false) }

    // Parse Data on Change
    LaunchedEffect(httpSensorData) {
        sensorState = parseSensorData(httpSensorData)
    }

    // Permission Launcher
    val healthLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(healthManager.permissions)) {
            hasHealthPermission = true
            coroutineScope.launch {
                stepsToday = healthManager.getStepsToday()
                sleepDuration = healthManager.getSleepDurationMinutes()
            }
        }
    }

    // Initial Load
    LaunchedEffect(Unit) {
        hasUsagePermission = hasUsagePermission(context)
        if (hasUsagePermission) {
            val stats = withContext(Dispatchers.IO) { getAppUsageStats(context) }
            topApps = getTopUsedApps(stats)
            totalScreenTime = getRealScreenTime(context)
        }
        hasHealthPermission = healthManager.hasPermissions()
        if (hasHealthPermission) {
            stepsToday = healthManager.getStepsToday()
            sleepDuration = healthManager.getSleepDurationMinutes()
        }
        viewModel.fetchHttpSensorData()
    }

    fun refreshData() {
        refreshing = true
        coroutineScope.launch {
            if (hasUsagePermission) {
                val stats = withContext(Dispatchers.IO) { getAppUsageStats(context) }
                topApps = getTopUsedApps(stats)
                totalScreenTime = getRealScreenTime(context)
            }
            if (hasHealthPermission) {
                stepsToday = healthManager.getStepsToday()
                sleepDuration = healthManager.getSleepDurationMinutes()
            }
            viewModel.fetchHttpSensorData()
            refreshing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Anima Dashboard", fontWeight = FontWeight.Bold)
                        Text("Connected: ESP32-S3", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                },
                actions = {
                    IconButton(onClick = { refreshData() }) {
                        if (refreshing) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. STATUS CARD
            item { StatusContextCard(sensorState) }

            // 2. DETAILED RAW SENSOR GRID
            item {
                Text("Raw Telemetry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                RawSensorGrid(sensorState)
            }

            // 3. PHYSICAL HEALTH
            item {
                HealthConnectCard(
                    hasPermission = hasHealthPermission,
                    steps = stepsToday,
                    sleepMinutes = sleepDuration,
                    onConnect = { healthLauncher.launch(healthManager.permissions) }
                )
            }

            // 4. DIGITAL WELLBEING
            item {
                DigitalWellbeingCard(
                    hasPermission = hasUsagePermission,
                    screenTime = totalScreenTime,
                    topApps = topApps,
                    onGrant = { openUsageAccessSettings(context) }
                )
            }

            // 5. FAQ GUIDE
            item { SensorGuideSection() }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

// -----------------------------------------------------------------------------
// UI COMPONENTS
// -----------------------------------------------------------------------------

@Composable
fun StatusContextCard(state: SensorState) {
    val isStressed = state.heartRate > 100 && state.status.contains("Sitting", true)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isStressed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isStressed) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("System Status", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = state.status.uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RawSensorGrid(state: SensorState) {
    // We manually layout the grid to ensure specific grouping
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // ROW 1: Vital Signs
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SensorTile(Modifier.weight(1f), "Heart Rate", "${state.heartRate}", "BPM", Icons.Rounded.Favorite, Color(0xFFE57373))
            SensorTile(Modifier.weight(1f), "Skin Temp", "${state.temp}", "°C", Icons.Rounded.Thermostat, Color(0xFF64B5F6))
        }

        // ROW 2: Motion (Gyroscope)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Explore, null, Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(Modifier.width(8.dp))
                    Text("Gyroscope (IMU)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("X: ${state.gyroX}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Text("Y: ${state.gyroY}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Text("Z: ${state.gyroZ}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }

        // ROW 3: Hardware State (Radar, IR, Touch, Pulse)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Pulse Raw
            SensorTile(Modifier.weight(1f), "Pulse Raw", "${state.pulseRaw}", "Amp", Icons.Rounded.GraphicEq, Color(0xFF9575CD))
            // Radar
            BinarySensorTile(Modifier.weight(1f), "Radar", state.radar == 1, Icons.Rounded.Radar)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // IR
            BinarySensorTile(Modifier.weight(1f), "IR Sensor", state.ir == 1, Icons.Rounded.Visibility)
            // Touch
            BinarySensorTile(Modifier.weight(1f), "Touch", state.touch == 1, Icons.Rounded.TouchApp)
        }
    }
}

@Composable
fun SensorTile(modifier: Modifier, title: String, value: String, unit: String, icon: ImageVector, color: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                Text(unit, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun BinarySensorTile(modifier: Modifier, title: String, isActive: Boolean, icon: ImageVector) {
    val activeColor = Color(0xFF4CAF50) // Green
    val inactiveColor = Color.Gray

    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(if (isActive) "DETECTED" else "IDLE", style = MaterialTheme.typography.labelSmall, color = if(isActive) activeColor else inactiveColor)
            }
            Icon(icon, null, tint = if (isActive) activeColor else inactiveColor)
        }
    }
}

// ... (Existing Cards: HealthConnectCard, DigitalWellbeingCard, SensorGuideSection - KEEP THEM AS THEY WERE) ...
@Composable
fun HealthConnectCard(hasPermission: Boolean, steps: Long, sleepMinutes: Long, onConnect: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HealthAndSafety, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Physical Wellbeing", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (hasPermission) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("Steps", style = MaterialTheme.typography.labelSmall); Text("$steps", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                    Column(horizontalAlignment = Alignment.End) { Text("Sleep", style = MaterialTheme.typography.labelSmall); Text("${sleepMinutes / 60}h ${sleepMinutes % 60}m", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                }
            } else {
                Text("Sync with Google Fit for better analysis.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = onConnect, modifier = Modifier.padding(top = 8.dp)) { Text("Connect Data") }
            }
        }
    }
}

@Composable
fun DigitalWellbeingCard(hasPermission: Boolean, screenTime: Long, topApps: List<Pair<String, Long>>, onGrant: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Smartphone, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Digital Wellbeing", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (!hasPermission) {
                Text("Screen time data permission needed.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = onGrant, modifier = Modifier.padding(top = 8.dp)) { Text("Allow Access") }
            } else {
                Text("Screen Time: ${formatTime(screenTime)}", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                topApps.take(3).forEach { (pkg, time) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(pkg.substringAfterLast('.').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }, style = MaterialTheme.typography.bodyMedium)
                        Text(formatTime(time), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun SensorGuideSection() {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sensor Guide & Thresholds", fontWeight = FontWeight.SemiBold)
                }
                Icon(if(expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    sensorGuideData.forEach { item ->
                        Text(item.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(item.value, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                        Text(item.desc, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 12.dp))
                        Divider(modifier = Modifier.padding(bottom = 12.dp))
                    }
                }
            }
        }
    }
}


// -------------------- PARSER LOGIC --------------------
fun parseSensorData(data: String): SensorState {
    return try {
        // Example: "Status: Walking\nHR: 72 BPM | Pulse: 2045 | T: 36.6°C\nGyro: X:-140 Y:20 Z:0\nTouch: 0 | IR: 1 | Radar: 1"
        val status = Regex("Status: (.*)").find(data)?.groupValues?.get(1)?.trim() ?: "Unknown"
        val hr = Regex("HR: (\\d+)").find(data)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val pulse = Regex("Pulse: (\\d+)").find(data)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val temp = Regex("T: ([\\d.]+)").find(data)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f

        val gyroX = Regex("X:(-?\\d+)").find(data)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val gyroY = Regex("Y:(-?\\d+)").find(data)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val gyroZ = Regex("Z:(-?\\d+)").find(data)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        val ir = Regex("IR: (\\d)").find(data)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val touch = Regex("Touch: (\\d)").find(data)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val radar = Regex("Radar: (\\d)").find(data)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        SensorState(status, hr, pulse, temp, gyroX, gyroY, gyroZ, touch, ir, radar)
    } catch (e: Exception) {
        SensorState()
    }
}

// -------------------- UTILS --------------------
fun hasUsagePermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

fun openUsageAccessSettings(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    context.startActivity(intent)
}

fun getAppUsageStats(context: Context): List<UsageStats> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, calendar.timeInMillis, System.currentTimeMillis()) ?: emptyList()
}

fun getTopUsedApps(stats: List<UsageStats>): List<Pair<String, Long>> {
    return stats.filter { it.totalTimeInForeground > 0 }
        .sortedByDescending { it.totalTimeInForeground }
        .take(5)
        .map { it.packageName to it.totalTimeInForeground }
}

fun getRealScreenTime(context: Context): Long {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val events = usageStatsManager.queryEvents(calendar.timeInMillis, System.currentTimeMillis())
    var totalTime = 0L
    var lastResumeTime = 0L
    while (events.hasNextEvent()) {
        val event = UsageEvents.Event()
        events.getNextEvent(event)
        when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> lastResumeTime = event.timeStamp
            UsageEvents.Event.ACTIVITY_PAUSED -> if (lastResumeTime > 0) {
                totalTime += (event.timeStamp - lastResumeTime)
                lastResumeTime = 0
            }
        }
    }
    if (lastResumeTime > 0) totalTime += (System.currentTimeMillis() - lastResumeTime)
    return totalTime
}

fun formatTime(millis: Long): String {
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}