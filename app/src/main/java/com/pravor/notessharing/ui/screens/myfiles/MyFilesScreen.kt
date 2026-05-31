package com.pravor.notessharing.ui.screens.myfiles

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.model.FileType
import com.pravor.notessharing.model.StudyFile
import com.pravor.notessharing.state.MyFilesUiState
import com.pravor.notessharing.ui.components.AdaptiveScrollbar
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.ui.components.StudyHubShelfCard
import com.pravor.notessharing.ui.navigation.LocalBottomBarPadding
import com.pravor.notessharing.viewmodel.MyFilesViewModel

@Composable
fun MyFilesRoute(
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    viewModel: MyFilesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MyFilesScreen(uiState = uiState, onDocumentClick = onDocumentClick, onVideoClick = onVideoClick)
}

@Composable
fun MyFilesScreen(
    uiState: MyFilesUiState,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(targetState = uiState, label = "my-files-state", modifier = Modifier.fillMaxSize()) { state ->
            when (state) {
                MyFilesUiState.Loading -> StatePanel("Loading downloads", "Collecting your downloaded study collection", loading = true, modifier = Modifier.padding(top = 96.dp))
                MyFilesUiState.Empty -> StatePanel("No downloads yet", "Your offline study collection will appear here", modifier = Modifier.padding(top = 96.dp))
                is MyFilesUiState.Error -> StatePanel("Downloads unavailable", state.message, modifier = Modifier.padding(top = 96.dp))
                is MyFilesUiState.Success -> MyFilesSuccessContent(
                    listState = listState,
                    onDocumentClick = { docId ->
                        // In mock downloads or real downloads, check fileType
                        if (docId.contains("video", ignoreCase = true)) {
                            onVideoClick(docId)
                        } else {
                            onDocumentClick(docId)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MyFilesSuccessContent(
    listState: androidx.compose.foundation.lazy.LazyListState,
    onDocumentClick: (String) -> Unit
) {
    val bottomPadding = LocalBottomBarPadding.current
    // Beautiful offline downloads mockup representing a premium downloadable study collection
    val downloadedFiles = remember {
        listOf(
            StudyFile("dl-os-scheduling", "OS CPU Scheduling Solved Examples", "Downloaded 2 days ago", FileType.StudyGuide, 203, 58, subject = "OS"),
            StudyFile("dl-dbms-sql", "DBMS SQL Queries Lab Sheet", "Downloaded 3 days ago", FileType.Notes, 146, 41, subject = "DBMS"),
            StudyFile("dl-cn-subnet", "CN TCP/IP Revision Sheet", "Downloaded 5 days ago", FileType.CheatSheet, 174, 46, subject = "CN"),
            StudyFile("dl-dsa-sorting", "Sorting Algorithms Lab Notes", "Downloaded 6 days ago", FileType.LabManual, 267, 73, subject = "DSA")
        )
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 14.dp + bottomPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "my-files-title", contentType = "header") {
                Text(
                    text = "My Files",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Your downloaded study collection",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(downloadedFiles, key = { it.id }, contentType = { "study-file" }) { file ->
                StudyHubShelfCard(file = file, onClick = { onDocumentClick(file.id) })
            }
        }
        AdaptiveScrollbar(listState = listState)
    }
}
