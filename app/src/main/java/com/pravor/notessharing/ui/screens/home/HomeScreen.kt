package com.pravor.notessharing.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import androidx.compose.material3.CircularProgressIndicator
import com.pravor.notessharing.model.FeedItem
import com.pravor.notessharing.state.HomeContent
import com.pravor.notessharing.state.HomeUiState
import com.pravor.notessharing.state.MyFilesUiState
import com.pravor.notessharing.ui.components.AdaptiveScrollbar
import com.pravor.notessharing.ui.components.CompactStudyFileRow
import com.pravor.notessharing.ui.components.NotesSearchBar
import com.pravor.notessharing.ui.components.SectionHeader
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.ui.components.DocumentPlaceholder
import com.pravor.notessharing.ui.components.VideoPlaceholder
import com.pravor.notessharing.ui.theme.NotesSharingTheme
import com.pravor.notessharing.viewmodel.DummyData
import com.pravor.notessharing.viewmodel.HomeViewModel
import com.pravor.notessharing.viewmodel.MyFilesViewModel

@Composable
fun HomeRoute(
    onViewAllLibraryClick: () -> Unit = {},
    onSeeMoreClick: () -> Unit = {},
    onDocumentClick: (String) -> Unit = {},
    onVideoClick: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
    myFilesViewModel: MyFilesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val myFilesUiState by myFilesViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshRecentlyOpened()
    }

    HomeScreen(
        uiState = uiState,
        myFilesUiState = myFilesUiState,
        onUpvoteClick = viewModel::toggleUpvote,
        onBookmarkClick = viewModel::toggleSaved,
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
    onUpvoteClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onViewAllLibraryClick: () -> Unit,
    onSeeMoreClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val repository = remember { com.pravor.notessharing.data.UploadRepository(context) }
    var selectedUploadForViewer by remember { mutableStateOf<com.pravor.notessharing.ui.components.UploadViewerData?>(null) }

    val feedListState = rememberLazyListState()
    val stateKey = when (uiState) {
        HomeUiState.Loading -> "loading"
        HomeUiState.Empty -> "empty"
        is HomeUiState.Error -> "error"
        is HomeUiState.Success -> "success"
    }

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(targetState = stateKey, label = "home-state", modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                HomeUiState.Loading -> StatePanel("Loading feed", "Preparing your study stream", loading = true, modifier = Modifier.padding(top = 96.dp))
                HomeUiState.Empty -> StatePanel("No notes yet", "Saved study resources will appear here", modifier = Modifier.padding(top = 96.dp))
                is HomeUiState.Error -> StatePanel("Something went wrong", state.message, modifier = Modifier.padding(top = 96.dp))
                is HomeUiState.Success -> HomeSuccessContent(
                    content = state.content,
                    myFilesUiState = myFilesUiState,
                    onUpvoteClick = onUpvoteClick,
                    onBookmarkClick = onBookmarkClick,
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
    }
}

@Composable
private fun HomeSuccessContent(
    content: HomeContent,
    myFilesUiState: MyFilesUiState,
    onUpvoteClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onViewAllLibraryClick: () -> Unit,
    onSeeMoreClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    val libraryFiles = when (myFilesUiState) {
        is MyFilesUiState.Success -> (myFilesUiState.content.savedFiles + myFilesUiState.content.uploadedFiles).take(5)
        else -> emptyList()
    }
    val visibleFeedItems = content.feedItems.take(4)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "home-title", contentType = "header") {
                Text(
                    text = "Study Social",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
            item(key = "home-search", contentType = "search") {
                NotesSearchBar("Search notes, PYQs, PDFs...")
            }
            if (content.recentlyOpened != null) {
                item(key = "continue-title", contentType = "section") {
                    Spacer(Modifier.height(4.dp))
                    SectionHeader("Continue Reading")
                }
                item(key = "continue-card", contentType = "continue-reading") {
                    ContinueReadingCard(
                        item = content.recentlyOpened,
                        onClick = { onDocumentClick(content.recentlyOpened.id) }
                    )
                }
            }
            item(key = "for-you-title", contentType = "section") {
                Spacer(Modifier.height(8.dp))
                SectionHeader("For You")
            }
            if (visibleFeedItems.isEmpty()) {
                item(key = "for-you-empty", contentType = "empty") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "No content available for your semester yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(visibleFeedItems, key = { it.id }, contentType = { "feed-card" }) { feedItem ->
                    HomeFeedCard(
                        item = feedItem,
                        onClick = { onDocumentClick(feedItem.id) },
                        onUpvoteClick = { onUpvoteClick(feedItem.id) },
                        onBookmarkClick = { onBookmarkClick(feedItem.id) }
                    )
                }
                if (content.feedItems.size > visibleFeedItems.size) {
                    item(key = "for-you-see-more", contentType = "action") {
                        TextButton(
                            onClick = onSeeMoreClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("See More")
                        }
                    }
                }
            }
            item(key = "library-title", contentType = "section") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("Your Library", modifier = Modifier.weight(1f))
                    TextButton(onClick = onViewAllLibraryClick) {
                        Text("View All")
                    }
                }
            }
            if (libraryFiles.isEmpty()) {
                item(key = "library-empty", contentType = "empty") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Text(
                            text = "Start exploring notes",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(libraryFiles, key = { "library-${it.id}" }, contentType = { "library-file" }) { file ->
                    CompactStudyFileRow(file = file, onClick = { onDocumentClick(file.id) })
                }
            }
        }
        AdaptiveScrollbar(listState = listState)
    }
}

