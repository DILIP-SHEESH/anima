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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// --- Color Palette for Clinical Data (Based on Medical UI standards) ---
val ClinicalRed = Color(0xFFE57373)   // High Stress / Alert
val ClinicalGreen = Color(0xFF81C784) // Normal / Recovery
val ClinicalBlue = Color(0xFF64B5F6)  // Sleep / Calm
val ClinicalAmber = Color(0xFFFFD54F) // Attention Needed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Medical Profile",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Patient ID: #ANIMA-8821",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = { /* Export PDF Logic */ }) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = "Export Report")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Changed to LazyColumn so the whole screen scrolls (crucial for phones)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Patient Header Card
            item {
                PatientHeader()
            }

            // 2. The "Doctor's View": Key Biomarkers Grid
            item {
                Text(
                    "Real-time Biomarkers (Avg 24h)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                BiomarkerGrid()
            }

            // 3. Clinical Trend Analysis (Custom Graph)
            item {
                ClinicalTrendCard(
                    title = "Stress Response Trend (HRV)",
                    subtitle = "Low HRV indicates high sympathetic nervous system activity (Stress)",
                    dataPoints = listOf(65f, 62f, 58f, 45f, 42f, 50f, 70f, 72f), // Mock HRV Data
                    chartColor = ClinicalRed
                )
            }

            // 4. Sleep & Recovery (Essential for Mental Health)
            item {
                ClinicalTrendCard(
                    title = "Sleep & Recovery Score",
                    subtitle = "Correlated with skin temperature drop",
                    dataPoints = listOf(80f, 85f, 60f, 55f, 90f, 88f, 82f),
                    chartColor = ClinicalBlue
                )
            }

            // 5. Patient Actions
            item {
                Text(
                    "Patient Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                ManagementOption(
                    icon = Icons.Outlined.History,
                    title = "View Full Medical History",
                    description = "Access logs from past 6 months"
                )
                ManagementOption(
                    icon = Icons.Default.Settings,
                    title = "Sensor Calibration",
                    description = "Recalibrate EDA and PPG thresholds"
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// -----------------------------------------------------------------------------
// UI COMPONENTS
// -----------------------------------------------------------------------------

@Composable
fun PatientHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "JS",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "John Snow",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Age: 29 | Blood: O+",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AssistChip(
                onClick = { },
                label = { Text("Monitoring Active") },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.MonitorHeart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ClinicalGreen
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = ClinicalGreen.copy(alpha = 0.1f),
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
fun BiomarkerGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BiomarkerCard(
                modifier = Modifier.weight(1f),
                title = "Heart Rate",
                value = "72",
                unit = "BPM",
                status = "Normal",
                color = ClinicalGreen
            )
            BiomarkerCard(
                modifier = Modifier.weight(1f),
                title = "HRV (Stress)",
                value = "42",
                unit = "ms",
                status = "Low Warning",
                color = ClinicalAmber
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BiomarkerCard(
                modifier = Modifier.weight(1f),
                title = "EDA (Sweat)",
                value = "1.2",
                unit = "µS",
                status = "Elevated",
                color = ClinicalRed // Red because EDA spikes mean acute stress
            )
            BiomarkerCard(
                modifier = Modifier.weight(1f),
                title = "Skin Temp",
                value = "36.4",
                unit = "°C",
                status = "Normal",
                color = ClinicalBlue
            )
        }
    }
}

@Composable
fun BiomarkerCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    status: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Default.Circle, contentDescription = null, tint = color, modifier = Modifier.size(8.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Text(unit, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
            }
            Text(
                status,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ClinicalTrendCard(
    title: String,
    subtitle: String,
    dataPoints: List<Float>,
    chartColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(16.dp))

            // CUSTOM DRAWING: A Real Graph instead of a placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                LineChart(dataPoints = dataPoints, lineColor = chartColor)
            }
        }
    }
}

@Composable
fun LineChart(dataPoints: List<Float>, lineColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (dataPoints.isEmpty()) return@Canvas

        // Normalize data to fit height
        val maxData = dataPoints.maxOrNull() ?: 100f
        val minData = dataPoints.minOrNull() ?: 0f
        val range = maxData - minData

        val widthPerPoint = size.width / (dataPoints.size - 1)
        val height = size.height

        val path = Path()

        dataPoints.forEachIndexed { index, data ->
            val x = index * widthPerPoint
            // Flip Y axis (0 is top in Canvas)
            val normalizedY = (data - minData) / range
            val y = height - (normalizedY * height)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                // Smooth curve (Cubic Bezier) logic can go here, using straight lines for simplicity
                path.lineTo(x, y)
            }
        }

        // Draw the line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw Fill Gradient
        val fillPath = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent)
            )
        )
    }
}

@Composable
fun ManagementOption(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}