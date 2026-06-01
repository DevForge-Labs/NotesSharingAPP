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

    private val bookmarkRepository = com.pravor.notessharing.bookmarks.BookmarkRepository()

    val uiState: StateFlow<TrendingNotesUiState> = combine(
        repository.trendingNotes,
        com.pravor.notessharing.bookmarks.BookmarkRepository.bookmarksFlow,
        repository.isRefreshing
    ) { notes, bookmarks, refreshing ->
        val bookmarkedIds = bookmarks.map { it.id }.toSet()
        val updatedNotes = notes.map { note ->
            note.copy(isBookmarked = bookmarkedIds.contains(note.id))
        }
        if (updatedNotes.isEmpty() && refreshing) {
            TrendingNotesUiState.Loading
        } else if (updatedNotes.isEmpty()) {
            TrendingNotesUiState.Empty
        } else {
            TrendingNotesUiState.Success(updatedNotes)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = if (repository.trendingNotes.value.isNotEmpty()) {
            val bookmarkedIds = com.pravor.notessharing.bookmarks.BookmarkRepository.bookmarksFlow.value.map { it.id }.toSet()
            val updated = repository.trendingNotes.value.map { note ->
                note.copy(isBookmarked = bookmarkedIds.contains(note.id))
            }
            TrendingNotesUiState.Success(updated)
        } else {
            TrendingNotesUiState.Loading
        }
    )

    init {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null) {
            viewModelScope.launch {
                bookmarkRepository.loadInitialBookmarksIfNeeded(currentUid)
            }
        }
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

    fun toggleBookmark(note: com.pravor.notessharing.model.TrendingNote) {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val wasBookmarked = com.pravor.notessharing.bookmarks.BookmarkRepository.bookmarksFlow.value.any { it.id == note.id }

        viewModelScope.launch {
            if (wasBookmarked) {
                bookmarkRepository.removeBookmark(note.id, currentUid)
            } else {
                val docType = note.documentType.ifBlank { note.type ?: "Notes" }
                val fileType = when (docType.lowercase(java.util.Locale.US)) {
                    "pyq" -> com.pravor.notessharing.model.FileType.Pyq
                    "cheat sheet" -> com.pravor.notessharing.model.FileType.CheatSheet
                    "assignment" -> com.pravor.notessharing.model.FileType.Notes
                    "video" -> com.pravor.notessharing.model.FileType.Video
                    else -> com.pravor.notessharing.model.FileType.Pdf
                }
                val studyFile = com.pravor.notessharing.model.StudyFile(
                    id = note.id,
                    title = note.title,
                    uploadDate = "Saved",
                    fileType = fileType,
                    downloads = note.downloads,
                    upvotes = note.upvotes,
                    thumbnailUrl = note.thumbnailUrl,
                    subject = note.subject,
                    documentType = docType
                )
                bookmarkRepository.addBookmark(studyFile, currentUid)
            }
        }
    }
}
