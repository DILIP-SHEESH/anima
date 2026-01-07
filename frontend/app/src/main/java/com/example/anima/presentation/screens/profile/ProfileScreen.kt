package com.example.anima.presentation.screens.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.anima.presentation.screens.dashboard.DashboardViewModel
import java.util.Locale

// --- Clinical Color Palette ---
val ClinicalRed = Color(0xFFE57373)   // High Stress / Alert
val ClinicalGreen = Color(0xFF81C784) // Normal / Safe
val ClinicalBlue = Color(0xFF64B5F6)  // Calm / Sleep
val ClinicalAmber = Color(0xFFFFD54F) // Activity / Warning
val ClinicalPurple = Color(0xFF9575CD) // Sensors / Radar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel = viewModel(),
    // We inject DashboardViewModel here to use its "Fetch" capability
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    // --- OBSERVE DATA FROM DATABASE ---
    val latestReading by profileViewModel.latestReading.collectAsState()
    val hrHistory by profileViewModel.heartRateHistory.collectAsState()
    val stressHistory by profileViewModel.stressHistory.collectAsState()
    val tempHistory by profileViewModel.tempHistory.collectAsState()

    // Snackbars for feedback
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Live Medical Profile", fontWeight = FontWeight.Bold)
                        Text(
                            "ID: #ANIMA-8821 | Status: ${if (latestReading != null) "Online" else "Connecting..."}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    // --- NEW REFRESH BUTTON ---
                    IconButton(onClick = {
                        dashboardViewModel.fetchHttpSensorData() // <--- TRIGGERS THE SYNC
                    }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Sync Now")
                    }
                    IconButton(onClick = { /* Export Logic */ }) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = "Export")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Patient Header
            item { PatientHeader(isActive = latestReading != null) }

            // 2. Real-time Grid (ALL SENSORS ADDED)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Sensor Array (Live)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // Mini Sync Indicator
                    Text(
                        "Tap ↻ to Sync",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Format Helpers
                fun fmt(v: Float?): String = if (v == null || v == 0f) "--" else String.format(Locale.US, "%.1f", v)
                fun boolStatus(v: Int?, yes: String, no: String): String = if (v == 1) yes else no

                // --- GRID OF ALL SENSORS ---
                BiomarkerGrid(
                    hr = fmt(latestReading?.heartRate),
                    stress = fmt((latestReading?.stressLevel ?: 0f) * 100),
                    temp = fmt(latestReading?.skinTemperature),
                    motion = boolStatus(latestReading?.motionActivity, "Active", "Resting"),
                    // These come from "anomalyScore" or custom mappings if you saved them
                    // Since we saved raw sensor data in 'motionActivity' or similar, we display what we have:
                    radar = if (latestReading?.motionActivity == 1) "Motion" else "Clear",
                    touch = if (latestReading?.motionActivity == 1) "Contact" else "None"
                    // Note: Ideally, map specific columns for Radar/Touch in DB if you want separate history.
                    // For now, we use the Motion/Activity flag as a proxy for the grid demo.
                )
            }

            // 3. Stress Graph
            item {
                ClinicalTrendCard(
                    title = "Stress Response Trend",
                    subtitle = "AI Confidence Level (0-100%)",
                    dataPoints = stressHistory,
                    chartColor = ClinicalRed
                )
            }

            // 4. Heart Rate Graph
            item {
                ClinicalTrendCard(
                    title = "Heart Rate Variability",
                    subtitle = "Beats Per Minute (Last 30 readings)",
                    dataPoints = hrHistory,
                    chartColor = ClinicalGreen
                )
            }

            // 5. Temperature Graph
            item {
                ClinicalTrendCard(
                    title = "Skin Temperature",
                    subtitle = "Thermal Regulation (°C)",
                    dataPoints = tempHistory,
                    chartColor = ClinicalBlue
                )
            }

            // 6. Footer
            item {
                Text("Device Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ManagementOption(Icons.Outlined.History, "View Full Sensor Logs", "Export CSV")
                ManagementOption(Icons.Outlined.Sensors, "Sensor Diagnostics", "Calibrate MPU6050 & Radar")
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// -----------------------------------------------------------------------------
// UI COMPONENTS
// -----------------------------------------------------------------------------

@Composable
fun PatientHeader(isActive: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text("JS", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text("John Snow", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("Age: 29 | Blood: O+", style = MaterialTheme.typography.bodyMedium)
            val color = if (isActive) ClinicalGreen else ClinicalRed
            AssistChip(
                onClick = {},
                label = { Text(if (isActive) "Receiving Data" else "Offline") },
                leadingIcon = { Icon(Icons.Outlined.MonitorHeart, null, Modifier.size(16.dp), tint = color) },
                colors = AssistChipDefaults.assistChipColors(containerColor = color.copy(alpha = 0.1f))
            )
        }
    }
}

@Composable
fun BiomarkerGrid(hr: String, stress: String, temp: String, motion: String, radar: String, touch: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Row 1: Vital Signs
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BiomarkerCard(Modifier.weight(1f), "Heart Rate", hr, "BPM", "PPG Sensor", ClinicalGreen)
            BiomarkerCard(Modifier.weight(1f), "Stress Level", stress, "%", "AI Analysis", ClinicalRed)
        }
        // Row 2: Environmental/Physical
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BiomarkerCard(Modifier.weight(1f), "Skin Temp", temp, "°C", "Thermistor", ClinicalBlue)
            BiomarkerCard(Modifier.weight(1f), "Body State", motion, "", "Gyroscope", ClinicalAmber)
        }
        // Row 3: Interaction Sensors (Radar & Touch)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BiomarkerCard(Modifier.weight(1f), "Radar Field", radar, "", "Motion", ClinicalPurple)
            BiomarkerCard(Modifier.weight(1f), "Touch Input", touch, "", "Capacitive", ClinicalPurple)
        }
    }
}

@Composable
fun BiomarkerCard(modifier: Modifier, title: String, value: String, unit: String, sensorName: String, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                Icon(Icons.Default.Circle, null, tint = color, modifier = Modifier.size(8.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(unit, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 2.dp))
                }
            }
            Text(sensorName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ClinicalTrendCard(title: String, subtitle: String, dataPoints: List<Float>, chartColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                if (dataPoints.isEmpty()) {
                    Text("Waiting for data...", Modifier.align(Alignment.Center), style = MaterialTheme.typography.bodySmall)
                } else {
                    LineChart(dataPoints, chartColor)
                }
            }
        }
    }
}

@Composable
fun LineChart(dataPoints: List<Float>, lineColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (dataPoints.size < 2) return@Canvas
        val maxData = dataPoints.maxOrNull()?.coerceAtLeast(1f) ?: 100f
        val minData = dataPoints.minOrNull() ?: 0f
        val range = (maxData - minData).coerceAtLeast(1f)
        val widthPerPoint = size.width / (dataPoints.size - 1)
        val height = size.height
        val path = Path()

        dataPoints.forEachIndexed { index, data ->
            val x = index * widthPerPoint
            val normalizedY = (data - minData) / range
            val y = height - (normalizedY * height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(3.dp.toPx()))
        val fillPath = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fillPath, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.3f), Color.Transparent)))
    }
}

@Composable
fun ManagementOption(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null)
    }
}