package com.example.anima.presentation.screens.suggestions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import kotlinx.coroutines.delay

// --- Clean Clinical Colors ---
val ClinicalBackground = Color(0xFFF5F6F8) // Soft Gray/White
val PrimaryBlue = Color(0xFF1976D2)        // Trustworthy Blue
val SuccessGreen = Color(0xFF43A047)       // Medical Green
val AlertRed = Color(0xFFE53935)           // Emergency Red

// --- Data Models ---
data class DailyQuest(
    val id: Int,
    val title: String,
    val description: String,
    val xpReward: Int,
    val durationSeconds: Int, // Actual duration in seconds
    val type: QuestType,
    var isCompleted: Boolean = false
)

enum class QuestType {
    BREATHING, PHYSICAL, SLEEP, COGNITIVE
}

object SuggestionsRoutes {
    const val WOMEN_SAFETY = "women_safety_screen"
    const val CUSTOM_ACTION = "custom_action_screen"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsScreen(navController: NavController) {
    // Real Quest Data with Durations
    val initialQuests = listOf(
        DailyQuest(1, "Cortisol Reset", "Box breathing to lower heart rate.", 50, 180, QuestType.BREATHING), // 3 Mins
        DailyQuest(2, "Metabolic Walk", "Walk 500 steps to clear waste.", 30, 300, QuestType.PHYSICAL),
        DailyQuest(3, "Cognitive Drill", "Learn one new fact or skill.", 40, 120, QuestType.COGNITIVE),
        DailyQuest(4, "Sleep Hygiene", "No blue light for 20 mins.", 20, 1200, QuestType.SLEEP)
    )

    var quests by remember { mutableStateOf(initialQuests) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var activeQuest by remember { mutableStateOf<DailyQuest?>(null) }

    val haptic = LocalHapticFeedback.current

    Scaffold(
        containerColor = ClinicalBackground,
        topBar = {
            TopAppBar(
                title = { Text("Daily Interventions", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ClinicalBackground)
            )
        },
        floatingActionButton = {
            // Hardware Sensor Action Button
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Hardware Trigger Logic
                    navController.navigate(SuggestionsRoutes.WOMEN_SAFETY)
                },
                containerColor = AlertRed,
                contentColor = Color.White
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Icon(Icons.Default.TouchApp, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SENSOR ACTION", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 1. Progress Overview
            val completedCount = quests.count { it.isCompleted }
            val progress = completedCount / quests.size.toFloat()

            ProgressCard(progress, completedCount, quests.size)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Today's Tasks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Task List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(quests) { quest ->
                    QuestCard(
                        quest = quest,
                        onStart = {
                            if (quest.type == QuestType.BREATHING) {
                                // Open Real Timer for Breathing
                                activeQuest = quest
                                showTimerDialog = true
                            } else {
                                // Instant Complete for others (or add navigation)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                quests = quests.map { if (it.id == quest.id) it.copy(isCompleted = true) else it }
                            }
                        }
                    )
                }
            }
        }
    }

    // --- REAL FEATURE: BREATHING TIMER DIALOG ---
    if (showTimerDialog && activeQuest != null) {
        BreathingTimerDialog(
            quest = activeQuest!!,
            onDismiss = { showTimerDialog = false },
            onComplete = {
                // Mark quest as done when timer finishes
                quests = quests.map { if (it.id == activeQuest!!.id) it.copy(isCompleted = true) else it }
                showTimerDialog = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        )
    }
}

// -----------------------------------------------------------------------------
// UI COMPONENTS
// -----------------------------------------------------------------------------

@Composable
fun ProgressCard(progress: Float, completed: Int, total: Int) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(60.dp),
                    color = Color.LightGray.copy(alpha = 0.3f),
                    strokeWidth = 6.dp
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(60.dp),
                    color = if (progress == 1f) SuccessGreen else PrimaryBlue,
                    strokeWidth = 6.dp
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = "Daily Compliance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$completed / $total Interventions Done",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun QuestCard(quest: DailyQuest, onStart: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(if (quest.isCompleted) 0.dp else 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (quest.isCompleted) Color.LightGray.copy(alpha = 0.2f) else PrimaryBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getQuestIcon(quest.type),
                    contentDescription = null,
                    tint = if (quest.isCompleted) Color.Gray else PrimaryBlue
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quest.title,
                    fontWeight = FontWeight.SemiBold,
                    color = if (quest.isCompleted) Color.Gray else Color.Black
                )
                Text(
                    text = "${quest.durationSeconds / 60} min • ${quest.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1
                )
            }

            // Action Button
            if (quest.isCompleted) {
                Icon(Icons.Rounded.CheckCircle, null, tint = SuccessGreen)
            } else {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Start")
                }
            }
        }
    }
}

// --- THE REAL BREATHING TIMER LOGIC ---
@Composable
fun BreathingTimerDialog(quest: DailyQuest, onDismiss: () -> Unit, onComplete: () -> Unit) {
    // Timer State
    var timeLeft by remember { mutableStateOf(quest.durationSeconds) }
    var isRunning by remember { mutableStateOf(true) }

    // Countdown Logic
    LaunchedEffect(isRunning) {
        while (isRunning && timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
        if (timeLeft == 0) {
            onComplete() // Auto-finish when time hits 0
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(quest.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Focus on your breathing...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

                Spacer(modifier = Modifier.height(32.dp))

                // Big Timer Text
                val minutes = timeLeft / 60
                val seconds = timeLeft % 60
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Controls
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(onClick = { isRunning = !isRunning }) {
                        Text(if (isRunning) "Pause" else "Resume")
                    }
                    Button(
                        onClick = onDismiss, // Cancel
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("Stop")
                    }
                }

                // Cheat Button for Demo (Finish Instantly)
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onComplete) {
                    Text("Skip (Demo Only)", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

// Helpers
fun getQuestIcon(type: QuestType): ImageVector {
    return when (type) {
        QuestType.BREATHING -> Icons.Default.SelfImprovement
        QuestType.PHYSICAL -> Icons.Default.DirectionsWalk
        QuestType.SLEEP -> Icons.Default.Bedtime
        QuestType.COGNITIVE -> Icons.Default.Psychology
    }
}