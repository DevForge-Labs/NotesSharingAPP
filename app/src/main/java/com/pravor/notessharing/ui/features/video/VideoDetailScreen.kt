package com.pravor.notessharing.ui.features.video

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.pravor.notessharing.ui.features.explore.components.RunningSquirrelScrollbar
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pravor.notessharing.data.repository.RecentlyOpenedRepository
import com.pravor.notessharing.domain.model.VideoDetail
import com.pravor.notessharing.data.repository.BookmarkRepository
import com.pravor.notessharing.domain.model.FileType
import com.pravor.notessharing.domain.model.StudyFile
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.ui.common.Avatar
import com.pravor.notessharing.ui.common.StatePanel
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import com.pravor.notessharing.ui.common.ReportBottomSheet
import androidx.compose.material.icons.outlined.Flag
import com.pravor.notessharing.data.repository.ReportRepository
import com.pravor.notessharing.ui.navigation.LocalSnackbarHostState
import com.pravor.notessharing.ui.theme.NotesSharingTheme
import kotlinx.coroutines.launch

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
                youtubeThumbnailUrl = video.youtubeThumbnailUrl
            )
            com.pravor.notessharing.data.repository.ContinueLearningRepository(context).saveLastOpened(
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
                youtubeThumbnailUrl = video.youtubeThumbnailUrl
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
        val videoId = (uiState as? VideoDetailUiState.Success)?.video?.id ?: ""
        videoId.isNotEmpty() && bookmarks.any { it.id == videoId }
    }

    val handleUpvoteClick = remember(onUpvoteClick) {
        { itemId: String ->
            val wasUpvoted = com.pravor.notessharing.data.repository.UpvoteRepository.upvotesFlow.value[itemId] ?: false
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
                                val contRepo = com.pravor.notessharing.data.repository.ContinueLearningRepository(context)
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

@OptIn(ExperimentalMaterial3Api::class)
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
        // 1. YouTube Player / Thumbnail section
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

        // 2. Video Information Card
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

        // 3. Description if present
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

        // 4. Related Videos section
        android.util.Log.d("REC_TRACE", "[VIDEO_UI] Composable render check: relatedVideosCount=${relatedVideos.size}")
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
        ReportBottomSheet(
            resourceId = video.id,
            resourceType = video.collection,
            resourceTitle = video.title,
            resourceThumbnail = video.thumbnailUrl ?: video.youtubeThumbnailUrl,
            uploaderUid = video.uploaderId,
            uploaderName = video.uploaderName,
            onDismissRequest = { showReportBottomSheet = false }
        )
    }
}

