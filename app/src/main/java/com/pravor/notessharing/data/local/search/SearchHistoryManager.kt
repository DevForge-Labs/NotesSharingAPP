package com.pravor.notessharing.data.local.search

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.searchHistoryDataStore by preferencesDataStore(name = "search_history")

class SearchHistoryManager(private val context: Context) {

    companion object {
        private val HISTORY_KEY = stringPreferencesKey("recent_searches")
    }

    val historyFlow: Flow<List<String>> = context.searchHistoryDataStore.data.map { preferences ->
        val jsonStr = preferences[HISTORY_KEY] ?: "[]"
        parseHistory(jsonStr)
    }

    suspend fun addSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        context.searchHistoryDataStore.edit { preferences ->
            val currentList = parseHistory(preferences[HISTORY_KEY] ?: "[]").toMutableList()
            // Deduplicate
            currentList.remove(trimmed)
            // Add to the front (most recent first)
            currentList.add(0, trimmed)
            // Limit to 10 items
            val limitedList = currentList.take(10)
            preferences[HISTORY_KEY] = JSONArray(limitedList).toString()
        }
    }

    suspend fun clearHistory() {
        context.searchHistoryDataStore.edit { preferences ->
            preferences[HISTORY_KEY] = "[]"
        }
    }

    private fun parseHistory(jsonStr: String): List<String> {
        return try {
            val arr = JSONArray(jsonStr)
            List(arr.length()) { index -> arr.getString(index) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
