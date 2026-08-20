package com.pravor.notessharing.ui.navigation

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import com.pravor.notessharing.data.local.preferences.*

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Snackbar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pravor.notessharing.ui.common.AppBottomBar
import com.pravor.notessharing.ui.features.explore.DiscoverRoute
import com.pravor.notessharing.ui.features.explore.ExploreRoute
import com.pravor.notessharing.ui.features.explore.RecommendedVideosRoute
import com.pravor.notessharing.ui.features.explore.ExamPrepRoute
import com.pravor.notessharing.ui.features.explore.AssignmentsRoute
import com.pravor.notessharing.ui.features.explore.SubjectResourcesRoute
import com.pravor.notessharing.ui.features.trending.TrendingNotesRoute
import com.pravor.notessharing.ui.features.home.HomeRoute
import com.pravor.notessharing.ui.features.myfiles.MyFilesRoute
import com.pravor.notessharing.ui.features.myfiles.MyUploadsScreen
import com.pravor.notessharing.ui.features.myfiles.MyBookmarksScreen
import com.pravor.notessharing.ui.features.profile.ProfileRoute
import com.pravor.notessharing.ui.features.profile.EditProfileRoute
import com.pravor.notessharing.ui.features.upload.UploadRoute
import com.pravor.notessharing.ui.features.upload.UploadSuccessRoute
import com.pravor.notessharing.ui.features.document.DocumentDetailRoute
import com.pravor.notessharing.ui.common.AppSettingsUiState
import com.pravor.notessharing.ui.common.ThemePreference
import com.pravor.notessharing.ui.features.auth.WelcomeScreen
import com.pravor.notessharing.ui.features.auth.LoginScreen
import com.pravor.notessharing.ui.features.auth.SignUpScreen
import com.pravor.notessharing.ui.features.auth.AuthViewModel
import com.pravor.notessharing.ui.features.auth.SessionState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.pravor.notessharing.ui.common.loading.StartupLoadingScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Context
import androidx.compose.foundation.layout.heightIn
import com.pravor.notessharing.ui.features.onboarding.UpdatesScreen


