package com.pravor.notessharing.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.data.mapper.ExploreMapper
import com.pravor.notessharing.model.TrendingNote
import com.pravor.notessharing.model.VideoRecommendation
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SubjectResourcesViewModel(
    application: Application,
    private val subjectName: String
) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val bookmarkRepository = com.pravor.notessharing.bookmarks.BookmarkRepository()
    private val upvoteRepository = com.pravor.notessharing.upvotes.UpvoteRepository()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _rawResources = MutableStateFlow<List<Any>>(emptyList())

    // Combine bookmarks, upvotes, and raw resources
    val resources: StateFlow<List<Any>> = combine(
        _rawResources,
        com.pravor.notessharing.bookmarks.BookmarkRepository.bookmarksFlow,
        combine(
            com.pravor.notessharing.upvotes.UpvoteRepository.upvotesFlow,
            com.pravor.notessharing.upvotes.UpvoteRepository.upvoteCountsFlow,
            ::Pair
        ),
        com.pravor.notessharing.upvotes.UpvoteRepository.downloadCountsFlow
    ) { rawList, bookmarks, upvoteStatePair, downloadCountsMap ->
        val (upvotesMap, upvoteCountsMap) = upvoteStatePair
        val bookmarkedIds = bookmarks.map { it.id }.toSet()

        rawList.map { res ->
            when (res) {
                is TrendingNote -> {
                    val isUpvoted = upvotesMap[res.id] ?: false
                    val upvotesCount = upvoteCountsMap[res.id] ?: res.upvotes
                    val downloadsCount = downloadCountsMap[res.id] ?: res.downloadsCount
                    res.copy(
                        isBookmarked = bookmarkedIds.contains(res.id),
                        isUpvoted = isUpvoted,
                        upvotes = upvotesCount,
                        downloadsCount = downloadsCount
                    )
                }
                is VideoRecommendation -> {
                    val isUpvoted = upvotesMap[res.id] ?: false
                    val upvotesCount = upvoteCountsMap[res.id] ?: res.upvotes
                    res.copy(
                        isBookmarked = bookmarkedIds.contains(res.id),
                        isUpvoted = isUpvoted,
                        upvotes = upvotesCount
                    )
                }
                else -> res
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null) {
            viewModelScope.launch {
                bookmarkRepository.loadInitialBookmarksIfNeeded(currentUid)
                upvoteRepository.loadInitialUpvotesIfNeeded(currentUid)
            }
        }

        loadResources()

        // Observe upvote paths for visible items
        viewModelScope.launch {
            resources.collect { list ->
                val paths = list.mapNotNull { res ->
                    when (res) {
                        is TrendingNote -> {
                            val col = upvoteRepository.getCollectionForDocType(res.documentType)
                            res.id to col
                        }
                        is VideoRecommendation -> {
                            val col = upvoteRepository.getCollectionForDocType(res.documentType ?: "video")
                            res.id to col
                        }
                        else -> null
                    }
                }
                upvoteRepository.observeVisibleDocuments("SubjectResources_${subjectName}", paths)
            }
        }
    }

    fun loadResources() {
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                val collections = listOf("documents", "notes", "pyqs", "assignments", "cheatsheets", "videos")
                val allDocs = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val deferreds = collections.map { col ->
                        async {
                            try {
                                firestore.collection(col).get().await().documents
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }
                    deferreds.awaitAll().flatten()
                }.sortedByDescending { doc ->
                    doc.getLong("uploadedAt") ?: 0L
                }

                val bookmarkedIds = com.pravor.notessharing.bookmarks.BookmarkRepository.bookmarksFlow.value.map { it.id }.toSet()
                val targetNormalized = com.pravor.notessharing.ui.components.utils.normalizeSubject(subjectName)

                val filtered = allDocs.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val docSubject = data["subject"] as? String ?: ""
                    val docSubjectNorm = com.pravor.notessharing.ui.components.utils.normalizeSubject(docSubject)

                    // Match by normalized subject name or ID
                    if (docSubjectNorm != targetNormalized) {
                        return@mapNotNull null
                    }

                    val note = ExploreMapper.documentToTrendingNote(doc, bookmarkedIds) ?: return@mapNotNull null

                    if (note.resourceType == com.pravor.notessharing.model.ResourceType.VIDEO ||
                        note.resourceType == com.pravor.notessharing.model.ResourceType.PLAYLIST) {
                        note.toVideoRecommendation()
                    } else {
                        note
                    }
                }

                _rawResources.value = filtered
            } catch (e: Exception) {
                // Ignore
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun toggleBookmark(note: TrendingNote) {
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

    fun toggleVideoBookmark(video: VideoRecommendation) {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val wasBookmarked = com.pravor.notessharing.bookmarks.BookmarkRepository.bookmarksFlow.value.any { it.id == video.id }

        viewModelScope.launch {
            if (wasBookmarked) {
                bookmarkRepository.removeBookmark(video.id, currentUid)
            } else {
                val docType = video.documentType.ifBlank { "Video" }
                val studyFile = com.pravor.notessharing.model.StudyFile(
                    id = video.id,
                    title = video.title,
                    uploadDate = "Saved",
                    fileType = com.pravor.notessharing.model.FileType.Video,
                    downloadsCount = 0,
                    upvotes = video.upvotes,
                    thumbnailUrl = video.thumbnailUrl ?: video.youtubeThumbnailUrl,
                    subject = video.subject,
                    documentType = docType
                )
                bookmarkRepository.addBookmark(studyFile, currentUid)
            }
        }
    }

    fun toggleUpvote(id: String, docType: String?, currentUpvotes: Int) {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val rawType = docType?.lowercase(java.util.Locale.US) ?: "notes"
        val collection = when (rawType) {
            "notes", "note" -> "notes"
            "pyq", "pyqs" -> "pyqs"
            "assignment", "assignments" -> "assignments"
            "cheat sheet", "cheatsheet", "cheatsheets" -> "cheatsheets"
            "video", "videos", "youtube resource" -> "videos"
            else -> "documents"
        }

        viewModelScope.launch {
            upvoteRepository.toggleUpvote(
                documentId = id,
                collectionName = collection,
                currentUpvotes = currentUpvotes,
                userId = currentUid
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        upvoteRepository.observeVisibleDocuments("SubjectResources_${subjectName}", emptyList())
    }

    companion object {
        fun provideFactory(
            application: Application,
            subjectName: String
        ): androidx.lifecycle.ViewModelProvider.Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return SubjectResourcesViewModel(application, subjectName) as T
            }
        }
    }
}
