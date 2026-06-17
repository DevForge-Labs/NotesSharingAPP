package com.pravor.notessharing.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
                        // Future auto-search trigger place:
                        // executeSearchFlow(query)
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
                    executeSearchFlow(_searchQuery.value)
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
                    executeSearchFlow(_searchQuery.value)
                }
            }
            SearchEvent.ResetFilters -> {
                _selectedFilters.value = emptySet()
                if (_searchQuery.value.isNotBlank()) {
                    executeSearchFlow(_searchQuery.value)
                }
            }
            SearchEvent.ClearRecentHistory -> {
                viewModelScope.launch {
                    searchHistoryManager.clearHistory()
                }
            }
            is SearchEvent.RecentSearchClicked -> {
                _searchQuery.value = event.query
                executeSearchFlow(event.query)
            }
            SearchEvent.SearchExecuted -> {
                executeSearchFlow(_searchQuery.value)
            }
            SearchEvent.RetryClicked -> {
                executeSearchFlow(_searchQuery.value)
            }
        }
    }

    private fun executeSearchFlow(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = SearchUiState.Idle
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            // Save to recent search history
            searchHistoryManager.addSearchQuery(trimmed)

            _uiState.value = SearchUiState.Loading

            // Simulate slight delay to allow verification of loading skeletons
            delay(600)

            // Default to empty state when no search backend is integrated yet
            _uiState.value = SearchUiState.Empty
        }
    }
}

class SearchViewModelFactory(
    private val searchHistoryManager: SearchHistoryManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(searchHistoryManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
