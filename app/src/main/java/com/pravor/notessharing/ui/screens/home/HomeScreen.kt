@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.pravor.notessharing.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.functions
import com.google.firebase.messaging.FirebaseMessaging
import com.pravor.notessharing.state.HomeContent
import com.pravor.notessharing.state.HomeUiState
import com.pravor.notessharing.state.MyFilesUiState
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.ui.components.loading.KnowledgeNetworkLoading
import com.pravor.notessharing.ui.components.home_components.HomeSuccessContent
import com.pravor.notessharing.ui.theme.NotesSharingTheme
import com.pravor.notessharing.viewmodel.DummyData
import com.pravor.notessharing.viewmodel.HomeViewModel
import com.pravor.notessharing.viewmodel.MyFilesViewModel
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import com.pravor.notessharing.model.getRelativeTime
import com.pravor.notessharing.model.Notification
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.lerp
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material.icons.filled.Delete
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

@Composable
fun HomeRoute(
    onViewAllLibraryClick: () -> Unit = {},
    onMyUploadsClick: () -> Unit = {},
    onMyBookmarksClick: () -> Unit = {},
    onMyDownloadsClick: () -> Unit = {},
    onSeeMoreClick: () -> Unit = {},
    onDocumentClick: (String) -> Unit = {},
    onVideoClick: (String) -> Unit = {},
    pendingNotificationId: String? = null,
    onClearPendingNotificationId: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
    myFilesViewModel: MyFilesViewModel = viewModel(),
    bookmarkViewModel: com.pravor.notessharing.bookmarks.BookmarkViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val myFilesUiState by myFilesViewModel.uiState.collectAsStateWithLifecycle()
    val bookmarkUiState by bookmarkViewModel.uiState.collectAsStateWithLifecycle()
    val activeDownloadsCount by com.pravor.notessharing.data.download.DownloadTracker.activeDownloadsCount.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshRecentlyOpened()
        // Refresh bookmarks count cleanly from bookmark layer
        bookmarkViewModel.loadBookmarksForCurrentUser()
        myFilesViewModel.loadDownloads(context)

        //firebase FCM token
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

    val bookmarksCount = when (val state = bookmarkUiState) {
        is com.pravor.notessharing.bookmarks.BookmarkUiState.Success -> state.bookmarks.size
        else -> 0
    }
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
 
    HomeScreen(
        uiState = uiState,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.loadRealDocuments(isPullToRefresh = true) },
        myFilesUiState = myFilesUiState,
        bookmarksCount = bookmarksCount,
        activeDownloadsCount = activeDownloadsCount,
        notifications = notifications,
        unreadNotificationsCount = unreadNotificationsCount,
        onMarkNotificationRead = viewModel::markNotificationAsRead,
        onMarkAllNotificationsRead = viewModel::markAllNotificationsAsRead,
        onDeleteNotification = viewModel::deleteNotification,
        onClearAllNotifications = viewModel::clearAllNotifications,
        onUpvoteClick = viewModel::toggleUpvote,
        onBookmarkClick = viewModel::toggleSaved,
        onMyUploadsClick = onMyUploadsClick,
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
    myFilesUiState: MyFilesUiState,
    bookmarksCount: Int,
    activeDownloadsCount: Int,
    notifications: List<com.pravor.notessharing.model.Notification>,
    unreadNotificationsCount: Int,
    onMarkNotificationRead: (String) -> Unit,
    onMarkAllNotificationsRead: () -> Unit,
    onDeleteNotification: (String) -> Unit,
    onClearAllNotifications: () -> Unit,
    onUpvoteClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onMyUploadsClick: () -> Unit,
    onMyBookmarksClick: () -> Unit,
    onMyDownloadsClick: () -> Unit,
    onViewAllLibraryClick: () -> Unit,
    onSeeMoreClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    pendingNotificationId: String? = null,
    onClearPendingNotificationId: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val recompositionCount = remember { AtomicInteger(0) }
    androidx.compose.runtime.SideEffect {
        android.util.Log.d("RECOMPOSE", "[RECOMPOSE] HomeScreen count=${recompositionCount.incrementAndGet()}")
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val repository = remember { com.pravor.notessharing.data.UploadRepository(context) }
    var selectedUploadForViewer by remember { mutableStateOf<com.pravor.notessharing.ui.components.UploadViewerData?>(null) }
 
    var pendingRemoveBookmarkId by remember { mutableStateOf<String?>(null) }
    var pendingRemoveUpvoteId by remember { mutableStateOf<String?>(null) }
 
    val feedListState = rememberLazyListState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
 
    var highlightedNotificationId by remember { mutableStateOf<String?>(null) }
    var animatedHighlightId by remember { mutableStateOf<String?>(null) }
    var dismissedNotificationIds by remember { mutableStateOf(setOf<String>()) }
    val visibleNotifications = remember(notifications, dismissedNotificationIds) {
        notifications.filter { it.id !in dismissedNotificationIds }
    }
    
    LaunchedEffect(showBottomSheet) {
        if (!showBottomSheet) {
            dismissedNotificationIds = emptySet()
            highlightedNotificationId = null
            animatedHighlightId = null
        }
    }
    var showClearAllConfirmation by remember { mutableStateOf(false) }
 
    LaunchedEffect(pendingNotificationId) {
        if (!pendingNotificationId.isNullOrBlank()) {
            showBottomSheet = true
            highlightedNotificationId = pendingNotificationId
            onClearPendingNotificationId()
        }
    }

    LaunchedEffect(feedListState) {
        androidx.compose.runtime.snapshotFlow {
            Pair(feedListState.firstVisibleItemIndex, feedListState.layoutInfo.visibleItemsInfo.size)
        }
        .distinctUntilChanged()
        .collect { (firstVisible, visibleCount) ->
            if (visibleCount > 0) {
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
 
    // Floating pill toast snackbar states
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastVisible by remember { mutableStateOf(false) }
    var toastIcon by remember { mutableStateOf<androidx.compose.ui.graphics.vector.ImageVector?>(null) }
 
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            toastVisible = true
            kotlinx.coroutines.delay(1800) // premium short non-intrusive duration
            toastVisible = false
            kotlinx.coroutines.delay(250) // wait for exit animation
            toastMessage = null
        }
    }
 
    val pullToRefreshState = rememberPullToRefreshState()
 
    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
            Crossfade(targetState = stateKey, label = "home-state", modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    HomeUiState.Loading -> KnowledgeNetworkLoading()
                    HomeUiState.Empty -> StatePanel("No notes yet", "Saved study resources will appear here", modifier = Modifier.padding(top = 96.dp))
                    is HomeUiState.Error -> StatePanel("Something went wrong", state.message, modifier = Modifier.padding(top = 96.dp))
                    is HomeUiState.Success -> HomeSuccessContent(
                        content = state.content,
                        myFilesUiState = myFilesUiState,
                        bookmarksCount = bookmarksCount,
                        activeDownloadsCount = activeDownloadsCount,
                        unreadNotificationsCount = unreadNotificationsCount,
                        onBellClick = { showBottomSheet = true },
                        onUpvoteClick = { itemId ->
                            val isCurrentlyUpvoted = com.pravor.notessharing.upvotes.UpvoteRepository.upvotesFlow.value[itemId] == true
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
                        onMyBookmarksClick = onMyBookmarksClick,
                        onMyDownloadsClick = onMyDownloadsClick,
                        onViewAllLibraryClick = onViewAllLibraryClick,
                        onSeeMoreClick = onSeeMoreClick,
                        onDocumentClick = { docId ->
                            val feedItem = state.content.feedItems.find { it.id == docId } ?: state.content.recentlyOpened?.takeIf { it.id == docId }
                            val savedFile = (myFilesUiState as? MyFilesUiState.Success)?.content?.savedFiles?.find { it.id == docId }
                            val uploadedFile = (myFilesUiState as? MyFilesUiState.Success)?.content?.uploadedFiles?.find { it.id == docId }
                            val fileType = feedItem?.fileType ?: savedFile?.fileType ?: uploadedFile?.fileType
                            
                            if (fileType == com.pravor.notessharing.model.FileType.Video) {
                                onVideoClick(docId)
                            } else {
                                onDocumentClick(docId)
                            }
                        },
                        listState = feedListState
                    )
                }
            }

            // Helper text overlay when user pulls down
            androidx.compose.animation.AnimatedVisibility(
                visible = pullToRefreshState.distanceFraction > 0.1f && !isRefreshing,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = "Pull down to refresh feed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        selectedUploadForViewer?.let { viewerData ->
            com.pravor.notessharing.ui.components.GroupedUploadViewerDialog(
                title = viewerData.title,
                fileUrls = viewerData.fileUrls,
                onDismiss = { selectedUploadForViewer = null },
                onFileClick = { url ->
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            )
        }

        // Custom Premium Pill Capsule Snackbar Toast feedback
        if (toastMessage != null) {
            androidx.compose.ui.window.Popup(
                alignment = Alignment.BottomCenter,
                properties = androidx.compose.ui.window.PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false
                )
            ) {
                AnimatedVisibility(
                    visible = toastVisible,
                    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) +
                            slideInVertically(initialOffsetY = { it / 2 }, animationSpec = androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.LinearOutSlowInEasing)),
                    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) +
                            slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = androidx.compose.animation.core.tween(200)),
                    modifier = Modifier
                        .padding(bottom = 110.dp) // Float height perfectly above the bottom bar with breathing room
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0F172A).copy(alpha = 0.98f), // premium dark slate surface
                        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.85f)), // subtle high-contrast outline
                        shadowElevation = 10.dp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp), // premium refined proportions
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            toastIcon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB45C), // subtle warm bookmark accent
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
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { pendingRemoveBookmarkId = null },
                title = { Text(text = "Remove this bookmark?") },
                confirmButton = {
                    androidx.compose.material3.TextButton(
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
                    androidx.compose.material3.TextButton(
                        onClick = { pendingRemoveBookmarkId = null }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (pendingRemoveUpvoteId != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { pendingRemoveUpvoteId = null },
                title = { Text(text = "Remove this upvote?") },
                confirmButton = {
                    androidx.compose.material3.TextButton(
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
                    androidx.compose.material3.TextButton(
                        onClick = { pendingRemoveUpvoteId = null }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showClearAllConfirmation) {
            androidx.compose.material3.AlertDialog(
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
                    TextButton(
                        onClick = { showClearAllConfirmation = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                    highlightedNotificationId = null
                },
                sheetState = sheetState,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = Color(0xFF0F172A),
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF334155)) }
            ) {
                Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.75f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(horizontal = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Notifications",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = Color.White
                            )
                            
                            if (unreadNotificationsCount > 0) {
                                TextButton(onClick = onMarkAllNotificationsRead) {
                                    Text(
                                        text = "Mark all read",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (visibleNotifications.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .navigationBarsPadding(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                ) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = Color(0xFF334155),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Text(
                                        text = "No notifications yet",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = "You'll see updates about notes,\nassignments, PYQs, videos and resources here.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF94A3B8),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            val notificationsListState = rememberLazyListState()
                            
                            LaunchedEffect(showBottomSheet, highlightedNotificationId, visibleNotifications) {
                                if (showBottomSheet && !highlightedNotificationId.isNullOrBlank() && visibleNotifications.isNotEmpty()) {
                                    val index = visibleNotifications.indexOfFirst { it.id == highlightedNotificationId }
                                    if (index >= 0) {
                                        val viewportHeight = notificationsListState.layoutInfo.viewportEndOffset
                                        val offset = if (viewportHeight > 0) -(viewportHeight / 3) else -300
                                        notificationsListState.animateScrollToItem(index, offset)
                                        animatedHighlightId = highlightedNotificationId
                                        val targetNotification = visibleNotifications[index]
                                        if (!targetNotification.read) {
                                            onMarkNotificationRead(targetNotification.id)
                                        }
                                    }
                                }
                            }

                            LazyColumn(
                                state = notificationsListState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 8.dp)
                            ) {
                                items(visibleNotifications, key = { it.id }) { notification ->
                                    val dismissState = rememberSwipeToDismissBoxState(
                                        confirmValueChange = { value ->
                                            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                                                dismissedNotificationIds = dismissedNotificationIds + notification.id
                                                onDeleteNotification(notification.id)
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                    )

                                    SwipeToDismissBox(
                                        state = dismissState,
                                        modifier = Modifier.animateItem(),
                                        backgroundContent = {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        color = Color(0xFF334155).copy(alpha = 0.4f),
                                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                                                    )
                                            )
                                        },
                                        content = {
                                            NotificationItemRow(
                                                notification = notification,
                                                isHighlighted = notification.id == animatedHighlightId,
                                                onMarkRead = { onMarkNotificationRead(notification.id) },
                                                onNavigate = {
                                                    showBottomSheet = false
                                                    val targetType = notification.type ?: ""
                                                    if (targetType.contains("video", ignoreCase = true)) {
                                                        onVideoClick(notification.targetId ?: "")
                                                    } else {
                                                        onDocumentClick(notification.targetId ?: "")
                                                    }
                                                }
                                            )
                                        }
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(top = 12.dp, bottom = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(
                                    onClick = {
                                        showClearAllConfirmation = true
                                    }
                                ) {
                                    Text(
                                        text = "Clear All Notifications",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8)
                                        )
                                    )
                                }
                            }
                        }
                    }


                }
            }
        }
    }
}

@Composable
private fun NotificationItemRow(
    notification: com.pravor.notessharing.model.Notification,
    isHighlighted: Boolean = false,
    onMarkRead: () -> Unit,
    onNavigate: () -> Unit
) {
    val isLong = notification.message.contains("\n") || notification.message.length > 55
    var isExpanded by remember { mutableStateOf(false) }
    
    val highlightAlpha = remember { Animatable(if (isHighlighted) 1f else 0f) }
    
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            highlightAlpha.snapTo(1f)
            kotlinx.coroutines.delay(800)
            highlightAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 2200, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
            )
        }
    }
    
    val highlightColor = MaterialTheme.colorScheme.primary
    val borderStroke = if (highlightAlpha.value > 0f) {
        BorderStroke(
            width = 2.dp,
            color = highlightColor.copy(alpha = highlightAlpha.value)
        )
    } else {
        if (notification.read) BorderStroke(1.dp, Color(0xFF1E293B)) else BorderStroke(1.dp, Color(0xFF334155))
    }
    
    val baseColor = if (notification.read) Color.Transparent else Color(0xFF1E293B)
    val finalContainerColor = if (highlightAlpha.value > 0f) {
        lerp(
            start = baseColor,
            stop = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            fraction = highlightAlpha.value
        )
    } else {
        baseColor
    }

    Card(
        onClick = {
            if (isLong) {
                isExpanded = !isExpanded
            } else {
                onMarkRead()
                if (!notification.targetId.isNullOrBlank()) {
                    onNavigate()
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = finalContainerColor
        ),
        border = borderStroke
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (!notification.read) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                        .align(Alignment.CenterVertically)
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Spacer(modifier = Modifier.width(4.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (notification.read) FontWeight.Medium else FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = if (notification.read) Color(0xFF94A3B8) else Color.White
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp
                    ),
                    color = if (notification.read) Color(0xFF64748B) else Color(0xFFCBD5E1),
                    maxLines = if (isLong && !isExpanded) 1 else Int.MAX_VALUE,
                    overflow = if (isLong && !isExpanded) TextOverflow.Ellipsis else TextOverflow.Clip
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = com.pravor.notessharing.model.getRelativeTime(notification.createdAt),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp
                        ),
                        color = Color(0xFF64748B)
                    )
                    
                    if (isLong && isExpanded) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!notification.read) {
                                TextButton(
                                    onClick = onMarkRead,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = "Mark as Read",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                            if (!notification.targetId.isNullOrBlank()) {
                                TextButton(
                                    onClick = {
                                        onMarkRead()
                                        onNavigate()
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = "Open Resource",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun HomePreview() {
    NotesSharingTheme {
        HomeScreen(
            uiState = HomeUiState.Success(
                HomeContent(DummyData.categories.first(), DummyData.categories, DummyData.feedItems)
            ),
            isRefreshing = false,
            onRefresh = {},
            myFilesUiState = MyFilesUiState.Success(
                com.pravor.notessharing.state.MyFilesContent(emptyList(), DummyData.uploadedFiles)
            ),
            bookmarksCount = 5,
            activeDownloadsCount = 0,
            notifications = emptyList(),
            unreadNotificationsCount = 0,
            onMarkNotificationRead = {},
            onMarkAllNotificationsRead = {},
            onDeleteNotification = {},
            onClearAllNotifications = {},
            onUpvoteClick = {},
            onBookmarkClick = {},
            onMyUploadsClick = {},
            onMyBookmarksClick = {},
            onMyDownloadsClick = {},
            onViewAllLibraryClick = {},
            onSeeMoreClick = {},
            onDocumentClick = {},
            onVideoClick = {}
        )
    }
}
