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
import com.pravor.notessharing.domain.model.DiscoverFeedItem
import com.pravor.notessharing.domain.model.FeedItem
import com.pravor.notessharing.domain.model.FileType
import com.pravor.notessharing.ui.common.ExploreContent
import com.pravor.notessharing.ui.common.ExploreUiState
import com.pravor.notessharing.ui.features.upload.CatalogSubject
import com.pravor.notessharing.data.repository.ExploreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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

    private val _uiState = MutableStateFlow<ExploreUiState>(ExploreUiState.Loading)
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()
    private val bookmarkRepository = com.pravor.notessharing.data.repository.BookmarkRepository()
    private val upvoteRepository = com.pravor.notessharing.data.repository.UpvoteRepository()

    private val profileRepository = com.pravor.notessharing.data.repository.ProfileRepository()
    private val metadataRepository = com.pravor.notessharing.data.repository.MetadataRepository()
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
                val rawCollege = profile?.college?.takeIf { it.isNotBlank() } ?: return@launch
                val collegeId = com.pravor.notessharing.core.util.LegacyAcademicCompatibilityResolver.resolveCollegeId(rawCollege)
                val branchId = profile?.branch?.let { com.pravor.notessharing.core.util.LegacyAcademicCompatibilityResolver.resolveBranchId(it) } ?: "cse"
                val semester = profile?.semester?.takeIf { it.isNotBlank() && it != "Not Set" } ?: return@launch

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

    private var exploreObservationJob: kotlinx.coroutines.Job? = null
    private var lastObservedScopeKey: String? = null

    init {
        if (com.pravor.notessharing.BuildConfig.DEBUG) {
            android.util.Log.d("PERF", "[PERF] Explore startup START thread=${Thread.currentThread().name}")
        }
        viewModelScope.launch {
            val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (currentUid != null) {
                profileRepository.observeProfile(currentUid).collect { profile ->
                    val scope = com.pravor.notessharing.core.util.AcademicScopeResolver.resolve(profile, metadataRepository)
                    if (scope.scopeKey != lastObservedScopeKey) {
                        lastObservedScopeKey = scope.scopeKey
                        exploreObservationJob?.cancel()
                        exploreObservationJob = launch {
                            exploreRepository.observeExploreContent(scope.scopeKey).collect { roomContent ->
                                if (roomContent != null) {
                                    _uiState.update { ExploreUiState.Success(mergeWithLatestStats(roomContent)) }
                                }
                            }
                        }
                        val cached = exploreRepository.getCachedContent(scope.scopeKey)
                        if (cached != null) {
                            _uiState.update { ExploreUiState.Success(mergeWithLatestStats(cached)) }
                        }
                        loadRealDocuments(silent = cached != null)
                        loadCatalogSubjects()
                    }
                }
            } else {
                loadRealDocuments()
                loadCatalogSubjects()
            }
        }

        viewModelScope.launch {
            val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (currentUid != null) {
                bookmarkRepository.loadInitialBookmarksIfNeeded(currentUid)
                upvoteRepository.loadInitialUpvotesIfNeeded(currentUid)
            }
        }

        viewModelScope.launch {
            com.pravor.notessharing.data.repository.BookmarkRepository.bookmarksFlow.collect { bookmarks ->
                val bookmarkedIds = bookmarks.map { it.id }.toSet()
                _uiState.update { current ->
                    if (current is ExploreUiState.Success) {
                        var hasChanges = false
                        val updatedNotes = current.content.notes.map { note ->
                            val isBookmarked = bookmarkedIds.contains(note.id)
                            if (note.isBookmarked != isBookmarked) {
                                hasChanges = true
                                note.copy(isBookmarked = isBookmarked)
                            } else {
                                note
                            }
                        }
                        val updatedExamPrep = current.content.examPrep.map { note ->
                            val isBookmarked = bookmarkedIds.contains(note.id)
                            if (note.isBookmarked != isBookmarked) {
                                hasChanges = true
                                note.copy(isBookmarked = isBookmarked)
                            } else {
                                note
                            }
                        }
                        val updatedAssignments = current.content.assignments.map { note ->
                            val isBookmarked = bookmarkedIds.contains(note.id)
                            if (note.isBookmarked != isBookmarked) {
                                hasChanges = true
                                note.copy(isBookmarked = isBookmarked)
                            } else {
                                note
                            }
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
                            if (video.isBookmarked != isBookmarkedNow || video.bookmarks != bookmarksCount) {
                                hasChanges = true
                                video.copy(isBookmarked = isBookmarkedNow, bookmarks = bookmarksCount)
                            } else {
                                video
                            }
                        }
                        if (!hasChanges) {
                            current
                        } else {
                            ExploreUiState.Success(current.content.copy(
                                notes = updatedNotes,
                                examPrep = updatedExamPrep,
                                assignments = updatedAssignments,
                                videos = updatedVideos
                            ))
                        }
                    } else {
                        current
                    }
                }
            }
        }

        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                com.pravor.notessharing.data.repository.UpvoteRepository.upvotesFlow,
                com.pravor.notessharing.data.repository.UpvoteRepository.upvoteCountsFlow
            ) { upvotesMap, upvoteCountsMap ->
                Pair(upvotesMap, upvoteCountsMap)
            }.collect { (upvotesMap, upvoteCountsMap) ->
                _uiState.update { current ->
                    if (current is ExploreUiState.Success) {
                        var hasChanges = false
                        val updatedPopular = current.content.popularUploads.map { item ->
                            val isUpvoted = upvotesMap[item.id] ?: item.isUpvoted
                            val count = upvoteCountsMap[item.id] ?: item.upvotes
                            if (item.isUpvoted != isUpvoted || item.upvotes != count) {
                                hasChanges = true
                                item.copy(isUpvoted = isUpvoted, upvotes = count)
                            } else {
                                item
                            }
                        }
                        val updatedNotes = current.content.notes.map { note ->
                            val isUpvoted = upvotesMap[note.id] ?: note.isUpvoted
                            val count = upvoteCountsMap[note.id] ?: note.upvotes
                            if (note.isUpvoted != isUpvoted || note.upvotes != count) {
                                hasChanges = true
                                note.copy(isUpvoted = isUpvoted, upvotes = count)
                            } else {
                                note
                            }
                        }
                        val updatedExamPrep = current.content.examPrep.map { note ->
                            val isUpvoted = upvotesMap[note.id] ?: note.isUpvoted
                            val count = upvoteCountsMap[note.id] ?: note.upvotes
                            if (note.isUpvoted != isUpvoted || note.upvotes != count) {
                                hasChanges = true
                                note.copy(isUpvoted = isUpvoted, upvotes = count)
                            } else {
                                note
                            }
                        }
                        val updatedAssignments = current.content.assignments.map { note ->
                            val isUpvoted = upvotesMap[note.id] ?: note.isUpvoted
                            val count = upvoteCountsMap[note.id] ?: note.upvotes
                            if (note.isUpvoted != isUpvoted || note.upvotes != count) {
                                hasChanges = true
                                note.copy(isUpvoted = isUpvoted, upvotes = count)
                            } else {
                                note
                            }
                        }
                        val updatedVideos = current.content.videos.map { video ->
                            val isUpvoted = upvotesMap[video.id] ?: video.isUpvoted
                            val count = upvoteCountsMap[video.id] ?: video.upvotes
                            if (video.isUpvoted != isUpvoted || video.upvotes != count) {
                                hasChanges = true
                                video.copy(isUpvoted = isUpvoted, upvotes = count)
                            } else {
                                video
                            }
                        }
                        if (!hasChanges) {
                            current
                        } else {
                            ExploreUiState.Success(current.content.copy(
                                popularUploads = updatedPopular,
                                notes = updatedNotes,
                                examPrep = updatedExamPrep,
                                assignments = updatedAssignments,
                                videos = updatedVideos
                            ))
                        }
                    } else {
                        current
                    }
                }
            }
        }

        viewModelScope.launch {
            com.pravor.notessharing.data.repository.UpvoteRepository.downloadCountsFlow.collect { downloadCountsMap ->
                _uiState.update { current ->
                    if (current is ExploreUiState.Success) {
                        var hasChanges = false
                        val updatedPopular = current.content.popularUploads.map { item ->
                            val count = downloadCountsMap[item.id] ?: item.downloadsCount
                            if (item.downloadsCount != count) {
                                hasChanges = true
                                item.copy(downloadsCount = count)
                            } else {
                                item
                            }
                        }
                        val updatedNotes = current.content.notes.map { note ->
                            val count = downloadCountsMap[note.id] ?: note.downloadsCount
                            if (note.downloadsCount != count) {
                                hasChanges = true
                                note.copy(downloadsCount = count)
                            } else {
                                note
                            }
                        }
                        val updatedExamPrep = current.content.examPrep.map { note ->
                            val count = downloadCountsMap[note.id] ?: note.downloadsCount
                            if (note.downloadsCount != count) {
                                hasChanges = true
                                note.copy(downloadsCount = count)
                            } else {
                                note
                            }
                        }
                        val updatedAssignments = current.content.assignments.map { note ->
                            val count = downloadCountsMap[note.id] ?: note.downloadsCount
                            if (note.downloadsCount != count) {
                                hasChanges = true
                                note.copy(downloadsCount = count)
                            } else {
                                note
                            }
                        }
                        val updatedVideos = current.content.videos.map { video ->
                            val count = downloadCountsMap[video.id] ?: video.downloadsCount
                            if (video.downloadsCount != count) {
                                hasChanges = true
                                video.copy(downloadsCount = count)
                            } else {
                                video
                            }
                        }
                        val updatedDiscover = current.content.discoverItems.map { item ->
                            if (item is com.pravor.notessharing.domain.model.DiscoverFeedItem.Note) {
                                val count = downloadCountsMap[item.id] ?: item.downloadsCount
                                if (item.downloadsCount != count) {
                                    hasChanges = true
                                    item.copy(downloadsCount = count)
                                } else {
                                    item
                                }
                            } else {
                                item
                            }
                        }
                        if (!hasChanges) {
                            current
                        } else {
                            ExploreUiState.Success(current.content.copy(
                                popularUploads = updatedPopular,
                                notes = updatedNotes,
                                examPrep = updatedExamPrep,
                                assignments = updatedAssignments,
                                videos = updatedVideos,
                                discoverItems = updatedDiscover
                            ))
                        }
                    } else {
                        current
                    }
                }
            }
        }

        viewModelScope.launch {
            _uiState
                .mapNotNull { (it as? ExploreUiState.Success)?.content }
                .map { content ->
                    val list = mutableListOf<Pair<String, String>>()
                    for (note in content.notes) {
                        list.add(note.id to note.documentType)
                    }
                    for (note in content.examPrep) {
                        list.add(note.id to note.documentType)
                    }
                    for (note in content.assignments) {
                        list.add(note.id to note.documentType)
                    }
                    for (video in content.videos) {
                        list.add(video.id to (video.documentType ?: "video"))
                    }
                    for (item in content.popularUploads) {
                        list.add(item.id to (item.documentType ?: item.fileType.label))
                    }
                    list.toList()
                }
                .distinctUntilChanged()
                .collect { items ->
                    val paths = items.map { (id, docType) ->
                        id to upvoteRepository.getCollectionForDocType(docType)
                    }
                    upvoteRepository.observeVisibleDocuments("Explore", paths)
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
            var activeScope: AcademicScope? = null
            try {
                val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                val userProfile = if (currentUid != null) profileRepository.getProfile(currentUid) else null
                val scope = AcademicScopeResolver.resolve(userProfile, metadataRepository)
                activeScope = scope

                if (!scope.isCollegeValid) {
                    android.util.Log.d("ExploreViewModel", "No valid college in user profile. Skipping explore content fetch.")
                    val emptyContent = ExploreContent(
                        topics = emptyList(),
                        popularUploads = emptyList(),
                        notes = emptyList(),
                        examPrep = emptyList(),
                        assignments = emptyList(),
                        videos = emptyList(),
                        studyCollections = emptyList(),
                        subjectHubs = emptyList(),
                        topContributors = emptyList(),
                        revisionCards = emptyList(),
                        discoverItems = emptyList()
                    )
                    _uiState.value = ExploreUiState.Success(emptyContent)
                    return@launch
                }

                if (!isPullToRefresh) {
                    val cachedContent = exploreRepository.getCachedContent(scope.scopeKey)
                    if (cachedContent != null) {
                        _uiState.update { ExploreUiState.Success(mergeWithLatestStats(cachedContent)) }
                        if (!exploreRepository.isCacheExpired(scope.scopeKey)) {
                            return@launch
                        }
                    } else {
                        if (!silent) {
                            _uiState.value = ExploreUiState.Loading
                        }
                    }
                }

                val freshContent = exploreRepository.fetchExploreContent(scope)
                _uiState.update { ExploreUiState.Success(mergeWithLatestStats(freshContent)) }

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
                val cachedKey = activeScope?.scopeKey
                val isCachedAvailable = cachedKey != null && exploreRepository.getCachedContent(cachedKey) != null
                if (!isCachedAvailable) {
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


    fun toggleBookmark(note: com.pravor.notessharing.domain.model.TrendingNote) {
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

    fun toggleVideoBookmark(video: com.pravor.notessharing.domain.model.VideoRecommendation) {
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

    fun toggleUpvote(itemId: String, documentType: String?, currentUpvotes: Int) {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val rawType = documentType?.lowercase(java.util.Locale.US) ?: "notes"
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
                documentId = itemId,
                collectionName = collection,
                currentUpvotes = currentUpvotes,
                userId = currentUid
            )
        }
    }

    private fun mergeWithLatestStats(content: ExploreContent): ExploreContent {
        val downloadCountsMap = com.pravor.notessharing.data.repository.UpvoteRepository.downloadCountsFlow.value
        val upvoteCountsMap = com.pravor.notessharing.data.repository.UpvoteRepository.upvoteCountsFlow.value
        val upvotesMap = com.pravor.notessharing.data.repository.UpvoteRepository.upvotesFlow.value
        val bookmarkedIds = com.pravor.notessharing.data.repository.BookmarkRepository.bookmarksFlow.value.map { it.id }.toSet()

        val updatedNotes = content.notes.map { note ->
            val count = downloadCountsMap[note.id] ?: note.downloadsCount
            val upvotes = upvoteCountsMap[note.id] ?: note.upvotes
            val isUpvoted = upvotesMap[note.id] ?: note.isUpvoted
            val isBookmarked = bookmarkedIds.contains(note.id)
            note.copy(
                downloadsCount = count,
                upvotes = upvotes,
                isUpvoted = isUpvoted,
                isBookmarked = isBookmarked
            )
        }

        val updatedExamPrep = content.examPrep.map { note ->
            note.copy(
                downloadsCount = downloadCountsMap[note.id] ?: note.downloadsCount,
                upvotes = upvoteCountsMap[note.id] ?: note.upvotes,
                isUpvoted = upvotesMap[note.id] ?: note.isUpvoted,
                isBookmarked = bookmarkedIds.contains(note.id)
            )
        }

        val updatedAssignments = content.assignments.map { note ->
            note.copy(
                downloadsCount = downloadCountsMap[note.id] ?: note.downloadsCount,
                upvotes = upvoteCountsMap[note.id] ?: note.upvotes,
                isUpvoted = upvotesMap[note.id] ?: note.isUpvoted,
                isBookmarked = bookmarkedIds.contains(note.id)
            )
        }

        val updatedVideos = content.videos.map { video ->
            val isBookmarkedNow = bookmarkedIds.contains(video.id)
            val originalIsBookmarked = video.isBookmarked
            val bookmarksCount = if (isBookmarkedNow && !originalIsBookmarked) {
                video.bookmarks + 1
            } else if (!isBookmarkedNow && originalIsBookmarked) {
                (video.bookmarks - 1).coerceAtLeast(0)
            } else {
                video.bookmarks
            }
            video.copy(
                downloadsCount = downloadCountsMap[video.id] ?: video.downloadsCount,
                upvotes = upvoteCountsMap[video.id] ?: video.upvotes,
                isUpvoted = upvotesMap[video.id] ?: video.isUpvoted,
                isBookmarked = isBookmarkedNow,
                bookmarks = bookmarksCount
            )
        }

        val updatedPopular = content.popularUploads.map { item ->
            item.copy(
                downloadsCount = downloadCountsMap[item.id] ?: item.downloadsCount,
                upvotes = upvoteCountsMap[item.id] ?: item.upvotes,
                isUpvoted = upvotesMap[item.id] ?: item.isUpvoted
            )
        }

        val updatedDiscover = content.discoverItems.map { item ->
            if (item is com.pravor.notessharing.domain.model.DiscoverFeedItem.Note) {
                item.copy(downloadsCount = downloadCountsMap[item.id] ?: item.downloadsCount)
            } else {
                item
            }
        }

        return content.copy(
            popularUploads = updatedPopular,
            notes = updatedNotes,
            examPrep = updatedExamPrep,
            assignments = updatedAssignments,
            videos = updatedVideos,
            discoverItems = updatedDiscover
        )
    }

    override fun onCleared() {
        super.onCleared()
        fetchJob?.cancel()
        upvoteRepository.observeVisibleDocuments("Explore", emptyList())
    }
}
