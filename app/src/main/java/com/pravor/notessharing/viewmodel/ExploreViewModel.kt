package com.pravor.notessharing.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.model.DiscoverFeedItem
import com.pravor.notessharing.model.FeedItem
import com.pravor.notessharing.model.FileType
import com.pravor.notessharing.state.ExploreContent
import com.pravor.notessharing.state.ExploreUiState
import com.pravor.notessharing.data.ExploreCacheRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class ExploreViewModel(application: Application) : AndroidViewModel(application) {
    private val cacheRepository = ExploreCacheRepository(application)

    private val _uiState = MutableStateFlow<ExploreUiState>(
        cacheRepository.getCache()?.let { ExploreUiState.Success(it) } ?: ExploreUiState.Loading
    )
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()
    private val bookmarkRepository = com.pravor.notessharing.bookmarks.BookmarkRepository()

    private var fetchJob: kotlinx.coroutines.Job? = null
    private val isRefreshingState = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = isRefreshingState.asStateFlow()

    init {
        val cached = cacheRepository.getCache()
        loadRealDocuments(silent = cached != null)

        viewModelScope.launch {
            val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (currentUid != null) {
                bookmarkRepository.loadInitialBookmarksIfNeeded(currentUid)
            }
        }

        viewModelScope.launch {
            com.pravor.notessharing.bookmarks.BookmarkRepository.bookmarksFlow.collect { bookmarks ->
                val bookmarkedIds = bookmarks.map { it.id }.toSet()
                _uiState.update { current ->
                    if (current is ExploreUiState.Success) {
                        val updatedTrending = current.content.trendingNotes.map { note ->
                            note.copy(isBookmarked = bookmarkedIds.contains(note.id))
                        }
                        ExploreUiState.Success(current.content.copy(trendingNotes = updatedTrending))
                    } else {
                        current
                    }
                }
            }
        }
    }

    fun loadRealDocuments(silent: Boolean = false, isPullToRefresh: Boolean = false) {
        if (isPullToRefresh) {
            isRefreshingState.value = true
        }

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            try {
                if (!silent && !isPullToRefresh && cacheRepository.getCache() == null) {
                    _uiState.value = ExploreUiState.Loading
                }

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
                    doc.getLong("uploadedAt") ?: doc.getLong("uploadTimestamp") ?: 0L
                }

                val realFeed = allDocs.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    documentToFeedItem(data)
                }

                val bookmarkedIds = com.pravor.notessharing.bookmarks.BookmarkRepository.bookmarksFlow.value.map { it.id }.toSet()

                val realTrending = allDocs.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    
                    val docType = (data["documentType"] as? String ?: data["type"] as? String ?: "").trim()
                    val contentType = (data["contentType"] as? String ?: "").trim()
                    val hasYoutubeLink = (data["hasYoutubeLink"] as? Boolean) == true || (data["hasYoutubeLink"] as? String)?.lowercase() == "true"
                    val sourceType = (data["sourceType"] as? String ?: "").trim()
                    val youtubeUrl = (data["youtubeUrl"] as? String ?: "").trim()
                    val youtubeVideoId = (data["youtubeVideoId"] as? String ?: "").trim()
                    val resourceType = (data["resourceType"] as? String ?: "").trim()
                    val source = (data["source"] as? String ?: "").trim()

                    val isVideo = docType.equals("VIDEO", ignoreCase = true) ||
                            docType.equals("YouTube Resource", ignoreCase = true) ||
                            docType.equals("Videos", ignoreCase = true) ||
                            contentType.equals("VIDEO", ignoreCase = true) ||
                            hasYoutubeLink ||
                            sourceType.equals("youtube", ignoreCase = true) ||
                            sourceType.equals("video", ignoreCase = true) ||
                            youtubeUrl.isNotBlank() ||
                            youtubeVideoId.isNotBlank() ||
                            resourceType.equals("VIDEO", ignoreCase = true) ||
                            source.equals("YOUTUBE", ignoreCase = true)

                    if (isVideo) {
                        return@mapNotNull null
                    }

                    val id = data["documentId"] as? String ?: ""
                    val title = data["title"] as? String ?: ""
                    val subject = data["subject"] as? String ?: ""
                    val downloads = (data["downloads"] as? Long ?: 0L).toInt()
                    val upvotes = (data["upvotes"] as? Long ?: 0L).toInt()
                    val thumbnailUrl = (data["thumbnailUrl"] as? String)?.ifBlank { null }
                        ?: (data["youtubeThumbnailUrl"] as? String)?.ifBlank { null }
                    val thumbnailGenerated = data["thumbnailGenerated"] as? Boolean
                    val thumbnailType = data["thumbnailType"] as? String
                    val documentTypeField = data["documentType"] as? String
                    val typeField = data["type"] as? String
                    val examYearVal = data["examYear"] as? String

                    com.pravor.notessharing.model.TrendingNote(
                        id = id,
                        title = title,
                        subject = subject,
                        downloads = downloads,
                        rating = 4.5,
                        upvotes = upvotes,
                        isBookmarked = bookmarkedIds.contains(id),
                        thumbnailUrl = thumbnailUrl,
                        thumbnailGenerated = thumbnailGenerated,
                        thumbnailType = thumbnailType,
                        documentType = documentTypeField ?: "",
                        type = typeField,
                        examYear = examYearVal
                    )
                }

                val realVideos = allDocs.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    
                    val docType = (data["documentType"] as? String ?: data["type"] as? String ?: "").trim()
                    val contentType = (data["contentType"] as? String ?: "").trim()
                    val hasYoutubeLink = (data["hasYoutubeLink"] as? Boolean) == true || (data["hasYoutubeLink"] as? String)?.lowercase() == "true"
                    val sourceType = (data["sourceType"] as? String ?: "").trim()
                    val youtubeUrl = (data["youtubeUrl"] as? String ?: "").trim()
                    val youtubeVideoId = (data["youtubeVideoId"] as? String ?: "").trim()

                    val isVideo = docType.equals("VIDEO", ignoreCase = true) ||
                            docType.equals("YouTube Resource", ignoreCase = true) ||
                            docType.equals("Videos", ignoreCase = true) ||
                            contentType.equals("VIDEO", ignoreCase = true) ||
                            hasYoutubeLink ||
                            sourceType.equals("youtube", ignoreCase = true) ||
                            sourceType.equals("video", ignoreCase = true) ||
                            youtubeUrl.isNotBlank() ||
                            youtubeVideoId.isNotBlank()

                    if (!isVideo) {
                        return@mapNotNull null
                    }

                    val id = data["documentId"] as? String ?: ""
                    val subject = data["subject"] as? String ?: ""
                    val title = data["title"] as? String ?: data["videoTitle"] as? String ?: "Untitled Video"
                    val uploaderName = data["uploaderName"] as? String ?: "Anonymous"
                    val upvotes = (data["upvotes"] as? Long ?: 0L).toInt()
                    val bookmarks = (data["bookmarks"] as? Long ?: 0L).toInt()
                    val thumbnailUrlVal = data["thumbnailUrl"] as? String
                    val youtubeThumbnailUrlVal = data["youtubeThumbnailUrl"] as? String
                    val semesterVal = data["semester"] as? String ?: "Semester 4"

                    com.pravor.notessharing.model.VideoRecommendation(
                        id = id,
                        title = title,
                        channelName = uploaderName,
                        duration = "",
                        subject = subject,
                        youtubeVideoId = youtubeVideoId,
                        upvotes = upvotes,
                        bookmarks = bookmarks,
                        thumbnailUrl = thumbnailUrlVal,
                        youtubeThumbnailUrl = youtubeThumbnailUrlVal,
                        documentType = docType,
                        semester = semesterVal,
                        youtubeUrl = youtubeUrl
                    )
                }

                val realDiscover = allDocs.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val id = data["documentId"] as? String ?: ""
                    val title = data["title"] as? String ?: ""
                    val subject = data["subject"] as? String ?: ""
                    val downloads = (data["downloads"] as? Long ?: 0L).toInt()
                    DiscoverFeedItem.Note(
                        id = id,
                        title = title,
                        subject = subject,
                        downloads = downloads
                    )
                }

                val freshContent = ExploreContent(
                    topics = DummyData.topics,
                    popularUploads = (realFeed + DummyData.feedItems).distinctBy { it.id },
                    trendingNotes = realTrending,
                    videoRecommendations = realVideos,
                    studyCollections = DummyData.studyCollections,
                    subjectHubs = DummyData.subjectHubs,
                    topContributors = DummyData.topContributors,
                    revisionCards = DummyData.revisionCards,
                    discoverItems = (realDiscover + DummyData.discoverItems).distinctBy { it.id }
                )

                cacheRepository.saveCache(freshContent)
                _uiState.update { ExploreUiState.Success(freshContent) }
            } catch (e: Exception) {
                if (cacheRepository.getCache() == null) {
                    _uiState.update {
                        ExploreUiState.Success(
                            ExploreContent(
                                topics = DummyData.topics,
                                popularUploads = DummyData.feedItems,
                                trendingNotes = emptyList(),
                                videoRecommendations = emptyList(),
                                studyCollections = DummyData.studyCollections,
                                subjectHubs = DummyData.subjectHubs,
                                topContributors = DummyData.topContributors,
                                revisionCards = DummyData.revisionCards,
                                discoverItems = DummyData.discoverItems
                            )
                        )
                    }
                }
            } finally {
                isRefreshingState.value = false
            }
        }
    }

    private fun documentToFeedItem(doc: Map<String, Any>): FeedItem {
        val id = doc["documentId"] as? String ?: ""
        val uploaderName = doc["uploaderName"] as? String ?: "Anonymous"
        val initials = uploaderName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
            .ifBlank { "AN" }
        
        val uploadTimestamp = doc["uploadedAt"] as? Long ?: (doc["uploadTimestamp"] as? Long ?: System.currentTimeMillis())
        val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
        val uploadDate = sdf.format(java.util.Date(uploadTimestamp))
        
        val docType = doc["documentType"] as? String ?: (doc["type"] as? String ?: "Notes")
        val fileType = when (docType) {
            "PYQ" -> FileType.Pyq
            "Cheat Sheet" -> FileType.CheatSheet
            "Assignment" -> FileType.Notes
            "Notes" -> FileType.Notes
            "YouTube Resource", "Videos" -> FileType.Video
            else -> FileType.Pdf
        }
        
        val subject = doc["subject"] as? String ?: ""
        val displayTitle = doc["title"] as? String ?: ""

        val description = doc["description"] as? String ?: ""
        val tags = (doc["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        
        val upvotes = (doc["upvotes"] as? Long ?: (doc["likesCount"] as? Long ?: 0L)).toInt()
        val downloads = (doc["downloads"] as? Long ?: (doc["downloadsCount"] as? Long ?: 0L)).toInt()
        val bookmarks = (doc["bookmarks"] as? Long ?: 0L).toInt()

        val youtubeUrl = doc["youtubeUrl"] as? String
        val youtubeVideoId = doc["youtubeVideoId"] as? String
        
        val thumbnailUrl = (doc["thumbnailUrl"] as? String)?.ifBlank { null }
            ?: (doc["youtubeThumbnailUrl"] as? String)?.ifBlank { null }
        val thumbnailGenerated = doc["thumbnailGenerated"] as? Boolean
        val thumbnailType = doc["thumbnailType"] as? String

        val documentTypeField = doc["documentType"] as? String
        val typeField = doc["type"] as? String
        val subjectField = doc["subject"] as? String
        val examYearField = (doc["examYear"] ?: doc["year"])?.toString()
        val sectionField = doc["section"] as? String
        val sectionDisplayField = doc["sectionDisplay"] as? String

        return FeedItem(
            id = id,
            uploaderName = uploaderName,
            uploaderInitials = initials,
            uploadDate = uploadDate,
            title = displayTitle,
            description = description,
            tags = tags,
            fileType = fileType,
            upvotes = upvotes,
            comments = 0,
            downloads = downloads,
            isUpvoted = false,
            isSaved = false,
            bookmarksCount = bookmarks,
            youtubeVideoId = youtubeVideoId,
            youtubeUrl = youtubeUrl,
            thumbnailUrl = thumbnailUrl,
            thumbnailGenerated = thumbnailGenerated,
            thumbnailType = thumbnailType,
            documentType = documentTypeField,
            type = typeField,
            subject = subjectField,
            examYear = examYearField,
            section = sectionField,
            sectionDisplay = sectionDisplayField
        )
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
