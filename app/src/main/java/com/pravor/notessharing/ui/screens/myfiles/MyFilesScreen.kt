package com.pravor.notessharing.ui.screens.myfiles

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.state.MyFilesContent
import com.pravor.notessharing.state.MyFilesUiState
import com.pravor.notessharing.ui.components.AdaptiveScrollbar
import com.pravor.notessharing.ui.components.SectionHeader
import com.pravor.notessharing.ui.components.StatePanel
import com.pravor.notessharing.ui.components.StudyFileCard
import com.pravor.notessharing.viewmodel.MyFilesViewModel

@Composable
fun MyFilesRoute(viewModel: MyFilesViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MyFilesScreen(uiState = uiState)
}

@Composable
fun MyFilesScreen(uiState: MyFilesUiState, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val repository = remember { com.pravor.notessharing.data.UploadRepository(context) }
    var selectedUploadForViewer by remember { mutableStateOf<com.pravor.notessharing.ui.components.UploadViewerData?>(null) }

    val listState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(targetState = uiState, label = "my-files-state", modifier = Modifier.fillMaxSize()) { state ->
            when (state) {
                MyFilesUiState.Loading -> StatePanel("Loading files", "Collecting your library", loading = true, modifier = Modifier.padding(top = 96.dp))
                MyFilesUiState.Empty -> StatePanel("No files yet", "Saved and uploaded files will live here", modifier = Modifier.padding(top = 96.dp))
                is MyFilesUiState.Error -> StatePanel("Files unavailable", state.message, modifier = Modifier.padding(top = 96.dp))
                is MyFilesUiState.Success -> MyFilesSuccessContent(
                    content = state.content,
                    listState = listState,
                    onDocumentClick = { docId ->
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
                    }
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
private fun MyFilesSuccessContent(
    content: MyFilesContent,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onDocumentClick: (String) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "my-files-title", contentType = "header") {
                Text(
                    text = "My Files",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
            item(key = "saved-title", contentType = "section") { SectionHeader("Saved files") }
            items(content.savedFiles, key = { it.id }, contentType = { "study-file" }) { file ->
                StudyFileCard(file, onClick = { onDocumentClick(file.id) })
            }
            item(key = "uploaded-title", contentType = "section") { SectionHeader("Uploaded files") }
            items(content.uploadedFiles, key = { it.id }, contentType = { "study-file" }) { file ->
                StudyFileCard(file, onClick = { onDocumentClick(file.id) })
            }
        }
        AdaptiveScrollbar(listState = listState)
    }
}
