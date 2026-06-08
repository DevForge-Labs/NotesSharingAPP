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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import kotlinx.coroutines.flow.distinctUntilChanged
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
    viewModel: HomeViewModel = viewModel(),
    myFilesViewModel: MyFilesViewModel = viewModel(),
    bookmarkViewModel: com.pravor.notessharing.bookmarks.BookmarkViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val myFilesUiState by myFilesViewModel.uiState.collectAsStateWithLifecycle()
    val bookmarkUiState by bookmarkViewModel.uiState.collectAsStateWithLifecycle()
    val activeDownloadsCount by com.pravor.notessharing.data.download.DownloadTracker.activeDownloadsCount.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshRecentlyOpened()
        // Refresh bookmarks count cleanly from bookmark layer
        bookmarkViewModel.loadBookmarksForCurrentUser()
        myFilesViewModel.loadDownloads(context)
    }

    val bookmarksCount = when (val state = bookmarkUiState) {
        is com.pravor.notessharing.bookmarks.BookmarkUiState.Success -> state.bookmarks.size
        else -> 0
    }

    HomeScreen(
        uiState = uiState,
        myFilesUiState = myFilesUiState,
        bookmarksCount = bookmarksCount,
        activeDownloadsCount = activeDownloadsCount,
        onUpvoteClick = viewModel::toggleUpvote,
        onBookmarkClick = viewModel::toggleSaved,
        onMyUploadsClick = onMyUploadsClick,
        onMyBookmarksClick = onMyBookmarksClick,
        onMyDownloadsClick = onMyDownloadsClick,
        onViewAllLibraryClick = onViewAllLibraryClick,
        onSeeMoreClick = onSeeMoreClick,
        onDocumentClick = onDocumentClick,
        onVideoClick = onVideoClick
    )
}

@SuppressLint("UnusedCrossfadeTargetStateParameter")
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    myFilesUiState: MyFilesUiState,
    bookmarksCount: Int,
    activeDownloadsCount: Int,
    onUpvoteClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onMyUploadsClick: () -> Unit,
    onMyBookmarksClick: () -> Unit,
    onMyDownloadsClick: () -> Unit,
    onViewAllLibraryClick: () -> Unit,
    onSeeMoreClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
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
 
    Box(modifier = modifier.fillMaxSize()) {
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
            myFilesUiState = MyFilesUiState.Success(
                com.pravor.notessharing.state.MyFilesContent(emptyList(), DummyData.uploadedFiles)
            ),
            bookmarksCount = 5,
            activeDownloadsCount = 0,
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