@Composable
fun YouTubeThumbnailPlayer(
    youtubeVideoId: String,
    youtubeUrl: String,
    title: String,
    context: Context,
    thumbnailUrl: String? = null,
    youtubeThumbnailUrl: String? = null,
    youtubeResourceType: String = "video",
    youtubePlaylistId: String = "",
    onPlayClick: () -> Unit
) {
    val finalImageUrl = if (!thumbnailUrl.isNullOrBlank()) {
        thumbnailUrl
    } else if (!youtubeThumbnailUrl.isNullOrBlank()) {
        youtubeThumbnailUrl
    } else {
        null
    }

    var hasError by remember { mutableStateOf(finalImageUrl.isNullOrBlank()) }

    LaunchedEffect(finalImageUrl) {
        hasError = finalImageUrl.isNullOrBlank()
    }

    if (hasError) {
        VideoFallbackUI(title = title)
    } else {
        // Embed the play icon over the thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black)
                .clickable {
                    onPlayClick()
                    launchYouTubeIntent(
                        resourceType = youtubeResourceType,
                        videoId = youtubeVideoId,
                        playlistId = youtubePlaylistId,
                        youtubeUrl = youtubeUrl,
                        context = context
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = finalImageUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = {
                    hasError = true
                }
            )

            // Glassmorphic translucent play button overlay
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VideoFallbackUI(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Video unavailable",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun launchYouTubeIntent(
    resourceType: String,
    videoId: String,
    playlistId: String,
    youtubeUrl: String,
    context: Context
) {
    val finalLaunchUrl = if (resourceType == "playlist") {
        if (playlistId.isNotBlank()) "https://www.youtube.com/playlist?list=$playlistId" else youtubeUrl
    } else {
        if (youtubeUrl.isNotBlank()) youtubeUrl else "https://www.youtube.com/watch?v=$videoId"
    }

    android.util.Log.d(
        "YouTubeLaunch",
        "Resource Type: $resourceType\nPlaylist Id: $playlistId\nVideo Id: $videoId\nFinal Launch URL: $finalLaunchUrl"
    )

    if (resourceType == "playlist") {
        val appIntent = Intent(Intent.ACTION_VIEW, finalLaunchUrl.toUri()).apply {
            setPackage("com.google.android.youtube")
        }
        val webIntent = Intent(Intent.ACTION_VIEW, finalLaunchUrl.toUri())

        try {
            context.startActivity(appIntent)
        } catch (e: Exception) {
            try {
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "No app available to open this playlist link", Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        val appUri = "vnd.youtube:$videoId".toUri()
        val webUri = finalLaunchUrl.toUri()
        val appIntent = Intent(Intent.ACTION_VIEW, appUri)
        val webIntent = Intent(Intent.ACTION_VIEW, webUri)

        try {
            context.startActivity(appIntent)
        } catch (e: Exception) {
            try {
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "No app available to open this video link", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun VideoInfoCard(
    video: VideoDetail,
    contributorLevel: String,
    currentUid: String,
    onUpvoteClick: (String) -> Unit,
    onShowRemoveUpvoteDialog: () -> Unit,
    onShareClick: () -> Unit,
    shareEnabled: Boolean,
    onReportClick: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Type Badge and Subject tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "VIDEO",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                val reportedMap by ReportRepository.instance.reportedFlow.collectAsStateWithLifecycle()
                val isReported = remember(reportedMap, video.id) {
                    reportedMap[video.id] == true
                }

                IconButton(
                    onClick = {
                        if (isReported && currentUid.isNotEmpty()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("You've already reported this resource.")
                            }
                        } else {
                            onReportClick()
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isReported) Icons.Filled.Flag else Icons.Outlined.Flag,
                        contentDescription = if (isReported) "Already Reported" else "Report Video",
                        tint = if (isReported) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title and Metadata
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${video.subject} | ${video.semester}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Upvote & Share action row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    VideoUpvoteButtonSection(
                        videoId = video.id,
                        initialUpvotes = video.upvotes,
                        currentUid = currentUid,
                        onUpvoteClick = onUpvoteClick,
                        onShowRemoveDialog = onShowRemoveUpvoteDialog
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .width(2.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    VideoShareButtonSection(
                        onClick = onShareClick,
                        enabled = shareEnabled
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // Uploader details section
            Row(verticalAlignment = Alignment.CenterVertically) {
                val initials = if (video.uploaderName.isNotBlank()) {
                    video.uploaderName.split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .map { it.first().uppercase() }
                        .joinToString("")
                        .ifBlank { "UN" }
                } else {
                    "UN"
                }

                Avatar(text = initials, modifier = Modifier.size(42.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Uploaded by: ${video.uploaderName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    val badgeColor = when (contributorLevel) {
                        "Gold Contributor" -> Color(0xFFFFD700)
                        "Silver Contributor" -> Color(0xFFC0C0C0)
                        "Bronze Contributor" -> Color(0xFFCD7F32)
                        "Platinum Contributor" -> Color(0xFF00E5FF)
                        else -> Color(0xFFD500F9)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = contributorLevel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RelatedVideoCard(video: VideoDetail, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val finalImageUrl = if (!video.thumbnailUrl.isNullOrBlank()) {
                    video.thumbnailUrl
                } else if (!video.youtubeThumbnailUrl.isNullOrBlank()) {
                    video.youtubeThumbnailUrl
                } else {
                    null
                }
                if (!finalImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = finalImageUrl,
                        contentDescription = video.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.uploaderName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun shareVideo(context: Context, videoUrl: String, videoTitle: String) {
    try {
        val shareText = "Check out this lecture on NoteShare!\n\n$videoUrl"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, videoTitle)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(intent, "Share Video")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share video", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun VideoUpvoteButtonSection(
    videoId: String,
    initialUpvotes: Int,
    currentUid: String,
    onUpvoteClick: (String) -> Unit,
    onShowRemoveDialog: () -> Unit,
    enabled: Boolean = true
) {
    val upvotesMap by com.pravor.notessharing.data.repository.UpvoteRepository.upvotesFlow.collectAsStateWithLifecycle()
    val upvoteCountsMap by com.pravor.notessharing.data.repository.UpvoteRepository.upvoteCountsFlow.collectAsStateWithLifecycle()

    val isUpvoted = remember(upvotesMap, videoId) {
        upvotesMap[videoId] == true
    }
    val upvoteCount = remember(upvoteCountsMap, videoId) {
        upvoteCountsMap[videoId] ?: initialUpvotes
    }

    val context = LocalContext.current
    val upvoteColor = if (!enabled) {
        Color(0xFF94A3B8)
    } else if (isUpvoted) {
        Color(0xFFFFB74D)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) {
                if (currentUid.isEmpty()) {
                    Toast.makeText(context, "Please sign in to upvote", Toast.LENGTH_SHORT).show()
                    return@clickable
                }
                if (isUpvoted) {
                    onShowRemoveDialog()
                } else {
                    onUpvoteClick(videoId)
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ThumbUp,
                contentDescription = "Upvote",
                tint = upvoteColor,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "$upvoteCount",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = upvoteColor
                )
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isUpvoted) "Upvoted" else "Upvote",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = upvoteColor
            )
        )
    }
}

@Composable
fun VideoShareButtonSection(
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val shareColor = if (!enabled) {
        Color(0xFF94A3B8)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) {
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.height(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share",
                tint = shareColor,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Share",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = shareColor
            )
        )
    }
}
