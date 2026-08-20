package com.pravor.notessharing.data.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object DownloadCountTracker {
    private val _downloadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadCounts: StateFlow<Map<String, Int>> = _downloadCounts.asStateFlow()

    fun updateDownloadCount(documentId: String, newCount: Int) {
        _downloadCounts.update { current ->
            current.toMutableMap().apply {
                this[documentId] = newCount
            }
        }
    }

    fun getDownloadCount(documentId: String, fallback: Int): Int {
        return _downloadCounts.value[documentId] ?: fallback
    }
}
