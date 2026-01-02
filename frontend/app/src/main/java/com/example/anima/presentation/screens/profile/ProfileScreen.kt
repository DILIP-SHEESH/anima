package com.example.anima.presentation.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.anima.R // Assuming R contains drawable resources

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "User Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Avatar Placeholder (Formal Look)
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder Icon for a professional touch
                Icon(
                    Icons.Default.Star,
                    contentDescription = "User Avatar",
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Info
            Text(
                text = "John snow",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
            )
            Text(
                text = "umar@email.com",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Mental Health Stats Card (Wellness Metrics)
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Physiological Baseline Metrics",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Stat Item 1: Directly related to the monitoring goal
                        StatItem("Baseline HR", "65 BPM")

                        // Stat Item 2: Summary of anomaly detection
                        StatItem("Stress Index", "Low (1.2/5)")

                        // Stat Item 3: Contextual metric
                        StatItem("Sleep Avg", "6h 45m")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Graph for a quick overview, e.g., daily stress index trend
                    Text(
                        text = "Recent Stress Index Trend",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    GraphPlaceholder(Modifier.fillMaxWidth().height(100.dp), MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Historical Data Graphs Card (NEW ADDITION)
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Historical Data & Trends",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))

                    Text(
                        text = "Heart Rate Variability (HRV) Over Time",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    GraphPlaceholder(Modifier.fillMaxWidth().height(120.dp), MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Skin Conductance (EDA) Daily Peaks",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    GraphPlaceholder(Modifier.fillMaxWidth().height(120.dp), MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Actions
            ProfileOption(
                icon = Icons.Filled.Edit,
                title = "Manage Account Details",
                description = "Update personal information and contact settings",
                onClick = { /* TODO: Open Edit Profile Screen */ }
            )
            ProfileOption(
                icon = Icons.Filled.Settings,
                title = "Application Configuration",
                description = "Adjust sensor calibration and notification settings",
                onClick = { /* TODO: Navigate to Settings Screen */ }
            )
            ProfileOption(
                icon = Icons.Filled.Logout,
                title = "End Session & Logout",
                description = "Securely log out of the monitoring system",
                onClick = { /* TODO: Implement Logout */ }
            )
        }
    }
}

@Composable
fun StatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
        Text(
            text = value,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun ProfileOption(icon: ImageVector, title: String, description: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun GraphPlaceholder(modifier: Modifier = Modifier, graphColor: Color = Color.Gray) {
    Box(
        modifier = modifier
            .background(
                color = graphColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // This is a simple visual placeholder. In a real app,
        // you would integrate a charting library here (e.g., MPAndroidChart, Compose-Charts).
        Text(
            text = "Graph Placeholder",
            color = graphColor.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}