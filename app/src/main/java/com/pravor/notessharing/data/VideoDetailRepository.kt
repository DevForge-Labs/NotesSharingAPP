package com.pravor.notessharing.data

import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.model.VideoDetail
import com.pravor.notessharing.model.toVideoDetail
import com.pravor.notessharing.data.ExploreRankingUtils
import com.pravor.notessharing.ui.components.utils.normalizeSubject
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class VideoDetailRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun getVideo(videoId: String): VideoDetail? {
        val startTime = System.currentTimeMillis()
        android.util.Log.d("PERF", "[PERF] getVideo START id=$videoId thread=${Thread.currentThread().name}")
        return try {
            val collections = listOf("documents", "videos")
            var foundData: Pair<Map<String, Any>, String>? = null
            coroutineScope {
                val deferreds = collections.map { col ->
                    async {
                        try {
                            val firestoreQueryStartTime = System.currentTimeMillis()
                            android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query START collection=$col document=$videoId thread=${Thread.currentThread().name}")
                            val snap = firestore.collection(col).document(videoId).get().await()
                            val firestoreQueryDuration = System.currentTimeMillis() - firestoreQueryStartTime
                            android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query END collection=$col document=$videoId duration=${firestoreQueryDuration}ms exists=${snap.exists()} thread=${Thread.currentThread().name}")
                            if (snap.exists() && snap.data != null) Pair(snap.data!!, col) else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                foundData = deferreds.awaitAll().firstOrNull { it != null }
            }
            val result = if (foundData != null) {
                foundData?.first?.toVideoDetail(videoId, foundData?.second ?: "videos")
            } else {
                null
            }
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.d("PERF", "[PERF] getVideo END duration=${duration}ms id=$videoId thread=${Thread.currentThread().name}")
            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.d("PERF", "[PERF] getVideo END duration=${duration}ms id=$videoId thread=${Thread.currentThread().name}")
            null
        }
    }

    suspend fun getUploaderContributorLevel(uploaderId: String): String? {
        if (uploaderId == "dummy-uid" || uploaderId.isEmpty()) {
            return "Gold Contributor"
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

    suspend fun getRelatedVideos(video: VideoDetail): List<VideoDetail> = coroutineScope {
        val startTime = System.currentTimeMillis()
        android.util.Log.d("PERF", "[PERF] getRelatedVideos START id=${video.id} thread=${Thread.currentThread().name}")
        
        val collections = listOf("documents", "videos")
        if (video.college.isBlank()) {
            return@coroutineScope emptyList()
        }
        
        try {
            val canonicalCollegeId = com.pravor.notessharing.util.LegacyAcademicCompatibilityResolver.resolveCollegeId(video.college)
            val deferreds = collections.map { col ->
                async {
                    try {
                        firestore.collection(col)
                            .whereEqualTo("college", canonicalCollegeId)
                            .get()
                            .await()
                            .documents
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }
            
            val results = deferreds.awaitAll()
            val candidates = results.flatten()
                .sortedWith(ExploreRankingUtils.documentSnapshotComparator)
                .take(100)
            android.util.Log.d("REC_TRACE", "[VIDEO] 1. Candidates fetched from collections=$collections count=${candidates.size}")
            
            val currentNormalizedSubject = normalizeSubject(video.subject)
            
            val sameSubjectPairs = mutableListOf<Pair<com.google.firebase.firestore.DocumentSnapshot, VideoDetail>>()
            val otherSubjectPairs = mutableListOf<Pair<com.google.firebase.firestore.DocumentSnapshot, VideoDetail>>()
            
            var afterTypeFilter = 0
            var afterCurrentItemExclusion = 0
            
            for (d in candidates) {
                val data = d.data ?: continue
                
                // Type filter (keeps only video resources)
                if (!isVideoResource(data)) continue
                afterTypeFilter++
                
                // Current item exclusion
                if (d.id == video.id) continue
                afterCurrentItemExclusion++
                
                val col = d.reference.parent.id
                val mappedVideo = data.toVideoDetail(d.id, col)
                val candidateNormalizedSubject = normalizeSubject(mappedVideo.subject)
                
                if (candidateNormalizedSubject == currentNormalizedSubject) {
                    sameSubjectPairs.add(d to mappedVideo)
                } else {
                    otherSubjectPairs.add(d to mappedVideo)
                }
            }
            
            android.util.Log.d("REC_TRACE", "[VIDEO] 2. Count after type filtering (videos only) count=$afterTypeFilter")
            android.util.Log.d("REC_TRACE", "[VIDEO] 3. Count after current-item exclusion count=$afterCurrentItemExclusion")
            android.util.Log.d("REC_TRACE", "[VIDEO] 4. Counts after subject partitioning: sameSubject=${sameSubjectPairs.size}, otherSubjects=${otherSubjectPairs.size}")
            
            val pairComparator = Comparator<Pair<com.google.firebase.firestore.DocumentSnapshot, VideoDetail>> { p1, p2 ->
                ExploreRankingUtils.documentSnapshotComparator.compare(p1.first, p2.first)
            }
            
            val sortedSame = ExploreRankingUtils.sortWithTieBreak(sameSubjectPairs, pairComparator).map { it.second }
            val sortedOther = ExploreRankingUtils.sortWithTieBreak(otherSubjectPairs, pairComparator).map { it.second }
            
            val combined = (sortedSame + sortedOther)
                .distinctBy { it.id }
                .take(5)
            
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.d("PERF", "[PERF] getRelatedVideos END duration=${duration}ms count=${combined.size} thread=${Thread.currentThread().name}")
            android.util.Log.d("REC_TRACE", "[VIDEO] 5. Final recommendations returned by repo count=${combined.size} items=${combined.map { it.id }}")
            combined
        } catch (e: Exception) {
            e.printStackTrace()
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.d("PERF", "[PERF] getRelatedVideos END duration=${duration}ms error thread=${Thread.currentThread().name}")
            android.util.Log.d("REC_TRACE", "[VIDEO] 5. Final recommendations returned by repo count=0 due to exception: ${e.message}")
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
