package com.pravor.notessharing.ui.screens.myfiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pravor.notessharing.ui.components.AdaptiveScrollbar
import com.pravor.notessharing.ui.components.StudyHubShelfCard
import com.pravor.notessharing.viewmodel.DummyData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookmarksScreen(
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    // Easy to replace with observed state or real saved files flow later
    val bookmarkedFiles = remember { DummyData.savedFiles }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Bookmarks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (bookmarkedFiles.isEmpty()) {
                com.pravor.notessharing.ui.components.StatePanel(
                    title = "No bookmarks yet",
                    message = "Saved study resources will appear here",
                    modifier = Modifier.padding(top = 96.dp)
                )
            } else {
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(bookmarkedFiles, key = { it.id }) { file ->
                            StudyHubShelfCard(
                                file = file,
                                onClick = {
                                    if (file.fileType == com.pravor.notessharing.model.FileType.Video) {
                                        onVideoClick(file.id)
                                    } else {
                                        onDocumentClick(file.id)
                                    }
                                }
                            )
                        }
                    }
                    AdaptiveScrollbar(listState = listState)
                }
            }
        }
    }
}
