@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.pravor.notessharing.ui.features.home

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.pravor.notessharing.core.util.RefreshCooldownManager
import com.pravor.notessharing.data.repository.UpvoteRepository
import com.pravor.notessharing.data.repository.UploadRepository
import com.pravor.notessharing.data.repository.ViewTrackingRepository
import com.pravor.notessharing.data.service.DownloadTracker
import com.pravor.notessharing.domain.model.FileType
import com.pravor.notessharing.domain.model.Notification
import com.pravor.notessharing.ui.common.CustomPullRefreshIndicator
import com.pravor.notessharing.ui.common.HomeUiState
import com.pravor.notessharing.ui.common.MyFilesUiState
import com.pravor.notessharing.ui.common.components.GroupedUploadViewerDialog
import com.pravor.notessharing.ui.common.components.StatePanel
import com.pravor.notessharing.ui.common.components.UploadViewerData
import com.pravor.notessharing.ui.common.loading.KnowledgeNetworkLoading
import com.pravor.notessharing.ui.features.home.components.HomeAtmosphericBackground
import com.pravor.notessharing.ui.features.home.components.HomeNotificationsBottomSheet
import com.pravor.notessharing.ui.features.home.components.HomeSuccessContent
import com.pravor.notessharing.ui.features.myfiles.BookmarkUiState
import com.pravor.notessharing.ui.features.myfiles.BookmarkViewModel
import com.pravor.notessharing.ui.features.myfiles.MyFilesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

@Composable
fun HomeRoute(
    onViewAllLibraryClick: () -> Unit = {},
    onMyUploadsClick: () -> Unit = {},
    onUploadClick: () -> Unit = {},
    onMyBookmarksClick: () -> Unit = {},
    onMyDownloadsClick: () -> Unit = {},
    onSeeMoreClick: () -> Unit = {},
    onDocumentClick: (String) -> Unit = {},
    onVideoClick: (String) -> Unit = {},
    pendingNotificationId: String? = null,
    onClearPendingNotificationId: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val activeDownloadsCount by DownloadTracker.activeDownloadsCount.collectAsStateWithLifecycle()
    val uploadsCount by viewModel.uploadsCount.collectAsStateWithLifecycle()
    val bookmarks by com.pravor.notessharing.data.repository.BookmarkRepository.bookmarksFlow.collectAsStateWithLifecycle()
    val bookmarksCount = bookmarks.size

    LaunchedEffect(Unit) {
        viewModel.refreshRecentlyOpened()

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    android.util.Log.e("FCM_TOKEN", "Failed", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                android.util.Log.d("FCM_TOKEN", token)

                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .update("fcmToken", token)
                }
            }
    }

    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val isGreetingVisible by viewModel.isGreetingVisible.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        isRefreshing = isRefreshing,
        onRefresh = {
            RefreshCooldownManager.runWithCooldown("home") {
                viewModel.loadRealDocuments(isPullToRefresh = true)
            }
        },
        uploadsCount = uploadsCount,
        bookmarksCount = bookmarksCount,
        activeDownloadsCount = activeDownloadsCount,
        notifications = notifications,
        unreadNotificationsCount = unreadNotificationsCount,
        isGreetingVisible = isGreetingVisible,
        onMarkNotificationRead = viewModel::markNotificationAsRead,
        onMarkAllNotificationsRead = viewModel::markAllNotificationsAsRead,
        onDeleteNotification = viewModel::deleteNotification,
        onClearAllNotifications = viewModel::clearAllNotifications,
        onUpvoteClick = viewModel::toggleUpvote,
        onBookmarkClick = viewModel::toggleSaved,
        onMyUploadsClick = onMyUploadsClick,
        onUploadClick = onUploadClick,
        onMyBookmarksClick = onMyBookmarksClick,
        onMyDownloadsClick = onMyDownloadsClick,
        onViewAllLibraryClick = {},
        onSeeMoreClick = onSeeMoreClick,
        onDocumentClick = onDocumentClick,
        onVideoClick = onVideoClick,
        pendingNotificationId = pendingNotificationId,
        onClearPendingNotificationId = onClearPendingNotificationId
    )
}

