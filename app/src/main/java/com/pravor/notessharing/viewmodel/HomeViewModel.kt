package com.pravor.notessharing.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.data.RecentlyOpenedRepository
import com.pravor.notessharing.model.Category
import com.pravor.notessharing.model.FeedItem
import com.pravor.notessharing.model.FileType
import com.pravor.notessharing.state.HomeContent
import com.pravor.notessharing.state.HomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val recentlyOpenedRepository = RecentlyOpenedRepository(application)
    private val notificationRepository = com.pravor.notessharing.data.NotificationRepository()
    val notifications = notificationRepository.notifications
    val unreadNotificationsCount = notificationRepository.unreadCount

    private val _uiState = MutableStateFlow<HomeUiState>(
        HomeUiState.Success(
            HomeContent(
                selectedCategory = Category.Notes,
                categories = DummyData.categories,
                feedItems = emptyList(),
                recentlyOpened = null,
                isLoadingFeed = true
            )
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _uploadsCount = MutableStateFlow(0)
    val uploadsCount: StateFlow<Int> = _uploadsCount.asStateFlow()
 
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    private val userService = com.pravor.notessharing.firebase.FirestoreUserService()
    private var profileJob: kotlinx.coroutines.Job? = null
    private val bookmarkRepository = com.pravor.notessharing.bookmarks.BookmarkRepository()
    private val upvoteRepository = com.pravor.notessharing.upvotes.UpvoteRepository()

    private var lastReloadCause = "InitialLoad"
    private var isFirstLoad = true
    private val startupStartTime = System.currentTimeMillis()

    private var previousProfile: com.pravor.notessharing.model.Profile? = null

    private fun getChangedFields(old: com.pravor.notessharing.model.Profile?, new: com.pravor.notessharing.model.Profile?): List<String> {
        if (old == null || new == null) return listOf("all")
        val changes = mutableListOf<String>()
        if (old.uid != new.uid) changes.add("uid")
        if (old.name != new.name) changes.add("name")
        if (old.email != new.email) changes.add("email")
        if (old.semester != new.semester) changes.add("semester")
        if (old.profileImageUrl != new.profileImageUrl) changes.add("profileImageUrl")
        if (old.role != new.role) changes.add("role")
        if (old.uploads != new.uploads) changes.add("uploads")
        if (old.bookmarks != new.bookmarks) changes.add("bookmarks")
        if (old.upvotes != new.upvotes) changes.add("upvotes")
        if (old.notesUploaded != new.notesUploaded) changes.add("notesUploaded")
        if (old.contributorLevel != new.contributorLevel) changes.add("contributorLevel")
        if (old.branch != new.branch) changes.add("branch")
        if (old.pyqUploads != new.pyqUploads) changes.add("pyqUploads")
        if (old.notesUploads != new.notesUploads) changes.add("notesUploads")
        if (old.assignmentUploads != new.assignmentUploads) changes.add("assignmentUploads")
        if (old.cheatSheetUploads != new.cheatSheetUploads) changes.add("cheatSheetUploads")
        if (old.youtubeUploads != new.youtubeUploads) changes.add("youtubeUploads")
        return changes
    }

    private val authListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            lastReloadCause = "InitialLoad"
            startObservingProfile(uid)
            notificationRepository.startObserving(uid)
        } else {
            lastReloadCause = "InitialLoad"
            profileJob?.cancel()
            _uploadsCount.value = 0
            loadRealDocuments(null)
            notificationRepository.stopObserving()
        }
    }

    init {
        android.util.Log.d("PERF", "[PERF] Home startup START thread=${Thread.currentThread().name}")
        observeUserProfileState()
        refreshRecentlyOpened()

        viewModelScope.launch {
            com.pravor.notessharing.bookmarks.BookmarkRepository.bookmarksFlow.collect { bookmarks ->
                val bookmarkedIds = bookmarks.map { it.id }.toSet()
                _uiState.update { current ->
                    if (current is HomeUiState.Success) {
                        val updatedFeed = current.content.feedItems.map { item ->
                            item.copy(isSaved = bookmarkedIds.contains(item.id))
                        }
                        current.copy(content = current.content.copy(feedItems = updatedFeed))
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
                    if (current is HomeUiState.Success) {
                        val updatedFeed = current.content.feedItems.map { item ->
                            val isUpvoted = upvotesMap[item.id] ?: false
                            val count = upvoteCountsMap[item.id] ?: item.upvotes
                            item.copy(isUpvoted = isUpvoted, upvotes = count)
                        }
                        val updatedRecentlyOpened = current.content.recentlyOpened?.let { item ->
                            val isUpvoted = upvotesMap[item.id] ?: false
                            val count = upvoteCountsMap[item.id] ?: item.upvotes
                            item.copy(isUpvoted = isUpvoted, upvotes = count)
                        }
                        current.copy(content = current.content.copy(
                            feedItems = updatedFeed,
                            recentlyOpened = updatedRecentlyOpened
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
                    if (current is HomeUiState.Success) {
                        val updatedFeed = current.content.feedItems.map { item ->
                            val count = downloadCountsMap[item.id] ?: item.downloadsCount
                            item.copy(downloadsCount = count)
                        }
                        val updatedRecentlyOpened = current.content.recentlyOpened?.let { item ->
                            val count = downloadCountsMap[item.id] ?: item.downloadsCount
                            item.copy(downloadsCount = count)
                        }
                        current.copy(content = current.content.copy(
                            feedItems = updatedFeed,
                            recentlyOpened = updatedRecentlyOpened
                        ))
                    } else {
                        current
                    }
                }
            }
        }

        viewModelScope.launch {
            _uiState.collect { state ->
                if (state is HomeUiState.Success) {
                    val feedItems = state.content.feedItems
                    val recentlyOpened = state.content.recentlyOpened
                    val paths = mutableListOf<Pair<String, String>>()
                    for (item in feedItems) {
                        val col = upvoteRepository.getCollectionForDocType(item.documentType ?: item.fileType.label)
                        paths.add(item.id to col)
                    }
                    if (recentlyOpened != null) {
                        val col = upvoteRepository.getCollectionForDocType(recentlyOpened.documentType ?: recentlyOpened.fileType.label)
                        paths.add(recentlyOpened.id to col)
                    }
                    upvoteRepository.observeVisibleDocuments("Home", paths)
                }
            }
        }
    }

    private fun observeUserProfileState() {
        viewModelScope.launch {
            val currentUid = auth.currentUser?.uid
            if (currentUid != null) {
                startObservingProfile(currentUid)
            }
            auth.addAuthStateListener(authListener)
        }
    }

    private fun startObservingProfile(uid: String) {
        profileJob?.cancel()
        notificationRepository.startObserving(uid)
        profileJob = viewModelScope.launch {
            userService.observeUserProfile(uid).collect { profile ->
                android.util.Log.d("PERF", "[PERF] Profile update received")
                val changedFields = getChangedFields(previousProfile, profile)
                android.util.Log.d("PERF", "[PERF] Changed fields=$changedFields")
                previousProfile = profile
                _uploadsCount.value = profile?.uploads ?: 0

                val semester = profile?.semester
                lastReloadCause = "ProfileUpdate"
                loadRealDocuments(semester)
            }
        }
    }

    fun refreshRecentlyOpened() {
        val lastOpened = recentlyOpenedRepository.getLastOpened()
        _uiState.update { current ->
            if (current is HomeUiState.Success) {
                current.copy(content = current.content.copy(recentlyOpened = lastOpened))
            } else {
                HomeUiState.Success(
                    HomeContent(
                        selectedCategory = Category.Notes,
                        categories = DummyData.categories,
                        feedItems = emptyList(),
                        recentlyOpened = lastOpened,
                        isLoadingFeed = true
                    )
                )
            }
        }

        if (lastOpened != null && lastOpened.fileType != FileType.Video) {
            viewModelScope.launch {
                val db = com.pravor.notessharing.data.download.DownloadDataStoreManager(getApplication())
                val isDownloaded = db.isDocumentDownloaded(lastOpened.id)
                var localFileExists = false
                if (isDownloaded) {
                    val attachments = db.getDownloadedAttachments().filter { it.documentId == lastOpened.id }
                    localFileExists = attachments.isNotEmpty() && attachments.all { java.io.File(it.localPath).exists() }
                }

                if (!localFileExists) {
                    val existsOnServer = checkDocumentExistsInFirestore(lastOpened.id)
                    if (!existsOnServer) {
                        recentlyOpenedRepository.clearLastOpened()
                        val continueRepo = com.pravor.notessharing.data.ContinueLearningRepository(getApplication())
                        continueRepo.clearLastOpened()

                        if (isDownloaded) {
                            db.removeDownload(lastOpened.id)
                        }

                        _uiState.update { current ->
                            if (current is HomeUiState.Success) {
                                current.copy(content = current.content.copy(recentlyOpened = null))
                            } else {
                                current
                            }
                        }
                    }
                }
            }
        }

        lastReloadCause = if (isFirstLoad) "InitialLoad" else "NavigationReturn"
        loadRealDocuments()
    }

    fun loadRealDocuments(forcedSemester: String? = null, isPullToRefresh: Boolean = false) {
        val stackTrace = Throwable().stackTrace
        val caller = stackTrace.getOrNull(1)?.let { "${it.className}.${it.methodName}:${it.lineNumber}" } ?: "unknown"
        val stackSource = stackTrace.drop(1).take(5).joinToString(" -> ") { "${it.className}.${it.methodName}:${it.lineNumber}" }

        viewModelScope.launch {
            if (isPullToRefresh) {
                _isRefreshing.value = true
            }
            try {
                val startTime = System.currentTimeMillis()
                android.util.Log.d("PERF", "[PERF] Home feed load START thread=${Thread.currentThread().name}")
                android.util.Log.d("PERF", "[PERF] Feed reload trigger source=$lastReloadCause")
                try {
                    val semester = forcedSemester ?: run {
                        val currentUid = auth.currentUser?.uid
                        if (currentUid != null) {
                            userService.getUserProfile(currentUid)?.semester
                        } else {
                            null
                        }
                    }

                    val isNecessary = when (lastReloadCause) {
                        "NavigationReturn" -> {
                            val currentState = _uiState.value
                            val hasFeedItems = currentState is HomeUiState.Success && currentState.content.feedItems.isNotEmpty()
                            !hasFeedItems
                        }
                        else -> true
                    }

                    android.util.Log.d("PERF", "[PERF] Home reload caller=$caller")
                    android.util.Log.d("PERF", "[PERF] Home reload reason=lastReloadCause=$lastReloadCause, forcedSemester=$semester, necessary=$isNecessary")
                    android.util.Log.d("PERF", "[PERF] Home reload stackSource=$stackSource")

                    if (lastReloadCause == "NavigationReturn" && !isNecessary) {
                        android.util.Log.d("PERF", "[PERF] Home reload skipped reason=NavigationReturn unnecessary=true")
                        _uiState.update { current ->
                            if (current is HomeUiState.Success) {
                                current.copy(content = current.content.copy(isLoadingFeed = false))
                            } else {
                                current
                            }
                        }
                        return@launch
                    }

                    val hasSemester = !semester.isNullOrBlank() && semester != "Not Set"

                    val collections = listOf("documents", "notes", "pyqs", "assignments", "cheatsheets")
                    val allDocs = coroutineScope {
                        val deferreds = collections.map { col ->
                            async {
                                try {
                                    val firestoreQueryStartTime = System.currentTimeMillis()
                                    android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query START collection=$col thread=${Thread.currentThread().name}")
                                    val documents = if (hasSemester) {
                                        firestore.collection(col)
                                            .whereEqualTo("semester", semester)
                                            .get()
                                            .await()
                                            .documents
                                    } else {
                                        firestore.collection(col)
                                            .get()
                                            .await()
                                            .documents
                                    }
                                    val firestoreQueryDuration = System.currentTimeMillis() - firestoreQueryStartTime
                                    android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query END collection=$col duration=${firestoreQueryDuration}ms docs=${documents.size} thread=${Thread.currentThread().name}")
                                    documents
                                } catch (e: Exception) {
                                    emptyList()
                                }
                            }
                        }
                        deferreds.awaitAll().flatten()
                    }
                    
                    val currentUid = auth.currentUser?.uid
                    if (currentUid != null) {
                        bookmarkRepository.loadInitialBookmarksIfNeeded(currentUid)
                        upvoteRepository.loadInitialUpvotesIfNeeded(currentUid)
                    }
                    val bookmarkedIds = com.pravor.notessharing.bookmarks.BookmarkRepository.bookmarksFlow.value.map { it.id }.toSet()
                    
                    val feedAssemblyStartTime = System.currentTimeMillis()
                    android.util.Log.d("PERF", "[PERF] MainThreadWork START operation=Feed assembly thread=${Thread.currentThread().name}")
                    val realItems = allDocs.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        val item = documentToFeedItem(data)
                        val timestamp = data["uploadedAt"] as? Long ?: 0L
                        item to timestamp
                    }.sortedWith(
                        compareByDescending<Pair<FeedItem, Long>> { it.first.upvotes }
                            .thenByDescending { it.second }
                    ).map { it.first }
                    
                    val mergedFeedItems = if (hasSemester) {
                        realItems.distinctBy { it.id }
                    } else {
                        (realItems + DummyData.feedItems).distinctBy { it.id }
                    }
                    
                    val finalFeedItems = mergedFeedItems.map { item ->
                        item.copy(isSaved = bookmarkedIds.contains(item.id))
                    }
                    val feedAssemblyDuration = System.currentTimeMillis() - feedAssemblyStartTime
                    android.util.Log.d("PERF", "[PERF] MainThreadWork END operation=Feed assembly duration=${feedAssemblyDuration}ms thread=${Thread.currentThread().name}")
                    
                    _uiState.update { current ->
                        val lastOpened = recentlyOpenedRepository.getLastOpened()
                        if (current is HomeUiState.Success) {
                            current.copy(content = current.content.copy(
                                feedItems = finalFeedItems,
                                recentlyOpened = lastOpened,
                                isLoadingFeed = false
                            ))
                        } else {
                            HomeUiState.Success(
                                HomeContent(
                                    selectedCategory = Category.Notes,
                                    categories = DummyData.categories,
                                    feedItems = finalFeedItems,
                                    recentlyOpened = lastOpened,
                                    isLoadingFeed = false
                                )
                            )
                        }
                    }
                    
                    val duration = System.currentTimeMillis() - startTime
                    android.util.Log.d("PERF", "[PERF] Home feed load END - duration=$duration ms thread=${Thread.currentThread().name}")
                    android.util.Log.d("PERF", "[PERF] Feed item count=${finalFeedItems.size}")
                    if (isFirstLoad) {
                        isFirstLoad = false
                        val startupDuration = System.currentTimeMillis() - startupStartTime
                        android.util.Log.d("PERF", "[PERF] Home startup END duration=${startupDuration}ms thread=${Thread.currentThread().name}")
                    }
                } catch (e: Exception) {
                    val semester = forcedSemester ?: run {
                        val currentUid = auth.currentUser?.uid
                        if (currentUid != null) {
                            userService.getUserProfile(currentUid)?.semester
                        } else {
                            null
                        }
                    }
                    val hasSemester = !semester.isNullOrBlank() && semester != "Not Set"
                    
                    val currentUid = auth.currentUser?.uid
                    if (currentUid != null) {
                        bookmarkRepository.loadInitialBookmarksIfNeeded(currentUid)
                        upvoteRepository.loadInitialUpvotesIfNeeded(currentUid)
                    }
                    val bookmarkedIds = com.pravor.notessharing.bookmarks.BookmarkRepository.bookmarksFlow.value.map { it.id }.toSet()
                    
                    _uiState.update { current ->
                        val lastOpened = recentlyOpenedRepository.getLastOpened()
                        val fallbackItems = if (hasSemester) emptyList() else DummyData.feedItems.map { item ->
                            item.copy(isSaved = bookmarkedIds.contains(item.id))
                        }
                        if (current is HomeUiState.Success) {
                            current.copy(content = current.content.copy(
                                feedItems = fallbackItems,
                                recentlyOpened = lastOpened,
                                isLoadingFeed = false
                            ))
                        } else {
                            HomeUiState.Success(
                                HomeContent(
                                    selectedCategory = Category.Notes,
                                    categories = DummyData.categories,
                                    feedItems = fallbackItems,
                                    recentlyOpened = lastOpened,
                                    isLoadingFeed = false
                                )
                            )
                        }
                    }

                    val duration = System.currentTimeMillis() - startTime
                    android.util.Log.d("PERF", "[PERF] Home feed load END - duration=$duration ms thread=${Thread.currentThread().name}")
                    val count = (_uiState.value as? HomeUiState.Success)?.content?.feedItems?.size ?: 0
                    android.util.Log.d("PERF", "[PERF] Feed item count=$count")
                    if (isFirstLoad) {
                        isFirstLoad = false
                        val startupDuration = System.currentTimeMillis() - startupStartTime
                        android.util.Log.d("PERF", "[PERF] Home startup END duration=${startupDuration}ms thread=${Thread.currentThread().name}")
                    }
                }
            } finally {
                if (isPullToRefresh) {
                    _isRefreshing.value = false
                }
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
        
        val rawDisplaySubject = doc["displaySubject"] as? String
        val rawSubject = doc["subject"] as? String ?: ""
        val subject = if (!rawDisplaySubject.isNullOrBlank()) rawDisplaySubject else rawSubject
        val displayTitle = doc["title"] as? String ?: ""

        val description = doc["description"] as? String ?: ""
        val tags = (doc["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        
        val upvotes = (doc["upvotes"] as? Long ?: (doc["likesCount"] as? Long ?: 0L)).toInt()
        val downloadsCount = (doc["downloadsCount"] as? Long ?: (doc["downloads"] as? Long ?: 0L)).toInt()
        val bookmarks = (doc["bookmarks"] as? Long ?: 0L).toInt()

        val youtubeUrl = doc["youtubeUrl"] as? String
        val youtubeVideoId = doc["youtubeVideoId"] as? String
        
        val thumbnailUrl = doc["thumbnailUrl"] as? String
        val youtubeThumbnailUrl = doc["youtubeThumbnailUrl"] as? String
        val thumbnailGenerated = doc["thumbnailGenerated"] as? Boolean
        val thumbnailType = doc["thumbnailType"] as? String

        val documentTypeField = doc["documentType"] as? String
        val typeField = doc["type"] as? String
        val subjectField = subject
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
            sectionDisplay = sectionDisplayField,
            youtubeThumbnailUrl = youtubeThumbnailUrl
        )
    }

    fun selectCategory(category: Category) {
        _uiState.update { current ->
            if (current is HomeUiState.Success) {
                current.copy(content = current.content.copy(selectedCategory = category))
            } else {
                current
            }
        }
    }

    fun toggleUpvote(itemId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val feedItems = (_uiState.value as? HomeUiState.Success)?.content?.feedItems ?: emptyList()
        val item = feedItems.find { it.id == itemId } ?: (_uiState.value as? HomeUiState.Success)?.content?.recentlyOpened?.takeIf { it.id == itemId } ?: return
        
        val collection = when (item.documentType?.lowercase(java.util.Locale.US) ?: item.fileType.label.lowercase(java.util.Locale.US)) {
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
                currentUpvotes = item.upvotes,
                userId = currentUid
            )
        }
    }

    fun toggleSaved(itemId: String) {
        var feedItemToPersist: FeedItem? = null
        _uiState.update { current ->
            if (current is HomeUiState.Success) {
                current.copy(
                    content = current.content.copy(
                        feedItems = current.content.feedItems.map { item ->
                            if (item.id == itemId) {
                                val updated = item.copy(isSaved = !item.isSaved)
                                feedItemToPersist = updated
                                updated
                            } else item
                        }
                    )
                )
            } else {
                current
            }
        }

        val currentUid = auth.currentUser?.uid
        val item = feedItemToPersist
        if (currentUid != null && item != null) {
            viewModelScope.launch {
                if (item.isSaved) {
                    bookmarkRepository.addBookmark(item, currentUid)
                } else {
                    bookmarkRepository.removeBookmark(itemId, currentUid)
                }
            }
        }
    }

    private fun updateFeed(
        itemId: String,
        transform: (FeedItem) -> FeedItem
    ) {
        _uiState.update { current ->
            if (current is HomeUiState.Success) {
                current.copy(
                    content = current.content.copy(
                        feedItems = current.content.feedItems.map { item ->
                            if (item.id == itemId) transform(item) else item
                        }
                    )
                )
            } else {
                current
            }
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            notificationRepository.markAsRead(uid, notificationId)
        }
    }

    fun markAllNotificationsAsRead() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            notificationRepository.markAllAsRead(uid)
        }
    }

    fun deleteNotification(notificationId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            notificationRepository.delete(uid, notificationId)
        }
    }

    fun clearAllNotifications() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            notificationRepository.clearAll(uid)
        }
    }

    private suspend fun checkDocumentExistsInFirestore(documentId: String): Boolean {
        val collections = listOf("documents", "notes", "pyqs", "assignments", "cheatsheets")
        val firestore = FirebaseFirestore.getInstance()
        for (col in collections) {
            try {
                val snap = firestore.collection(col).document(documentId).get(com.google.firebase.firestore.Source.CACHE).await()
                if (snap.exists()) return true
            } catch (e: Exception) {
                try {
                    val snap = firestore.collection(col).document(documentId).get().await()
                    if (snap.exists()) return true
                } catch (e2: Exception) {
                    // Try next collection
                }
            }
        }
        return false
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        profileJob?.cancel()
        upvoteRepository.observeVisibleDocuments("Home", emptyList())
        notificationRepository.stopObserving()
    }
}
