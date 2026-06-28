package com.pravor.notessharing.bookmarks

import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.model.FeedItem
import com.pravor.notessharing.model.StudyFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import java.util.Locale

class BookmarkRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val bookmarksCollection = firestore.collection("bookmarks")

    companion object {
        private val _bookmarksFlow = MutableStateFlow<List<StudyFile>>(emptyList())
        val bookmarksFlow: StateFlow<List<StudyFile>> = _bookmarksFlow.asStateFlow()
        var hasLoadedInitial = false
        private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
        private var activeUserId: String? = null
    }

    suspend fun loadInitialBookmarksIfNeeded(userId: String): List<StudyFile> {
        if (!hasLoadedInitial) {
            return getBookmarks(userId)
        }
        return bookmarksFlow.value
    }

    suspend fun getBookmarks(userId: String): List<StudyFile> {
        try {
            if (listenerRegistration == null || activeUserId != userId) {
                listenerRegistration?.remove()
                activeUserId = userId
                listenerRegistration = bookmarksCollection
                    .whereEqualTo("userId", userId)
                    .addSnapshotListener { querySnapshot, error ->
                        if (error != null) return@addSnapshotListener
                        if (querySnapshot != null) {
                            val list = querySnapshot.documents.mapNotNull { doc ->
                                val data = doc.data ?: return@mapNotNull null
                                val docId = data["documentId"] as? String ?: ""
                                val title = data["title"] as? String ?: ""
                                val docTypeStr = data["documentType"] as? String ?: "Notes"
                                val fileType = when (docTypeStr.lowercase(Locale.US).replace(" ", "")) {
                                    "pyq" -> com.pravor.notessharing.model.FileType.Pyq
                                    "cheatsheet", "cheatsheets" -> com.pravor.notessharing.model.FileType.CheatSheet
                                    "assignment" -> com.pravor.notessharing.model.FileType.Notes
                                    "video" -> com.pravor.notessharing.model.FileType.Video
                                    else -> com.pravor.notessharing.model.FileType.Pdf
                                }
                                val subject = data["subject"] as? String ?: ""
                                val thumbnailUrl = data["thumbnailUrl"] as? String
                                val uploaderName = data["uploaderName"] as? String ?: "Anonymous"
                                val bookmarkedAt = data["bookmarkedAt"] as? Long ?: System.currentTimeMillis()
                                
                                val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                                val dateStr = "Saved " + sdf.format(java.util.Date(bookmarkedAt))
                                
                                StudyFile(
                                    id = docId,
                                    title = title,
                                    uploadDate = dateStr,
                                    fileType = fileType,
                                    downloadsCount = 0,
                                    upvotes = 0,
                                    thumbnailUrl = thumbnailUrl,
                                    subject = subject,
                                    documentType = docTypeStr
                                )
                            }
                            _bookmarksFlow.value = list
                            hasLoadedInitial = true
                        }
                    }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return bookmarksFlow.value
    }

    suspend fun addBookmark(feedItem: FeedItem, userId: String) {
        val docId = feedItem.id
        val rawDocType = feedItem.documentType ?: feedItem.fileType.label
        val docType = if (rawDocType.lowercase(java.util.Locale.US).replace(" ", "") == "cheatsheet") {
            "CheatSheet"
        } else {
            rawDocType
        }
        val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
        val dateStr = "Saved " + sdf.format(java.util.Date(System.currentTimeMillis()))
        
        val newBookmark = StudyFile(
            id = docId,
            title = feedItem.title,
            uploadDate = dateStr,
            fileType = feedItem.fileType,
            downloadsCount = feedItem.downloadsCount,
            upvotes = feedItem.upvotes,
            thumbnailUrl = feedItem.thumbnailUrl,
            subject = feedItem.subject ?: feedItem.tags.firstOrNull() ?: "General",
            documentType = docType
        )
        
        // Optimistic UI Update: immediately emit in-memory
        _bookmarksFlow.update { current ->
            if (current.none { it.id == docId }) current + newBookmark else current
        }

        try {
            val bookmarkId = "${userId}_${docId}"
            val data = mapOf(
                "userId" to userId,
                "documentId" to docId,
                "bookmarkedAt" to System.currentTimeMillis(),
                "documentType" to docType,
                "title" to feedItem.title,
                "subject" to (feedItem.subject ?: feedItem.tags.firstOrNull() ?: "General"),
                "thumbnailUrl" to feedItem.thumbnailUrl,
                "uploaderName" to feedItem.uploaderName
            )
            bookmarksCollection.document(bookmarkId).set(data).await()
            try {
                com.pravor.notessharing.widget.WidgetUpdateManager.updateAllWidgets(
                    com.google.firebase.FirebaseApp.getInstance().applicationContext
                )
            } catch (ex: Exception) {
                android.util.Log.e("BookmarkRepository", "Widget update error: ${ex.message}", ex)
            }
        } catch (e: Exception) {
            // lightweight fallback or log
        }
    }

    suspend fun addBookmark(studyFile: StudyFile, userId: String) {
        val docId = studyFile.id
        // Optimistic UI Update
        _bookmarksFlow.update { current ->
            if (current.none { it.id == docId }) current + studyFile else current
        }

        try {
            val bookmarkId = "${userId}_${docId}"
            val rawDocType = studyFile.documentType ?: studyFile.fileType.label
            val docType = if (rawDocType.lowercase(java.util.Locale.US).replace(" ", "") == "cheatsheet") {
                "CheatSheet"
            } else {
                rawDocType
            }
            val data = mapOf(
                "userId" to userId,
                "documentId" to docId,
                "bookmarkedAt" to System.currentTimeMillis(),
                "documentType" to docType,
                "title" to studyFile.title,
                "subject" to (studyFile.subject ?: "General"),
                "thumbnailUrl" to studyFile.thumbnailUrl,
                "uploaderName" to "Contributor"
            )
            bookmarksCollection.document(bookmarkId).set(data).await()
            try {
                com.pravor.notessharing.widget.WidgetUpdateManager.updateAllWidgets(
                    com.google.firebase.FirebaseApp.getInstance().applicationContext
                )
            } catch (ex: Exception) {
                android.util.Log.e("BookmarkRepository", "Widget update error: ${ex.message}", ex)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun removeBookmark(documentId: String, userId: String) {
        // Optimistic UI Update: immediately remove in-memory
        _bookmarksFlow.update { current ->
            current.filter { it.id != documentId }
        }

        try {
            val bookmarkId = "${userId}_${documentId}"
            bookmarksCollection.document(bookmarkId).delete().await()
            try {
                com.pravor.notessharing.widget.WidgetUpdateManager.updateAllWidgets(
                    com.google.firebase.FirebaseApp.getInstance().applicationContext
                )
            } catch (ex: Exception) {
                android.util.Log.e("BookmarkRepository", "Widget update error: ${ex.message}", ex)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    suspend fun isBookmarked(documentId: String, userId: String): Boolean {
        if (hasLoadedInitial) {
            return _bookmarksFlow.value.any { it.id == documentId }
        }
        return try {
            val bookmarkId = "${userId}_${documentId}"
            bookmarksCollection.document(bookmarkId).get().await().exists()
        } catch (e: Exception) {
            false
        }
    }
}
