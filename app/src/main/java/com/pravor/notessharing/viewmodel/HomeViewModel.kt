package com.pravor.notessharing.viewmodel

import androidx.lifecycle.ViewModel
import com.pravor.notessharing.model.Category
import com.pravor.notessharing.state.HomeContent
import com.pravor.notessharing.state.HomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(
        HomeUiState.Success(
            HomeContent(
                selectedCategory = Category.Notes,
                categories = DummyData.categories,
                feedItems = DummyData.feedItems
            )
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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
        updateFeed(itemId) { item -> item.copy(isSaved = !item.isSaved) }
    }

    private fun updateFeed(
        itemId: String,
        transform: (com.pravor.notessharing.model.FeedItem) -> com.pravor.notessharing.model.FeedItem
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
}
