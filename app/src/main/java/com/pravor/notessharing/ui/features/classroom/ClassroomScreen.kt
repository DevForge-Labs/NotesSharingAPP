package com.pravor.notessharing.ui.features.classroom

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pravor.notessharing.ui.common.CustomPullRefreshIndicator
import com.pravor.notessharing.ui.common.components.StatePanel
import com.pravor.notessharing.ui.features.classroom.components.ClassVisibilityBottomSheet
import com.pravor.notessharing.ui.features.classroom.components.ClassroomAccountCard
import com.pravor.notessharing.ui.features.classroom.components.ClassroomClassesHeader
import com.pravor.notessharing.ui.features.classroom.components.ClassroomConnectCard
import com.pravor.notessharing.ui.features.classroom.components.ClassroomCourseCard
import com.pravor.notessharing.ui.features.classroom.components.ClassroomCoursesAllHiddenCard
import com.pravor.notessharing.ui.features.classroom.components.ClassroomCoursesEmptyCard
import com.pravor.notessharing.ui.features.classroom.components.ClassroomCoursesErrorCard
import com.pravor.notessharing.ui.features.classroom.components.ClassroomCoursesLoadingCard
import com.pravor.notessharing.ui.features.classroom.components.ClassroomDisconnectDialog
import com.pravor.notessharing.ui.features.classroom.components.ClassroomDisconnectRow
import com.pravor.notessharing.ui.features.classroom.components.ClassroomUpcomingCard
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import com.pravor.notessharing.ui.theme.ElectricBlue

