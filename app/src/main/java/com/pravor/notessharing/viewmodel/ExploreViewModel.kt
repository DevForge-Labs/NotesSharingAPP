package com.pravor.notessharing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.model.DiscoverFeedItem
import com.pravor.notessharing.model.FeedItem
import com.pravor.notessharing.model.FileType
import com.pravor.notessharing.state.ExploreContent
import com.pravor.notessharing.state.ExploreUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ExploreViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ExploreUiState>(
        ExploreUiState.Success(
            ExploreContent(
                topics = DummyData.topics,
                popularUploads = DummyData.feedItems,
                trendingNotes = DummyData.trendingNotes,
                videoRecommendations = DummyData.videoRecommendations,
                studyCollections = DummyData.studyCollections,
                subjectHubs = DummyData.subjectHubs,
                topContributors = DummyData.topContributors,
                revisionCards = DummyData.revisionCards,
                discoverItems = DummyData.discoverItems
            )
        )
    )
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()

    init {
        loadRealDocuments()
    }

    fun loadRealDocuments() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("documents")
                    .orderBy("uploadedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val realFeed = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    documentToFeedItem(data)
                }

                val realTrending = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val id = data["documentId"] as? String ?: ""
                    val title = data["title"] as? String ?: ""
                    val subject = data["subject"] as? String ?: ""
                    val downloads = (data["downloads"] as? Long ?: 0L).toInt()
                    val upvotes = (data["upvotes"] as? Long ?: 0L).toInt()
                    com.pravor.notessharing.model.TrendingNote(
                        id = id,
                        title = title,
                        subject = subject,
                        downloads = downloads,
                        rating = 4.5,
                        upvotes = upvotes,
                        isBookmarked = false
                    )
                }

                val realDiscover = snapshot.documents.mapNotNull { doc ->
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

                _uiState.update { current ->
                    if (current is ExploreUiState.Success) {
                        current.copy(
                            content = current.content.copy(
                                popularUploads = (realFeed + DummyData.feedItems).distinctBy { it.id },
                                trendingNotes = (realTrending + DummyData.trendingNotes).distinctBy { it.id },
                                discoverItems = (realDiscover + DummyData.discoverItems).distinctBy { it.id }
                            )
                        )
                    } else {
                        ExploreUiState.Success(
                            ExploreContent(
                                topics = DummyData.topics,
                                popularUploads = (realFeed + DummyData.feedItems).distinctBy { it.id },
                                trendingNotes = (realTrending + DummyData.trendingNotes).distinctBy { it.id },
                                videoRecommendations = DummyData.videoRecommendations,
                                studyCollections = DummyData.studyCollections,
                                subjectHubs = DummyData.subjectHubs,
                                topContributors = DummyData.topContributors,
                                revisionCards = DummyData.revisionCards,
                                discoverItems = (realDiscover + DummyData.discoverItems).distinctBy { it.id }
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Keep dummy data on failure
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
        
        val title = doc["title"] as? String ?: ""
        val description = doc["description"] as? String ?: ""
        val tags = (doc["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        
        val docType = doc["documentType"] as? String ?: (doc["type"] as? String ?: "Notes")
        val fileType = when (docType) {
            "PYQ" -> FileType.Pyq
            "Cheat Sheet" -> FileType.CheatSheet
            "Assignment" -> FileType.Notes
            "Notes" -> FileType.Notes
            "YouTube Resource", "Videos" -> FileType.Video
            else -> FileType.Pdf
        }
        
        val upvotes = (doc["upvotes"] as? Long ?: (doc["likesCount"] as? Long ?: 0L)).toInt()
        val downloads = (doc["downloads"] as? Long ?: (doc["downloadsCount"] as? Long ?: 0L)).toInt()
        
        return FeedItem(
            id = id,
            uploaderName = uploaderName,
            uploaderInitials = initials,
            uploadDate = uploadDate,
            title = title,
            description = description,
            tags = tags,
            fileType = fileType,
            upvotes = upvotes,
            comments = 0,
            downloads = downloads,
            isUpvoted = false,
            isSaved = false
        )
    }
}
