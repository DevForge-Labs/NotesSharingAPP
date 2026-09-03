package com.pravor.notessharing.ui.features.myfiles

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import com.pravor.notessharing.data.repository.BookmarkRepository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.domain.model.StudyFile
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
    private val repository: BookmarkRepository = BookmarkRepository(),
    private val profileRepository: com.pravor.notessharing.data.repository.ProfileRepository = com.pravor.notessharing.data.repository.ProfileRepository(),
    private val metadataRepository: com.pravor.notessharing.data.repository.MetadataRepository = com.pravor.notessharing.data.repository.MetadataRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<BookmarkUiState>(BookmarkUiState.Loading)
    val uiState: StateFlow<BookmarkUiState> = _uiState.asStateFlow()
    private var currentScope: com.pravor.notessharing.core.util.AcademicScope? = null

    init {
        // Reactively observe user profile and bookmarksFlow from repository
        viewModelScope.launch {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUid != null) {
                profileRepository.observeProfile(currentUid).collect { profile ->
                    currentScope = com.pravor.notessharing.core.util.AcademicScopeResolver.resolve(profile, metadataRepository)
                    applyFilteredBookmarks(BookmarkRepository.bookmarksFlow.value)
                }
            }
        }

        viewModelScope.launch {
            BookmarkRepository.bookmarksFlow.collect { list ->
                applyFilteredBookmarks(list)
            }
        }
        loadBookmarksForCurrentUser()
    }

    private fun applyFilteredBookmarks(list: List<StudyFile>) {
        val scope = currentScope
        val filtered = if (scope != null && scope.isCollegeValid) {
            list.filter { item ->
                scope.isDocumentPermitted(
                    docCollege = item.college,
                    docBranch = item.branch,
                    docSemester = item.semester,
                    docSubjectId = item.subjectId,
                    docSubjectName = item.subject
                )
            }
        } else {
            list
        }
        _uiState.update {
            if (filtered.isEmpty()) BookmarkUiState.Empty else BookmarkUiState.Success(filtered)
        }
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

    fun removeBookmark(documentId: String) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            repository.removeBookmark(documentId, currentUid)
        }
    }
}
