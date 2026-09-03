package com.pravor.notessharing.data.repository

import com.pravor.notessharing.domain.util.ExploreRankingUtils

import com.pravor.notessharing.core.util.*

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.BuildConfig
import com.pravor.notessharing.data.local.cache.TimedMemoryCache
import com.pravor.notessharing.domain.model.DocumentDetail
import com.pravor.notessharing.domain.model.toDocumentDetail
import com.pravor.notessharing.ui.common.utils.normalizeSubject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

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
        
        if (BuildConfig.DEBUG) {
            Log.d("FIRESTORE", "[FIRESTORE] User fetch uid=$uid")
            if (isDuplicate) {
                Log.d("FIRESTORE", "[FIRESTORE] User fetch duplicate=true uid=$uid")
            }
            Log.d("FIRESTORE", "[FIRESTORE] User fetch cacheHit=$fromCache uid=$uid")
        }
    }
}

class DocumentDetailRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    companion object {
        // 10 minutes timed cache for contributor levels
        private val contributorLevelCache = TimedMemoryCache<String, String>(10 * 60 * 1000L)
    }

    suspend fun getDocument(
        documentId: String,
        requestingScope: com.pravor.notessharing.core.util.AcademicScope?
    ): DocumentDetail? = getDocument(documentId, collectionName = null, requestingScope = requestingScope)

    suspend fun getDocument(
        documentId: String,
        collectionName: String? = null,
        requestingScope: com.pravor.notessharing.core.util.AcademicScope? = null
    ): DocumentDetail? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (BuildConfig.DEBUG) {
            Log.d("PERF", "[PERF] getDocument START id=$documentId collection=$collectionName thread=${Thread.currentThread().name}")
        }
        return@withContext try {
            val targetCollections = if (!collectionName.isNullOrBlank()) {
                val clean = collectionName.lowercase().trim()
                val mapped = when {
                    clean.contains("pyq") -> "pyqs"
                    clean.contains("assignment") -> "assignments"
                    clean.contains("cheat") || clean.contains("formula") -> "cheatsheets"
                    clean.contains("notes") || clean.contains("note") -> "notes"
                    clean.contains("video") || clean.contains("youtube") -> "videos"
                    else -> clean
                }
                listOf(mapped)
            } else {
                listOf("notes", "pyqs", "assignments", "cheatsheets")
            }

            var foundData: Pair<Map<String, Any>, String>? = null
            coroutineScope {
                val deferreds = targetCollections.map { col ->
                    async {
                        try {
                            if (BuildConfig.DEBUG) {
                                Log.d("PERF", "[PERF] Collection searched=$col")
                                Log.d("FIRESTORE", "[FIRESTORE] Firestore query START collection=$col document=$documentId thread=${Thread.currentThread().name}")
                            }
                            val snap = firestore.collection(col).document(documentId).get().await()
                            if (BuildConfig.DEBUG) {
                                val firestoreQueryDuration = System.currentTimeMillis() - startTime
                                Log.d("FIRESTORE", "[FIRESTORE] Firestore query END collection=$col document=$documentId duration=${firestoreQueryDuration}ms exists=${snap.exists()} thread=${Thread.currentThread().name}")
                            }
                            if (snap.exists() && snap.data != null) {
                                if (BuildConfig.DEBUG) {
                                    Log.d("PERF", "[PERF] Found in collection=$col")
                                }
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
                foundData?.first?.toDocumentDetail(documentId, foundData?.second ?: "notes")
            } else {
                null
            }

            // Academic Authorization Scope Validation
            if (result != null && requestingScope != null && requestingScope.isCollegeValid) {
                val resolvedSubjectId = result.subjectId ?: (foundData?.first?.get("subjectId") as? String)
                val isPermitted = requestingScope.isDocumentPermitted(
                    docCollege = result.college,
                    docBranch = result.branch,
                    docSemester = result.semester,
                    docSubjectId = resolvedSubjectId,
                    docSubjectName = result.subject
                )
                if (!isPermitted) {
                    if (BuildConfig.DEBUG) {
                        Log.w("SECURITY", "[SECURITY] Document $documentId access DENIED for scope: ${requestingScope.scopeKey}")
                    }
                    return@withContext null
                }
            }

            if (BuildConfig.DEBUG) {
                val duration = System.currentTimeMillis() - startTime
                Log.d("PERF", "[PERF] getDocument END duration=${duration}ms")
            }
            result
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                val duration = System.currentTimeMillis() - startTime
                Log.d("PERF", "[PERF] getDocument END duration=${duration}ms")
            }
            null
        }
    }

    suspend fun getUploaderContributorLevel(uploaderId: String): String? {
        if (uploaderId == "dummy-uid" || uploaderId.isEmpty()) {
            return "Gold Contributor" // Premium look for dummy uploader
        }

        // Check timed memory cache first
        val cachedLevel = contributorLevelCache.get(uploaderId)
        if (cachedLevel != null) {
            UserFetchDiagnostics.recordFetch(uploaderId, fromCache = true)
            return cachedLevel
        }

        val startTime = System.currentTimeMillis()
        return try {
            if (BuildConfig.DEBUG) {
                Log.d("FIRESTORE", "[FIRESTORE] Firestore query START collection=users document=$uploaderId thread=${Thread.currentThread().name}")
            }
            val snapshot = usersCollection.document(uploaderId).get().await()
            if (BuildConfig.DEBUG) {
                val duration = System.currentTimeMillis() - startTime
                Log.d("FIRESTORE", "[FIRESTORE] Firestore query END collection=users document=$uploaderId duration=${duration}ms exists=${snapshot.exists()} thread=${Thread.currentThread().name}")
            }
            
            val fromCache = snapshot.metadata.isFromCache
            UserFetchDiagnostics.recordFetch(uploaderId, fromCache)
 
            val resolvedLevel = if (snapshot.exists()) {
                val level = snapshot.getLong("contributorLevel")?.toInt() ?: 1
                getContributorLevelName(level)
            } else {
                "Bronze Contributor"
            }

            // Save to cache
            contributorLevelCache.put(uploaderId, resolvedLevel)
            resolvedLevel
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                val duration = System.currentTimeMillis() - startTime
                Log.d("FIRESTORE", "[FIRESTORE] Firestore query END collection=users document=$uploaderId duration=${duration}ms exists=false thread=${Thread.currentThread().name}")
            }
            "Bronze Contributor"
        }
    }

    private fun isVideoResource(data: Map<String, Any>): Boolean {
        val docType = (data["documentType"] as? String ?: data["type"] as? String ?: "").trim()
        val contentType = (data["contentType"] as? String ?: "").trim()
        val hasYoutubeLink = (data["hasYoutubeLink"] as? Boolean) == true || (data["hasYoutubeLink"] as? String)?.lowercase() == "true"
        val sourceType = (data["sourceType"] as? String ?: "").trim()
        val youtubeUrl = (data["youtubeUrl"] as? String ?: "").trim()
        val youtubeVideoId = (data["youtubeVideoId"] as? String ?: "").trim()
        val resourceType = (data["resourceType"] as? String ?: "").trim()
        val source = (data["source"] as? String ?: "").trim()

        return docType.equals("VIDEO", ignoreCase = true) ||
                docType.equals("YouTube Resource", ignoreCase = true) ||
                docType.equals("Videos", ignoreCase = true) ||
                contentType.equals("VIDEO", ignoreCase = true) ||
                hasYoutubeLink ||
                sourceType.equals("youtube", ignoreCase = true) ||
                sourceType.equals("video", ignoreCase = true) ||
                youtubeUrl.isNotBlank() ||
                youtubeVideoId.isNotBlank() ||
                resourceType.equals("VIDEO", ignoreCase = true) ||
                source.equals("YOUTUBE", ignoreCase = true)
    }

    suspend fun getRelatedDocuments(
        doc: DocumentDetail,
        requestingScope: com.pravor.notessharing.core.util.AcademicScope? = null
    ): List<DocumentDetail> = coroutineScope {
        val startTime = System.currentTimeMillis()
        if (BuildConfig.DEBUG) {
            Log.d("PERF", "[PERF] getRelatedDocuments START id=${doc.id} thread=${Thread.currentThread().name}")
        }
        
        val col = doc.collection
        if (col.isBlank() || doc.college.isBlank()) {
            if (BuildConfig.DEBUG) {
                Log.e("RECOMMENDATIONS", "getRelatedDocuments: collection or college is blank for docId=${doc.id}. Returning empty recommendations.")
                Log.d("REC_TRACE", "[DOC] 1. Candidates fetched: 0 (collection or college is blank)")
                Log.d("REC_TRACE", "[DOC] 2. Count after type filtering: 0")
                Log.d("REC_TRACE", "[DOC] 3. Count after current-item exclusion: 0")
                Log.d("REC_TRACE", "[DOC] 4. Counts after subject partitioning: sameSubject=0, otherSubjects=0")
                Log.d("REC_TRACE", "[DOC] 5. Final recommendations returned by repo count=0")
            }
            return@coroutineScope emptyList()
        }
        
        try {
            val canonicalCollegeId = com.pravor.notessharing.core.util.LegacyAcademicCompatibilityResolver.resolveCollegeId(doc.college)
            val firestoreQueryStartTime = System.currentTimeMillis()
            val querySnapshot = firestore.collection(col)
                .whereEqualTo("college", canonicalCollegeId)
                .get()
                .await()
            
            if (BuildConfig.DEBUG) {
                val firestoreQueryDuration = System.currentTimeMillis() - firestoreQueryStartTime
                Log.d("FIRESTORE", "[FIRESTORE] getRelatedDocuments single query collection=$col duration=${firestoreQueryDuration}ms docs=${querySnapshot.documents.size}")
            }
            
            val candidates = querySnapshot.documents
                .sortedWith(ExploreRankingUtils.documentSnapshotComparator)
                .take(100)
            
            if (BuildConfig.DEBUG) {
                Log.d("REC_TRACE", "[DOC] 1. Candidates fetched from collection=$col count=${candidates.size}")
            }
            
            val currentNormalizedSubject = normalizeSubject(doc.subject)
            
            val sameSubjectPairs = mutableListOf<Pair<com.google.firebase.firestore.DocumentSnapshot, DocumentDetail>>()
            val otherSubjectPairs = mutableListOf<Pair<com.google.firebase.firestore.DocumentSnapshot, DocumentDetail>>()
            
            var afterTypeFilter = 0
            var afterCurrentItemExclusion = 0
            
            for (d in candidates) {
                val data = d.data ?: continue
                
                // Type filter (excludes video resources)
                if (isVideoResource(data)) continue
                afterTypeFilter++
                
                // Current item exclusion
                if (d.id == doc.id) continue
                afterCurrentItemExclusion++
                
                val mappedDoc = data.toDocumentDetail(d.id, col)
                
                // Academic Scope Validation
                if (requestingScope != null && requestingScope.isCollegeValid) {
                    val isPermitted = requestingScope.isDocumentPermitted(
                        docCollege = mappedDoc.college,
                        docBranch = mappedDoc.branch,
                        docSemester = mappedDoc.semester,
                        docSubjectId = mappedDoc.subjectId ?: (data["subjectId"] as? String),
                        docSubjectName = mappedDoc.subject
                    )
                    if (!isPermitted) continue
                }

                val candidateNormalizedSubject = normalizeSubject(mappedDoc.subject)
                
                if (candidateNormalizedSubject == currentNormalizedSubject) {
                    sameSubjectPairs.add(d to mappedDoc)
                } else {
                    otherSubjectPairs.add(d to mappedDoc)
                }
            }
            
            if (BuildConfig.DEBUG) {
                Log.d("REC_TRACE", "[DOC] 2. Count after type filtering (non-videos only) count=$afterTypeFilter")
                Log.d("REC_TRACE", "[DOC] 3. Count after current-item exclusion count=$afterCurrentItemExclusion")
                Log.d("REC_TRACE", "[DOC] 4. Counts after subject partitioning: sameSubject=${sameSubjectPairs.size}, otherSubjects=${otherSubjectPairs.size}")
            }
            
            val pairComparator = Comparator<Pair<com.google.firebase.firestore.DocumentSnapshot, DocumentDetail>> { p1, p2 ->
                ExploreRankingUtils.documentSnapshotComparator.compare(p1.first, p2.first)
            }
            
            val sortedSame = ExploreRankingUtils.sortWithTieBreak(sameSubjectPairs, pairComparator).map { it.second }
            val sortedOther = ExploreRankingUtils.sortWithTieBreak(otherSubjectPairs, pairComparator).map { it.second }
            
            val combined = (sortedSame + sortedOther)
                .distinctBy { it.id }
                .take(5)
            
            if (BuildConfig.DEBUG) {
                val duration = System.currentTimeMillis() - startTime
                Log.d("PERF", "[PERF] getRelatedDocuments END duration=${duration}ms count=${combined.size}")
                Log.d("REC_TRACE", "[DOC] 5. Final recommendations returned by repo count=${combined.size} items=${combined.map { it.id }}")
            }
            combined
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                val duration = System.currentTimeMillis() - startTime
                Log.d("PERF", "[PERF] getRelatedDocuments END duration=${duration}ms error")
                Log.d("REC_TRACE", "[DOC] 5. Final recommendations returned by repo count=0 due to exception: ${e.message}")
            }
            emptyList()
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
}
