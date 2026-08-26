package com.pravor.notessharing.ui.navigation

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pravor.notessharing.ui.common.AppSettingsUiState
import com.pravor.notessharing.ui.common.ThemePreference
import com.pravor.notessharing.ui.common.loading.StartupLoadingScreen
import com.pravor.notessharing.ui.features.auth.AuthViewModel
import com.pravor.notessharing.ui.features.auth.LoginScreen
import com.pravor.notessharing.ui.features.auth.SessionState
import com.pravor.notessharing.ui.features.auth.SignUpScreen
import com.pravor.notessharing.ui.features.auth.WelcomeScreen
import com.pravor.notessharing.ui.features.classroom.ClassroomCourseRoute
import com.pravor.notessharing.ui.features.classroom.ClassroomRoute
import com.pravor.notessharing.ui.features.classroom.ClassroomUpcomingRoute
import com.pravor.notessharing.ui.features.document.DocumentDetailRoute
import com.pravor.notessharing.ui.features.explore.AssignmentsRoute
import com.pravor.notessharing.ui.features.explore.DiscoverRoute
import com.pravor.notessharing.ui.features.explore.ExamPrepRoute
import com.pravor.notessharing.ui.features.explore.ExploreRoute
import com.pravor.notessharing.ui.features.explore.RecommendedVideosRoute
import com.pravor.notessharing.ui.features.explore.SubjectResourcesRoute
import com.pravor.notessharing.ui.features.home.HomeRoute
import com.pravor.notessharing.ui.features.myfiles.MyBookmarksScreen
import com.pravor.notessharing.ui.features.myfiles.MyFilesRoute
import com.pravor.notessharing.ui.features.myfiles.MyUploadsScreen
import com.pravor.notessharing.ui.features.onboarding.UpdatesScreen
import com.pravor.notessharing.ui.features.profile.EditProfileRoute
import com.pravor.notessharing.ui.features.profile.ProfileRoute
import com.pravor.notessharing.ui.features.trending.TrendingNotesRoute
import com.pravor.notessharing.ui.features.upload.UploadRoute
import com.pravor.notessharing.ui.features.upload.UploadSuccessRoute

@Composable
fun NavGraph(
    navController: NavHostController,
    sessionState: SessionState,
    hasBootstrapped: Boolean,
    authViewModel: AuthViewModel,
    activity: Activity?,
    appSettings: AppSettingsUiState,
    onDarkModeChange: (Boolean) -> Unit,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    innerPadding: PaddingValues
) {
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
                Box(Modifier.fillMaxSize())
            }
        }
        composable("onboarding") {
            val context = androidx.compose.ui.platform.LocalContext.current
            val prefs = remember {
                context.getSharedPreferences(
                    "app_settings",
                    Context.MODE_PRIVATE
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
                onUploadClick = {
                    navController.navigate(AppDestination.Upload.route) {
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
                        AppDestination.DocumentDetail.createRoute(docId)
                    )
                },
                onVideoClick = { videoId ->
                    navController.navigate(
                        AppDestination.VideoDetail.createRoute(videoId)
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
                        AppDestination.SubjectResources.createRoute(subjectName)
                    ) {
                        launchSingleTop = true
                    }
                },
                onDocumentClick = { docId ->
                    navController.navigate(
                        AppDestination.DocumentDetail.createRoute(docId)
                    )
                },
                onVideoClick = { videoId ->
                    navController.navigate(
                        AppDestination.VideoDetail.createRoute(videoId)
                    )
                }
            )
        }
        composable(AppDestination.Search.route) {
            com.pravor.notessharing.ui.features.search.SearchRoute(
                onBackClick = { navController.popBackStack() },
                onDocumentClick = { docId ->
                    navController.navigate(
                        AppDestination.DocumentDetail.createRoute(docId)
                    )
                },
                onVideoClick = { videoId ->
                    navController.navigate(
                        AppDestination.VideoDetail.createRoute(videoId)
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
                        AppDestination.DocumentDetail.createRoute(docId)
                    )
                },
                onVideoClick = { videoId ->
                    navController.navigate(
                        AppDestination.VideoDetail.createRoute(videoId)
                    )
                }
            )
        }
        composable(AppDestination.TrendingNotes.route) {
            TrendingNotesRoute(
                onBackClick = { navController.popBackStack() },
                onDocumentClick = { docId ->
                    navController.navigate(
                        AppDestination.DocumentDetail.createRoute(docId)
                    )
                }
            )
        }
        composable(AppDestination.RecommendedVideos.route) {
            RecommendedVideosRoute(
                onBackClick = { navController.popBackStack() },
                onVideoClick = { videoId ->
                    navController.navigate(
                        AppDestination.VideoDetail.createRoute(videoId)
                    )
                }
            )
        }
        composable(AppDestination.Discover.route) {
            DiscoverRoute(
                onBackClick = { navController.popBackStack() },
                onDocumentClick = { docId ->
                    navController.navigate(
                        AppDestination.DocumentDetail.createRoute(docId)
                    )
                },
                onVideoClick = { videoId ->
                    navController.navigate(
                        AppDestination.VideoDetail.createRoute(videoId)
                    )
                }
            )
        }
        composable(AppDestination.ExamPrep.route) {
            ExamPrepRoute(
                onBackClick = { navController.popBackStack() },
                onDocumentClick = { docId ->
                    navController.navigate(
                        AppDestination.DocumentDetail.createRoute(docId)
                    )
                }
            )
        }
        composable(AppDestination.Assignments.route) {
            AssignmentsRoute(
                onBackClick = { navController.popBackStack() },
                onDocumentClick = { docId ->
                    navController.navigate(
                        AppDestination.DocumentDetail.createRoute(docId)
                    )
                }
            )
        }
        composable(AppDestination.Classroom.route) {
            ClassroomRoute(
                onCourseClick = { courseId ->
                    navController.navigate(AppDestination.ClassroomCourse.createRoute(courseId)) {
                        launchSingleTop = true
                    }
                },
                onUpcomingClick = {
                    navController.navigate(AppDestination.ClassroomUpcoming.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(AppDestination.ClassroomUpcoming.route) {
            ClassroomUpcomingRoute(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        composable(AppDestination.ClassroomCourse.route) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId").orEmpty()
            ClassroomCourseRoute(
                courseId = courseId,
                onBackClick = {
                    navController.popBackStack()
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
                        AppDestination.DocumentDetail.createRoute(docId)
                    )
                },
                onVideoClick = { videoId ->
                    navController.navigate(
                        AppDestination.VideoDetail.createRoute(videoId)
                    )
                }
            )
        }
        composable(AppDestination.MyUploads.route) {
            MyUploadsScreen(
                onBackClick = { navController.popBackStack() },
                onDocumentClick = { docId ->
                    navController.navigate(
                        AppDestination.DocumentDetail.createRoute(docId)
                    )
                },
                onVideoClick = { videoId ->
                    navController.navigate(
                        AppDestination.VideoDetail.createRoute(videoId)
                    )
                }
            )
        }
        composable(AppDestination.MyBookmarks.route) {
            MyBookmarksScreen(
                onBackClick = { navController.popBackStack() },
                onDocumentClick = { docId ->
                    navController.navigate(
                        AppDestination.DocumentDetail.createRoute(docId)
                    )
                },
                onVideoClick = { videoId ->
                    navController.navigate(
                        AppDestination.VideoDetail.createRoute(videoId)
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
                        AppDestination.DocumentDetail.createRoute(docId)
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
}
