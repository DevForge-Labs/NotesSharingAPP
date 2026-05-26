package com.pratyush.notessharing.state

import androidx.compose.runtime.Immutable
import com.pratyush.notessharing.model.Category
import com.pratyush.notessharing.model.Contributor
import com.pratyush.notessharing.model.DiscoverFeedItem
import com.pratyush.notessharing.model.FeedItem
import com.pratyush.notessharing.model.Profile
import com.pratyush.notessharing.model.RevisionCard
import com.pratyush.notessharing.model.StudyFile
import com.pratyush.notessharing.model.StudyCollection
import com.pratyush.notessharing.model.TrendingNote
import com.pratyush.notessharing.model.TrendingTopic
import com.pratyush.notessharing.model.VideoRecommendation

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Success(val content: HomeContent) : HomeUiState
}

@Immutable
data class HomeContent(
    val selectedCategory: Category,
    val categories: List<Category>,
    val feedItems: List<FeedItem>
)

sealed interface ExploreUiState {
    data object Loading : ExploreUiState
    data object Empty : ExploreUiState
    data class Error(val message: String) : ExploreUiState
    data class Success(val content: ExploreContent) : ExploreUiState
}

@Immutable
data class ExploreContent(
    val topics: List<TrendingTopic>,
    val popularUploads: List<FeedItem>,
    val trendingNotes: List<TrendingNote>,
    val videoRecommendations: List<VideoRecommendation>,
    val studyCollections: List<StudyCollection>,
    val subjectHubs: List<String>,
    val topContributors: List<Contributor>,
    val revisionCards: List<RevisionCard>,
    val discoverItems: List<DiscoverFeedItem>
)

sealed interface MyFilesUiState {
    data object Loading : MyFilesUiState
    data object Empty : MyFilesUiState
    data class Error(val message: String) : MyFilesUiState
    data class Success(val content: MyFilesContent) : MyFilesUiState
}

@Immutable
data class MyFilesContent(
    val savedFiles: List<StudyFile>,
    val uploadedFiles: List<StudyFile>
)

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data object Empty : ProfileUiState
    data class Error(val message: String) : ProfileUiState
    data class Success(val profile: Profile) : ProfileUiState
}

@Immutable
data class AppSettingsUiState(
    val darkModeEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val themePreference: ThemePreference = ThemePreference.Dark
)

enum class ThemePreference(val label: String) {
    System("System"),
    Light("Light"),
    Dark("Dark")
}
