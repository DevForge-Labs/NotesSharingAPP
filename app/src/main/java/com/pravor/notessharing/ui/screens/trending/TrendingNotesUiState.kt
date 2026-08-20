package com.pravor.notessharing.ui.screens.trending

import androidx.compose.runtime.Immutable
import com.pravor.notessharing.domain.model.TrendingNote

@Immutable
sealed interface TrendingNotesUiState {
    object Loading : TrendingNotesUiState
    object Empty : TrendingNotesUiState
    data class Error(val message: String) : TrendingNotesUiState
    data class Success(val trendingNotes: List<TrendingNote>) : TrendingNotesUiState
}
