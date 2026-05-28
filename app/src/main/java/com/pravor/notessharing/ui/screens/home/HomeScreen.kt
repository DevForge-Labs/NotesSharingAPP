package com.pravor.notessharing.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.model.FeedItem
import com.pravor.notessharing.state.HomeContent
import com.pravor.notessharing.state.HomeUiState
import com.pravor.notessharing.state.MyFilesUiState
import com.pravor.notessharing.ui.components.AdaptiveScrollbar
import com.pravor.notessharing.ui.components.CompactStudyFileRow
import com.pravor.notessharing.ui.components.NotesSearchBar
import com.pravor.notessharing.ui.components.SectionHeader
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.ui.theme.NotesSharingTheme
import com.pravor.notessharing.viewmodel.DummyData
import com.pravor.notessharing.viewmodel.HomeViewModel
import com.pravor.notessharing.viewmodel.MyFilesViewModel

@Composable
fun HomeRoute(
    onViewAllLibraryClick: () -> Unit = {},
    onSeeMoreClick: () -> Unit = {},
    onDocumentClick: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
    myFilesViewModel: MyFilesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val myFilesUiState by myFilesViewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        myFilesUiState = myFilesUiState,
        onUpvoteClick = viewModel::toggleUpvote,
        onBookmarkClick = viewModel::toggleSaved,
        onViewAllLibraryClick = onViewAllLibraryClick,
        onSeeMoreClick = onSeeMoreClick,
        onDocumentClick = onDocumentClick
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
                        val feedItem = state.content.feedItems.find { it.id == docId }
                        val savedFile = (myFilesUiState as? MyFilesUiState.Success)?.content?.savedFiles?.find { it.id == docId }
                        val uploadedFile = (myFilesUiState as? MyFilesUiState.Success)?.content?.uploadedFiles?.find { it.id == docId }
                        val fileType = feedItem?.fileType ?: savedFile?.fileType ?: uploadedFile?.fileType
                        
                        if (fileType == com.pravor.notessharing.model.FileType.Video) {
                            coroutineScope.launch {
                                try {
                                    val (title, fileUrls) = repository.resolveFilesForDocument(docId)
                                    if (fileUrls.isNotEmpty()) {
                                        selectedUploadForViewer = com.pravor.notessharing.ui.components.UploadViewerData(title, fileUrls)
                                    }
                                } catch (e: Exception) {
                                    // Ignore
                                }
                            }
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
            item(key = "continue-title", contentType = "section") {
                Spacer(Modifier.height(4.dp))
                SectionHeader("Continue Reading")
            }
            item(key = "continue-card", contentType = "continue-reading") {
                ContinueReadingCard(
                    item = content.feedItems.firstOrNull(),
                    onClick = { content.feedItems.firstOrNull()?.let { onDocumentClick(it.id) } }
                )
            }
            item(key = "for-you-title", contentType = "section") {
                Spacer(Modifier.height(8.dp))
                SectionHeader("For You")
            }
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
    if (item == null) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Text(
                text = "Start exploring notes",
                modifier = Modifier.padding(18.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FilePresent,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Page 18/42 | Last opened 2h ago",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { 0.43f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "43% completed",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onClick, shape = RoundedCornerShape(18.dp)) {
                    Text("Continue Reading")
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
            myFilesUiState = MyFilesUiState.Success(
                com.pravor.notessharing.state.MyFilesContent(DummyData.savedFiles, DummyData.uploadedFiles)
            ),
            onUpvoteClick = {},
            onBookmarkClick = {},
            onViewAllLibraryClick = {},
            onSeeMoreClick = {},
            onDocumentClick = {}
        )
    }
}
