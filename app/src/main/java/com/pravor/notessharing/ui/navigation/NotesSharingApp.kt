package com.pravor.notessharing.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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
import com.pravor.notessharing.ui.components.BottomNavBar
import com.pravor.notessharing.ui.screens.explore.DiscoverRoute
import com.pravor.notessharing.ui.screens.explore.ExploreRoute
import com.pravor.notessharing.ui.screens.explore.RecommendedVideosRoute
import com.pravor.notessharing.ui.screens.explore.ExamPrepRoute
import com.pravor.notessharing.ui.screens.explore.AssignmentsRoute
import com.pravor.notessharing.ui.screens.trending.TrendingNotesRoute
import com.pravor.notessharing.ui.screens.home.HomeRoute
import com.pravor.notessharing.ui.screens.myfiles.MyFilesRoute
import com.pravor.notessharing.ui.screens.myfiles.MyUploadsScreen
import com.pravor.notessharing.ui.screens.myfiles.MyBookmarksScreen
import com.pravor.notessharing.ui.screens.profile.ProfileRoute
import com.pravor.notessharing.ui.screens.profile.EditProfileRoute
import com.pravor.notessharing.ui.screens.upload.UploadRoute
import com.pravor.notessharing.ui.screens.upload.UploadSuccessRoute
import com.pravor.notessharing.ui.screens.document.DocumentDetailRoute
import com.pravor.notessharing.state.AppSettingsUiState
import com.pravor.notessharing.state.ThemePreference
import com.pravor.notessharing.ui.screens.auth.SplashScreen
import com.pravor.notessharing.ui.screens.auth.WelcomeScreen
import com.pravor.notessharing.ui.screens.auth.LoginScreen
import com.pravor.notessharing.ui.screens.auth.SignUpScreen
import com.pravor.notessharing.auth.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

object PdfDebugState {
    var lastOriginalUrl: String = ""
}

