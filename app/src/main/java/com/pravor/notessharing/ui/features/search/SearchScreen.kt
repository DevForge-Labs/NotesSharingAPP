package com.pravor.notessharing.ui.features.search

import com.pravor.notessharing.ui.features.search.components.*

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*


import com.pravor.notessharing.ui.common.*

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pravor.notessharing.data.local.search.SearchHistoryManager
import kotlinx.coroutines.delay

@Composable
fun SearchRoute(
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val searchHistoryManager = remember { SearchHistoryManager(context) }
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(searchHistoryManager)
    )

    val state by searchViewModel.state.collectAsStateWithLifecycle()

    SearchScreen(
        state = state,
        onEvent = searchViewModel::onEvent,
        onBackClick = onBackClick,
        onDocumentClick = onDocumentClick,
        onVideoClick = onVideoClick,
        modifier = modifier
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    state: SearchScreenState,
    onEvent: (SearchEvent) -> Unit,
    onBackClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isKeyboardVisible = WindowInsets.isImeVisible

    var filterDropdownExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (isKeyboardVisible) {
            keyboardController?.hide()
            focusManager.clearFocus()
        } else {
            onBackClick()
        }
    }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
        delay(100)
        keyboardController?.show()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Background atmospheric illustration
            SearchAtmosphericBackground(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    SearchTextField(
                        value = state.query,
                        onValueChange = { onEvent(SearchEvent.QueryChanged(it)) },
                        placeholder = "Search notes, subjects, users...",
                        focusRequester = focusRequester,
                        onSearchAction = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onEvent(SearchEvent.SearchExecuted)
                        },
                        onFilterClick = { filterDropdownExpanded = true },
                        isFilterActive = state.selectedFilters.isNotEmpty()
                    )

                    SearchFilterDropdown(
                        expanded = filterDropdownExpanded,
                        onDismissRequest = { filterDropdownExpanded = false },
                        selectedFilters = state.selectedFilters,
                        onFilterOptionClick = { onEvent(SearchEvent.FilterOptionClicked(it)) },
                        onResetFilters = { onEvent(SearchEvent.ResetFilters) },
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }

                if (state.selectedFilters.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.Start)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onEvent(SearchEvent.ResetFilters) }
                            .background(Color(0xFF58D6D1).copy(alpha = 0.08f))
                            .border(
                                width = 0.5.dp,
                                color = Color(0xFF58D6D1).copy(alpha = 0.25f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Active filters count",
                            tint = Color(0xFF58D6D1),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Clear (${state.selectedFilters.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF58D6D1)
                        )
                    }
                }
            }

            if (state.query.isBlank()) {
                RecentSearchSection(
                    recentSearches = state.recentSearches,
                    onItemClick = { query ->
                        onEvent(SearchEvent.RecentSearchClicked(query))
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                    onClearAllClick = { onEvent(SearchEvent.ClearRecentHistory) }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (state.uiState) {
                        is SearchUiState.Idle -> {
                            // Future-ready suggestion/pre-search layout
                        }
                        is SearchUiState.Loading -> {
                            SearchLoadingState()
                        }
                        is SearchUiState.Empty -> {
                            SearchEmptyState()
                        }
                        is SearchUiState.Error -> {
                            SearchErrorState(
                                onRetryClick = { onEvent(SearchEvent.RetryClicked) }
                            )
                        }
                        is SearchUiState.Results -> {
                            val results = (state.uiState as SearchUiState.Results).results
                            if (results.isEmpty()) {
                                SearchEmptyState()
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    itemsIndexed(
                                        items = results,
                                        key = { _, it -> it.id }
                                    ) { index, result ->
                                        SearchResultCard(
                                            result = result,
                                            onClick = {
                                                com.pravor.notessharing.core.analytics.AnalyticsManager.logSearchResultClick(
                                                    searchTerm = state.query,
                                                    contentId = result.id,
                                                    contentType = result.type,
                                                    position = index
                                                )
                                                val isVideo = result.type.equals("Video", ignoreCase = true) ||
                                                        result.type.equals("Videos", ignoreCase = true) ||
                                                        result.type.equals("YouTube Resource", ignoreCase = true) ||
                                                        result.type.equals("youtube", ignoreCase = true)
                                                if (isVideo) {
                                                    onVideoClick(result.id)
                                                } else {
                                                    onDocumentClick(result.id)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
