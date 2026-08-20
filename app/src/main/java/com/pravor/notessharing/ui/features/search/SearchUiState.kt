package com.pravor.notessharing.ui.features.search

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import androidx.compose.runtime.Immutable

@Immutable
data class SearchResultModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: String, // e.g. "Note", "User", "Subject"
    val additionalInfo: String = "",
    val thumbnailUrl: String = "",
    val sectionDisplay: String = "",
    val examYear: String = "",
    val examType: String = "",
    val branch: String = "",
    val semester: String = "",
    val college: String = "",
    val channelName: String = "",
    val playlistTitle: String = "",
    val subject: String = "",
    val documentType: String = ""
)

enum class SearchCategory(val displayName: String) {
    ALL("All"),
    NOTES("Notes"),
    USERS("Users"),
    SUBJECTS("Subjects")
}

enum class FilterOption(val displayName: String) {
    NOTES("Notes"),
    ASSIGNMENTS("Assignments"),
    VIDEOS("Videos"),
    CHEAT_SHEETS("Cheat Sheets"),
    PYQS("PYQs"),
    PLAYLISTS("Playlists")
}

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Results(val query: String, val results: List<SearchResultModel>) : SearchUiState
    object Empty : SearchUiState
    data class Error(val message: String) : SearchUiState
}

data class SearchScreenState(
    val query: String = "",
    val selectedCategory: SearchCategory = SearchCategory.ALL,
    val selectedFilters: Set<FilterOption> = emptySet(),
    val recentSearches: List<String> = emptyList(),
    val uiState: SearchUiState = SearchUiState.Idle
)
