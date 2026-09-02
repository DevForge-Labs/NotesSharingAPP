package com.pravor.notessharing.ui.features.explore

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import com.pravor.notessharing.data.repository.BookmarkRepository

import com.pravor.notessharing.domain.model.*
import com.pravor.notessharing.data.repository.*
import com.pravor.notessharing.core.util.*

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.data.mapper.ExploreMapper
import com.pravor.notessharing.domain.model.TrendingNote
import com.pravor.notessharing.domain.model.VideoRecommendation
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
    private val bookmarkRepository = com.pravor.notessharing.data.repository.BookmarkRepository()
    private val upvoteRepository = com.pravor.notessharing.data.repository.UpvoteRepository()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _rawResources = MutableStateFlow<List<Any>>(emptyList())

    // Combine bookmarks, upvotes, and raw resources
    val resources: StateFlow<List<Any>> = combine(
        _rawResources,
        com.pravor.notessharing.data.repository.BookmarkRepository.bookmarksFlow,
        combine(
            com.pravor.notessharing.data.repository.UpvoteRepository.upvotesFlow,
            com.pravor.notessharing.data.repository.UpvoteRepository.upvoteCountsFlow,
            ::Pair
        ),
        com.pravor.notessharing.data.repository.UpvoteRepository.downloadCountsFlow
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

    private val profileRepository = com.pravor.notessharing.data.repository.ProfileRepository()
    private val metadataRepository = com.pravor.notessharing.data.repository.MetadataRepository()

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
                val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                val userProfile = if (currentUid != null) profileRepository.getProfile(currentUid) else null
                val scope = com.pravor.notessharing.core.util.AcademicScopeResolver.resolve(userProfile, metadataRepository)

                if (!scope.isCollegeValid) {
                    android.util.Log.d("SubjectResourcesVM", "No valid college found in user profile. Skipping resources load.")
                    _rawResources.value = emptyList()
                    return@launch
                }

                val canonicalCollegeId = scope.canonicalCollegeId
                val targetNormalized = com.pravor.notessharing.ui.common.utils.normalizeSubject(subjectName)
                val cleanSubjectName = subjectName.trim().lowercase(java.util.Locale.ROOT)
                val candidateSubjectIds = listOfNotNull(
                    targetNormalized.takeIf { it.isNotBlank() },
                    cleanSubjectName.takeIf { it.isNotBlank() && it != targetNormalized }
                ).distinct()

                val collections = listOf("notes", "pyqs", "assignments", "cheatsheets", "videos")
                val hasSemester = !scope.semester.isNullOrBlank() && scope.semester != "Not Set"

                val allDocs = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val deferreds = collections.map { col ->
                        async {
                            try {
                                val colRef = firestore.collection(col)
                                val docsBySubjectId = if (candidateSubjectIds.isNotEmpty()) {
                                    colRef.whereEqualTo("college", canonicalCollegeId)
                                        .whereIn("subjectId", candidateSubjectIds.take(30))
                                        .get()
                                        .await()
                                        .documents
                                } else {
                                    emptyList()
                                }

                                // If documents by subjectId were found, use them; otherwise, narrow down by semester or college
                                if (docsBySubjectId.isNotEmpty()) {
                                    docsBySubjectId
                                } else if (hasSemester) {
                                    colRef.whereEqualTo("college", canonicalCollegeId)
                                        .whereEqualTo("semester", scope.semester)
                                        .get()
                                        .await()
                                        .documents
                                } else {
                                    colRef.whereEqualTo("college", canonicalCollegeId)
                                        .get()
                                        .await()
                                        .documents
                                }
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }
                    deferreds.awaitAll().flatten().distinctBy { it.id }
                }.sortedByDescending { doc ->
                    doc.getLong("uploadedAt") ?: 0L
                }

                val bookmarkedIds = com.pravor.notessharing.data.repository.BookmarkRepository.bookmarksFlow.value.map { it.id }.toSet()

                val filtered = allDocs.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val docSubject = data["subject"] as? String ?: ""
                    val docDisplaySubject = data["displaySubject"] as? String ?: ""
                    val docSubjectNorm = com.pravor.notessharing.ui.common.utils.normalizeSubject(docSubject)
                    val docDisplaySubjectNorm = com.pravor.notessharing.ui.common.utils.normalizeSubject(docDisplaySubject)
                    val docSubjectId = data["subjectId"] as? String

                    // Match by normalized subject name, display subject, or subject ID
                    val matchesSubject = (docSubjectNorm == targetNormalized) ||
                            (docDisplaySubjectNorm == targetNormalized) ||
                            (!docSubjectId.isNullOrBlank() && (targetNormalized.contains(docSubjectId.lowercase(java.util.Locale.ROOT)) || docSubjectId.lowercase(java.util.Locale.ROOT).contains(targetNormalized))) ||
                            (docSubjectNorm.isNotBlank() && targetNormalized.contains(docSubjectNorm)) ||
                            (targetNormalized.isNotBlank() && docSubjectNorm.contains(targetNormalized))

                    if (!matchesSubject) {
                        return@mapNotNull null
                    }

                    // Enforce academic context (semester, branch, college)
                    val matchesScope = scope.isDocumentPermitted(
                        docCollege = data["college"] as? String ?: canonicalCollegeId,
                        docBranch = data["branch"] as? String,
                        docSemester = data["semester"] as? String,
                        docSubjectId = docSubjectId
                    )

                    if (!matchesScope) {
                        return@mapNotNull null
                    }

                    val note = ExploreMapper.documentToTrendingNote(doc, bookmarkedIds) ?: return@mapNotNull null

                    if (note.resourceType == com.pravor.notessharing.domain.model.ResourceType.VIDEO ||
                        note.resourceType == com.pravor.notessharing.domain.model.ResourceType.PLAYLIST) {
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
        val wasBookmarked = com.pravor.notessharing.data.repository.BookmarkRepository.bookmarksFlow.value.any { it.id == note.id }

        viewModelScope.launch {
            if (wasBookmarked) {
                bookmarkRepository.removeBookmark(note.id, currentUid)
            } else {
                val docType = note.documentType.ifBlank { note.type ?: "Notes" }
                val fileType = when (docType.lowercase(java.util.Locale.US)) {
                    "pyq" -> com.pravor.notessharing.domain.model.FileType.Pyq
                    "cheat sheet", "cheatsheet", "cheatsheets" -> com.pravor.notessharing.domain.model.FileType.CheatSheet
                    "assignment" -> com.pravor.notessharing.domain.model.FileType.Notes
                    "video" -> com.pravor.notessharing.domain.model.FileType.Video
                    else -> com.pravor.notessharing.domain.model.FileType.Pdf
                }
                val studyFile = com.pravor.notessharing.domain.model.StudyFile(
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
        val wasBookmarked = com.pravor.notessharing.data.repository.BookmarkRepository.bookmarksFlow.value.any { it.id == video.id }

        viewModelScope.launch {
            if (wasBookmarked) {
                bookmarkRepository.removeBookmark(video.id, currentUid)
            } else {
                val docType = video.documentType.ifBlank { "Video" }
                val studyFile = com.pravor.notessharing.domain.model.StudyFile(
                    id = video.id,
                    title = video.title,
                    uploadDate = "Saved",
                    fileType = com.pravor.notessharing.domain.model.FileType.Video,
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
            "video", "videos", "youtube resource", "playlist", "playlists", "video playlist" -> "videos"
            else -> "notes"
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
