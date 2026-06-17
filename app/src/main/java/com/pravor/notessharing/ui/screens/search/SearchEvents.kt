package com.pravor.notessharing.ui.screens.search

sealed interface SearchEvent {
    data class QueryChanged(val query: String) : SearchEvent
    data class CategorySelected(val category: SearchCategory) : SearchEvent
    data class FilterOptionClicked(val option: FilterOption) : SearchEvent
    object ResetFilters : SearchEvent
    object ClearRecentHistory : SearchEvent
    data class RecentSearchClicked(val query: String) : SearchEvent
    object SearchExecuted : SearchEvent
    object RetryClicked : SearchEvent
}
