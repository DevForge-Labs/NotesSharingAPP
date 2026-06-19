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

    private var isFirstLoad = true
    private val startupStartTime = System.currentTimeMillis()

    private val _uiState = MutableStateFlow<ExploreUiState>(
        cacheRepository.getCache()?.let { ExploreUiState.Success(it) } ?: ExploreUiState.Loading
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
                val branch = profile?.branch ?: "Computer Science"
                val semester = profile?.semester ?: "Semester 4"

                val branchCode = com.pravor.notessharing.model.AcademicCatalog.getDisplayBranch(branch)

                val semNum = semester.filter { it.isDigit() }

                val key = when {
                    semester.trim().lowercase(java.util.Locale.ROOT).contains("semester 1") || semester.trim().lowercase(java.util.Locale.ROOT).contains("sem 1") || semester.trim() == "1" || semester.trim().lowercase(java.util.Locale.ROOT).startsWith("1st") -> "GROUP_A"
                    semester.trim().lowercase(java.util.Locale.ROOT).contains("semester 2") || semester.trim().lowercase(java.util.Locale.ROOT).contains("sem 2") || semester.trim() == "2" || semester.trim().lowercase(java.util.Locale.ROOT).startsWith("2nd") -> "GROUP_B"
                    branchCode.isNotBlank() && semNum.isNotEmpty() -> "${branchCode}_$semNum"
                    else -> null
                }

                if (key != null) {
                    val firestoreQueryStartTime = System.currentTimeMillis()
                    android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query START collection=app_config/subject_catalog thread=${Thread.currentThread().name}")
                    val snapshot = firestore.collection("app_config")
                        .document("subject_catalog")
                        .get()
                        .await()
                    val firestoreQueryDuration = System.currentTimeMillis() - firestoreQueryStartTime
                    android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query END collection=app_config/subject_catalog duration=${firestoreQueryDuration}ms docs=${if (snapshot.exists()) 1 else 0} thread=${Thread.currentThread().name}")
                    if (snapshot.exists()) {
                        val catalogData = snapshot.data?.get(key)
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
                }
            } catch (e: Exception) {
                // Ignore and keep defaults
            }
        }
    }

    init {
        android.util.Log.d("PERF", "[PERF] Explore startup START thread=${Thread.currentThread().name}")
        val cached = cacheRepository.getCache()
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
                        val updatedTrending = current.content.trendingNotes.map { note ->
                            note.copy(isBookmarked = bookmarkedIds.contains(note.id))
                        }
                        val updatedVideos = current.content.videoRecommendations.map { video ->
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
                            trendingNotes = updatedTrending,
                            videoRecommendations = updatedVideos
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
                        val updatedTrending = current.content.trendingNotes.map { note ->
                            val isUpvoted = upvotesMap[note.id] ?: false
                            val count = upvoteCountsMap[note.id] ?: note.upvotes
                            note.copy(isUpvoted = isUpvoted, upvotes = count)
                        }
                        val updatedVideos = current.content.videoRecommendations.map { video ->
                            val isUpvoted = upvotesMap[video.id] ?: false
                            val count = upvoteCountsMap[video.id] ?: video.upvotes
                            video.copy(isUpvoted = isUpvoted, upvotes = count)
                        }
                        ExploreUiState.Success(current.content.copy(
                            popularUploads = updatedPopular,
                            trendingNotes = updatedTrending,
                            videoRecommendations = updatedVideos
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
                        val updatedTrending = current.content.trendingNotes.map { note ->
                            val count = downloadCountsMap[note.id] ?: note.downloadsCount
                            note.copy(downloadsCount = count)
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
                            trendingNotes = updatedTrending,
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
                    
                    for (note in content.trendingNotes) {
                        val col = upvoteRepository.getCollectionForDocType(note.documentType)
                        paths.add(note.id to col)
                    }
                    for (video in content.videoRecommendations) {
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
            android.util.Log.d("PERF", "[PERF] Explore load START thread=${Thread.currentThread().name}")
            try {
                if (!silent && !isPullToRefresh && cacheRepository.getCache() == null) {
                    _uiState.value = ExploreUiState.Loading
                }

                val collections = listOf("documents", "notes", "pyqs", "assignments", "cheatsheets", "videos")
                val allDocs = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val deferreds = collections.map { col ->
                        async {
                            try {
                                val firestoreQueryStartTime = System.currentTimeMillis()
                                android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query START collection=$col thread=${Thread.currentThread().name}")
                                val documents = firestore.collection(col).get().await().documents
                                val firestoreQueryDuration = System.currentTimeMillis() - firestoreQueryStartTime
                                android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query END collection=$col duration=${firestoreQueryDuration}ms docs=${documents.size} thread=${Thread.currentThread().name}")
                                documents
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }
                    deferreds.awaitAll().flatten()
                }.sortedWith(
                    compareByDescending<com.google.firebase.firestore.DocumentSnapshot> { doc ->
                        (doc.data?.get("trendingScore") as? Number)?.toDouble() ?: 0.0
                    }.thenByDescending { doc ->
                        doc.getLong("uploadedAt") ?: 0L
                    }
                )

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
                    val displaySubjectVal = data["displaySubject"] as? String
                    val downloadsCount = (data["downloadsCount"] as? Long ?: 0L).toInt()
                    val upvotes = (data["upvotes"] as? Long ?: data["likesCount"] as? Long ?: 0L).toInt()
                    val thumbnailUrl = (data["thumbnailUrl"] as? String)?.ifBlank { null }
                        ?: (data["youtubeThumbnailUrl"] as? String)?.ifBlank { null }
                    val thumbnailGenerated = data["thumbnailGenerated"] as? Boolean
                    val thumbnailType = data["thumbnailType"] as? String
                    val documentTypeField = data["documentType"] as? String
                    val typeField = data["type"] as? String
                    val examYearVal = data["examYear"] as? String
                    val branchVal = data["branch"] as? String ?: ""

                    val resolvedIsUpvoted = com.pravor.notessharing.upvotes.UpvoteRepository.upvotesFlow.value[id] ?: false
                    val resolvedUpvotes = com.pravor.notessharing.upvotes.UpvoteRepository.upvoteCountsFlow.value[id] ?: upvotes
                    val trendingScore = (data["trendingScore"] as? Number)?.toDouble() ?: 0.0

                    com.pravor.notessharing.model.TrendingNote(
                        id = id,
                        title = title,
                        subject = subject,
                        downloadsCount = downloadsCount,
                        rating = 4.5,
                        upvotes = resolvedUpvotes,
                        isBookmarked = bookmarkedIds.contains(id),
                        thumbnailUrl = thumbnailUrl,
                        thumbnailGenerated = thumbnailGenerated,
                        thumbnailType = thumbnailType,
                        documentType = documentTypeField ?: "",
                        type = typeField,
                        examYear = examYearVal,
                        isUpvoted = resolvedIsUpvoted,
                        branch = branchVal,
                        trendingScore = trendingScore,
                        displaySubject = displaySubjectVal
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

                    val resolvedIsUpvoted = com.pravor.notessharing.upvotes.UpvoteRepository.upvotesFlow.value[id] ?: false
                    val resolvedUpvotes = com.pravor.notessharing.upvotes.UpvoteRepository.upvoteCountsFlow.value[id] ?: upvotes
                    val resolvedIsBookmarked = bookmarkedIds.contains(id)

                    com.pravor.notessharing.model.VideoRecommendation(
                        id = id,
                        title = title,
                        channelName = uploaderName,
                        duration = "",
                        subject = subject,
                        youtubeVideoId = youtubeVideoId,
                        upvotes = resolvedUpvotes,
                        bookmarks = bookmarks,
                        thumbnailUrl = thumbnailUrlVal,
                        youtubeThumbnailUrl = youtubeThumbnailUrlVal,
                        documentType = docType,
                        semester = semesterVal,
                        youtubeUrl = youtubeUrl,
                        isUpvoted = resolvedIsUpvoted,
                        isBookmarked = resolvedIsBookmarked
                    )
                }

                val realDiscover = allDocs.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val id = data["documentId"] as? String ?: ""
                    val title = data["title"] as? String ?: ""
                    val subject = data["subject"] as? String ?: ""
                    val downloadsCount = (data["downloadsCount"] as? Long ?: 0L).toInt()
                    DiscoverFeedItem.Note(
                        id = id,
                        title = title,
                        subject = subject,
                        downloadsCount = downloadsCount
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

                val duration = System.currentTimeMillis() - startTime
                android.util.Log.d("PERF", "[PERF] Explore load END - duration=$duration ms thread=${Thread.currentThread().name}")
                android.util.Log.d("PERF", "[PERF] Explore documents loaded=${allDocs.size}")
                if (isFirstLoad) {
                    isFirstLoad = false
                    val startupDuration = System.currentTimeMillis() - startupStartTime
                    android.util.Log.d("PERF", "[PERF] Explore startup END duration=${startupDuration}ms thread=${Thread.currentThread().name}")
                }
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
                val duration = System.currentTimeMillis() - startTime
                android.util.Log.d("PERF", "[PERF] Explore load END - duration=$duration ms thread=${Thread.currentThread().name}")
                android.util.Log.d("PERF", "[PERF] Explore documents loaded=0")
                if (isFirstLoad) {
                    isFirstLoad = false
                    val startupDuration = System.currentTimeMillis() - startupStartTime
                    android.util.Log.d("PERF", "[PERF] Explore startup END duration=${startupDuration}ms thread=${Thread.currentThread().name}")
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
        
        val uploadedAt = doc["uploadedAt"] as? Long ?: (doc["uploadTimestamp"] as? Long ?: System.currentTimeMillis())
        val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
        val uploadDate = sdf.format(java.util.Date(uploadedAt))
        
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
        val downloadsCount = (doc["downloadsCount"] as? Long ?: 0L).toInt()
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
        val examTypeField = doc["examType"] as? String
        val sectionField = doc["section"] as? String
        val sectionDisplayField = doc["sectionDisplay"] as? String

        val upvotedMap = com.pravor.notessharing.upvotes.UpvoteRepository.upvotesFlow.value
        val upvoteCountsMap = com.pravor.notessharing.upvotes.UpvoteRepository.upvoteCountsFlow.value
        val resolvedIsUpvoted = upvotedMap[id] ?: false
        val resolvedUpvotes = upvoteCountsMap[id] ?: upvotes

        return FeedItem(
            id = id,
            uploaderName = uploaderName,
            uploaderInitials = initials,
            uploadDate = uploadDate,
            title = displayTitle,
            description = description,
            tags = tags,
            fileType = fileType,
            upvotes = resolvedUpvotes,
            comments = 0,
            downloadsCount = downloadsCount,
            isUpvoted = resolvedIsUpvoted,
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
            examType = examTypeField,
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
