package com.example.anima.presentation.screens.suggestions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// --- Gamification Colors ---
val QuestGold = Color(0xFFFFD700)
val QuestPurple = Color(0xFF7E57C2)
val SafetyRed = Color(0xFFE53935)
val SuccessGreen = Color(0xFF43A047)

// --- Data Model for Quests ---
data class DailyQuest(
    val id: Int,
    val title: String,
    val description: String,
    val xpReward: Int,
    val type: QuestType,
    var isCompleted: Boolean = false
)

enum class QuestType {
    STRESS_REDUCTION, PHYSICAL, SLEEP, COGNITIVE
}

// Navigation Placeholders
object SuggestionsRoutes {
    const val WOMEN_SAFETY = "women_safety_screen"
    const val CUSTOM_ACTION = "custom_action_screen"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsScreen(navController: NavController) {
    // Mock Data: In a real app, this comes from your Stress Classification Model
    // If Stress > Threshold, add "Breathing" quest.
    val initialQuests = listOf(
        DailyQuest(1, "Cortisol Reset", "Perform 3 mins of box breathing to lower heart rate.", 50, QuestType.STRESS_REDUCTION),
        DailyQuest(2, "Movement Break", "Walk 500 steps to clear metabolic waste.", 30, QuestType.PHYSICAL),
        DailyQuest(3, "Digital Sunset", "Avoid blue light for 20 mins.", 40, QuestType.SLEEP),
        DailyQuest(4, "Gratitude Log", "Log one positive interaction today.", 20, QuestType.COGNITIVE)
    )

    var quests by remember { mutableStateOf(initialQuests) }
    var userXP by remember { mutableStateOf(1250) } // Current total XP
    var dailyProgress by remember { mutableStateOf(0.3f) } // 30% done today

    // Logic for Touch Sensor (Hardware integration simulation)
    var userGender by remember { mutableStateOf("Female") } // Configurable setting

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Wellness Quests", fontWeight = FontWeight.Bold)
                        Text("Day 12 Streak 🔥", style = MaterialTheme.typography.labelSmall, color = QuestGold)
                    }
                },
                actions = {
                    // Level Indicator
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(QuestPurple.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Lvl 5 • ${userXP} XP", color = QuestPurple, fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        floatingActionButton = {
            // THE "HARDWARE" TOUCH SENSOR BUTTON
            // Designed to look like a physical emergency button
            EmergencySensorFab(
                gender = userGender,
                onClick = {
                    when (userGender) {
                        "Female" -> {
                            // Trigger SOS logic
                            navController.navigate(SuggestionsRoutes.WOMEN_SAFETY)
                        }
                        else -> {
                            // Trigger Custom Action
                            navController.navigate(SuggestionsRoutes.CUSTOM_ACTION)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // 1. Progress Header (The "Game" Status)
            GamificationHeader(progress = dailyProgress, completedTasks = quests.count { it.isCompleted }, totalTasks = quests.size)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Today's Interventions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // 2. The Quest List
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(quests) { quest ->
                    QuestCard(
                        quest = quest,
                        onClaim = {
                            // Mark as complete and add XP
                            val updatedList = quests.map {
                                if (it.id == quest.id) it.copy(isCompleted = true) else it
                            }
                            quests = updatedList
                            userXP += quest.xpReward
                            dailyProgress = updatedList.count { it.isCompleted } / updatedList.size.toFloat()
                        }
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// UI COMPONENTS
// -----------------------------------------------------------------------------

@Composable
fun GamificationHeader(progress: Float, completedTasks: Int, totalTasks: Int) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "Progress")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Progress Indicator
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(70.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                    strokeWidth = 6.dp,
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
                    text = "Daily Vitality Goal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$completedTasks / $totalTasks Interventions Completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (progress >= 1.0f) {
                    Text("🎉 Goal Reached! High Recovery.", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                } else {
                    Text("Complete tasks to optimize HRV.", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun QuestCard(quest: DailyQuest, onClaim: () -> Unit) {
    val cardColor = if (quest.isCompleted)
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    else
        MaterialTheme.colorScheme.surfaceVariant

    val textColor = if (quest.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(if (quest.isCompleted) 0.dp else 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(getQuestColor(quest.type).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getQuestIcon(quest.type),
                    contentDescription = null,
                    tint = getQuestColor(quest.type)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quest.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textColor,
                    style = androidx.compose.ui.text.TextStyle(textDecoration = if(quest.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
                )
                Text(
                    text = quest.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f),
                    maxLines = 2
                )
                if (!quest.isCompleted) {
                    Text(
                        text = "+${quest.xpReward} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = QuestGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (quest.isCompleted) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Done", tint = SuccessGreen)
            } else {
                Button(
                    onClick = onClaim,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Start", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun EmergencySensorFab(gender: String, onClick: () -> Unit) {
    // This visually represents the Touch Sensor on the glasses
    val colorStops = arrayOf(
        0.0f to SafetyRed,
        1.0f to Color(0xFFB71C1C)
    )

    FloatingActionButton(
        onClick = onClick,
        containerColor = Color.Transparent, // Using Box for gradient
        elevation = FloatingActionButtonDefaults.elevation(8.dp),
        modifier = Modifier.size(72.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(colorStops = colorStops))
                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.TouchApp,
                    contentDescription = "Touch Sensor",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    if (gender == "Female") "SOS" else "Action",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// Helpers
fun getQuestColor(type: QuestType): Color {
    return when (type) {
        QuestType.STRESS_REDUCTION -> Color(0xFF42A5F5) // Blue
        QuestType.PHYSICAL -> Color(0xFF66BB6A)       // Green
        QuestType.SLEEP -> Color(0xFFAB47BC)          // Purple
        QuestType.COGNITIVE -> Color(0xFFFFA726)      // Orange
    }
}

fun getQuestIcon(type: QuestType): ImageVector {
    return when (type) {
        QuestType.STRESS_REDUCTION -> Icons.Filled.SelfImprovement
        QuestType.PHYSICAL -> Icons.Filled.DirectionsWalk
        QuestType.SLEEP -> Icons.Filled.Bedtime
        QuestType.COGNITIVE -> Icons.Filled.Psychology
    }
}