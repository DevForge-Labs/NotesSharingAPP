package com.pravor.notessharing.data

import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.model.DocumentDetail
import com.pravor.notessharing.model.toDocumentDetail
import com.pravor.notessharing.viewmodel.DummyData
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

object UserFetchDiagnostics {
    val fetchedUids = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    val totalFetches = java.util.concurrent.atomic.AtomicInteger(0)
    val duplicateFetches = java.util.concurrent.atomic.AtomicInteger(0)
    val cacheHits = java.util.concurrent.atomic.AtomicInteger(0)
    val cacheMisses = java.util.concurrent.atomic.AtomicInteger(0)
    
    fun recordFetch(uid: String, fromCache: Boolean) {
        val total = totalFetches.incrementAndGet()
        val isDuplicate = !fetchedUids.add(uid)
        if (isDuplicate) {
            duplicateFetches.incrementAndGet()
        }
        if (fromCache) {
            cacheHits.incrementAndGet()
        } else {
            cacheMisses.incrementAndGet()
        }
        
        android.util.Log.d("FIRESTORE", "[FIRESTORE] User fetch uid=$uid")
        if (isDuplicate) {
            android.util.Log.d("FIRESTORE", "[FIRESTORE] User fetch duplicate=true uid=$uid")
        }
        android.util.Log.d("FIRESTORE", "[FIRESTORE] User fetch cacheHit=$fromCache uid=$uid")
    }
}

class DocumentDetailRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun getDocument(documentId: String): DocumentDetail? {
        val startTime = System.currentTimeMillis()
        android.util.Log.d("PERF", "[PERF] getDocument START id=$documentId")
        return try {
            val collections = listOf("documents", "notes", "pyqs", "assignments", "cheatsheets")
            var foundData: Pair<Map<String, Any>, String>? = null
            coroutineScope {
                val deferreds = collections.map { col ->
                    async {
                        try {
                            android.util.Log.d("PERF", "[PERF] Collection searched=$col")
                            val firestoreQueryStartTime = System.currentTimeMillis()
                            android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query START collection=$col document=$documentId thread=${Thread.currentThread().name}")
                            val snap = firestore.collection(col).document(documentId).get().await()
                            val firestoreQueryDuration = System.currentTimeMillis() - firestoreQueryStartTime
                            android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query END collection=$col document=$documentId duration=${firestoreQueryDuration}ms exists=${snap.exists()} thread=${Thread.currentThread().name}")
                            if (snap.exists() && snap.data != null) {
                                android.util.Log.d("PERF", "[PERF] Found in collection=$col")
                                Pair(snap.data!!, col)
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                foundData = deferreds.awaitAll().firstOrNull { it != null }
            }
            val result = if (foundData != null) {
                foundData?.first?.toDocumentDetail(documentId)
            } else {
                getDummyDocumentDetail(documentId)
            }
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.d("PERF", "[PERF] getDocument END duration=${duration}ms")
            result
        } catch (e: Exception) {
            val result = getDummyDocumentDetail(documentId)
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.d("PERF", "[PERF] getDocument END duration=${duration}ms")
            result
        }
    }

    suspend fun getUploaderContributorLevel(uploaderId: String): String? {
        if (uploaderId == "dummy-uid" || uploaderId.isEmpty()) {
            return "Gold Contributor" // Premium look for dummy uploader
        }
        val startTime = System.currentTimeMillis()
        return try {
            android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query START collection=users document=$uploaderId thread=${Thread.currentThread().name}")
            val snapshot = usersCollection.document(uploaderId).get().await()
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query END collection=users document=$uploaderId duration=${duration}ms exists=${snapshot.exists()} thread=${Thread.currentThread().name}")
            
            val fromCache = snapshot.metadata.isFromCache
            UserFetchDiagnostics.recordFetch(uploaderId, fromCache)

            if (snapshot.exists()) {
                val level = snapshot.getLong("contributorLevel")?.toInt() ?: 1
                getContributorLevelName(level)
            } else {
                "Bronze Contributor"
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query END collection=users document=$uploaderId duration=${duration}ms exists=false thread=${Thread.currentThread().name}")
            "Bronze Contributor"
        }
    }

    suspend fun getRelatedDocuments(doc: DocumentDetail): List<DocumentDetail> {
        val startTime = System.currentTimeMillis()
        android.util.Log.d("PERF", "[PERF] getRelatedDocuments START id=${doc.id} thread=${Thread.currentThread().name}")
        return try {
            val collections = listOf("documents", "notes", "pyqs", "assignments", "cheatsheets")
            val allRelatedDocs = coroutineScope {
                val deferreds = collections.map { col ->
                    async {
                        try {
                            val firestoreQueryStartTime = System.currentTimeMillis()
                            android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query START collection=$col thread=${Thread.currentThread().name}")
                            val snapshot = firestore.collection(col)
                                .whereEqualTo("semester", doc.semester)
                                .whereEqualTo("subject", doc.subject)
                                .whereEqualTo("documentType", doc.documentType)
                                .limit(4)
                                .get()
                                .await()
                            val firestoreQueryDuration = System.currentTimeMillis() - firestoreQueryStartTime
                            android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query END collection=$col duration=${firestoreQueryDuration}ms docs=${snapshot.size()} thread=${Thread.currentThread().name}")
                            snapshot.documents
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                }
                deferreds.awaitAll().flatten()
            }

            val firestoreRelated = allRelatedDocs.mapNotNull { d ->
                if (d.id == doc.id) null
                else d.data?.toDocumentDetail(d.id)
            }.distinctBy { it.id }.take(3)

            val result = if (firestoreRelated.isNotEmpty()) {
                firestoreRelated
            } else {
                getDummyRelatedDocuments(doc)
            }
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.d("PERF", "[PERF] getRelatedDocuments END duration=${duration}ms count=${result.size} thread=${Thread.currentThread().name}")
            result
        } catch (e: Exception) {
            val result = getDummyRelatedDocuments(doc)
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.d("PERF", "[PERF] getRelatedDocuments END duration=${duration}ms count=${result.size} thread=${Thread.currentThread().name}")
            result
        }
    }

    private fun getContributorLevelName(level: Int): String {
        return when (level) {
            1 -> "Bronze Contributor"
            2 -> "Silver Contributor"
            3 -> "Gold Contributor"
            4 -> "Platinum Contributor"
            else -> "Mythic Contributor"
        }
    }

    private fun getDummyRelatedDocuments(doc: DocumentDetail): List<DocumentDetail> {
        // Query from DummyData
        val allDummyDocs = DummyData.feedItems.map { it.id to getDummyDocumentDetail(it.id) }
            .mapNotNull { it.second }
        
        return allDummyDocs.filter { dummy ->
            dummy.id != doc.id &&
            (dummy.semester == doc.semester || dummy.subject == doc.subject || dummy.documentType == doc.documentType)
        }.take(3)
    }

    private fun getDummyDocumentDetail(id: String): DocumentDetail? {
        val feedItem = DummyData.feedItems.find { it.id == id }
        if (feedItem != null) {
            val fileUrls = when (id) {
                "feed-dbms-4" -> listOf(
                    "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                    "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=500",
                    "https://images.unsplash.com/photo-1506784983877-45594efa4cbe?w=500"
                )
                "feed-cn-cheat" -> listOf("https://images.unsplash.com/photo-1517842645767-c639042777db?w=500")
                "feed-coa-notes" -> listOf(
                    "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=500",
                    "https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=500"
                )
                else -> listOf("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
            }
            return DocumentDetail(
                id = feedItem.id,
                title = feedItem.title,
                description = feedItem.description,
                branch = "Computer Science",
                semester = "Semester 3",
                subject = feedItem.tags.firstOrNull() ?: "General",
                documentType = feedItem.fileType.label,
                uploaderId = "dummy-uid",
                uploaderName = feedItem.uploaderName,
                uploaderPhotoUrl = "",
                uploadedAt = System.currentTimeMillis() - 86400000,
                downloads = feedItem.downloads,
                upvotes = feedItem.upvotes,
                bookmarks = if (feedItem.isSaved) 1 else 0,
                fileUrls = fileUrls,
                fileSize = 1024 * 1024 * 3L,
                fileExtension = if (fileUrls.firstOrNull()?.contains("pdf") == true) "pdf" else "jpg",
                fileType = if (fileUrls.firstOrNull()?.contains("pdf") == true) "pdf" else "image",
                attachmentCount = fileUrls.size
            )
        }

        val studyFile = DummyData.savedFiles.find { it.id == id } 
            ?: DummyData.uploadedFiles.find { it.id == id }
        if (studyFile != null) {
            val fileUrls = listOf("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
            return DocumentDetail(
                id = studyFile.id,
                title = studyFile.title,
                description = "Saved document from your study social library.",
                branch = "Computer Science",
                semester = "Semester 3",
                subject = "General",
                documentType = studyFile.fileType.label,
                uploaderId = "dummy-uid",
                uploaderName = "System",
                uploaderPhotoUrl = "",
                uploadedAt = System.currentTimeMillis() - 86400000 * 2,
                downloads = studyFile.downloads,
                upvotes = studyFile.upvotes,
                bookmarks = 1,
                fileUrls = fileUrls,
                fileSize = 1024 * 1024L,
                fileExtension = "pdf",
                fileType = "pdf",
                attachmentCount = fileUrls.size
            )
        }

        val trending = DummyData.trendingNotes.find { it.id == id }
        if (trending != null) {
            val fileUrls = listOf("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
            return DocumentDetail(
                id = trending.id,
                title = trending.title,
                description = "Trending study guide or collection notes.",
                branch = "Computer Science",
                semester = "Semester 3",
                subject = trending.subject,
                documentType = "Notes",
                uploaderId = "dummy-uid",
                uploaderName = "Top Contributor",
                uploaderPhotoUrl = "",
                uploadedAt = System.currentTimeMillis(),
                downloads = trending.downloads,
                upvotes = trending.upvotes,
                bookmarks = if (trending.isBookmarked) 1 else 0,
                fileUrls = fileUrls,
                fileSize = 1024 * 1500L,
                fileExtension = "pdf",
                fileType = "pdf",
                attachmentCount = 1
            )
        }
        return null
    }
}
