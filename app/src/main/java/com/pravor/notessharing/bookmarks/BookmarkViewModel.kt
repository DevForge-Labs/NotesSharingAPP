package com.pravor.notessharing.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.model.StudyFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface BookmarkUiState {
    data object Loading : BookmarkUiState
    data object Empty : BookmarkUiState
    data class Error(val message: String) : BookmarkUiState
    data class Success(val bookmarks: List<StudyFile>) : BookmarkUiState
}

class BookmarkViewModel(
    private val repository: BookmarkRepository = BookmarkRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<BookmarkUiState>(BookmarkUiState.Loading)
    val uiState: StateFlow<BookmarkUiState> = _uiState.asStateFlow()

    init {
        // Reactively observe bookmarksFlow from repository
        viewModelScope.launch {
            BookmarkRepository.bookmarksFlow.collect { list ->
                _uiState.update {
                    if (list.isEmpty()) BookmarkUiState.Empty else BookmarkUiState.Success(list)
                }
            }
        }
        loadBookmarksForCurrentUser()
    }

    fun loadBookmarksForCurrentUser() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null) {
            loadBookmarks(currentUid)
        } else {
            _uiState.update { BookmarkUiState.Success(emptyList()) }
        }
    }

    fun loadBookmarks(userId: String) {
        viewModelScope.launch {
            // Only show Loading if we haven't loaded anything yet to keep optimistic updates seamless
            if (_uiState.value is BookmarkUiState.Loading) {
                _uiState.update { BookmarkUiState.Loading }
            }
            try {
                repository.getBookmarks(userId)
            } catch (e: Exception) {
                _uiState.update { BookmarkUiState.Error(e.message ?: "Failed to fetch bookmarks") }
            }
        }
    }
}
