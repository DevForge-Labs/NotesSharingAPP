package com.pratyush.notessharing.ui.screens.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pratyush.notessharing.model.Category
import com.pratyush.notessharing.state.HomeContent
import com.pratyush.notessharing.state.HomeUiState
import com.pratyush.notessharing.ui.components.AdaptiveScrollbar
import com.pratyush.notessharing.ui.components.CategoryChip
import com.pratyush.notessharing.ui.components.FeedCard
import com.pratyush.notessharing.ui.components.NotesSearchBar
import com.pratyush.notessharing.ui.components.SectionHeader
import com.pratyush.notessharing.ui.components.StatePanel
import com.pratyush.notessharing.ui.theme.NotesSharingTheme
import com.pratyush.notessharing.viewmodel.DummyData
import com.pratyush.notessharing.viewmodel.HomeViewModel

@Composable
fun HomeRoute(viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onCategoryClick = viewModel::selectCategory,
        onUpvoteClick = viewModel::toggleUpvote,
        onSaveClick = viewModel::toggleSaved
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onCategoryClick: (Category) -> Unit,
    onUpvoteClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val feedListState = rememberLazyListState()
    val stateKey = when (uiState) {
        HomeUiState.Loading -> "loading"
        HomeUiState.Empty -> "empty"
        is HomeUiState.Error -> "error"
        is HomeUiState.Success -> "success"
    }

    Crossfade(targetState = stateKey, label = "home-state", modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            HomeUiState.Loading -> StatePanel("Loading feed", "Preparing your study stream", loading = true, modifier = Modifier.padding(top = 96.dp))
            HomeUiState.Empty -> StatePanel("No notes yet", "Saved study resources will appear here", modifier = Modifier.padding(top = 96.dp))
            is HomeUiState.Error -> StatePanel("Something went wrong", state.message, modifier = Modifier.padding(top = 96.dp))
            is HomeUiState.Success -> HomeSuccessContent(
                content = state.content,
                onCategoryClick = onCategoryClick,
                onUpvoteClick = onUpvoteClick,
                onSaveClick = onSaveClick,
                listState = feedListState
            )
        }
    }
}

@Composable
private fun HomeSuccessContent(
    content: HomeContent,
    onCategoryClick: (Category) -> Unit,
    onUpvoteClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 14.dp),
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
            item(key = "home-search", contentType = "search") { NotesSearchBar("Search notes, subjects, PDFs...") }
            item(key = "home-categories", contentType = "categories") {
                val categoryListState = rememberLazyListState()
                Box {
                    LazyRow(
                        state = categoryListState,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(content.categories, key = { it.name }, contentType = { "category" }) { category ->
                            CategoryChip(
                                category = category,
                                selected = content.selectedCategory == category,
                                onClick = { onCategoryClick(category) }
                            )
                        }
                    }
                    AdaptiveScrollbar(
                        listState = categoryListState,
                        orientation = com.pratyush.notessharing.ui.components.ScrollbarOrientation.Horizontal
                    )
                }
            }
            item(key = "home-section", contentType = "section") {
                Spacer(Modifier.height(8.dp))
                SectionHeader("Fresh from your campus")
            }
            items(content.feedItems, key = { it.id }, contentType = { "feed-card" }) { feedItem ->
                FeedCard(
                    item = feedItem,
                    onUpvoteClick = { onUpvoteClick(feedItem.id) },
                    onSaveClick = { onSaveClick(feedItem.id) }
                )
            }
        }
        AdaptiveScrollbar(listState = listState)
    }
}

@Preview
@Composable
private fun HomePreview() {
    NotesSharingTheme {
        HomeScreen(
            uiState = HomeUiState.Success(
                HomeContent(Category.Notes, DummyData.categories, DummyData.feedItems)
            ),
            onCategoryClick = {},
            onUpvoteClick = {},
            onSaveClick = {}
        )
    }
}
