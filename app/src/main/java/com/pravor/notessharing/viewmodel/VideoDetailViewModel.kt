package com.pravor.notessharing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.data.VideoDetailRepository
import com.pravor.notessharing.model.VideoDetail
import com.pravor.notessharing.upvotes.UpvoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface VideoDetailUiState {
    object Loading : VideoDetailUiState
    data class Success(
        val video: VideoDetail,
        val contributorLevel: String,
        val relatedVideos: List<VideoDetail>
    ) : VideoDetailUiState
    data class Error(val message: String) : VideoDetailUiState
}

class VideoDetailViewModel(
    private val repository: VideoDetailRepository = VideoDetailRepository(),
    private val viewTrackingRepository: com.pravor.notessharing.data.ViewTrackingRepository = com.pravor.notessharing.data.ViewTrackingRepository()
) : ViewModel() {
    private var loadedVideoId: String? = null
    private val upvoteRepository = UpvoteRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _uiState = MutableStateFlow<VideoDetailUiState>(VideoDetailUiState.Loading)
    val uiState: StateFlow<VideoDetailUiState> = _uiState.asStateFlow()
    private var hasIncremented = false
    
    fun loadVideoDetail(videoId: String) {
        loadedVideoId = videoId
        
        val currentUid = auth.currentUser?.uid
        if (!currentUid.isNullOrBlank()) {
            viewModelScope.launch {
                com.pravor.notessharing.data.ReportRepository.instance.hasUserReported(videoId, currentUid)
            }
        }

        viewModelScope.launch {
            _uiState.value = VideoDetailUiState.Loading
            try {
                val video = repository.getVideo(videoId)
                if (video != null) {
                    val contributorLevel = repository.getUploaderContributorLevel(video.uploaderId) ?: "Bronze Contributor"
                    val related = repository.getRelatedVideos(video)
                    android.util.Log.d("REC_TRACE", "[VIDEO_VM] 6. Received by ViewModel count=${related.size}")
                    
                    observeUpvotes(video.id, video.collection, related)

                    val uiStateToSet = VideoDetailUiState.Success(video, contributorLevel, related)
                    android.util.Log.d("REC_TRACE", "[VIDEO_VM] 7. Exposed through UI State success count=${uiStateToSet.relatedVideos.size}")
                    _uiState.value = uiStateToSet
                } else {
                    _uiState.value = VideoDetailUiState.Error("Video not found in repository")
                }
            } catch (e: Exception) {
                _uiState.value = VideoDetailUiState.Error(e.message ?: "Failed to load video details")
            }
        }
    }

    fun observeUpvotes(videoId: String, collection: String, relatedVideos: List<VideoDetail> = emptyList()) {
        val currentUid = auth.currentUser?.uid
        viewModelScope.launch {
            if (currentUid != null) {
                upvoteRepository.loadInitialUpvotesIfNeeded(currentUid)
            }
            val targets = mutableListOf(videoId to collection)
            for (v in relatedVideos) {
                targets.add(v.id to v.collection)
            }
            upvoteRepository.observeVisibleDocuments("VideoDetailsScreen_$videoId", targets)
        }
    }

    fun clearUpvotesObservation() {
        val videoId = loadedVideoId
        if (videoId != null) {
            upvoteRepository.observeVisibleDocuments("VideoDetailsScreen_$videoId", emptyList())
        }
    }

    fun toggleUpvote(itemId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val successState = (_uiState.value as? VideoDetailUiState.Success) ?: return

        val (col, currentUpvotes) = if (successState.video.id == itemId) {
            Pair(successState.video.collection, successState.video.upvotes)
        } else {
            val relatedDoc = successState.relatedVideos.find { it.id == itemId } ?: return
            Pair(relatedDoc.collection, relatedDoc.upvotes)
        }

        viewModelScope.launch {
            upvoteRepository.toggleUpvote(
                documentId = itemId,
                collectionName = col,
                currentUpvotes = currentUpvotes,
                userId = currentUid
            )
        }
    }

    fun incrementVideoViews(videoId: String, collection: String, resourceType: String) {
        if (hasIncremented) return
        hasIncremented = true
        viewModelScope.launch {
            viewTrackingRepository.incrementViewCountDirect(videoId, collection, resourceType)
        }
    }

    override fun onCleared() {
        super.onCleared()
        clearUpvotesObservation()
    }
}
