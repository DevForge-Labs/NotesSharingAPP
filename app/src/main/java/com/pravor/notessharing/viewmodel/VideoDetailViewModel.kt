package com.pravor.notessharing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pravor.notessharing.data.VideoDetailRepository
import com.pravor.notessharing.model.VideoDetail
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
    
    private val _uiState = MutableStateFlow<VideoDetailUiState>(VideoDetailUiState.Loading)
    val uiState: StateFlow<VideoDetailUiState> = _uiState.asStateFlow()
    private var hasIncremented = false
    
    fun loadVideoDetail(videoId: String) {
        viewModelScope.launch {
            _uiState.value = VideoDetailUiState.Loading
            try {
                val video = repository.getVideo(videoId)
                if (video != null) {
                    val contributorLevel = repository.getUploaderContributorLevel(video.uploaderId) ?: "Bronze Contributor"
                    val related = repository.getRelatedVideos(video)
                    _uiState.value = VideoDetailUiState.Success(video, contributorLevel, related)
                } else {
                    _uiState.value = VideoDetailUiState.Error("Video not found in repository")
                }
            } catch (e: Exception) {
                _uiState.value = VideoDetailUiState.Error(e.message ?: "Failed to load video details")
            }
        }
    }

    fun incrementVideoViews(videoId: String, collection: String, resourceType: String) {
        if (hasIncremented) return
        hasIncremented = true
        viewModelScope.launch {
            viewTrackingRepository.incrementViewCountDirect(videoId, collection, resourceType)
        }
    }
}
