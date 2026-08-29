package com.pravor.notessharing.ui.features.home

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import com.pravor.notessharing.data.repository.BookmarkRepository

import com.pravor.notessharing.data.local.preferences.*

import com.pravor.notessharing.domain.model.*
import com.pravor.notessharing.data.repository.*
import com.pravor.notessharing.core.util.*

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.data.repository.RecentlyOpenedRepository
import com.pravor.notessharing.domain.model.Category
import com.pravor.notessharing.domain.model.FeedItem
import com.pravor.notessharing.domain.model.FileType
import com.pravor.notessharing.ui.common.HomeContent
import com.pravor.notessharing.ui.common.HomeUiState
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val recentlyOpenedRepository = RecentlyOpenedRepository(application)
    private val profileRepository = com.pravor.notessharing.data.repository.ProfileRepository(application)
    private val homeFeedRepository = com.pravor.notessharing.data.repository.HomeFeedRepository(application)
    private var feedObservationJob: kotlinx.coroutines.Job? = null
    private val notificationRepository = com.pravor.notessharing.data.repository.NotificationRepository()
    private val metadataRepository = com.pravor.notessharing.data.repository.MetadataRepository()
    val notifications = notificationRepository.notifications
    val unreadNotificationsCount = notificationRepository.unreadCount

    private val _uiState = MutableStateFlow<HomeUiState>(
        HomeUiState.Success(
            HomeContent(
                selectedCategory = Category.Notes,
                categories = Category.entries,
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
    private val userService = com.pravor.notessharing.core.firebase.FirestoreUserService()
    private var profileJob: kotlinx.coroutines.Job? = null
    private val bookmarkRepository = com.pravor.notessharing.data.repository.BookmarkRepository()
    private val upvoteRepository = com.pravor.notessharing.data.repository.UpvoteRepository()

    private var lastReloadCause = "InitialLoad"
    private var isFirstLoad = true
    private var hasLoadedInitialFirestoreFeed = false
    private val startupStartTime = System.currentTimeMillis()

    private var previousProfile: com.pravor.notessharing.domain.model.Profile? = null

    private fun getChangedFields(old: com.pravor.notessharing.domain.model.Profile?, new: com.pravor.notessharing.domain.model.Profile?): List<String> {
        if (old == null || new == null) return listOf("all")
        val changes = mutableListOf<String>()
        if (old.uid != new.uid) changes.add("uid")
        if (old.name != new.name) changes.add("name")
        if (old.email != new.email) changes.add("email")
        if (old.semester != new.semester) changes.add("semester")
        if (old.profileImageUrl != new.profileImageUrl) changes.add("profileImageUrl")
        if (old.role != new.role) changes.add("role")
        if (old.totalUploads != new.totalUploads) changes.add("totalUploads")
        if (old.bookmarks != new.bookmarks) changes.add("bookmarks")
        if (old.upvotes != new.upvotes) changes.add("upvotes")
        if (old.contributorLevel != new.contributorLevel) changes.add("contributorLevel")
        if (old.branch != new.branch) changes.add("branch")
        if (old.pyqUploads != new.pyqUploads) changes.add("pyqUploads")
        if (old.notesUploads != new.notesUploads) changes.add("notesUploads")
        if (old.assignmentUploads != new.assignmentUploads) changes.add("assignmentUploads")
        if (old.cheatSheetUploads != new.cheatSheetUploads) changes.add("cheatSheetUploads")
        if (old.youtubeResourceUploads != new.youtubeResourceUploads) changes.add("youtubeResourceUploads")
        return changes
    }

    private var activeLoadJob: kotlinx.coroutines.Job? = null
    private var activeLoadKey: String? = null

    private val authListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
        val uid = firebaseAuth.currentUser?.uid
        
        // Immediately refresh recently opened state to avoid any stale data leakage across accounts
        val lastOpened = recentlyOpenedRepository.getLastOpened()
        _uiState.update { current ->
            if (current is HomeUiState.Success) {
                current.copy(content = current.content.copy(recentlyOpened = lastOpened))
            } else {
                current
            }
        }

        if (uid != null) {
            startObservingProfile(uid)
            notificationRepository.startObserving(uid)
        } else {
            profileJob?.cancel()
            _uploadsCount.value = 0
            notificationRepository.stopObserving()
        }
    }

    init {
        android.util.Log.d("PERF", "[PERF] Home startup START thread=${Thread.currentThread().name}")
        loadCachedRoomFeedFirst()
        observeUserProfileState()
        refreshRecentlyOpened()

        viewModelScope.launch {
            com.pravor.notessharing.data.repository.BookmarkRepository.bookmarksFlow.collect { bookmarks ->
                val bookmarkedIds = bookmarks.map { it.id }.toSet()
                _uiState.update { current ->
                    if (current is HomeUiState.Success) {
                        var hasChanges = false
                        val updatedFeed = current.content.feedItems.map { item ->
                            val isSaved = bookmarkedIds.contains(item.id)
                            if (item.isSaved != isSaved) {
                                hasChanges = true
                                item.copy(isSaved = isSaved)
                            } else {
                                item
                            }
                        }
                        if (!hasChanges) {
                            current
                        } else {
                            current.copy(content = current.content.copy(feedItems = updatedFeed))
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
                    if (current is HomeUiState.Success) {
                        var hasChanges = false
                        val updatedFeed = current.content.feedItems.map { item ->
                            val isUpvoted = upvotesMap[item.id] ?: item.isUpvoted
                            val count = upvoteCountsMap[item.id] ?: item.upvotes
                            if (item.isUpvoted != isUpvoted || item.upvotes != count) {
                                hasChanges = true
                                item.copy(isUpvoted = isUpvoted, upvotes = count)
                            } else {
                                item
                            }
                        }
                        val updatedRecentlyOpened = current.content.recentlyOpened?.let { item ->
                            val isUpvoted = upvotesMap[item.id] ?: item.isUpvoted
                            val count = upvoteCountsMap[item.id] ?: item.upvotes
                            if (item.isUpvoted != isUpvoted || item.upvotes != count) {
                                hasChanges = true
                                item.copy(isUpvoted = isUpvoted, upvotes = count)
                            } else {
                                item
                            }
                        }
                        if (!hasChanges) {
                            current
                        } else {
                            current.copy(content = current.content.copy(
                                feedItems = updatedFeed,
                                recentlyOpened = updatedRecentlyOpened
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
                    if (current is HomeUiState.Success) {
                        var hasChanges = false
                        val updatedFeed = current.content.feedItems.map { item ->
                            val count = downloadCountsMap[item.id] ?: item.downloadsCount
                            if (item.downloadsCount != count) {
                                hasChanges = true
                                item.copy(downloadsCount = count)
                            } else {
                                item
                            }
                        }
                        val updatedRecentlyOpened = current.content.recentlyOpened?.let { item ->
                            val count = downloadCountsMap[item.id] ?: item.downloadsCount
                            if (item.downloadsCount != count) {
                                hasChanges = true
                                item.copy(downloadsCount = count)
                            } else {
                                item
                            }
                        }
                        if (!hasChanges) {
                            current
                        } else {
                            current.copy(content = current.content.copy(
                                feedItems = updatedFeed,
                                recentlyOpened = updatedRecentlyOpened
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
                .mapNotNull { (it as? HomeUiState.Success)?.content }
                .map { content ->
                    val list = mutableListOf<Pair<String, String>>()
                    for (item in content.feedItems) {
                        list.add(item.id to (item.documentType ?: item.fileType.label))
                    }
                    content.recentlyOpened?.let { ro ->
                        list.add(ro.id to (ro.documentType ?: ro.fileType.label))
                    }
                    list.toList()
                }
                .distinctUntilChanged()
                .collect { items ->
                    val paths = items.map { (id, docType) ->
                        id to upvoteRepository.getCollectionForDocType(docType)
                    }
                    upvoteRepository.observeVisibleDocuments("Home", paths)
                }
        }
    }

    private fun loadCachedRoomFeedFirst() {
        viewModelScope.launch(Dispatchers.IO) {
            val totalStart = System.currentTimeMillis()
            android.util.Log.d("PERF", "[PERF] Room feed read START thread=${Thread.currentThread().name}")

            // 1. Measure Room DAO query duration directly from SQLite
            val daoQueryStartTime = System.currentTimeMillis()
            android.util.Log.d("PERF", "[PERF] Room DAO query START")
            val currentUid = auth.currentUser?.uid
            val localProfile = if (currentUid != null) {
                try { profileRepository.getLocalProfile(currentUid) } catch (_: Exception) { null }
            } else null

            val initialScopeKey = localProfile?.let {
                val canonicalCollege = com.pravor.notessharing.core.util.LegacyAcademicCompatibilityResolver.resolveCollegeId(it.college)
                val canonicalBranch = it.branch.takeIf { b -> b.isNotBlank() }?.let { b -> com.pravor.notessharing.core.util.LegacyAcademicCompatibilityResolver.resolveBranchId(b) }
                val semDigit = it.semester.filter { c -> c.isDigit() }
                if (canonicalCollege.isNotBlank()) {
                    val parts = mutableListOf(canonicalCollege)
                    if (!canonicalBranch.isNullOrBlank()) parts.add(canonicalBranch)
                    if (semDigit.isNotBlank()) parts.add(semDigit)
                    parts.joinToString("_")
                } else ""
            } ?: ""

            val cachedItems: List<FeedItem> = try {
                if (initialScopeKey.isNotBlank()) {
                    homeFeedRepository.getCachedHomeFeed(initialScopeKey)
                } else {
                    homeFeedRepository.getAllCachedHomeFeed()
                }
            } catch (e: Exception) {
                emptyList()
            }
            val daoQueryDuration = System.currentTimeMillis() - daoQueryStartTime
            android.util.Log.d("PERF", "[PERF] Room DAO query END duration=${daoQueryDuration}ms rows=${cachedItems.size}")

            // 2. Measure Room entity -> domain FeedItem mapping duration
            val mappingStartTime = System.currentTimeMillis()
            android.util.Log.d("PERF", "[PERF] Room -> FeedItem mapping START")
            val domainItems = cachedItems.filter { isEligibleHomeFeedItem(it) }
            val mappingDuration = System.currentTimeMillis() - mappingStartTime
            android.util.Log.d("PERF", "[PERF] Room -> FeedItem mapping END duration=${mappingDuration}ms")

            // 3. Measure Feed assembly duration
            val assemblyStartTime = System.currentTimeMillis()
            android.util.Log.d("PERF", "[PERF] Feed assembly START")
            val lastOpened = recentlyOpenedRepository.getLastOpened()
            val upvotesMap = UpvoteRepository.upvotesFlow.value
            val upvoteCountsMap = UpvoteRepository.upvoteCountsFlow.value
            val downloadCountsMap = UpvoteRepository.downloadCountsFlow.value
            val bookmarkedIds = BookmarkRepository.bookmarksFlow.value.map { it.id }.toSet()

            val assembledItems = domainItems.map { item ->
                item.copy(
                    isSaved = bookmarkedIds.contains(item.id),
                    isUpvoted = upvotesMap[item.id] ?: item.isUpvoted,
                    upvotes = upvoteCountsMap[item.id] ?: item.upvotes,
                    downloadsCount = downloadCountsMap[item.id] ?: item.downloadsCount
                )
            }
            val assemblyDuration = System.currentTimeMillis() - assemblyStartTime
            android.util.Log.d("PERF", "[PERF] Feed assembly END duration=${assemblyDuration}ms")

            val totalDuration = System.currentTimeMillis() - totalStart
            android.util.Log.d("PERF", "[PERF] Room feed read END duration=${totalDuration}ms items=${assembledItems.size}")

            if (assembledItems.isNotEmpty()) {
                _uiState.update { current ->
                    HomeUiState.Success(
                        HomeContent(
                            selectedCategory = Category.Notes,
                            categories = Category.entries,
                            feedItems = assembledItems,
                            recentlyOpened = lastOpened,
                            isLoadingFeed = false
                        )
                    )
                }
                android.util.Log.d("PERF", "[PERF] First feed state emitted items=${assembledItems.size} thread=${Thread.currentThread().name}")
                android.util.Log.d("PERF", "[PERF] First feed visible from Room cache! items=${assembledItems.size} thread=${Thread.currentThread().name}")
            }

            observeRoomFeed(initialScopeKey)
        }
    }

    private fun observeUserProfileState() {
        auth.addAuthStateListener(authListener)
    }

    private fun startObservingProfile(uid: String) {
        profileJob?.cancel()
        notificationRepository.startObserving(uid)
        viewModelScope.launch(Dispatchers.IO) {
            upvoteRepository.loadInitialUpvotesIfNeeded(uid)
        }
        profileJob = viewModelScope.launch {
            profileRepository.observeProfile(uid).collect { profile ->
                val isFirstProfileEmission = previousProfile == null
                val collegeChanged = previousProfile?.college != profile?.college
                val semesterChanged = previousProfile?.semester != profile?.semester
                val branchChanged = previousProfile?.branch != profile?.branch
                val criteriaChanged = collegeChanged || semesterChanged || branchChanged
                previousProfile = profile
                _uploadsCount.value = profile?.totalUploads ?: 0

                val scope = com.pravor.notessharing.core.util.AcademicScopeResolver.resolve(profile, metadataRepository)
                android.util.Log.d("PERF", "[PERF] Profile update received criteriaChanged=$criteriaChanged isFirst=$isFirstProfileEmission hasLoadedFirestore=$hasLoadedInitialFirestoreFeed")

                if (criteriaChanged) {
                    observeRoomFeed(scope.scopeKey)
                    lastReloadCause = "ProfileCriteriaChanged"
                    loadRealDocuments(profile?.semester)
                } else if (isFirstProfileEmission && !hasLoadedInitialFirestoreFeed) {
                    observeRoomFeed(scope.scopeKey)
                    lastReloadCause = "InitialLoad"
                    loadRealDocuments(profile?.semester)
                } else {
                    android.util.Log.d("PERF", "[PERF] Feed reload skipped: profile emission criteria unchanged")
                }
            }
        }
    }

    private fun observeRoomFeed(scopeKey: String) {
        feedObservationJob?.cancel()
        feedObservationJob = viewModelScope.launch {
            homeFeedRepository.observeHomeFeed(scopeKey)
                .distinctUntilChanged()
                .collect { cachedItems ->
                val nonVideoCached = cachedItems.filter { isEligibleHomeFeedItem(it) }
                if (nonVideoCached.isNotEmpty()) {
                    val upvotesMap = com.pravor.notessharing.data.repository.UpvoteRepository.upvotesFlow.value
                    val upvoteCountsMap = com.pravor.notessharing.data.repository.UpvoteRepository.upvoteCountsFlow.value
                    val downloadCountsMap = com.pravor.notessharing.data.repository.UpvoteRepository.downloadCountsFlow.value
                    val bookmarkedIds = com.pravor.notessharing.data.repository.BookmarkRepository.bookmarksFlow.value.map { it.id }.toSet()

                    val updatedCached = nonVideoCached.map { item ->
                        val isUpvoted = upvotesMap[item.id] ?: item.isUpvoted
                        val upvotesCount = upvoteCountsMap[item.id] ?: item.upvotes
                        val downloadsCount = downloadCountsMap[item.id] ?: item.downloadsCount
                        item.copy(
                            isSaved = bookmarkedIds.contains(item.id),
                            isUpvoted = isUpvoted,
                            upvotes = upvotesCount,
                            downloadsCount = downloadsCount
                        )
                    }
                    _uiState.update { current ->
                        if (current is HomeUiState.Success) {
                            if (current.content.feedItems == updatedCached && !current.content.isLoadingFeed) {
                                current
                            } else {
                                current.copy(content = current.content.copy(
                                    feedItems = updatedCached,
                                    isLoadingFeed = false
                                ))
                            }
                        } else {
                            HomeUiState.Success(
                                HomeContent(
                                    selectedCategory = Category.Notes,
                                    categories = Category.entries,
                                    feedItems = updatedCached,
                                    recentlyOpened = null,
                                    isLoadingFeed = false
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun refreshRecentlyOpened() {
        viewModelScope.launch(Dispatchers.IO) {
            val lastOpened = recentlyOpenedRepository.getLastOpened()
            _uiState.update { current ->
                if (current is HomeUiState.Success) {
                    if (current.content.recentlyOpened == lastOpened) {
                        current
                    } else {
                        current.copy(content = current.content.copy(recentlyOpened = lastOpened))
                    }
                } else {
                    current
                }
            }

            if (lastOpened != null && lastOpened.fileType != FileType.Video) {
                val db = com.pravor.notessharing.data.local.preferences.DownloadDataStoreManager(getApplication())
                val isDownloaded = db.isDocumentDownloaded(lastOpened.id)
                var localFileExists = false
                if (isDownloaded) {
                    val attachments = db.getDownloadedAttachments().filter { it.documentId == lastOpened.id }
                    localFileExists = attachments.isNotEmpty() && attachments.all { java.io.File(it.localPath).exists() }
                }

                if (!localFileExists) {
                    val existsOnServer = checkDocumentExistsInFirestore(lastOpened.id, lastOpened.documentType)
                    if (!existsOnServer) {
                        recentlyOpenedRepository.clearLastOpened()
                        val continueRepo = com.pravor.notessharing.data.repository.ContinueLearningRepository(getApplication())
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
    }

    fun loadRealDocuments(forcedSemester: String? = null, isPullToRefresh: Boolean = false) {
        val stackTrace = Throwable().stackTrace
        val caller = stackTrace.getOrNull(1)?.let { "${it.className}.${it.methodName}:${it.lineNumber}" } ?: "unknown"
        val stackSource = stackTrace.drop(1).take(5).joinToString(" -> ") { "${it.className}.${it.methodName}:${it.lineNumber}" }

        if (isPullToRefresh) {
            android.util.Log.d("PullToRefresh", "HomeViewModel refresh started")
            activeLoadJob?.cancel()
        }

        val targetScopeKey = "load_${forcedSemester ?: previousProfile?.semester ?: "default"}"
        if (!isPullToRefresh && activeLoadJob?.isActive == true && activeLoadKey == targetScopeKey) {
            android.util.Log.d("PERF", "[PERF] Home feed load coalesced for key=$targetScopeKey")
            return
        }

        activeLoadKey = targetScopeKey
        activeLoadJob = viewModelScope.launch(Dispatchers.IO) {
            if (isPullToRefresh) {
                _isRefreshing.value = true
            }
            try {
                val startTime = System.currentTimeMillis()
                android.util.Log.d("PERF", "[PERF] Background Firestore refresh START thread=${Thread.currentThread().name}")
                android.util.Log.d("PERF", "[PERF] Feed reload trigger source=$lastReloadCause")
                try {
                    val userProfile = previousProfile ?: run {
                        val currentUid = auth.currentUser?.uid
                        if (currentUid != null) {
                            userService.getUserProfile(currentUid)
                        } else {
                            null
                        }
                    }

                    val semester = forcedSemester ?: userProfile?.semester
                    val rawBranch = userProfile?.branch
                    val rawCollege = userProfile?.college?.takeIf { it.isNotBlank() }

                    if (rawCollege.isNullOrBlank()) {
                        android.util.Log.d("HomeViewModel", "No college found in user profile. Skipping campus content fetch.")
                        _uiState.update { current ->
                            val lastOpened = recentlyOpenedRepository.getLastOpened()
                            if (current is HomeUiState.Success) {
                                current.copy(content = current.content.copy(
                                    feedItems = emptyList(),
                                    recentlyOpened = lastOpened,
                                    isLoadingFeed = false
                                ))
                            } else {
                                HomeUiState.Success(
                                    HomeContent(
                                        selectedCategory = Category.Notes,
                                        categories = Category.entries,
                                        feedItems = emptyList(),
                                        recentlyOpened = lastOpened,
                                        isLoadingFeed = false
                                    )
                                )
                            }
                        }
                        hasLoadedInitialFirestoreFeed = true
                        return@launch
                    }

                    val canonicalCollegeId = com.pravor.notessharing.core.util.LegacyAcademicCompatibilityResolver.resolveCollegeId(rawCollege)

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

                    if (lastReloadCause == "NavigationReturn" && !isNecessary && !isPullToRefresh) {
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
                    val hasBranch = !rawBranch.isNullOrBlank()

                    val subjectIds = if (hasSemester && hasBranch) {
                        try {
                            val branchId = com.pravor.notessharing.core.util.LegacyAcademicCompatibilityResolver.resolveBranchId(rawBranch!!)
                            val catalog = metadataRepository.getSubjectCatalog()
                            val collegeCatalog = catalog[canonicalCollegeId.lowercase()] as? Map<*, *>
                            
                            val semLower = semester!!.trim().lowercase(java.util.Locale.ROOT)
                            val isFirstYear = semLower.contains("semester 1") || semLower.contains("sem 1") || semLower == "1" || semLower.startsWith("1st") ||
                                              semLower.contains("semester 2") || semLower.contains("sem 2") || semLower == "2" || semLower.startsWith("2nd")
                            
                            val semesterData = if (isFirstYear) {
                                val isGroupA = semLower.contains("semester 1") || semLower.contains("sem 1") || semLower == "1" || semLower.startsWith("1st")
                                val groupKey = if (isGroupA) "GROUP_A" else "GROUP_B"
                                collegeCatalog?.get(groupKey)
                            } else {
                                val branchCatalog = collegeCatalog?.get(branchId) as? Map<*, *>
                                val semNum = semester.filter { it.isDigit() }
                                branchCatalog?.get(semester) ?: (if (semNum.isNotEmpty()) branchCatalog?.get(semNum) else null)
                            }

                            when (semesterData) {
                                is List<*> -> semesterData.mapNotNull { item ->
                                  when (item) {
                                      is Map<*, *> -> (item["id"] ?: item["subjectId"])?.toString()
                                      is String -> item
                                      else -> null
                                  }
                                }
                                is Map<*, *> -> semesterData.keys.mapNotNull { it?.toString() }
                                else -> emptyList()
                            }.filter { it.isNotBlank() }
                        } catch (e: Exception) {
                            android.util.Log.e("CURRICULUM", "Error loading subject catalog: ${e.message}", e)
                            emptyList()
                        }
                    } else {
                        emptyList()
                    }

                    val collections = listOf("notes", "pyqs", "assignments", "cheatsheets")
                    val allDocs = coroutineScope {
                        val deferreds = collections.map { col ->
                            async(Dispatchers.IO) {
                                try {
                                    val firestoreQueryStartTime = System.currentTimeMillis()
                                    android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query START collection=$col thread=${Thread.currentThread().name}")
                                    val documents = if (subjectIds.isNotEmpty()) {
                                        val colRef = firestore.collection(col)
                                        val chunks = subjectIds.chunked(30)
                                        chunks.flatMap { chunk ->
                                            colRef.whereEqualTo("college", canonicalCollegeId)
                                                .whereIn("subjectId", chunk)
                                                .get()
                                                .await()
                                                .documents
                                        }
                                    } else if (hasSemester) {
                                        firestore.collection(col)
                                            .whereEqualTo("college", canonicalCollegeId)
                                            .whereEqualTo("semester", semester)
                                            .get()
                                            .await()
                                            .documents
                                    } else {
                                        firestore.collection(col)
                                            .whereEqualTo("college", canonicalCollegeId)
                                            .get()
                                            .await()
                                            .documents
                                    }
                                    val firestoreQueryDuration = System.currentTimeMillis() - firestoreQueryStartTime
                                    android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query END collection=$col duration=${firestoreQueryDuration}ms docs=${documents.size} thread=${Thread.currentThread().name}")
                                    documents
                                } catch (e: Exception) {
                                    android.util.Log.e("FIRESTORE", "Error querying collection $col: ${e.message}", e)
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
                    val bookmarkedIds = com.pravor.notessharing.data.repository.BookmarkRepository.bookmarksFlow.value.map { it.id }.toSet()
                    
                    val feedAssemblyStartTime = System.currentTimeMillis()
                    val realItems = allDocs.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        val id = data["documentId"] as? String ?: ""
                        val title = data["title"] as? String ?: ""
                        if (id.isBlank() || title.isBlank()) return@mapNotNull null
                        
                        // Completely exclude video and playlist resources for Home Feed
                        if (isVideoOrPlaylistResource(data)) return@mapNotNull null

                        val matchesScope = com.pravor.notessharing.core.util.AcademicScope(
                            collegeId = canonicalCollegeId,
                            branchId = rawBranch,
                            semester = semester,
                            subjectIds = subjectIds
                        ).isDocumentPermitted(
                            docCollege = data["college"] as? String ?: canonicalCollegeId,
                            docBranch = data["branch"] as? String,
                            docSemester = data["semester"] as? String,
                            docSubjectId = data["subjectId"] as? String
                        )
                        if (!matchesScope) return@mapNotNull null

                        val item = documentToFeedItem(data)
                        if (!isEligibleHomeFeedItem(item)) return@mapNotNull null

                        val timestamp = data["uploadedAt"] as? Long ?: 0L
                        item to timestamp
                    }.sortedWith(
                        compareByDescending<Pair<FeedItem, Long>> { it.first.upvotes }
                            .thenByDescending { it.second }
                    ).map { it.first }
                    
                    val mergedFeedItems = realItems.distinctBy { it.id }
                    
                    val finalFeedItems = mergedFeedItems.map { item ->
                        item.copy(isSaved = bookmarkedIds.contains(item.id))
                    }
                    val feedAssemblyDuration = System.currentTimeMillis() - feedAssemblyStartTime
                    android.util.Log.d("PERF", "[PERF] Feed assembly duration=${feedAssemblyDuration}ms items=${finalFeedItems.size} thread=${Thread.currentThread().name}")
                    
                    val currentAcademicScope = com.pravor.notessharing.core.util.AcademicScope(
                        collegeId = canonicalCollegeId,
                        branchId = rawBranch,
                        semester = semester,
                        subjectIds = subjectIds
                    )

                    if (currentAcademicScope.isCollegeValid && finalFeedItems.isNotEmpty()) {
                        homeFeedRepository.saveHomeFeed(currentAcademicScope.scopeKey, finalFeedItems)
                    }

                    val currentItems = (_uiState.value as? HomeUiState.Success)?.content?.feedItems ?: emptyList()
                    val isFeedChanged = currentItems.size != finalFeedItems.size ||
                            currentItems.zip(finalFeedItems).any { (a, b) -> a.id != b.id || a.upvotes != b.upvotes || a.downloadsCount != b.downloadsCount }

                    if (isFeedChanged || isPullToRefresh || currentItems.isEmpty()) {
                        _uiState.update { current ->
                            val lastOpened = recentlyOpenedRepository.getLastOpened(currentAcademicScope)
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
                                        categories = Category.entries,
                                        feedItems = finalFeedItems,
                                        recentlyOpened = lastOpened,
                                        isLoadingFeed = false
                                    )
                                )
                            }
                        }
                        android.util.Log.d("PERF", "[PERF] UI updated with fresh Firestore feed data items=${finalFeedItems.size}")
                    } else {
                        android.util.Log.d("PERF", "[PERF] Background Firestore refresh completed - UI feed data identical, skipping extra recomposition")
                    }
                    
                    val duration = System.currentTimeMillis() - startTime
                    android.util.Log.d("PERF", "[PERF] Background Firestore refresh END - duration=$duration ms items=${finalFeedItems.size} thread=${Thread.currentThread().name}")
                    hasLoadedInitialFirestoreFeed = true
                    if (isFirstLoad) {
                        isFirstLoad = false
                        val refreshDuration = System.currentTimeMillis() - startupStartTime
                        android.util.Log.d("PERF", "[PERF] Initial background Firestore sync completed duration=${refreshDuration}ms thread=${Thread.currentThread().name}")
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
                    
                    val currentUid = auth.currentUser?.uid
                    if (currentUid != null) {
                        bookmarkRepository.loadInitialBookmarksIfNeeded(currentUid)
                        upvoteRepository.loadInitialUpvotesIfNeeded(currentUid)
                    }
                    val bookmarkedIds = com.pravor.notessharing.data.repository.BookmarkRepository.bookmarksFlow.value.map { it.id }.toSet()
                    
                    val rawCollege = previousProfile?.college ?: "kiit"
                    val canonicalCollegeId = com.pravor.notessharing.core.util.LegacyAcademicCompatibilityResolver.resolveCollegeId(rawCollege)
                    val cachedFallback = try {
                        homeFeedRepository.getCachedHomeFeed(canonicalCollegeId)
                    } catch (_: Exception) {
                        emptyList()
                    }
                    val fallbackItems = if (cachedFallback.isNotEmpty()) {
                        cachedFallback.filter { isEligibleHomeFeedItem(it) }.map { item -> item.copy(isSaved = bookmarkedIds.contains(item.id)) }
                    } else {
                        emptyList()
                    }

                    _uiState.update { current ->
                        val lastOpened = recentlyOpenedRepository.getLastOpened()
                        if (current is HomeUiState.Success) {
                            val itemsToUse = if (current.content.feedItems.isNotEmpty()) current.content.feedItems else fallbackItems
                            current.copy(content = current.content.copy(
                                feedItems = itemsToUse,
                                recentlyOpened = lastOpened,
                                isLoadingFeed = false
                            ))
                        } else {
                            HomeUiState.Success(
                                HomeContent(
                                    selectedCategory = Category.Notes,
                                    categories = Category.entries,
                                    feedItems = fallbackItems,
                                    recentlyOpened = lastOpened,
                                    isLoadingFeed = false
                                )
                            )
                        }
                    }

                    val duration = System.currentTimeMillis() - startTime
                    android.util.Log.d("PERF", "[PERF] Background Firestore refresh END - duration=$duration ms thread=${Thread.currentThread().name}")
                    val count = (_uiState.value as? HomeUiState.Success)?.content?.feedItems?.size ?: 0
                    android.util.Log.d("PERF", "[PERF] Feed item count=$count")
                    hasLoadedInitialFirestoreFeed = true
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
                if (activeLoadKey == targetScopeKey) {
                    activeLoadKey = null
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
            "CheatSheet", "Cheat Sheet" -> FileType.CheatSheet
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

        val upvotedMap = com.pravor.notessharing.data.repository.UpvoteRepository.upvotesFlow.value
        val upvoteCountsMap = com.pravor.notessharing.data.repository.UpvoteRepository.upvoteCountsFlow.value
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

    private fun isVideoOrPlaylistResource(data: Map<String, Any>): Boolean {
        val docType = (data["documentType"] as? String ?: data["type"] as? String ?: "").trim()
        val contentType = (data["contentType"] as? String ?: "").trim()
        val hasYoutubeLink = (data["hasYoutubeLink"] as? Boolean) == true || (data["hasYoutubeLink"] as? String)?.lowercase(java.util.Locale.ROOT) == "true"
        val sourceType = (data["sourceType"] as? String ?: "").trim()
        val youtubeUrl = (data["youtubeUrl"] as? String ?: "").trim()
        val youtubeVideoId = (data["youtubeVideoId"] as? String ?: data["youtubeId"] as? String ?: "").trim()
        val resourceType = (data["resourceType"] as? String ?: "").trim()
        val source = (data["source"] as? String ?: "").trim()

        return docType.equals("VIDEO", ignoreCase = true) ||
                docType.equals("YouTube Resource", ignoreCase = true) ||
                docType.equals("Videos", ignoreCase = true) ||
                docType.equals("Video", ignoreCase = true) ||
                contentType.equals("VIDEO", ignoreCase = true) ||
                hasYoutubeLink ||
                sourceType.equals("youtube", ignoreCase = true) ||
                sourceType.equals("video", ignoreCase = true) ||
                youtubeUrl.isNotBlank() ||
                youtubeVideoId.isNotBlank() ||
                resourceType.equals("VIDEO", ignoreCase = true) ||
                resourceType.equals("PLAYLIST", ignoreCase = true) ||
                source.equals("YOUTUBE", ignoreCase = true)
    }

    private fun isEligibleHomeFeedItem(item: FeedItem): Boolean {
        val isVideoType = item.fileType == FileType.Video
        val hasYoutubeId = !item.youtubeVideoId.isNullOrBlank()
        val hasYoutubeUrl = !item.youtubeUrl.isNullOrBlank()
        val hasYoutubeThumbnail = !item.youtubeThumbnailUrl.isNullOrBlank()
        val docTypeVideo = item.documentType.equals("VIDEO", ignoreCase = true) ||
                item.documentType.equals("YouTube Resource", ignoreCase = true) ||
                item.documentType.equals("Videos", ignoreCase = true) ||
                item.documentType.equals("Video", ignoreCase = true)
        val typeVideo = item.type.equals("VIDEO", ignoreCase = true) ||
                item.type.equals("YouTube Resource", ignoreCase = true) ||
                item.type.equals("Videos", ignoreCase = true) ||
                item.type.equals("Video", ignoreCase = true)

        return !isVideoType && !hasYoutubeId && !hasYoutubeUrl && !hasYoutubeThumbnail && !docTypeVideo && !typeVideo
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
            "video", "videos", "youtube resource", "playlist", "playlists", "video playlist" -> "videos"
            else -> "notes"
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

    private suspend fun checkDocumentExistsInFirestore(documentId: String, collectionName: String? = null): Boolean = withContext(Dispatchers.IO) {
        val collections = if (!collectionName.isNullOrBlank()) {
            val clean = collectionName.lowercase().trim()
            val mapped = when {
                clean.contains("pyq") -> "pyqs"
                clean.contains("assignment") -> "assignments"
                clean.contains("cheat") || clean.contains("formula") -> "cheatsheets"
                clean.contains("notes") || clean.contains("note") -> "notes"
                clean.contains("video") || clean.contains("youtube") -> "videos"
                else -> clean
            }
            listOf(mapped)
        } else {
            listOf("notes", "pyqs", "assignments", "cheatsheets", "videos")
        }
        val firestore = FirebaseFirestore.getInstance()
        for (col in collections) {
            try {
                val snap = firestore.collection(col).document(documentId).get(com.google.firebase.firestore.Source.CACHE).await()
                if (snap.exists()) return@withContext true
            } catch (e: Exception) {
                try {
                    val snap = firestore.collection(col).document(documentId).get().await()
                    if (snap.exists()) return@withContext true
                } catch (e2: Exception) {
                    // Try next collection
                }
            }
        }
        false
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        profileJob?.cancel()
        upvoteRepository.observeVisibleDocuments("Home", emptyList())
        notificationRepository.stopObserving()
    }
}