object PdfDebugState {
    var lastOriginalUrl: String = ""
}

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

    // Redundant LaunchedEffect removed. Intent checks handled directly in auth_gate and addOnNewIntentListener.

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
                NavHost(
                    navController = navController,
                    startDestination = "auth_gate",
                    modifier = Modifier.padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = 0.dp,
                        start = innerPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        end = innerPadding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
                    )
                ) {
                    composable("auth_gate") {
                        if (sessionState == SessionState.Checking && !hasBootstrapped) {
                            StartupLoadingScreen()
                        } else {
                            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize())
                        }
                    }
                    composable("onboarding") {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val prefs = remember {
                            context.getSharedPreferences(
                                "app_settings",
                                android.content.Context.MODE_PRIVATE
                            )
                        }
                        val completeOnboarding = {
                            prefs.edit().putBoolean("has_completed_onboarding", true).apply()
                        }
                        UpdatesScreen(
                            viewModel = authViewModel,
                            onNavigateToLogin = {
                                completeOnboarding()
                                navController.navigate(AppDestination.Welcome.route) {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                                navController.navigate(AppDestination.Login.route)
                            },
                            onNavigateToSignUp = {
                                completeOnboarding()
                                navController.navigate(AppDestination.Welcome.route) {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                                navController.navigate(AppDestination.SignUp.route)
                            },
                            onSkip = {
                                completeOnboarding()
                                navController.navigate(AppDestination.Welcome.route) {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            },
                            onCompleteOnboarding = {
                                completeOnboarding()
                            }
                        )
                    }
                    composable(AppDestination.Welcome.route) {

                        WelcomeScreen(
                            viewModel = authViewModel,
                            onNavigateToLogin = { navController.navigate(AppDestination.Login.route) },
                            onNavigateToSignUp = { navController.navigate(AppDestination.SignUp.route) },
                            onNavigateToHome = {
                                navController.navigate(AppDestination.Home.route) {
                                    popUpTo(AppDestination.Welcome.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(AppDestination.Login.route) {
                        LoginScreen(
                            viewModel = authViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToSignUp = {
                                navController.navigate(AppDestination.SignUp.route) {
                                    popUpTo(AppDestination.Welcome.route)
                                }
                            },
                            onNavigateToHome = {
                                navController.navigate(AppDestination.Home.route) {
                                    popUpTo(AppDestination.Welcome.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(AppDestination.SignUp.route) {
                        SignUpScreen(
                            viewModel = authViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToLogin = {
                                navController.navigate(AppDestination.Login.route) {
                                    popUpTo(AppDestination.Welcome.route)
                                }
                            },
                            onNavigateToHome = {
                                navController.navigate(AppDestination.Home.route) {
                                    popUpTo(AppDestination.Welcome.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("google_onboarding") {
                        com.pravor.notessharing.ui.features.auth.GoogleOnboardingScreen(
                            viewModel = authViewModel,
                            onNavigateToHome = {
                                navController.navigate(AppDestination.Home.route) {
                                    popUpTo("google_onboarding") { inclusive = true }
                                }
                            },
                            onNavigateBack = {
                                authViewModel.logout()
                                navController.navigate(AppDestination.Welcome.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(AppDestination.Home.route) {
                        val intent = activity?.intent
                        val notificationId = intent?.getStringExtra("notification_id")

                        HomeRoute(
                            onMyUploadsClick = {
                                navController.navigate(AppDestination.MyUploads.route) {
                                    launchSingleTop = true
                                }
                            },
                            onMyBookmarksClick = {
                                navController.navigate(AppDestination.MyBookmarks.route) {
                                    launchSingleTop = true
                                }
                            },
                            onMyDownloadsClick = {
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
                            },
                            onDocumentClick = { docId ->
                                navController.navigate(
                                    AppDestination.DocumentDetail.createRoute(
                                        docId
                                    )
                                )
                            },
                            onVideoClick = { videoId ->
                                navController.navigate(
                                    AppDestination.VideoDetail.createRoute(
                                        videoId
                                    )
                                )
                            },
                            pendingNotificationId = notificationId,
                            onClearPendingNotificationId = {
                                intent?.removeExtra("notification_id")
                            }
                        )
                    }
                    composable(AppDestination.Explore.route) {
                        ExploreRoute(
                            onSearchClick = {
                                navController.navigate(AppDestination.Search.route)
                            },
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
                            },
                            onExamPrepSeeMoreClick = {
                                navController.navigate(AppDestination.ExamPrep.route) {
                                    launchSingleTop = true
                                }
                            },
                            onAssignmentsSeeMoreClick = {
                                navController.navigate(AppDestination.Assignments.route) {
                                    launchSingleTop = true
                                }
                            },
                            onSubjectSeeMoreClick = { subjectName ->
                                navController.navigate(
                                    AppDestination.SubjectResources.createRoute(
                                        subjectName
                                    )
                                ) {
                                    launchSingleTop = true
                                }
                            },
                            onDocumentClick = { docId ->
                                navController.navigate(
                                    AppDestination.DocumentDetail.createRoute(
                                        docId
                                    )
                                )
                            },
                            onVideoClick = { videoId ->
                                navController.navigate(
                                    AppDestination.VideoDetail.createRoute(
                                        videoId
                                    )
                                )
                            }
                        )
                    }
                    composable(AppDestination.Search.route) {
                        com.pravor.notessharing.ui.features.search.SearchRoute(
                            onBackClick = { navController.popBackStack() },
                            onDocumentClick = { docId ->
                                navController.navigate(
                                    AppDestination.DocumentDetail.createRoute(
                                        docId
                                    )
                                )
                            },
                            onVideoClick = { videoId ->
                                navController.navigate(
                                    AppDestination.VideoDetail.createRoute(
                                        videoId
                                    )
                                )
                            }
                        )
                    }
                    composable(
                        route = AppDestination.SubjectResources.route,
                        arguments = listOf(
                            androidx.navigation.navArgument("subjectName") {
                                type = androidx.navigation.NavType.StringType
                            }
                        )
                    ) { backStackEntry ->
                        val subjectName = backStackEntry.arguments?.getString("subjectName") ?: ""
                        SubjectResourcesRoute(
                            subjectName = subjectName,
                            onBackClick = { navController.popBackStack() },
                            onDocumentClick = { docId ->
                                navController.navigate(
                                    AppDestination.DocumentDetail.createRoute(
                                        docId
                                    )
                                )
                            },
                            onVideoClick = { videoId ->
                                navController.navigate(
                                    AppDestination.VideoDetail.createRoute(
                                        videoId
                                    )
                                )
                            }
                        )
                    }
                    composable(AppDestination.TrendingNotes.route) {
                        TrendingNotesRoute(
                            onBackClick = { navController.popBackStack() },
                            onDocumentClick = { docId ->
                                navController.navigate(
                                    AppDestination.DocumentDetail.createRoute(
                                        docId
                                    )
                                )
                            }
                        )
                    }
                    composable(AppDestination.RecommendedVideos.route) {
                        RecommendedVideosRoute(
                            onBackClick = { navController.popBackStack() },
                            onVideoClick = { videoId ->
                                navController.navigate(
                                    AppDestination.VideoDetail.createRoute(
                                        videoId
                                    )
                                )
                            }
                        )
                    }
                    composable(AppDestination.Discover.route) {
                        DiscoverRoute(
                            onBackClick = { navController.popBackStack() },
                            onDocumentClick = { docId ->
                                navController.navigate(
                                    AppDestination.DocumentDetail.createRoute(
                                        docId
                                    )
                                )
                            },
                            onVideoClick = { videoId ->
                                navController.navigate(
                                    AppDestination.VideoDetail.createRoute(
                                        videoId
                                    )
                                )
                            }
                        )
                    }
                    composable(AppDestination.ExamPrep.route) {
                        ExamPrepRoute(
                            onBackClick = { navController.popBackStack() },
                            onDocumentClick = { docId ->
                                navController.navigate(
                                    AppDestination.DocumentDetail.createRoute(
                                        docId
                                    )
                                )
                            }
                        )
                    }
                    composable(AppDestination.Assignments.route) {
                        AssignmentsRoute(
                            onBackClick = { navController.popBackStack() },
                            onDocumentClick = { docId ->
                                navController.navigate(
                                    AppDestination.DocumentDetail.createRoute(
                                        docId
                                    )
                                )
                            }
                        )
                    }
                    composable(AppDestination.Upload.route) {
                        UploadRoute(
                            onUploadSuccess = {
                                navController.navigate(AppDestination.UploadSuccess.route)
                            }
                        )
                    }
                    composable(AppDestination.UploadSuccess.route) {
                        UploadSuccessRoute(
                            onUploadAgain = {
                                navController.popBackStack(
                                    AppDestination.Upload.route,
                                    inclusive = false
                                )
                            },
                            onBackClick = {
                                navController.navigate(AppDestination.Profile.route) {
                                    popUpTo(AppDestination.Upload.route) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                    composable(AppDestination.MyFiles.route) {
                        MyFilesRoute(
                            onBackClick = { navController.popBackStack() },
                            onDocumentClick = { docId ->
                                navController.navigate(
                                    AppDestination.DocumentDetail.createRoute(
                                        docId
                                    )
                                )
                            },
                            onVideoClick = { videoId ->
                                navController.navigate(
                                    AppDestination.VideoDetail.createRoute(
                                        videoId
                                    )
                                )
                            }
                        )
                    }
                    composable(AppDestination.MyUploads.route) {
                        MyUploadsScreen(
                            onBackClick = { navController.popBackStack() },
                            onDocumentClick = { docId ->
                                navController.navigate(
                                    AppDestination.DocumentDetail.createRoute(
                                        docId
                                    )
                                )
                            },
                            onVideoClick = { videoId ->
                                navController.navigate(
                                    AppDestination.VideoDetail.createRoute(
                                        videoId
                                    )
                                )
                            }
                        )
                    }
                    composable(AppDestination.MyBookmarks.route) {
                        MyBookmarksScreen(
                            onBackClick = { navController.popBackStack() },
                            onDocumentClick = { docId ->
                                navController.navigate(
                                    AppDestination.DocumentDetail.createRoute(
                                        docId
                                    )
                                )
                            },
                            onVideoClick = { videoId ->
                                navController.navigate(
                                    AppDestination.VideoDetail.createRoute(
                                        videoId
                                    )
                                )
                            }
                        )
                    }
                    composable(
                        route = AppDestination.DocumentDetail.route,
                        arguments = listOf(
                            androidx.navigation.navArgument("documentId") {
                                type = androidx.navigation.NavType.StringType
                            }
                        )
                    ) { backStackEntry ->
                        val documentId = backStackEntry.arguments?.getString("documentId") ?: ""
                        DocumentDetailRoute(
                            documentId = documentId,
                            onBackClick = { navController.popBackStack() },
                            onNavigateToDetail = { docId ->
                                navController.navigate(
                                    AppDestination.DocumentDetail.createRoute(
                                        docId
                                    )
                                )
                            },
                            onNavigateToPdfViewer = { docId, fileUrl, title ->
                                PdfDebugState.lastOriginalUrl = fileUrl
                                navController.navigate(
                                    AppDestination.PdfViewing.createRoute(
                                        docId,
                                        fileUrl,
                                        title
                                    )
                                )
                            },
                            onNavigateToImageViewer = { docId, fileUrl, title ->
                                navController.navigate(
                                    AppDestination.ImageViewing.createRoute(
                                        docId,
                                        fileUrl,
                                        title
                                    )
                                )
                            }
                        )
                    }
                    composable(
                        route = AppDestination.PdfViewing.route,
                        arguments = listOf(
                            androidx.navigation.navArgument("documentId") {
                                type = androidx.navigation.NavType.StringType
                            },
                            androidx.navigation.navArgument("fileUrl") {
                                type = androidx.navigation.NavType.StringType
                            },
                            androidx.navigation.navArgument("title") {
                                type = androidx.navigation.NavType.StringType
                            }
                        )
                    ) { backStackEntry ->
                        val documentId = backStackEntry.arguments?.getString("documentId") ?: ""
                        val encodedFileUrl = backStackEntry.arguments?.getString("fileUrl") ?: ""
                        val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""
                        val fileUrl = android.net.Uri.decode(encodedFileUrl)
                        val title = android.net.Uri.decode(encodedTitle)

                        android.util.Log.d(
                            "PDF_DEBUG",
                            "URL_MATCH=${PdfDebugState.lastOriginalUrl == fileUrl}"
                        )

                        com.pravor.notessharing.ui.features.documentViewing.PdfViewingScreen(
                            documentId = documentId,
                            fileUrl = fileUrl,
                            title = title,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = AppDestination.ImageViewing.route,
                        arguments = listOf(
                            androidx.navigation.navArgument("documentId") {
                                type = androidx.navigation.NavType.StringType
                            },
                            androidx.navigation.navArgument("fileUrl") {
                                type = androidx.navigation.NavType.StringType
                            },
                            androidx.navigation.navArgument("title") {
                                type = androidx.navigation.NavType.StringType
                            }
                        )
                    ) { backStackEntry ->
                        val documentId = backStackEntry.arguments?.getString("documentId") ?: ""
                        val encodedFileUrl = backStackEntry.arguments?.getString("fileUrl") ?: ""
                        val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""
                        val fileUrl = android.net.Uri.decode(encodedFileUrl)
                        val title = android.net.Uri.decode(encodedTitle)

                        com.pravor.notessharing.ui.features.documentViewing.ImageViewingScreen(
                            documentId = documentId,
                            fileUrl = fileUrl,
                            title = title,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = AppDestination.VideoDetail.route,
                        arguments = listOf(
                            androidx.navigation.navArgument("videoId") {
                                type = androidx.navigation.NavType.StringType
                            }
                        )
                    ) { backStackEntry ->
                        val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                        com.pravor.notessharing.ui.features.video.VideoDetailRoute(
                            videoId = videoId,
                            onBackClick = { navController.popBackStack() },
                            onNavigateToVideoDetail = { vidId ->
                                navController.navigate(AppDestination.VideoDetail.createRoute(vidId))
                            }
                        )
                    }
                    composable(AppDestination.Profile.route) {
                        ProfileRoute(
                            appSettings = appSettings,
                            onDarkModeChange = onDarkModeChange,
                            onThemePreferenceChange = onThemePreferenceChange,
                            onNotificationPreferencesClick = {
                                navController.navigate(AppDestination.NotificationPreferences.route)
                            },
                            onLogoutClick = {
                                authViewModel.logout()
                                navController.navigate(AppDestination.Welcome.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onEditProfileClick = {
                                navController.navigate(AppDestination.EditProfile.route)
                            },
                            onMyUploadsClick = {
                                navController.navigate(AppDestination.MyFiles.route) {
                                    launchSingleTop = true
                                }
                            },
                            onAboutClick = {
                                navController.navigate(AppDestination.About.route)
                            }
                        )
                    }
                    composable(AppDestination.EditProfile.route) {
                        EditProfileRoute(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToProfile = {
                                navController.popBackStack(
                                    AppDestination.Profile.route,
                                    inclusive = false
                                )
                            }
                        )
                    }
                    composable(AppDestination.NotificationPreferences.route) {
                        com.pravor.notessharing.ui.features.profile.NotificationPreferencesScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(AppDestination.About.route) {
                        com.pravor.notessharing.ui.features.about.AboutScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }

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
                            .heightIn(min = 44.dp)      // <- important
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
                            modifier = Modifier.padding(vertical = 2.dp) // <- reduce this
                        )
                    }
                }
            }
        }
    }
}