@SuppressLint("UnusedCrossfadeTargetStateParameter")
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    uploadsCount: Int,
    bookmarksCount: Int,
    activeDownloadsCount: Int,
    notifications: List<Notification>,
    unreadNotificationsCount: Int,
    isGreetingVisible: Boolean = true,
    onMarkNotificationRead: (String) -> Unit,
    onMarkAllNotificationsRead: () -> Unit,
    onDeleteNotification: (String) -> Unit,
    onClearAllNotifications: () -> Unit,
    onUpvoteClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onMyUploadsClick: () -> Unit,
    onUploadClick: () -> Unit = {},
    onMyBookmarksClick: () -> Unit,
    onMyDownloadsClick: () -> Unit,
    onViewAllLibraryClick: () -> Unit,
    onSeeMoreClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    pendingNotificationId: String? = null,
    onClearPendingNotificationId: () -> Unit = {},
    myFilesUiState: MyFilesUiState = MyFilesUiState.Loading,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedUploadForViewer by remember { mutableStateOf<UploadViewerData?>(null) }

    var pendingRemoveBookmarkId by remember { mutableStateOf<String?>(null) }
    var pendingRemoveUpvoteId by remember { mutableStateOf<String?>(null) }

    val feedListState = rememberLazyListState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var highlightedNotificationId by remember { mutableStateOf<String?>(null) }
    var showClearAllConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(pendingNotificationId) {
        if (!pendingNotificationId.isNullOrBlank()) {
            showBottomSheet = true
            highlightedNotificationId = pendingNotificationId
            onClearPendingNotificationId()
        }
    }

    LaunchedEffect(feedListState) {
        snapshotFlow {
            Pair(feedListState.firstVisibleItemIndex, feedListState.layoutInfo.visibleItemsInfo.size)
        }
        .distinctUntilChanged()
        .collect { (firstVisible, visibleCount) ->
            if (visibleCount > 0) {
                android.util.Log.d("PERF", "[PERF] First feed actually rendered firstVisible=$firstVisible visibleCount=$visibleCount thread=${Thread.currentThread().name}")
                android.util.Log.d("PERF", "[PERF] First visible item=$firstVisible")
                android.util.Log.d("PERF", "[PERF] Visible items count=$visibleCount")
            }
        }
    }
    val stateKey = when (uiState) {
        HomeUiState.Loading -> "loading"
        HomeUiState.Empty -> "empty"
        is HomeUiState.Error -> "error"
        is HomeUiState.Success -> "success"
    }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastVisible by remember { mutableStateOf(false) }
    var toastIcon by remember { mutableStateOf<ImageVector?>(null) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            toastVisible = true
            delay(1800)
            toastVisible = false
            delay(250)
            toastMessage = null
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Atmospheric drifting ambient geometry background
        HomeAtmosphericBackground(modifier = Modifier.fillMaxSize())

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            indicator = {
                CustomPullRefreshIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    restingOffset = 112.dp,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        val pullProgress = if (isRefreshing) 1f else pullToRefreshState.distanceFraction.coerceIn(0f, 1f)
                        val dimAlpha = pullProgress * 0.08f
                        if (dimAlpha > 0f) {
                            drawRect(Color.Black.copy(alpha = dimAlpha))
                        }
                    }
            ) {
                when (val state = uiState) {
                    HomeUiState.Loading -> KnowledgeNetworkLoading()
                    HomeUiState.Empty -> StatePanel("No notes yet", "Saved study resources will appear here", modifier = Modifier.padding(top = 96.dp))
                    is HomeUiState.Error -> StatePanel("Something went wrong", state.message, modifier = Modifier.padding(top = 96.dp))
                    is HomeUiState.Success -> HomeSuccessContent(
                        content = state.content,
                        myFilesUiState = myFilesUiState,
                        uploadsCount = uploadsCount,
                        bookmarksCount = bookmarksCount,
                        activeDownloadsCount = activeDownloadsCount,
                        unreadNotificationsCount = unreadNotificationsCount,
                        isGreetingVisible = isGreetingVisible,
                        onBellClick = { showBottomSheet = true },
                        onUpvoteClick = { itemId ->
                            val isCurrentlyUpvoted = UpvoteRepository.upvotesFlow.value[itemId] == true
                            if (isCurrentlyUpvoted) {
                                pendingRemoveUpvoteId = itemId
                            } else {
                                onUpvoteClick(itemId)
                            }
                        },
                        onBookmarkClick = { itemId ->
                            val isCurrentlySaved = state.content.feedItems.find { it.id == itemId }?.isSaved == true
                            if (isCurrentlySaved) {
                                pendingRemoveBookmarkId = itemId
                            } else {
                                toastMessage = "Saved to bookmarks"
                                toastIcon = Icons.Default.Bookmark
                                onBookmarkClick(itemId)
                            }
                        },
                        onMyUploadsClick = onMyUploadsClick,
                        onUploadClick = onUploadClick,
                        onMyBookmarksClick = onMyBookmarksClick,
                        onMyDownloadsClick = onMyDownloadsClick,
                        onViewAllLibraryClick = onViewAllLibraryClick,
                        onSeeMoreClick = onSeeMoreClick,
                        onDocumentClick = { docId ->
                            val feedItem = state.content.feedItems.find { it.id == docId } ?: state.content.recentlyOpened?.takeIf { it.id == docId }
                            val savedFile = (myFilesUiState as? MyFilesUiState.Success)?.content?.savedFiles?.find { it.id == docId }
                            val uploadedFile = (myFilesUiState as? MyFilesUiState.Success)?.content?.uploadedFiles?.find { it.id == docId }
                            val fileType = feedItem?.fileType ?: savedFile?.fileType ?: uploadedFile?.fileType

                            if (fileType == FileType.Video) {
                                onVideoClick(docId)
                            } else {
                                onDocumentClick(docId)
                            }
                        },
                        listState = feedListState
                    )
                }
            }
        }

        selectedUploadForViewer?.let { viewerData ->
            GroupedUploadViewerDialog(
                title = viewerData.title,
                fileUrls = viewerData.fileUrls,
                onDismiss = { selectedUploadForViewer = null },
                onFileClick = { url ->
                    try {
                        if (viewerData.id.isNotEmpty()) {
                            coroutineScope.launch {
                                ViewTrackingRepository().incrementViewCount(viewerData.id)
                            }
                        }
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            )
        }

        if (toastMessage != null) {
            Popup(
                alignment = Alignment.BottomCenter,
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false
                )
            ) {
                AnimatedVisibility(
                    visible = toastVisible,
                    enter = fadeIn(animationSpec = tween(250, easing = LinearOutSlowInEasing)) +
                            slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(250, easing = LinearOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(200)) +
                            slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200)),
                    modifier = Modifier.padding(bottom = 110.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0F172A).copy(alpha = 0.98f),
                        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.85f)),
                        shadowElevation = 10.dp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            toastIcon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB45C),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Text(
                                text = toastMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.15.sp
                                ),
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }
        }

        if (pendingRemoveBookmarkId != null) {
            AlertDialog(
                onDismissRequest = { pendingRemoveBookmarkId = null },
                title = { Text(text = "Remove this bookmark?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val itemId = pendingRemoveBookmarkId
                            if (itemId != null) {
                                toastMessage = "Removed from bookmarks"
                                toastIcon = Icons.Default.BookmarkBorder
                                onBookmarkClick(itemId)
                            }
                            pendingRemoveBookmarkId = null
                        }
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingRemoveBookmarkId = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (pendingRemoveUpvoteId != null) {
            AlertDialog(
                onDismissRequest = { pendingRemoveUpvoteId = null },
                title = { Text(text = "Remove this upvote?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val itemId = pendingRemoveUpvoteId
                            if (itemId != null) {
                                onUpvoteClick(itemId)
                            }
                            pendingRemoveUpvoteId = null
                        }
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingRemoveUpvoteId = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showClearAllConfirmation) {
            AlertDialog(
                onDismissRequest = { showClearAllConfirmation = false },
                title = { Text(text = "Clear all notifications?") },
                text = { Text(text = "This will permanently remove all notifications.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onClearAllNotifications()
                            showClearAllConfirmation = false
                        }
                    ) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showBottomSheet) {
            HomeNotificationsBottomSheet(
                sheetState = sheetState,
                unreadNotificationsCount = unreadNotificationsCount,
                visibleNotifications = notifications,
                highlightedNotificationId = highlightedNotificationId,
                onDismissRequest = {
                    showBottomSheet = false
                    highlightedNotificationId = null
                },
                onMarkAllNotificationsRead = onMarkAllNotificationsRead,
                onMarkNotificationRead = onMarkNotificationRead,
                onDeleteNotification = onDeleteNotification,
                onClearAllClick = { showClearAllConfirmation = true },
                onDocumentClick = onDocumentClick,
                onVideoClick = onVideoClick
            )
        }
    }
}
