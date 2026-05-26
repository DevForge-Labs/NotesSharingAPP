package com.pratyush.notessharing.viewmodel

import androidx.lifecycle.ViewModel
import com.pratyush.notessharing.state.ExploreContent
import com.pratyush.notessharing.state.ExploreUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
}
