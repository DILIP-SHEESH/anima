package com.example.anima.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Lightbulb // ✅ Fix here
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.anima.presentation.screens.dashboard.DashboardScreen
import com.example.anima.presentation.screens.suggestions.SuggestionsScreen
import com.example.anima.presentation.screens.profile.ProfileScreen

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : BottomNavItem("dashboard", "Dashboard", Icons.Filled.Home)
    object Suggestions : BottomNavItem("suggestions", "Suggestions", Icons.Outlined.Lightbulb) // ✅ Changed
    object Profile : BottomNavItem("profile", "Profile", Icons.Filled.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimaNavigation() {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Suggestions,
        BottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Dashboard.route,
            modifier = Modifier.padding(padding) // ✅ Fixed import
        ) {
            composable(BottomNavItem.Dashboard.route) { DashboardScreen(navController) }
            composable(BottomNavItem.Suggestions.route) { SuggestionsScreen(navController) }
            composable(BottomNavItem.Profile.route) { ProfileScreen(navController) }
        }
    }
}
