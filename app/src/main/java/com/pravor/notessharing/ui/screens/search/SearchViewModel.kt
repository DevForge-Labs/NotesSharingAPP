package com.pravor.notessharing.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pravor.notessharing.data.SearchRepository
import com.pravor.notessharing.data.local.search.SearchHistoryManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val searchHistoryManager: SearchHistoryManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow(SearchCategory.ALL)
    private val _selectedFilters = MutableStateFlow<Set<FilterOption>>(emptySet())
    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)

    private val _state = MutableStateFlow(SearchScreenState())
    val state: StateFlow<SearchScreenState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Combine states to expose a single unified screen state
        viewModelScope.launch {
            combine(
                _searchQuery,
                _selectedCategory,
                _selectedFilters,
                _uiState,
                searchHistoryManager.historyFlow
            ) { query, category, filters, uiState, history ->
                SearchScreenState(
                    query = query,
                    selectedCategory = category,
                    selectedFilters = filters,
                    recentSearches = history,
                    uiState = uiState
                )
            }.collect { newState ->
                _state.value = newState
            }
        }

        // Debounce readiness for auto-search integration in the future
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            _searchQuery
                .map { it.trim() }
                .distinctUntilChanged()
                .debounce(500)
                .collect { query ->
                    if (query.isNotEmpty()) {
                        executeSearchFlow(query, saveToHistory = false)
                    }
                }
        }
    }

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChanged -> {
                _searchQuery.value = event.query
                if (event.query.isEmpty()) {
                    searchJob?.cancel()
                    _uiState.value = SearchUiState.Idle
                }
            }
            is SearchEvent.CategorySelected -> {
                _selectedCategory.value = event.category
                if (_searchQuery.value.isNotBlank()) {
                    executeSearchFlow(_searchQuery.value, saveToHistory = false)
                }
            }
            is SearchEvent.FilterOptionClicked -> {
                _selectedFilters.update { current ->
                    if (current.contains(event.option)) {
                        current - event.option
                    } else {
                        current + event.option
                    }
                }
                if (_searchQuery.value.isNotBlank()) {
                    executeSearchFlow(_searchQuery.value, saveToHistory = false)
                }
            }
            SearchEvent.ResetFilters -> {
                _selectedFilters.value = emptySet()
                if (_searchQuery.value.isNotBlank()) {
                    executeSearchFlow(_searchQuery.value, saveToHistory = false)
                }
            }
            SearchEvent.ClearRecentHistory -> {
                viewModelScope.launch {
                    searchHistoryManager.clearHistory()
                }
            }
            is SearchEvent.RecentSearchClicked -> {
                _searchQuery.value = event.query
                executeSearchFlow(event.query, saveToHistory = true)
            }
            SearchEvent.SearchExecuted -> {
                executeSearchFlow(_searchQuery.value, saveToHistory = true)
            }
            SearchEvent.RetryClicked -> {
                executeSearchFlow(_searchQuery.value, saveToHistory = false)
            }
        }
    }

    private fun executeSearchFlow(query: String, saveToHistory: Boolean = false) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = SearchUiState.Idle
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (saveToHistory) {
                searchHistoryManager.addSearchQuery(trimmed)
            }

            _uiState.value = SearchUiState.Loading

            try {
                val docTypes = _selectedFilters.value.flatMap { option ->
                    when (option) {
                        FilterOption.NOTES -> listOf("Notes")
                        FilterOption.ASSIGNMENTS -> listOf("Assignment")
                        FilterOption.VIDEOS -> listOf("Video")
                        FilterOption.CHEAT_SHEETS -> listOf("CheatSheet", "Cheat Sheet")
                        FilterOption.PYQS -> listOf("PYQ")
                        FilterOption.PLAYLISTS -> listOf("Playlist")
                    }
                }.toSet()

                val results = searchRepository.search(trimmed, docTypes)
                if (results.isEmpty()) {
                    _uiState.value = SearchUiState.Empty
                } else {
                    _uiState.value = SearchUiState.Results(query = trimmed, results = results)
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _uiState.value = SearchUiState.Error(e.message ?: "Unknown error occurred")
                }
            }
        }
    }
}

class SearchViewModelFactory(
    private val searchHistoryManager: SearchHistoryManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val searchRepository = SearchRepository()
            return SearchViewModel(searchRepository, searchHistoryManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