val LocalBottomBarPadding = androidx.compose.runtime.compositionLocalOf { 0.dp }

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
            val docId = intent.getStringExtra("document_id")
            android.util.Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "NotesSharingApp onNewIntent listener - docId: $docId")
            if (!docId.isNullOrBlank()) {
                navController.navigate(AppDestination.DocumentDetail.createRoute(docId)) {
                    popUpTo(AppDestination.Home.route)
                }
                intent.removeExtra("document_id")
            }
        }
        activity?.addOnNewIntentListener(listener)
        onDispose {
            activity?.removeOnNewIntentListener(listener)
        }
    }

    androidx.compose.runtime.LaunchedEffect(navController, activity) {
        val intent = activity?.intent
        val docId = intent?.getStringExtra("document_id")
        android.util.Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "NotesSharingApp LaunchedEffect intent check - docId: $docId")
        if (!docId.isNullOrBlank()) {
            navController.navigate(AppDestination.DocumentDetail.createRoute(docId)) {
                popUpTo(AppDestination.Home.route)
            }
            intent.removeExtra("document_id")
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedBottomRoute = if (currentRoute?.startsWith("${AppDestination.Explore.route}/") == true) {
        AppDestination.Explore.route
    } else {
        currentRoute
    }

    val isAuthScreen = currentRoute == AppDestination.Splash.route ||
            currentRoute == AppDestination.Welcome.route ||
            currentRoute == AppDestination.Login.route ||
            currentRoute == AppDestination.SignUp.route

    val showBottomBar = !isAuthScreen &&
            currentRoute?.startsWith("pdf_viewing") != true &&
            currentRoute?.startsWith("image_viewing") != true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    destinations = bottomDestinations,
                    currentRoute = selectedBottomRoute,
                    onDestinationClick = { destination ->
                        // Find the active tab root route in the backstack
                        val activeTabRoute = navController.currentBackStack.value
                            .lastOrNull { entry ->
                                entry.destination.route in bottomDestinations.map { it.route }
                            }?.destination?.route

                        // Pop all nested destinations above the active tab root
                        if (activeTabRoute != null) {
                            navController.popBackStack(activeTabRoute, inclusive = false)
                        }

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

        androidx.compose.runtime.CompositionLocalProvider(
            LocalBottomBarPadding provides innerPadding.calculateBottomPadding()
        ) {
            NavHost(
                navController = navController,
                startDestination = AppDestination.Splash.route,
                modifier = Modifier.padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 0.dp,
                    start = innerPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = innerPadding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
                )
            ) {
                composable(AppDestination.Splash.route) {
                    SplashScreen(
                        viewModel = authViewModel,
                        onNavigateToHome = {
                            navController.navigate(AppDestination.Home.route) {
                                popUpTo(AppDestination.Splash.route) { inclusive = true }
                            }
                        },
                        onNavigateToAuth = {
                            navController.navigate(AppDestination.Welcome.route) {
                                popUpTo(AppDestination.Splash.route) { inclusive = true }
                            }
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
                composable(AppDestination.Home.route) {
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
                            navController.navigate(AppDestination.DocumentDetail.createRoute(docId))
                        },
                        onVideoClick = { videoId ->
                            navController.navigate(AppDestination.VideoDetail.createRoute(videoId))
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
                        onDocumentClick = { docId ->
                            navController.navigate(AppDestination.DocumentDetail.createRoute(docId))
                        },
                        onVideoClick = { videoId ->
                            navController.navigate(AppDestination.VideoDetail.createRoute(videoId))
                        }
                    )
                }
                composable(AppDestination.TrendingNotes.route) {
                    TrendingNotesRoute(
                        onBackClick = { navController.popBackStack() },
                        onDocumentClick = { docId ->
                            navController.navigate(AppDestination.DocumentDetail.createRoute(docId))
                        }
                    )
                }
                composable(AppDestination.RecommendedVideos.route) {
                    RecommendedVideosRoute(
                        onBackClick = { navController.popBackStack() },
                        onVideoClick = { videoId ->
                            navController.navigate(AppDestination.VideoDetail.createRoute(videoId))
                        }
                    )
                }
                composable(AppDestination.Discover.route) {
                    DiscoverRoute(
                        onBackClick = { navController.popBackStack() },
                        onDocumentClick = { docId ->
                            navController.navigate(AppDestination.DocumentDetail.createRoute(docId))
                        },
                        onVideoClick = { videoId ->
                            navController.navigate(AppDestination.VideoDetail.createRoute(videoId))
                        }
                    )
                }
                composable(AppDestination.ExamPrep.route) {
                    ExamPrepRoute(
                        onBackClick = { navController.popBackStack() },
                        onDocumentClick = { docId ->
                            navController.navigate(AppDestination.DocumentDetail.createRoute(docId))
                        }
                    )
                }
                composable(AppDestination.Assignments.route) {
                    AssignmentsRoute(
                        onBackClick = { navController.popBackStack() },
                        onDocumentClick = { docId ->
                            navController.navigate(AppDestination.DocumentDetail.createRoute(docId))
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
                            navController.navigate(AppDestination.DocumentDetail.createRoute(docId))
                        },
                        onVideoClick = { videoId ->
                            navController.navigate(AppDestination.VideoDetail.createRoute(videoId))
                        }
                    )
                }
                composable(AppDestination.MyUploads.route) {
                    MyUploadsScreen(
                        onBackClick = { navController.popBackStack() },
                        onDocumentClick = { docId ->
                            navController.navigate(AppDestination.DocumentDetail.createRoute(docId))
                        },
                        onVideoClick = { videoId ->
                            navController.navigate(AppDestination.VideoDetail.createRoute(videoId))
                        }
                    )
                }
                composable(AppDestination.MyBookmarks.route) {
                    MyBookmarksScreen(
                        onBackClick = { navController.popBackStack() },
                        onDocumentClick = { docId ->
                            navController.navigate(AppDestination.DocumentDetail.createRoute(docId))
                        },
                        onVideoClick = { videoId ->
                            navController.navigate(AppDestination.VideoDetail.createRoute(videoId))
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
                            navController.navigate(AppDestination.DocumentDetail.createRoute(docId))
                        },
                        onNavigateToPdfViewer = { docId, fileUrl, title ->
                            PdfDebugState.lastOriginalUrl = fileUrl
                            navController.navigate(AppDestination.PdfViewing.createRoute(docId, fileUrl, title))
                        },
                        onNavigateToImageViewer = { docId, fileUrl, title ->
                            navController.navigate(AppDestination.ImageViewing.createRoute(docId, fileUrl, title))
                        }
                    )
                }
                composable(
                    route = AppDestination.PdfViewing.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("documentId") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("fileUrl") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("title") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val documentId = backStackEntry.arguments?.getString("documentId") ?: ""
                    val encodedFileUrl = backStackEntry.arguments?.getString("fileUrl") ?: ""
                    val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""
                    val fileUrl = android.net.Uri.decode(encodedFileUrl)
                    val title = android.net.Uri.decode(encodedTitle)
                    
                    android.util.Log.d("PDF_DEBUG", "URL_MATCH=${PdfDebugState.lastOriginalUrl == fileUrl}")
                    
                    com.pravor.notessharing.ui.screens.documentViewing.PdfViewingScreen(
                        documentId = documentId,
                        fileUrl = fileUrl,
                        title = title,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(
                    route = AppDestination.ImageViewing.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("documentId") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("fileUrl") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("title") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val documentId = backStackEntry.arguments?.getString("documentId") ?: ""
                    val encodedFileUrl = backStackEntry.arguments?.getString("fileUrl") ?: ""
                    val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""
                    val fileUrl = android.net.Uri.decode(encodedFileUrl)
                    val title = android.net.Uri.decode(encodedTitle)

                    com.pravor.notessharing.ui.screens.documentViewing.ImageViewingScreen(
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
                    com.pravor.notessharing.ui.screens.video.VideoDetailRoute(
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
                        onNotificationsChange = onNotificationsChange,
                        onLogoutClick = {
                            authViewModel.logout()
                            navController.navigate(AppDestination.Splash.route) {
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
            }
        }
    }
}
