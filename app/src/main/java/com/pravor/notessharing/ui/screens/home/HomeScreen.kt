package com.pravor.notessharing.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.state.HomeContent
import com.pravor.notessharing.state.HomeUiState
import com.pravor.notessharing.state.MyFilesUiState
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.ui.components.home_components.HomeSuccessContent
import com.pravor.notessharing.ui.theme.NotesSharingTheme
import com.pravor.notessharing.viewmodel.DummyData
import com.pravor.notessharing.viewmodel.HomeViewModel
import com.pravor.notessharing.viewmodel.MyFilesViewModel

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