@Composable
fun ClassroomRoute(
    onCourseClick: (String) -> Unit = {},
    onUpcomingClick: () -> Unit = {},
    viewModel: ClassroomViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleAuthResult(result.data)
    }

    ClassroomScreen(
        uiState = uiState,
        onConnectClick = {
            authLauncher.launch(viewModel.getAuthIntent())
        },
        onDisconnectConfirmed = viewModel::disconnectClassroom,
        onCourseClick = onCourseClick,
        onUpcomingClick = onUpcomingClick,
        onRefresh = viewModel::refreshAuth,
        onRetryCourses = { viewModel.syncCourses(isPullToRefresh = false) },
        onSaveHiddenCourses = viewModel::saveHiddenCourses
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomScreen(
    uiState: ClassroomUiState,
    onConnectClick: () -> Unit,
    onDisconnectConfirmed: () -> Unit,
    onCourseClick: (String) -> Unit,
    onUpcomingClick: () -> Unit = {},
    onRefresh: () -> Unit,
    onRetryCourses: () -> Unit,
    onSaveHiddenCourses: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomPadding = LocalBottomBarPadding.current
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showVisibilitySheet by remember { mutableStateOf(false) }

    val isRefreshing = (uiState as? ClassroomUiState.Connected)?.isRefreshing == true
    val pullToRefreshState = rememberPullToRefreshState()

    // Classroom background animation for connected state
    val bgLottieCompositionResult = rememberLottieComposition(
        LottieCompositionSpec.Asset("App_animations/classroom_home_back.json")
    )
    val bgLottieComposition = bgLottieCompositionResult.value
    val bgLottieProgress by animateLottieCompositionAsState(
        composition = bgLottieComposition,
        iterations = LottieConstants.IterateForever
    )

    val connectedEmail = (uiState as? ClassroomUiState.Connected)?.account?.email.orEmpty()

    if (showDisconnectDialog && connectedEmail.isNotBlank()) {
        ClassroomDisconnectDialog(
            accountEmail = connectedEmail,
            onConfirm = {
                showDisconnectDialog = false
                onDisconnectConfirmed()
            },
            onDismiss = {
                showDisconnectDialog = false
            }
        )
    }

    if (showVisibilitySheet && uiState is ClassroomUiState.Connected) {
        ClassVisibilityBottomSheet(
            allCourses = uiState.allCourses,
            initiallyHiddenIds = uiState.hiddenCourseIds,
            onDismissRequest = { showVisibilitySheet = false },
            onSavePreferences = onSaveHiddenCourses
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ElectricBlue.copy(alpha = 0.15f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = "Classroom",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Background animated Lottie illustration for connected state
            if (bgLottieComposition != null && uiState is ClassroomUiState.Connected) {
                LottieAnimation(
                    composition = bgLottieComposition,
                    progress = { bgLottieProgress },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .offset(y = 75.dp)
                        .alpha(0.30f)
                )
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                state = pullToRefreshState,
                indicator = {
                    CustomPullRefreshIndicator(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        restingOffset = 64.dp,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 12.dp,
                    bottom = 16.dp + bottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (uiState) {
                    is ClassroomUiState.Disconnected -> {
                        item(key = "connect-card") {
                            ClassroomConnectCard(
                                onConnectClick = onConnectClick
                            )
                        }
                    }
                    is ClassroomUiState.Loading -> {
                        item(key = "loading-state") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(color = ElectricBlue)
                                    Text(
                                        text = "Connecting to Google Classroom...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    is ClassroomUiState.Connected -> {
                        // 1. Hero Account & Sync Card
                        item(key = "account-card") {
                            ClassroomAccountCard(
                                account = uiState.account,
                                syncStatus = uiState.syncStatus,
                                onRefreshClick = onRefresh
                            )
                        }

                        // 2. Upcoming Assignments Summary Card
                        item(key = "upcoming-card") {
                            ClassroomUpcomingCard(
                                upcomingCount = uiState.upcomingCount,
                                isLoading = uiState.isCoursesLoading && uiState.allCourses.isEmpty(),
                                onClick = onUpcomingClick
                            )
                        }

                        // 3. "My Classes" Section Header with Class Visibility Filter Action
                        item(key = "classes-header") {
                            ClassroomClassesHeader(
                                hasHiddenCourses = uiState.hiddenCourseIds.isNotEmpty(),
                                showFilterButton = uiState.allCourses.isNotEmpty(),
                                onFilterClick = { showVisibilitySheet = true }
                            )
                        }

                        // 4. Courses List / State
                        if (uiState.isCoursesLoading && uiState.allCourses.isEmpty()) {
                            item(key = "loading-courses") {
                                ClassroomCoursesLoadingCard()
                            }
                        } else if (uiState.coursesError != null && uiState.allCourses.isEmpty()) {
                            item(key = "error-courses") {
                                ClassroomCoursesErrorCard(
                                    errorMessage = uiState.coursesError,
                                    onRetry = onRetryCourses
                                )
                            }
                        } else if (uiState.allCourses.isEmpty()) {
                            item(key = "empty-courses") {
                                ClassroomCoursesEmptyCard()
                            }
                        } else if (uiState.visibleCourses.isEmpty()) {
                            item(key = "all-hidden-courses") {
                                ClassroomCoursesAllHiddenCard(
                                    onManageHiddenClick = { showVisibilitySheet = true }
                                )
                            }
                        } else {
                            itemsIndexed(uiState.visibleCourses, key = { _, course -> course.id }) { index, course ->
                                ClassroomCourseCard(
                                    course = course,
                                    index = index,
                                    onClick = { onCourseClick(course.id) }
                                )
                            }
                        }

                        // 5. Dedicated Disconnect Row at the bottom of the list
                        item(key = "disconnect-row") {
                            Spacer(Modifier.height(8.dp))
                            ClassroomDisconnectRow(
                                onClick = { showDisconnectDialog = true }
                            )
                        }
                    }
                    is ClassroomUiState.Error -> {
                        item(key = "error-state") {
                            StatePanel(
                                title = "Classroom Connection",
                                message = uiState.message,
                                modifier = Modifier.padding(top = 40.dp)
                            )
                        }
                    }
                    is ClassroomUiState.Empty -> {
                        item(key = "empty-state") {
                            StatePanel(
                                title = "No Classroom Data",
                                message = "No classroom resources available for this account.",
                                modifier = Modifier.padding(top = 40.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
}
