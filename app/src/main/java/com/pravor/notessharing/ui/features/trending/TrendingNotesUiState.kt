package com.pravor.notessharing.ui.features.trending

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import androidx.compose.runtime.Immutable
import com.pravor.notessharing.domain.model.TrendingNote

@Immutable
sealed interface TrendingNotesUiState {
    object Loading : TrendingNotesUiState
    object Empty : TrendingNotesUiState
    data class Error(val message: String) : TrendingNotesUiState
    data class Success(val trendingNotes: List<TrendingNote>) : TrendingNotesUiState
}
