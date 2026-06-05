package com.pravor.notessharing.widget

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.pravor.notessharing.bookmarks.BookmarkRepository
import com.pravor.notessharing.data.download.DownloadDataStoreManager
import kotlinx.coroutines.tasks.await

class WidgetCountRepository(private val context: Context) {

    suspend fun getBookmarksCount(): Int {
        val hasLoaded = BookmarkRepository.hasLoadedInitial
        android.util.Log.d("WidgetCountRepository", "getBookmarksCount() - hasLoadedInitial: $hasLoaded")
        
        // 1. Reuse existing in-memory repository list if already loaded
        if (hasLoaded) {
            val count = BookmarkRepository.bookmarksFlow.value.size
            android.util.Log.d("WidgetCountRepository", "getBookmarksCount() - returned $count from bookmarksFlow")
            return count
        }

        // 2. Fallback to querying Firestore local offline database cache directly (no network requests)
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            android.util.Log.d("WidgetCountRepository", "getBookmarksCount() - no authenticated user, returning 0")
            return 0
        }
        return try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("bookmarks")
                .whereEqualTo("userId", currentUid)
                .get(Source.CACHE)
                .await()
            val count = snapshot.size()
            android.util.Log.d("WidgetCountRepository", "getBookmarksCount() - returned $count from Firestore CACHE")
            count
        } catch (e: Exception) {
            android.util.Log.e("WidgetCountRepository", "getBookmarksCount() - Firestore CACHE lookup failed: ${e.message}", e)
            0
        }
    }

    suspend fun getDownloadsCount(): Int {
        return try {
            val downloadManager = DownloadDataStoreManager(context)
            val docs = downloadManager.getDownloadedDocuments()
            val count = docs.size
            android.util.Log.d("WidgetCountRepository", "getDownloadsCount() - returned $count using DownloadDataStoreManager")
            count
        } catch (e: Exception) {
            android.util.Log.e("WidgetCountRepository", "getDownloadsCount() - lookup failed: ${e.message}", e)
            0
        }
    }
}
