package com.pravor.notessharing.ui.features.classroom

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.ui.common.CustomPullRefreshIndicator
import com.pravor.notessharing.ui.common.components.StatePanel
import com.pravor.notessharing.ui.features.classroom.components.ClassVisibilityBottomSheet
import com.pravor.notessharing.ui.features.classroom.components.ClassroomAccountCard
import com.pravor.notessharing.ui.features.classroom.components.ClassroomConnectCard
import com.pravor.notessharing.ui.features.classroom.components.ClassroomCourseCard
import com.pravor.notessharing.ui.features.classroom.components.ClassroomDisconnectDialog
import com.pravor.notessharing.ui.features.classroom.components.ClassroomDisconnectRow
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import com.pravor.notessharing.ui.theme.ElectricBlue

@Composable
fun ClassroomRoute(
    onCourseClick: (String) -> Unit = {},
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
            viewModel.launchClassroomAuth { intent ->
                authLauncher.launch(intent)
            }
        },
        onDisconnectConfirmed = viewModel::disconnectClassroom,
        onCourseClick = onCourseClick,
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
    onRefresh: () -> Unit,
    onRetryCourses: () -> Unit,
    onSaveHiddenCourses: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomPadding = LocalBottomBarPadding.current
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showVisibilitySheet by remember { mutableStateOf(false) }

    val isConnected = uiState is ClassroomUiState.Connected
    val isRefreshing = (uiState as? ClassroomUiState.Connected)?.isRefreshing == true
    val pullToRefreshState = rememberPullToRefreshState()

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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                        // 1. Redesigned Hero Account & Sync Card
                        item(key = "account-card") {
                            ClassroomAccountCard(
                                account = uiState.account,
                                syncStatus = uiState.syncStatus,
                                onRefreshClick = onRefresh
                            )
                        }

                        // 2. "My Classes" Section Header with Class Visibility Filter Action
                        item(key = "classes-header") {
                            val hasHidden = uiState.hiddenCourseIds.isNotEmpty()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "My Classes",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                if (uiState.allCourses.isNotEmpty()) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (hasHidden) ElectricBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        border = if (hasHidden) BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.35f)) else null,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        IconButton(
                                            onClick = { showVisibilitySheet = true },
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            BadgedBox(
                                                badge = {
                                                    if (hasHidden) {
                                                        Badge(
                                                            containerColor = ElectricBlue,
                                                            modifier = Modifier.size(6.dp)
                                                        ) {}
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Tune,
                                                    contentDescription = "Manage class visibility",
                                                    tint = if (hasHidden) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Courses List State
                        if (uiState.isCoursesLoading && uiState.allCourses.isEmpty()) {
                            item(key = "loading-courses") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = ElectricBlue,
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(Modifier.width(14.dp))
                                        Text(
                                            text = "Loading classes from Google Classroom...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else if (uiState.coursesError != null && uiState.allCourses.isEmpty()) {
                            item(key = "error-courses") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Couldn't load your classes",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = uiState.coursesError,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        Button(
                                            onClick = onRetryCourses,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text(
                                                text = "Retry",
                                                color = Color(0xFF07121E),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (uiState.allCourses.isEmpty()) {
                            item(key = "empty-courses") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            modifier = Modifier.size(52.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Class,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            text = "No active classes found",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Your Google Classroom courses will appear here when you're enrolled.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else if (uiState.visibleCourses.isEmpty()) {
                            // User hid all their active classes
                            item(key = "all-hidden-courses") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            modifier = Modifier.size(52.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Tune,
                                                    contentDescription = null,
                                                    tint = ElectricBlue,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            text = "No classes visible",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "You've hidden all your classes from the main list.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        OutlinedButton(
                                            onClick = { showVisibilitySheet = true },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = "Manage hidden classes",
                                                color = ElectricBlue,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            items(uiState.visibleCourses, key = { it.id }) { course ->
                                ClassroomCourseCard(
                                    course = course,
                                    onClick = { onCourseClick(course.id) }
                                )
                            }
                        }

                        // 4. Dedicated Disconnect Row at the bottom of the list
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
