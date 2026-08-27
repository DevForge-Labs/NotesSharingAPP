package com.pravor.notessharing.data.repository


import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.domain.model.FeedItem
import com.pravor.notessharing.domain.model.StudyFile
import com.pravor.notessharing.domain.model.removeFileExtension
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
                                val title = (data["title"] as? String ?: "").removeFileExtension()
                                val docTypeStr = data["documentType"] as? String ?: "Notes"
                                val fileType = when (docTypeStr.lowercase(Locale.US).replace(" ", "")) {
                                    "pyq" -> com.pravor.notessharing.domain.model.FileType.Pyq
                                    "cheatsheet", "cheatsheets" -> com.pravor.notessharing.domain.model.FileType.CheatSheet
                                    "assignment" -> com.pravor.notessharing.domain.model.FileType.Notes
                                    "video" -> com.pravor.notessharing.domain.model.FileType.Video
                                    else -> com.pravor.notessharing.domain.model.FileType.Pdf
                                }
                                val subject = data["subject"] as? String ?: ""
                                val thumbnailUrl = data["thumbnailUrl"] as? String
                                val uploaderName = data["uploaderName"] as? String ?: "Anonymous"
                                val bookmarkedAt = data["bookmarkedAt"] as? Long ?: System.currentTimeMillis()
                                
                                val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                                val dateStr = "Saved " + sdf.format(java.util.Date(bookmarkedAt))
                                
                                val college = data["college"] as? String
                                val branch = data["branch"] as? String
                                val semester = data["semester"] as? String
                                val subjectId = data["subjectId"] as? String
                                
                                StudyFile(
                                    id = docId,
                                    title = title,
                                    uploadDate = dateStr,
                                    fileType = fileType,
                                    downloadsCount = 0,
                                    upvotes = 0,
                                    thumbnailUrl = thumbnailUrl,
                                    subject = subject,
                                    documentType = docTypeStr,
                                    college = college,
                                    branch = branch,
                                    semester = semester,
                                    subjectId = subjectId
                                )
                            }
                            if (_bookmarksFlow.value != list) {
                                _bookmarksFlow.value = list
                            }
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
            documentType = docType,
            examYear = feedItem.examYear,
            examType = feedItem.examType,
            sectionDisplay = feedItem.sectionDisplay,
            branch = feedItem.tags.firstOrNull()
        )
        
        // Optimistic UI Update: immediately emit in-memory
        _bookmarksFlow.update { current ->
            if (current.none { it.id == docId }) current + newBookmark else current
        }

        try {
            val bookmarkId = "${userId}_${docId}"
            val data = mutableMapOf<String, Any?>(
                "userId" to userId,
                "documentId" to docId,
                "bookmarkedAt" to System.currentTimeMillis(),
                "documentType" to docType,
                "title" to feedItem.title,
                "subject" to (feedItem.subject ?: feedItem.tags.firstOrNull() ?: "General"),
                "thumbnailUrl" to feedItem.thumbnailUrl,
                "uploaderName" to feedItem.uploaderName
            )
            if (!feedItem.examYear.isNullOrBlank()) data["examYear"] = feedItem.examYear
            if (!feedItem.examType.isNullOrBlank()) data["examType"] = feedItem.examType
            if (!feedItem.sectionDisplay.isNullOrBlank()) data["sectionDisplay"] = feedItem.sectionDisplay
            
            bookmarksCollection.document(bookmarkId).set(data.filterValues { it != null }).await()
            try {
                com.pravor.notessharing.core.widget.WidgetUpdateManager.updateAllWidgets(
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
            val data = mutableMapOf<String, Any?>(
                "userId" to userId,
                "documentId" to docId,
                "bookmarkedAt" to System.currentTimeMillis(),
                "documentType" to docType,
                "title" to studyFile.title,
                "subject" to (studyFile.subject ?: "General"),
                "thumbnailUrl" to studyFile.thumbnailUrl,
                "uploaderName" to "Contributor"
            )
            if (!studyFile.college.isNullOrBlank()) data["college"] = studyFile.college
            if (!studyFile.branch.isNullOrBlank()) data["branch"] = studyFile.branch
            if (!studyFile.semester.isNullOrBlank()) data["semester"] = studyFile.semester
            if (!studyFile.subjectId.isNullOrBlank()) data["subjectId"] = studyFile.subjectId
            if (!studyFile.examYear.isNullOrBlank()) data["examYear"] = studyFile.examYear
            if (!studyFile.examType.isNullOrBlank()) data["examType"] = studyFile.examType
            if (!studyFile.sectionDisplay.isNullOrBlank()) data["sectionDisplay"] = studyFile.sectionDisplay

            bookmarksCollection.document(bookmarkId).set(data.filterValues { it != null }).await()
            try {
                com.pravor.notessharing.core.widget.WidgetUpdateManager.updateAllWidgets(
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
                com.pravor.notessharing.core.widget.WidgetUpdateManager.updateAllWidgets(
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
