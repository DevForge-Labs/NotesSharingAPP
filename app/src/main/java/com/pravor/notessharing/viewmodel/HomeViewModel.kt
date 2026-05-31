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

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    private val userService = com.pravor.notessharing.firebase.FirestoreUserService()
    private var profileJob: kotlinx.coroutines.Job? = null
    private val bookmarkRepository = com.pravor.notessharing.bookmarks.BookmarkRepository()

    private val authListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            startObservingProfile(uid)
        } else {
            profileJob?.cancel()
            loadRealDocuments(null)
        }
    }

    init {
        observeUserProfileState()
        refreshRecentlyOpened()
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
        profileJob = viewModelScope.launch {
            userService.observeUserProfile(uid).collect { profile ->
                val semester = profile?.semester
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
        loadRealDocuments()
    }

    fun loadRealDocuments(forcedSemester: String? = null) {
        viewModelScope.launch {
            try {
                val semester = forcedSemester ?: run {
                    val currentUid = auth.currentUser?.uid
                    if (currentUid != null) {
                        userService.getUserProfile(currentUid)?.semester
                    } else {
                        null
                    }
                }

                val hasSemester = !semester.isNullOrBlank() && semester != "Not Set"

                val collections = listOf("documents", "notes", "pyqs", "assignments", "cheatsheets")
                val allDocs = coroutineScope {
                    val deferreds = collections.map { col ->
                        async {
                            try {
                                if (hasSemester) {
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
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }
                    deferreds.awaitAll().flatten()
                }
                
                val currentUid = auth.currentUser?.uid
                val bookmarkedIds = if (currentUid != null) {
                    try {
                        firestore.collection("bookmarks")
                            .whereEqualTo("userId", currentUid)
                            .get()
                            .await()
                            .documents
                            .mapNotNull { it.getString("documentId") }
                            .toSet()
                    } catch (e: Exception) {
                        emptySet()
                    }
                } else {
                    emptySet()
                }
                
                val realItems = allDocs.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val item = documentToFeedItem(data)
                    val timestamp = data["uploadedAt"] as? Long ?: (data["uploadTimestamp"] as? Long ?: 0L)
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
                val bookmarkedIds = if (currentUid != null) {
                    try {
                        firestore.collection("bookmarks")
                            .whereEqualTo("userId", currentUid)
                            .get()
                            .await()
                            .documents
                            .mapNotNull { it.getString("documentId") }
                            .toSet()
                    } catch (e: Exception) {
                        emptySet()
                    }
                } else {
                    emptySet()
                }
                
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
        
        val thumbnailUrl = doc["thumbnailUrl"] as? String
        val youtubeThumbnailUrl = doc["youtubeThumbnailUrl"] as? String
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
        updateFeed(itemId) { item ->
            val nextUpvoted = !item.isUpvoted
            item.copy(
                isUpvoted = nextUpvoted,
                upvotes = item.upvotes + if (nextUpvoted) 1 else -1
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

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        profileJob?.cancel()
    }
}
