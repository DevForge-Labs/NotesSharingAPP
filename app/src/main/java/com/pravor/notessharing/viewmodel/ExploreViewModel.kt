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
import com.pravor.notessharing.state.CatalogSubject
import com.pravor.notessharing.data.ExploreRepository
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
    private val exploreRepository = ExploreRepository(application)

    private var isFirstLoad = true
    private val startupStartTime = System.currentTimeMillis()

    private val _uiState = MutableStateFlow<ExploreUiState>(
        exploreRepository.getCachedContent()?.let { ExploreUiState.Success(it) } ?: ExploreUiState.Loading
    )
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()
    private val bookmarkRepository = com.pravor.notessharing.bookmarks.BookmarkRepository()
    private val upvoteRepository = com.pravor.notessharing.upvotes.UpvoteRepository()

    private val profileRepository = com.pravor.notessharing.profile.ProfileRepository()
    private val _allowedSubjects = MutableStateFlow<List<CatalogSubject>>(emptyList())
    val allowedSubjects: StateFlow<List<CatalogSubject>> = _allowedSubjects.asStateFlow()

    private var fetchJob: kotlinx.coroutines.Job? = null
    private val isRefreshingState = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = isRefreshingState.asStateFlow()

    private fun loadCatalogSubjects() {
        viewModelScope.launch {
            val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            try {
                val profile = profileRepository.getProfile(currentUid)
                val collegeId = profile?.college?.let { com.pravor.notessharing.util.LegacyAcademicCompatibilityResolver.resolveCollegeId(it) } ?: "kiit"
                val branchId = profile?.branch?.let { com.pravor.notessharing.util.LegacyAcademicCompatibilityResolver.resolveBranchId(it) } ?: "cse"
                val semester = profile?.semester ?: "Semester 4"

                val firestoreQueryStartTime = System.currentTimeMillis()
                android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query START collection=app_config/subject_catalog thread=${Thread.currentThread().name}")
                val snapshot = firestore.collection("app_config")
                    .document("subject_catalog")
                    .get()
                    .await()
                val firestoreQueryDuration = System.currentTimeMillis() - firestoreQueryStartTime
                android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query END collection=app_config/subject_catalog duration=${firestoreQueryDuration}ms docs=${if (snapshot.exists()) 1 else 0} thread=${Thread.currentThread().name}")
                
                if (snapshot.exists()) {
                    val collegeCatalog = snapshot.data?.get(collegeId) as? Map<*, *>
                    
                    val isFirstYear = semester.trim().lowercase(java.util.Locale.ROOT).contains("semester 1") || 
                                     semester.trim().lowercase(java.util.Locale.ROOT).contains("sem 1") || 
                                     semester.trim() == "1" || 
                                     semester.trim().lowercase(java.util.Locale.ROOT).startsWith("1st") ||
                                     semester.trim().lowercase(java.util.Locale.ROOT).contains("semester 2") || 
                                     semester.trim().lowercase(java.util.Locale.ROOT).contains("sem 2") || 
                                     semester.trim() == "2" || 
                                     semester.trim().lowercase(java.util.Locale.ROOT).startsWith("2nd")
                                     
                    val catalogData = if (isFirstYear) {
                        val groupKey = when {
                            semester.trim().lowercase(java.util.Locale.ROOT).contains("semester 1") || semester.trim().lowercase(java.util.Locale.ROOT).contains("sem 1") || semester.trim() == "1" || semester.trim().lowercase(java.util.Locale.ROOT).startsWith("1st") -> "GROUP_A"
                            else -> "GROUP_B"
                        }
                        collegeCatalog?.entries?.firstOrNull {
                            it.key.toString().equals(groupKey, ignoreCase = true)
                        }?.value
                    } else {
                        val branchCatalog = collegeCatalog?.entries?.firstOrNull {
                            it.key.toString().equals(branchId, ignoreCase = true)
                        }?.value as? Map<*, *>
                        
                        val semNum = semester.filter { it.isDigit() }
                        var semesterData = branchCatalog?.entries?.firstOrNull {
                            it.key.toString().equals(semester, ignoreCase = true)
                        }?.value
                        
                        if (semesterData == null && semNum.isNotEmpty()) {
                            semesterData = branchCatalog?.entries?.firstOrNull {
                                it.key.toString() == semNum
                            }?.value
                        }
                        semesterData
                    }

                    val resolvedSubjects = mutableListOf<CatalogSubject>()
                    if (catalogData != null) {
                        if (catalogData is Map<*, *>) {
                            for ((subId, subVal) in catalogData) {
                                val id = subId.toString()
                                val name = when (subVal) {
                                    is Map<*, *> -> subVal["name"]?.toString() ?: id
                                    is String -> subVal
                                    else -> id
                                }
                                resolvedSubjects.add(CatalogSubject(id, name))
                            }
                        } else if (catalogData is List<*>) {
                            for (item in catalogData) {
                                when (item) {
                                    is Map<*, *> -> {
                                        val id = item["subjectId"]?.toString() ?: item["id"]?.toString() ?: ""
                                        val name = item["name"]?.toString() ?: item["subject"]?.toString() ?: id
                                        if (id.isNotEmpty()) {
                                            resolvedSubjects.add(CatalogSubject(id, name))
                                        }
                                    }
                                    is String -> {
                                        if (item.isNotEmpty()) {
                                            resolvedSubjects.add(CatalogSubject(item, item))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Preserves exact ordering from firestore catalog document mapping
                    _allowedSubjects.value = resolvedSubjects
                }
            } catch (e: Exception) {
                // Ignore and keep defaults
            }
        }
    }

    init {
        if (com.pravor.notessharing.BuildConfig.DEBUG) {
            android.util.Log.d("PERF", "[PERF] Explore startup START thread=${Thread.currentThread().name}")
        }
        val cached = exploreRepository.getCachedContent()
        loadRealDocuments(silent = cached != null)
        loadCatalogSubjects()

        viewModelScope.launch {
            val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (currentUid != null) {
                bookmarkRepository.loadInitialBookmarksIfNeeded(currentUid)
                upvoteRepository.loadInitialUpvotesIfNeeded(currentUid)
            }
        }

        viewModelScope.launch {
            com.pravor.notessharing.bookmarks.BookmarkRepository.bookmarksFlow.collect { bookmarks ->
                val bookmarkedIds = bookmarks.map { it.id }.toSet()
                _uiState.update { current ->
                    if (current is ExploreUiState.Success) {
                        val updatedNotes = current.content.notes.map { note ->
                            note.copy(isBookmarked = bookmarkedIds.contains(note.id))
                        }
                        val updatedExamPrep = current.content.examPrep.map { note ->
                            note.copy(isBookmarked = bookmarkedIds.contains(note.id))
                        }
                        val updatedAssignments = current.content.assignments.map { note ->
                            note.copy(isBookmarked = bookmarkedIds.contains(note.id))
                        }
                        val updatedVideos = current.content.videos.map { video ->
                            val isBookmarkedNow = bookmarkedIds.contains(video.id)
                            val originalIsBookmarked = video.isBookmarked
                            val bookmarksCount = if (isBookmarkedNow && !originalIsBookmarked) {
                                video.bookmarks + 1
                            } else if (!isBookmarkedNow && originalIsBookmarked) {
                                (video.bookmarks - 1).coerceAtLeast(0)
                            } else {
                                video.bookmarks
                            }
                            video.copy(isBookmarked = isBookmarkedNow, bookmarks = bookmarksCount)
                        }
                        ExploreUiState.Success(current.content.copy(
                            notes = updatedNotes,
                            examPrep = updatedExamPrep,
                            assignments = updatedAssignments,
                            videos = updatedVideos
                        ))
                    } else {
                        current
                    }
                }
            }
        }

        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                com.pravor.notessharing.upvotes.UpvoteRepository.upvotesFlow,
                com.pravor.notessharing.upvotes.UpvoteRepository.upvoteCountsFlow
            ) { upvotesMap, upvoteCountsMap ->
                Pair(upvotesMap, upvoteCountsMap)
            }.collect { (upvotesMap, upvoteCountsMap) ->
                _uiState.update { current ->
                    if (current is ExploreUiState.Success) {
                        val updatedPopular = current.content.popularUploads.map { item ->
                            val isUpvoted = upvotesMap[item.id] ?: false
                            val count = upvoteCountsMap[item.id] ?: item.upvotes
                            item.copy(isUpvoted = isUpvoted, upvotes = count)
                        }
                        val updatedNotes = current.content.notes.map { note ->
                            val isUpvoted = upvotesMap[note.id] ?: false
                            val count = upvoteCountsMap[note.id] ?: note.upvotes
                            note.copy(isUpvoted = isUpvoted, upvotes = count)
                        }
                        val updatedExamPrep = current.content.examPrep.map { note ->
                            val isUpvoted = upvotesMap[note.id] ?: false
                            val count = upvoteCountsMap[note.id] ?: note.upvotes
                            note.copy(isUpvoted = isUpvoted, upvotes = count)
                        }
                        val updatedAssignments = current.content.assignments.map { note ->
                            val isUpvoted = upvotesMap[note.id] ?: false
                            val count = upvoteCountsMap[note.id] ?: note.upvotes
                            note.copy(isUpvoted = isUpvoted, upvotes = count)
                        }
                        val updatedVideos = current.content.videos.map { video ->
                            val isUpvoted = upvotesMap[video.id] ?: false
                            val count = upvoteCountsMap[video.id] ?: video.upvotes
                            video.copy(isUpvoted = isUpvoted, upvotes = count)
                        }
                        ExploreUiState.Success(current.content.copy(
                            popularUploads = updatedPopular,
                            notes = updatedNotes,
                            examPrep = updatedExamPrep,
                            assignments = updatedAssignments,
                            videos = updatedVideos
                        ))
                    } else {
                        current
                    }
                }
            }
        }

        viewModelScope.launch {
            com.pravor.notessharing.upvotes.UpvoteRepository.downloadCountsFlow.collect { downloadCountsMap ->
                _uiState.update { current ->
                    if (current is ExploreUiState.Success) {
                        val updatedPopular = current.content.popularUploads.map { item ->
                            val count = downloadCountsMap[item.id] ?: item.downloadsCount
                            item.copy(downloadsCount = count)
                        }
                        val updatedNotes = current.content.notes.map { note ->
                            val count = downloadCountsMap[note.id] ?: note.downloadsCount
                            note.copy(downloadsCount = count)
                        }
                        val updatedExamPrep = current.content.examPrep.map { note ->
                            val count = downloadCountsMap[note.id] ?: note.downloadsCount
                            note.copy(downloadsCount = count)
                        }
                        val updatedAssignments = current.content.assignments.map { note ->
                            val count = downloadCountsMap[note.id] ?: note.downloadsCount
                            note.copy(downloadsCount = count)
                        }
                        val updatedVideos = current.content.videos.map { video ->
                            val count = downloadCountsMap[video.id] ?: video.downloadsCount
                            video.copy(downloadsCount = count)
                        }
                        val updatedDiscover = current.content.discoverItems.map { item ->
                            if (item is com.pravor.notessharing.model.DiscoverFeedItem.Note) {
                                val count = downloadCountsMap[item.id] ?: item.downloadsCount
                                item.copy(downloadsCount = count)
                            } else {
                                item
                            }
                        }
                        ExploreUiState.Success(current.content.copy(
                            popularUploads = updatedPopular,
                            notes = updatedNotes,
                            examPrep = updatedExamPrep,
                            assignments = updatedAssignments,
                            videos = updatedVideos,
                            discoverItems = updatedDiscover
                        ))
                    } else {
                        current
                    }
                }
            }
        }

        viewModelScope.launch {
            _uiState.collect { state ->
                if (state is ExploreUiState.Success) {
                    val content = state.content
                    val paths = mutableListOf<Pair<String, String>>()
                    
                    for (note in content.notes) {
                        val col = upvoteRepository.getCollectionForDocType(note.documentType)
                        paths.add(note.id to col)
                    }
                    for (note in content.examPrep) {
                        val col = upvoteRepository.getCollectionForDocType(note.documentType)
                        paths.add(note.id to col)
                    }
                    for (note in content.assignments) {
                        val col = upvoteRepository.getCollectionForDocType(note.documentType)
                        paths.add(note.id to col)
                    }
                    for (video in content.videos) {
                        val col = upvoteRepository.getCollectionForDocType(video.documentType ?: "video")
                        paths.add(video.id to col)
                    }
                    for (item in content.popularUploads) {
                        val col = upvoteRepository.getCollectionForDocType(item.documentType ?: item.fileType.label)
                        paths.add(item.id to col)
                    }
                    
                    upvoteRepository.observeVisibleDocuments("Explore", paths)
                }
            }
        }
    }

    fun loadRealDocuments(silent: Boolean = false, isPullToRefresh: Boolean = false) {
        if (isPullToRefresh) {
            isRefreshingState.value = true
            loadCatalogSubjects()
        }

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            if (com.pravor.notessharing.BuildConfig.DEBUG) {
                android.util.Log.d("PERF", "[PERF] Explore load START thread=${Thread.currentThread().name}")
            }
            try {
                if (!isPullToRefresh) {
                    val cachedContent = exploreRepository.getCachedContent()
                    if (cachedContent != null) {
                        _uiState.update { ExploreUiState.Success(cachedContent) }
                        if (!exploreRepository.isCacheExpired()) {
                            return@launch
                        }
                    } else {
                        if (!silent) {
                            _uiState.value = ExploreUiState.Loading
                        }
                    }
                }

                val freshContent = exploreRepository.fetchExploreContent()
                _uiState.update { ExploreUiState.Success(freshContent) }

                if (com.pravor.notessharing.BuildConfig.DEBUG) {
                    val duration = System.currentTimeMillis() - startTime
                    android.util.Log.d("PERF", "[PERF] Explore load END - duration=$duration ms thread=${Thread.currentThread().name}")
                }
                if (isFirstLoad) {
                    isFirstLoad = false
                    if (com.pravor.notessharing.BuildConfig.DEBUG) {
                        val startupDuration = System.currentTimeMillis() - startupStartTime
                        android.util.Log.d("PERF", "[PERF] Explore startup END duration=${startupDuration}ms thread=${Thread.currentThread().name}")
                    }
                }
            } catch (e: Exception) {
                if (exploreRepository.getCachedContent() == null) {
                    _uiState.update {
                        ExploreUiState.Error(e.localizedMessage ?: "Failed to load explore content")
                    }
                }
                if (com.pravor.notessharing.BuildConfig.DEBUG) {
                    val duration = System.currentTimeMillis() - startTime
                    android.util.Log.d("PERF", "[PERF] Explore load END - duration=$duration ms thread=${Thread.currentThread().name}")
                }
                if (isFirstLoad) {
                    isFirstLoad = false
                    if (com.pravor.notessharing.BuildConfig.DEBUG) {
                        val startupDuration = System.currentTimeMillis() - startupStartTime
                        android.util.Log.d("PERF", "[PERF] Explore startup END duration=${startupDuration}ms thread=${Thread.currentThread().name}")
                    }
                }
            } finally {
                isRefreshingState.value = false
            }
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

    fun toggleVideoBookmark(video: com.pravor.notessharing.model.VideoRecommendation) {
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

    fun toggleUpvote(itemId: String, documentType: String?, currentUpvotes: Int) {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val rawType = documentType?.lowercase(java.util.Locale.US) ?: "notes"
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
                documentId = itemId,
                collectionName = collection,
                currentUpvotes = currentUpvotes,
                userId = currentUid
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        fetchJob?.cancel()
        upvoteRepository.observeVisibleDocuments("Explore", emptyList())
    }
}
