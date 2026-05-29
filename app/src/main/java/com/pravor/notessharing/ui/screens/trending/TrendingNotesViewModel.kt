package com.pravor.notessharing.ui.screens.trending

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pravor.notessharing.data.TrendingFeedRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrendingNotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TrendingFeedRepository(application)

    val isRefreshing: StateFlow<Boolean> = repository.isRefreshing
    val isLoadingMore: StateFlow<Boolean> = repository.isLoadingMore

    val uiState: StateFlow<TrendingNotesUiState> = combine(
        repository.trendingNotes,
        repository.isRefreshing
    ) { notes, refreshing ->
        if (notes.isEmpty() && refreshing) {
            TrendingNotesUiState.Loading
        } else if (notes.isEmpty()) {
            TrendingNotesUiState.Empty
        } else {
            TrendingNotesUiState.Success(notes)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = if (repository.trendingNotes.value.isNotEmpty()) {
            TrendingNotesUiState.Success(repository.trendingNotes.value)
        } else {
            TrendingNotesUiState.Loading
        }
    )

    init {
        // Background refresh on start (Stale-While-Revalidate)
        viewModelScope.launch {
            repository.refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refresh()
        }
    }

    fun loadMore() {
        viewModelScope.launch {
            repository.loadMore()
        }
    }
}
