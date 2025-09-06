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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

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
        }
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
