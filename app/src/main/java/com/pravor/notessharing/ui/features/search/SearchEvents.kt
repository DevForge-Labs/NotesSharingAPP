package com.pravor.notessharing.ui.features.search

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

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
