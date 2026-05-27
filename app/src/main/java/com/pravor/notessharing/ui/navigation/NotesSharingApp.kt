package com.pravor.notessharing.ui.navigation

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
import com.pravor.notessharing.ui.components.BottomNavBar
import com.pravor.notessharing.ui.screens.explore.DiscoverRoute
import com.pravor.notessharing.ui.screens.explore.ExploreRoute
import com.pravor.notessharing.ui.screens.explore.RecommendedVideosRoute
import com.pravor.notessharing.ui.screens.explore.TrendingNotesRoute
import com.pravor.notessharing.ui.screens.home.HomeRoute
import com.pravor.notessharing.ui.screens.myfiles.MyFilesRoute
import com.pravor.notessharing.ui.screens.profile.ProfileRoute
import com.pravor.notessharing.ui.screens.upload.UploadRoute
import com.pravor.notessharing.state.AppSettingsUiState
import com.pravor.notessharing.state.ThemePreference

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
    val selectedBottomRoute = if (currentRoute?.startsWith("${AppDestination.Explore.route}/") == true) {
        AppDestination.Explore.route
    } else {
        currentRoute
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavBar(
                destinations = bottomDestinations,
                currentRoute = selectedBottomRoute,
                onDestinationClick = { destination ->
                    if (destination == AppDestination.Home) {
                        navController.popBackStack(AppDestination.Home.route, inclusive = false)
                    } else {
                        navController.navigate(destination.route) {
                            popUpTo(AppDestination.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
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
            composable(AppDestination.Home.route) {
                HomeRoute(
                    onViewAllLibraryClick = {
                        navController.navigate(AppDestination.MyFiles.route) {
                            launchSingleTop = true
                        }
                    },
                    onSeeMoreClick = {
                        navController.navigate(AppDestination.Explore.route) {
                            popUpTo(AppDestination.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(AppDestination.Explore.route) {
                ExploreRoute(
                    onTrendingSeeMoreClick = {
                        navController.navigate(AppDestination.TrendingNotes.route) {
                            launchSingleTop = true
                        }
                    },
                    onRecommendedVideosSeeMoreClick = {
                        navController.navigate(AppDestination.RecommendedVideos.route) {
                            launchSingleTop = true
                        }
                    },
                    onDiscoverSeeMoreClick = {
                        navController.navigate(AppDestination.Discover.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppDestination.TrendingNotes.route) {
                TrendingNotesRoute(onBackClick = { navController.popBackStack() })
            }
            composable(AppDestination.RecommendedVideos.route) {
                RecommendedVideosRoute(onBackClick = { navController.popBackStack() })
            }
            composable(AppDestination.Discover.route) {
                DiscoverRoute(onBackClick = { navController.popBackStack() })
            }
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
