package com.pravor.notessharing.ui.navigation

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pravor.notessharing.ui.common.AppBottomBar
import com.pravor.notessharing.ui.common.AppSettingsUiState
import com.pravor.notessharing.ui.common.ThemePreference
import com.pravor.notessharing.ui.features.auth.AuthViewModel
import com.pravor.notessharing.ui.features.auth.SessionState

val LocalBottomBarPadding = androidx.compose.runtime.compositionLocalOf { 0.dp }

val LocalSnackbarHostState = androidx.compose.runtime.compositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided")
}

@SuppressLint("RestrictedApi")
@Composable
fun NotesSharingApp(
    appSettings: AppSettingsUiState,
    onDarkModeChange: (Boolean) -> Unit,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onNotificationsChange: (Boolean) -> Unit
) {
    val authViewModel: AuthViewModel = viewModel()
    val navController = rememberNavController()

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity

    androidx.compose.runtime.DisposableEffect(activity) {
        val listener = androidx.core.util.Consumer<android.content.Intent> { intent ->
            val notificationId = intent.getStringExtra("notification_id")
            android.util.Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "NotesSharingApp onNewIntent listener - notificationId: $notificationId")
            if (!notificationId.isNullOrBlank()) {
                navController.navigate(AppDestination.Home.route) {
                    popUpTo(0) { inclusive = true }
                }
            }

            val docId = intent.getStringExtra("document_id")
            android.util.Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "NotesSharingApp onNewIntent listener - docId: $docId")
            if (!docId.isNullOrBlank()) {
                navController.navigate(AppDestination.DocumentDetail.createRoute(docId)) {
                    popUpTo(AppDestination.Home.route)
                }
                intent.removeExtra("document_id")
            }

            val videoId = intent.getStringExtra("video_id")
            android.util.Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "NotesSharingApp onNewIntent listener - videoId: $videoId")
            if (!videoId.isNullOrBlank()) {
                navController.navigate(AppDestination.VideoDetail.createRoute(videoId)) {
                    popUpTo(AppDestination.Home.route)
                }
                intent.removeExtra("video_id")
            }

            val widgetDest = intent.getStringExtra("destination")
            if (!widgetDest.isNullOrBlank()) {
                val route = when (widgetDest) {
                    "bookmarks" -> AppDestination.MyBookmarks.route
                    "upload" -> AppDestination.Upload.route
                    "downloads" -> AppDestination.MyFiles.route
                    else -> null
                }
                if (route != null) {
                    navController.navigate(route) {
                        popUpTo(AppDestination.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                intent.removeExtra("destination")
            }
        }
        activity?.addOnNewIntentListener(listener)
        onDispose {
            activity?.removeOnNewIntentListener(listener)
        }
    }

    val sessionState by authViewModel.sessionState.collectAsState()
    var hasBootstrapped by rememberSaveable { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(sessionState) {
        if (sessionState != SessionState.Checking) {
            hasBootstrapped = true
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    androidx.compose.runtime.LaunchedEffect(sessionState, currentRoute) {
        if (sessionState != SessionState.Checking) {
            when (sessionState) {
                SessionState.LoggedIn -> {
                    val isCurrentlyAuth = currentRoute == "auth_gate" ||
                            currentRoute == "onboarding" ||
                            currentRoute == AppDestination.Welcome.route ||
                            currentRoute == AppDestination.Login.route ||
                            currentRoute == AppDestination.SignUp.route ||
                            currentRoute == "google_onboarding"
                    
                    if (isCurrentlyAuth) {
                        val intent = activity?.intent
                        val docId = intent?.getStringExtra("document_id")
                        val videoId = intent?.getStringExtra("video_id")
                        val notificationId = intent?.getStringExtra("notification_id")
                        if (!notificationId.isNullOrBlank()) {
                            navController.navigate(AppDestination.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        } else if (!docId.isNullOrBlank()) {
                            navController.navigate(AppDestination.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                            navController.navigate(AppDestination.DocumentDetail.createRoute(docId))
                            intent.removeExtra("document_id")
                        } else if (!videoId.isNullOrBlank()) {
                            navController.navigate(AppDestination.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                            navController.navigate(AppDestination.VideoDetail.createRoute(videoId))
                            intent.removeExtra("video_id")
                        } else {
                            navController.navigate(AppDestination.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }
                SessionState.OnboardingRequired -> {
                    if (currentRoute != "google_onboarding") {
                        navController.navigate("google_onboarding") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
                SessionState.LoggedOut -> {
                    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                    val forceShow = com.pravor.notessharing.core.config.DeveloperConfig.FORCE_SHOW_ONBOARDING
                    val hasCompletedOnboarding = prefs.getBoolean("has_completed_onboarding", false)
                    
                    val isCurrentlyAuth = currentRoute == "onboarding" ||
                            currentRoute == AppDestination.Welcome.route ||
                            currentRoute == AppDestination.Login.route ||
                            currentRoute == AppDestination.SignUp.route
                    
                    if (!isCurrentlyAuth || (forceShow && currentRoute != "onboarding")) {
                        val startRoute = if (hasCompletedOnboarding && !forceShow) {
                            AppDestination.Welcome.route
                        } else {
                            "onboarding"
                        }
                        navController.navigate(startRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(currentRoute) {
        if (currentRoute == AppDestination.Home.route) {
            val intent = activity?.intent ?: return@LaunchedEffect
            val widgetDest = intent.getStringExtra("destination")
            if (!widgetDest.isNullOrBlank()) {
                val route = when (widgetDest) {
                    "bookmarks" -> AppDestination.MyBookmarks.route
                    "upload" -> AppDestination.Upload.route
                    "downloads" -> AppDestination.MyFiles.route
                    else -> null
                }
                if (route != null) {
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
                intent.removeExtra("destination")
            }
        }
    }

    val selectedBottomRoute = if (currentRoute?.startsWith("${AppDestination.Explore.route}/") == true) {
        AppDestination.Explore.route
    } else {
        currentRoute
    }

    val isAuthScreen = currentRoute == "auth_gate" ||
            currentRoute == "onboarding" ||
            currentRoute == AppDestination.Welcome.route ||
            currentRoute == AppDestination.Login.route ||
            currentRoute == AppDestination.SignUp.route ||
            currentRoute == "google_onboarding"

    val showBottomBar = !isAuthScreen &&
            currentRoute?.startsWith("pdf_viewing") != true &&
            currentRoute?.startsWith("image_viewing") != true &&
            currentRoute?.startsWith("profile/notification_preferences") != true &&
            currentRoute?.startsWith("profile/about") != true &&
            currentRoute?.startsWith("search") != true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    destinations = bottomDestinations,
                    currentRoute = selectedBottomRoute,
                    onDestinationClick = { destination ->
                        if (currentRoute != destination.route) {
                            val popped = navController.popBackStack(destination.route, inclusive = false)
                            if (!popped) {
                                navController.navigate(destination.route) {
                                    popUpTo(AppDestination.Home.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        val navigationBarsPaddingValues =
            androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues()
        val bottomPadding = remember(
            innerPadding.calculateBottomPadding(),
            navigationBarsPaddingValues.calculateBottomPadding(),
            showBottomBar
        ) {
            if (!showBottomBar) {
                0.dp
            } else {
                val totalBottom = innerPadding.calculateBottomPadding()
                val systemBottom = navigationBarsPaddingValues.calculateBottomPadding()
                (totalBottom - systemBottom).coerceAtLeast(0.dp)
            }
        }

        val snackbarHostState = remember { SnackbarHostState() }

        androidx.compose.runtime.CompositionLocalProvider(
            LocalBottomBarPadding provides innerPadding.calculateBottomPadding(),
            LocalSnackbarHostState provides snackbarHostState
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                NavGraph(
                    navController = navController,
                    sessionState = sessionState,
                    hasBootstrapped = hasBootstrapped,
                    authViewModel = authViewModel,
                    activity = activity,
                    appSettings = appSettings,
                    onDarkModeChange = onDarkModeChange,
                    onThemePreferenceChange = onThemePreferenceChange,
                    innerPadding = innerPadding
                )

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(
                            bottom = if (showBottomBar)
                                innerPadding.calculateBottomPadding() + 12.dp
                            else 12.dp
                        )
                ) { data ->
                    Snackbar(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .heightIn(min = 44.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        shape = RoundedCornerShape(14.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Text(
                            text = data.visuals.message,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
