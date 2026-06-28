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
    private val upvoteRepository = com.pravor.notessharing.upvotes.UpvoteRepository()

    private val upvoteState = combine(
        com.pravor.notessharing.upvotes.UpvoteRepository.upvotesFlow,
        com.pravor.notessharing.upvotes.UpvoteRepository.upvoteCountsFlow
    ) { upvotesMap, upvoteCountsMap ->
        Pair(upvotesMap, upvoteCountsMap)
    }

    val uiState: StateFlow<TrendingNotesUiState> = combine(
        repository.trendingNotes,
        com.pravor.notessharing.bookmarks.BookmarkRepository.bookmarksFlow,
        upvoteState,
        com.pravor.notessharing.upvotes.UpvoteRepository.downloadCountsFlow,
        repository.isRefreshing
    ) { notes, bookmarks, upvoteStatePair, downloadCountsMap, refreshing ->
        val (upvotesMap, upvoteCountsMap) = upvoteStatePair
        val bookmarkedIds = bookmarks.map { it.id }.toSet()
        val updatedNotes = notes.map { note ->
            val isUpvoted = upvotesMap[note.id] ?: false
            val upvotesCount = upvoteCountsMap[note.id] ?: note.upvotes
            val downloadsCount = downloadCountsMap[note.id] ?: note.downloadsCount
            note.copy(
                isBookmarked = bookmarkedIds.contains(note.id),
                isUpvoted = isUpvoted,
                upvotes = upvotesCount,
                downloadsCount = downloadsCount
            )
        }
        val postFilteredNotes = updatedNotes.filter { it.isTrendingNote() }
        android.util.Log.d("DEBUG_TRENDING", "Flow emission - Repository Count: ${notes.size}, Post-Filter Count: ${postFilteredNotes.size}")
        if (postFilteredNotes.isEmpty() && refreshing) {
            TrendingNotesUiState.Loading
        } else if (postFilteredNotes.isEmpty()) {
            TrendingNotesUiState.Empty
        } else {
            TrendingNotesUiState.Success(postFilteredNotes)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = if (repository.trendingNotes.value.isNotEmpty()) {
            val bookmarkedIds = com.pravor.notessharing.bookmarks.BookmarkRepository.bookmarksFlow.value.map { it.id }.toSet()
            val upvotesMap = com.pravor.notessharing.upvotes.UpvoteRepository.upvotesFlow.value
            val upvoteCountsMap = com.pravor.notessharing.upvotes.UpvoteRepository.upvoteCountsFlow.value
            val downloadCountsMap = com.pravor.notessharing.upvotes.UpvoteRepository.downloadCountsFlow.value
            val updated = repository.trendingNotes.value.map { note ->
                val isUpvoted = upvotesMap[note.id] ?: false
                val upvotesCount = upvoteCountsMap[note.id] ?: note.upvotes
                val downloadsCount = downloadCountsMap[note.id] ?: note.downloadsCount
                note.copy(
                    isBookmarked = bookmarkedIds.contains(note.id),
                    isUpvoted = isUpvoted,
                    upvotes = upvotesCount,
                    downloadsCount = downloadsCount
                )
            }
            val postFilteredNotes = updated.filter { it.isTrendingNote() }
            android.util.Log.d("DEBUG_TRENDING", "Initial value - Repository Count: ${repository.trendingNotes.value.size}, Post-Filter Count: ${postFilteredNotes.size}")
            TrendingNotesUiState.Success(postFilteredNotes)
        } else {
            TrendingNotesUiState.Loading
        }
    )

    init {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null) {
            viewModelScope.launch {
                bookmarkRepository.loadInitialBookmarksIfNeeded(currentUid)
                upvoteRepository.loadInitialUpvotesIfNeeded(currentUid)
            }
        }
        viewModelScope.launch {
            uiState.collect { state ->
                if (state is TrendingNotesUiState.Success) {
                    val paths = state.trendingNotes.map { note ->
                        val col = upvoteRepository.getCollectionForDocType(note.documentType)
                        note.id to col
                    }
                    upvoteRepository.observeVisibleDocuments("TrendingNotes", paths)
                }
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
                    "cheat sheet", "cheatsheet", "cheatsheets" -> com.pravor.notessharing.model.FileType.CheatSheet
                    "assignment" -> com.pravor.notessharing.model.FileType.Notes
                    "video" -> com.pravor.notessharing.model.FileType.Video
                    else -> com.pravor.notessharing.model.FileType.Pdf
                }
                val studyFile = com.pravor.notessharing.model.StudyFile(
                    id = note.id,
                    title = note.title,
                    uploadDate = "Saved",
                    fileType = fileType,
                    downloadsCount = note.downloadsCount,
                    upvotes = note.upvotes,
                    thumbnailUrl = note.thumbnailUrl,
                    subject = note.subject,
                    documentType = docType
                )
                bookmarkRepository.addBookmark(studyFile, currentUid)
            }
        }
    }

    fun toggleUpvote(note: com.pravor.notessharing.model.TrendingNote) {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val docType = note.documentType.ifBlank { note.type ?: "Notes" }
        val collection = when (docType.lowercase(java.util.Locale.US)) {
            "notes", "note" -> "notes"
            "pyq", "pyqs" -> "pyqs"
            "assignment", "assignments" -> "assignments"
            "cheat sheet", "cheatsheet", "cheatsheets" -> "cheatsheets"
            "video", "videos", "youtube resource" -> "videos"
            else -> "documents"
        }

        viewModelScope.launch {
            upvoteRepository.toggleUpvote(
                documentId = note.id,
                collectionName = collection,
                currentUpvotes = note.upvotes,
                userId = currentUid
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        upvoteRepository.observeVisibleDocuments("TrendingNotes", emptyList())
    }
}
