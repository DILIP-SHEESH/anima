package com.example.anima.presentation.screens.suggestions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Warning // Added for safety feature
import androidx.compose.material.icons.filled.PanTool // Added for touch sensor representation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Define navigation routes (placeholders for now)
object SuggestionsRoutes {
    const val WOMEN_SAFETY = "women_safety_screen"
    const val CUSTOM_ACTION = "custom_action_screen"
}

data class SuggestionItem(
    val title: String,
    val description: String,
    val icon: @Composable () -> Unit
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsScreen(navController: NavController) {
    val suggestions = listOf(
        SuggestionItem(
            title = "Mindful Breathing",
            description = "Take a 5-minute mindful breathing session to reduce stress and reset focus.",
            icon = { Icon(Icons.Filled.SelfImprovement, contentDescription = null, tint = Color.White) }
        ),
        SuggestionItem(
            title = "Hydration Reminder",
            description = "Drink at least 2 liters of water daily to improve brain performance and mood.",
            icon = { Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = Color.White) }
        ),
        SuggestionItem(
            title = "Sleep Quality",
            description = "Try avoiding screens 30 minutes before sleep for better rest and relaxation.",
            icon = { Icon(Icons.Filled.Nightlight, contentDescription = null, tint = Color.White) }
        ),
        SuggestionItem(
            title = "Daily Activity",
            description = "A short 15-minute walk can boost serotonin levels and mental health.",
            icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = Color.White) }
        ),
        SuggestionItem(
            title = "Positive Mindset",
            description = "Keep a gratitude journal and list 3 things you’re thankful for each day.",
            icon = { Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = Color.White) }
        )
    )

    // For demonstration: user's gender or a customizable setting
    // In a real app, this would come from a ViewModel or user preferences
    var userGender by remember { mutableStateOf("Female") } // Can be "Male", "Female", "Custom"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Personalized Suggestions",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            // This FAB simulates the "touch sensor" interaction
            FloatingActionButton(
                onClick = {
                    // Logic to handle touch sensor event
                    when (userGender) {
                        "Female" -> {
                            // Navigate to women's safety screen
                            navController.navigate(SuggestionsRoutes.WOMEN_SAFETY)
                            // In a real app, this would also trigger an actual alert/action
                            println("WOMEN SAFETY ALERT TRIGGERED!")
                        }
                        "Male" -> {
                            // Navigate to a customizable action screen for men
                            navController.navigate(SuggestionsRoutes.CUSTOM_ACTION)
                            println("CUSTOM ACTION TRIGGERED FOR MALE USER!")
                        }
                        else -> {
                            // Default or other custom action
                            navController.navigate(SuggestionsRoutes.CUSTOM_ACTION)
                            println("DEFAULT/CUSTOM ACTION TRIGGERED!")
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.error, // Red for urgency/alert
                shape = CircleShape,
                modifier = Modifier.size(64.dp) // Make it prominent
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.PanTool, // Represents a touch/press
                        contentDescription = "Quick Action Sensor",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Press",
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End // Position the FAB
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(suggestions) { suggestion ->
                SuggestionCard(suggestion)
            }
        }
    }
}

@Composable
fun SuggestionCard(suggestion: SuggestionItem) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                suggestion.icon()
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = suggestion.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = suggestion.description,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )
            }

            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Done",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}