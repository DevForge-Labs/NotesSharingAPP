package com.pratyush.notessharing.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pratyush.notessharing.ui.components.BottomNavBar
import com.pratyush.notessharing.ui.screens.explore.ExploreRoute
import com.pratyush.notessharing.ui.screens.home.HomeRoute
import com.pratyush.notessharing.ui.screens.myfiles.MyFilesRoute
import com.pratyush.notessharing.ui.screens.profile.ProfileRoute
import com.pratyush.notessharing.ui.screens.upload.UploadRoute
import com.pratyush.notessharing.state.AppSettingsUiState
import com.pratyush.notessharing.state.ThemePreference

@Composable
fun NotesSharingApp(
    appSettings: AppSettingsUiState,
    onDarkModeChange: (Boolean) -> Unit,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onNotificationsChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavBar(
                destinations = bottomDestinations,
                currentRoute = currentRoute,
                onDestinationClick = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestination.Home.route) { HomeRoute() }
            composable(AppDestination.Explore.route) { ExploreRoute() }
            composable(AppDestination.Upload.route) { UploadRoute() }
            composable(AppDestination.MyFiles.route) { MyFilesRoute() }
            composable(AppDestination.Profile.route) {
                ProfileRoute(
                    appSettings = appSettings,
                    onDarkModeChange = onDarkModeChange,
                    onThemePreferenceChange = onThemePreferenceChange,
                    onNotificationsChange = onNotificationsChange
                )
            }
        }
    }
}