@Composable
private fun ContinueReadingCard(
    item: FeedItem?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (item == null) return

    val isVideo = item.fileType == com.pravor.notessharing.model.FileType.Video
    val cardBgColor = if (isVideo) {
        Color(0xFF161E2E) // Premium deep navy tint for video content
    } else {
        Color(0xFF1A222B) // Premium dark steel/slate tint for non-video content
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (isVideo) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Compact Video Thumbnail
                    Box(
                        modifier = Modifier
                            .width(104.dp)
                            .height(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        var hasThumbnailError by remember { mutableStateOf(item.youtubeVideoId.isNullOrBlank()) }
                        if (!hasThumbnailError) {
                            AsyncImage(
                                model = "https://img.youtube.com/vi/${item.youtubeVideoId}/hqdefault.jpg",
                                contentDescription = item.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                onError = { hasThumbnailError = true }
                            )
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.45f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        } else {
                            VideoPlaceholder(modifier = Modifier.fillMaxSize())
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    // Right: Title, metadata, and button
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = formatRelativeTime(item.uploadDate, isVideo = true),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.height(6.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = onClick,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                )
                            ) {
                                Text(
                                    text = "Continue Watching",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            } else {
                val isAssignment = item.title.contains("assignment", ignoreCase = true) || 
                                   item.description.contains("assignment", ignoreCase = true)
                
                val previewIcon = when {
                    item.fileType == com.pravor.notessharing.model.FileType.Pyq -> Icons.Default.Help
                    isAssignment -> Icons.Default.Assignment
                    item.fileType == com.pravor.notessharing.model.FileType.CheatSheet -> Icons.Default.Bolt
                    item.fileType == com.pravor.notessharing.model.FileType.Notes -> Icons.Default.Description
                    else -> Icons.Default.FilePresent
                }
                
                val accentColor = when {
                    item.fileType == com.pravor.notessharing.model.FileType.Notes -> Color(0xFF90CAF9) // Blue tint
                    item.fileType == com.pravor.notessharing.model.FileType.Pyq -> Color(0xFFD1C4E9) // Purple tint
                    isAssignment -> Color(0xFFFFE082) // Amber tint
                    item.fileType == com.pravor.notessharing.model.FileType.CheatSheet -> Color(0xFFFFB74D) // Orange tint
                    else -> Color(0xFFEF9A9A) // Red tint
                }
                
                val badgeText = when {
                    item.fileType == com.pravor.notessharing.model.FileType.Pyq -> "PYQ"
                    isAssignment -> "ASSIGNMENT"
                    item.fileType == com.pravor.notessharing.model.FileType.CheatSheet -> "CHEAT SHEET"
                    item.fileType == com.pravor.notessharing.model.FileType.Notes -> "NOTES"
                    else -> "PDF"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Tinted Icon Container or Thumbnail
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        var hasImageLoaded by remember { mutableStateOf(false) }
                        var imageLoadError by remember { mutableStateOf(false) }
                        
                        if (!item.thumbnailUrl.isNullOrBlank() && !imageLoadError) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(item.thumbnailUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = item.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                onSuccess = { hasImageLoaded = true },
                                onError = { imageLoadError = true }
                            )
                        }
                        
                        // Show fallback or shimmer
                        if (item.thumbnailUrl.isNullOrBlank() || imageLoadError) {
                            DocumentPlaceholder(
                                documentType = badgeText,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (!hasImageLoaded) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    // Middle: Title & metadata
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = formatRelativeTime(item.uploadDate, isVideo = false),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // Right: Type Chip & Button
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = accentColor.copy(alpha = 0.12f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = badgeText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Button(
                            onClick = onClick,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            )
                        ) {
                            Text(
                                text = "Continue Reading",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatRelativeTime(timestampStr: String, isVideo: Boolean): String {
    val timestamp = timestampStr.toLongOrNull() ?: return "Recently viewed"
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val action = if (isVideo) "watched" else "opened"
    
    if (diff < 0) return "Opened just now"
    
    val minutes = diff / (1000 * 60)
    if (minutes < 1) return "Opened just now"
    if (minutes < 60) return "Opened $minutes min ago"
    
    val hours = minutes / 60
    if (hours < 24) return "Last $action ${hours}h ago"
    
    val days = hours / 24
    if (days == 1L) return "Last $action yesterday"
    if (days < 7) return "Last $action $days days ago"
    
    val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
    return "Last $action on ${sdf.format(java.util.Date(timestamp))}"
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
                com.pravor.notessharing.state.MyFilesContent(DummyData.savedFiles, DummyData.uploadedFiles)
            ),
            onUpvoteClick = {},
            onBookmarkClick = {},
            onViewAllLibraryClick = {},
            onSeeMoreClick = {},
            onDocumentClick = {},
            onVideoClick = {}
        )
    }
}
