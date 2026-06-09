package com.pravor.notessharing.ui.screens.video

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pravor.notessharing.data.RecentlyOpenedRepository
import com.pravor.notessharing.model.VideoDetail
import com.pravor.notessharing.bookmarks.BookmarkRepository
import com.pravor.notessharing.model.FileType
import com.pravor.notessharing.model.StudyFile
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.ui.components.Avatar
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import com.pravor.notessharing.ui.theme.NotesSharingTheme
import com.pravor.notessharing.viewmodel.VideoDetailUiState
import com.pravor.notessharing.viewmodel.VideoDetailViewModel
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
            com.pravor.notessharing.data.ContinueLearningRepository(context).saveLastOpened(
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
            com.pravor.notessharing.widget.WidgetUpdateManager.updateAllWidgets(context)
        }
    }

    VideoDetailScreen(
        videoId = videoId,
        uiState = uiState,
        onBackClick = onBackClick,
        onNavigateToVideoDetail = onNavigateToVideoDetail
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(
    videoId: String,
    uiState: VideoDetailUiState,
    onBackClick: () -> Unit,
    onNavigateToVideoDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val bookmarks by BookmarkRepository.bookmarksFlow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showRemoveBookmarkDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            BookmarkRepository().loadInitialBookmarksIfNeeded(currentUid)
        }
    }

    val isBookmarked = remember(bookmarks, uiState) {
        val videoId = (uiState as? VideoDetailUiState.Success)?.video?.id ?: ""
        videoId.isNotEmpty() && bookmarks.any { it.id == videoId }
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
                                            downloads = 0,
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
                                val contRepo = com.pravor.notessharing.data.ContinueLearningRepository(context)
                                if (contRepo.getLastOpened()?.id == videoId) {
                                    contRepo.clearLastOpened()
                                }
                                com.pravor.notessharing.widget.WidgetUpdateManager.updateAllWidgets(context)
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
                            context = context
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
    }
}

@Composable
private fun VideoDetailContent(
    video: VideoDetail,
    contributorLevel: String,
    relatedVideos: List<VideoDetail>,
    onNavigateToVideoDetail: (String) -> Unit,
    context: Context
) {
    val bottomPadding = LocalBottomBarPadding.current
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
                youtubePlaylistId = video.youtubePlaylistId
            )
        }

        // 2. Video Information Card
        item(key = "info-section") {
            VideoInfoCard(video = video, contributorLevel = contributorLevel)
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
                LazyRow(
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
            }
        }
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
    youtubePlaylistId: String = ""
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
fun VideoInfoCard(video: VideoDetail, contributorLevel: String) {
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
