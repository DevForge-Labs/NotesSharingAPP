@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.pravor.notessharing.ui.features.video

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.data.repository.BookmarkRepository
import com.pravor.notessharing.data.repository.ContinueLearningRepository
import com.pravor.notessharing.data.repository.RecentlyOpenedRepository
import com.pravor.notessharing.data.repository.UpvoteRepository
import com.pravor.notessharing.domain.model.FileType
import com.pravor.notessharing.domain.model.StudyFile
import com.pravor.notessharing.domain.model.VideoDetail
import com.pravor.notessharing.ui.common.ReportBottomSheet
import com.pravor.notessharing.ui.common.components.StatePanel
import com.pravor.notessharing.ui.features.explore.components.RunningSquirrelScrollbar
import com.pravor.notessharing.ui.features.video.components.RelatedVideoCard
import com.pravor.notessharing.ui.features.video.components.VideoInfoCard
import com.pravor.notessharing.ui.features.video.components.YouTubeThumbnailPlayer
import com.pravor.notessharing.ui.features.video.components.shareVideo
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailRoute(
    videoId: String,
    onBackClick: () -> Unit,
    onNavigateToVideoDetail: (String) -> Unit,
    viewModel: VideoDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(videoId) {
        viewModel.loadVideoDetail(videoId)
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is VideoDetailUiState.Success) {
            val video = state.video
            RecentlyOpenedRepository(context).saveLastOpened(
                id = video.id,
                type = "video",
                title = video.title,
                subject = video.subject,
                youtubeVideoId = video.youtubeVideoId,
                uploaderName = video.uploaderName,
                thumbnailUrl = video.thumbnailUrl,
                thumbnailGenerated = null,
                thumbnailType = null,
                documentType = "YouTube Resource",
                youtubeThumbnailUrl = video.youtubeThumbnailUrl,
                college = video.college,
                branch = video.branch,
                semester = video.semester
            )
            ContinueLearningRepository(context).saveLastOpened(
                id = video.id,
                type = "video",
                title = video.title,
                subject = video.subject,
                youtubeVideoId = video.youtubeVideoId,
                uploaderName = video.uploaderName,
                thumbnailUrl = video.thumbnailUrl,
                thumbnailGenerated = null,
                thumbnailType = null,
                documentType = "YouTube Resource",
                youtubeThumbnailUrl = video.youtubeThumbnailUrl,
                college = video.college,
                branch = video.branch,
                semester = video.semester
            )
            com.pravor.notessharing.core.widget.WidgetUpdateManager.updateAllWidgets(context)
        }
    }

    VideoDetailScreen(
        videoId = videoId,
        uiState = uiState,
        onBackClick = onBackClick,
        onNavigateToVideoDetail = onNavigateToVideoDetail,
        onPlayClick = { video ->
            viewModel.incrementVideoViews(video.id, video.collection, "Video")
        },
        onUpvoteClick = viewModel::toggleUpvote
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(
    videoId: String,
    uiState: VideoDetailUiState,
    onBackClick: () -> Unit,
    onNavigateToVideoDetail: (String) -> Unit,
    onPlayClick: (VideoDetail) -> Unit,
    onUpvoteClick: (String) -> Unit
) {
    val context = LocalContext.current
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val bookmarks by BookmarkRepository.bookmarksFlow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showRemoveBookmarkDialog by remember { mutableStateOf(false) }
    var pendingRemoveUpvoteId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            BookmarkRepository().loadInitialBookmarksIfNeeded(currentUid)
        }
    }

    val isBookmarked = remember(bookmarks, uiState) {
        val id = (uiState as? VideoDetailUiState.Success)?.video?.id ?: ""
        id.isNotEmpty() && bookmarks.any { it.id == id }
    }

    val handleUpvoteClick = remember(onUpvoteClick) {
        { itemId: String ->
            val wasUpvoted = UpvoteRepository.upvotesFlow.value[itemId] ?: false
            if (wasUpvoted) {
                pendingRemoveUpvoteId = itemId
            } else {
                onUpvoteClick(itemId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = when (uiState) {
                        is VideoDetailUiState.Success -> uiState.video.title
                        else -> "Video Detail"
                    }
                    Text(
                        text = titleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (currentUid.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "Please sign in to bookmark videos",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@IconButton
                            }
                            val video = (uiState as? VideoDetailUiState.Success)?.video
                            if (video != null) {
                                if (isBookmarked) {
                                    showRemoveBookmarkDialog = true
                                } else {
                                    scope.launch {
                                        val bookmarkRepository = BookmarkRepository()
                                        val studyFile = StudyFile(
                                            id = video.id,
                                            title = video.title,
                                            uploadDate = "Saved",
                                            fileType = FileType.Video,
                                            downloadsCount = 0,
                                            upvotes = video.upvotes,
                                            thumbnailUrl = video.thumbnailUrl
                                                ?: video.youtubeThumbnailUrl,
                                            subject = video.subject,
                                            documentType = "YouTube Resource"
                                        )
                                        bookmarkRepository.addBookmark(studyFile, currentUid)
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Crossfade(targetState = uiState, label = "video-detail-fade") { state ->
                when (state) {
                    VideoDetailUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is VideoDetailUiState.Error -> {
                        val isDeleted = state.message.contains("not found", ignoreCase = true)
                        if (isDeleted) {
                            LaunchedEffect(videoId) {
                                val recentRepo = RecentlyOpenedRepository(context)
                                if (recentRepo.getLastOpened()?.id == videoId) {
                                    recentRepo.clearLastOpened()
                                }
                                val contRepo = ContinueLearningRepository(context)
                                if (contRepo.getLastOpened()?.id == videoId) {
                                    contRepo.clearLastOpened()
                                }
                                com.pravor.notessharing.core.widget.WidgetUpdateManager.updateAllWidgets(context)
                            }
                        }
                        StatePanel(
                            title = if (isDeleted) "Not Available" else "Load Failed",
                            message = if (isDeleted) "This resource is no longer available." else state.message
                        )
                    }

                    is VideoDetailUiState.Success -> {
                        VideoDetailContent(
                            video = state.video,
                            contributorLevel = state.contributorLevel,
                            relatedVideos = state.relatedVideos,
                            onNavigateToVideoDetail = onNavigateToVideoDetail,
                            context = context,
                            onPlayClick = onPlayClick,
                            currentUid = currentUid,
                            onUpvoteClick = handleUpvoteClick,
                            onShowRemoveUpvoteDialog = {
                                pendingRemoveUpvoteId = state.video.id
                            }
                        )
                    }
                }
            }
        }

        if (showRemoveBookmarkDialog) {
            val video = (uiState as? VideoDetailUiState.Success)?.video
            AlertDialog(
                onDismissRequest = { showRemoveBookmarkDialog = false },
                title = { Text(text = "Remove this bookmark?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (video != null && currentUid.isNotEmpty()) {
                                scope.launch {
                                    BookmarkRepository().removeBookmark(video.id, currentUid)
                                }
                            }
                            showRemoveBookmarkDialog = false
                        }
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showRemoveBookmarkDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (pendingRemoveUpvoteId != null) {
            AlertDialog(
                onDismissRequest = { pendingRemoveUpvoteId = null },
                title = { Text(text = "Remove your upvote?") },
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
                        Text("Remove Upvote")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { pendingRemoveUpvoteId = null }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun VideoDetailContent(
    video: VideoDetail,
    contributorLevel: String,
    relatedVideos: List<VideoDetail>,
    onNavigateToVideoDetail: (String) -> Unit,
    context: Context,
    onPlayClick: (VideoDetail) -> Unit,
    currentUid: String,
    onUpvoteClick: (String) -> Unit,
    onShowRemoveUpvoteDialog: () -> Unit
) {
    val bottomPadding = LocalBottomBarPadding.current
    var showReportBottomSheet by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "player-section") {
            YouTubeThumbnailPlayer(
                youtubeVideoId = video.youtubeVideoId,
                youtubeUrl = video.youtubeUrl,
                title = video.title,
                context = context,
                thumbnailUrl = video.thumbnailUrl,
                youtubeThumbnailUrl = video.youtubeThumbnailUrl,
                youtubeResourceType = video.youtubeResourceType,
                youtubePlaylistId = video.youtubePlaylistId,
                onPlayClick = { onPlayClick(video) }
            )
        }

        item(key = "info-section") {
            VideoInfoCard(
                video = video,
                contributorLevel = contributorLevel,
                currentUid = currentUid,
                onUpvoteClick = onUpvoteClick,
                onShowRemoveUpvoteDialog = onShowRemoveUpvoteDialog,
                onShareClick = {
                    shareVideo(context, video.youtubeUrl, video.title)
                },
                shareEnabled = video.youtubeUrl.isNotBlank(),
                onReportClick = {
                    if (currentUid.isEmpty()) {
                        Toast.makeText(context, "Please sign in to report resources", Toast.LENGTH_SHORT).show()
                    } else {
                        showReportBottomSheet = true
                    }
                }
            )
        }

        if (video.description.isNotBlank()) {
            item(key = "description-section") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = video.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (relatedVideos.isNotEmpty()) {
            item(key = "related-title") {
                Text(
                    text = "Related Videos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            item(key = "related-carousel") {
                val listState = rememberLazyListState()
                Box(modifier = Modifier.fillMaxWidth()) {
                    LazyRow(
                        state = listState,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(relatedVideos, key = { it.id }) { relatedVideo ->
                            RelatedVideoCard(
                                video = relatedVideo,
                                onClick = { onNavigateToVideoDetail(relatedVideo.id) }
                            )
                        }
                    }
                    RunningSquirrelScrollbar(listState = listState)
                }
            }
        }
    }

    if (showReportBottomSheet) {
        val reportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ReportBottomSheet(
            resourceId = video.id,
            resourceType = video.collection,
            resourceTitle = video.title,
            resourceThumbnail = video.thumbnailUrl ?: video.youtubeThumbnailUrl,
            uploaderUid = video.uploaderId,
            uploaderName = video.uploaderName,
            onDismissRequest = { showReportBottomSheet = false },
            sheetState = reportSheetState
        )
    }
}
