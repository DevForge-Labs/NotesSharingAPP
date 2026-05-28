package com.pravor.notessharing.data

import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.model.VideoDetail
import com.pravor.notessharing.model.toVideoDetail
import com.pravor.notessharing.viewmodel.DummyData
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class VideoDetailRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun getVideo(videoId: String): VideoDetail? {
        return try {
            val collections = listOf("documents", "videos")
            var foundData: Map<String, Any>? = null
            coroutineScope {
                val deferreds = collections.map { col ->
                    async {
                        try {
                            val snap = firestore.collection(col).document(videoId).get().await()
                            if (snap.exists()) snap.data else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                foundData = deferreds.awaitAll().firstOrNull { it != null }
            }
            if (foundData != null) {
                foundData?.toVideoDetail(videoId)
            } else {
                getDummyVideoDetail(videoId)
            }
        } catch (e: Exception) {
            getDummyVideoDetail(videoId)
        }
    }

    suspend fun getUploaderContributorLevel(uploaderId: String): String? {
        if (uploaderId == "dummy-uid" || uploaderId.isEmpty()) {
            return "Gold Contributor"
        }
        return try {
            val snapshot = usersCollection.document(uploaderId).get().await()
            if (snapshot.exists()) {
                val level = snapshot.getLong("contributorLevel")?.toInt() ?: 1
                getContributorLevelName(level)
            } else {
                "Bronze Contributor"
            }
        } catch (e: Exception) {
            "Bronze Contributor"
        }
    }

    suspend fun getRelatedVideos(video: VideoDetail): List<VideoDetail> {
        return try {
            val collections = listOf("documents", "videos")
            val allRelatedDocs = coroutineScope {
                val deferreds = collections.map { col ->
                    async {
                        try {
                            firestore.collection(col)
                                .whereEqualTo("semester", video.semester)
                                .whereEqualTo("subject", video.subject)
                                .limit(10)
                                .get()
                                .await()
                                .documents
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                }
                deferreds.awaitAll().flatten()
            }

            val realRelated = allRelatedDocs.mapNotNull { d ->
                if (d.id == video.id) return@mapNotNull null
                val data = d.data ?: return@mapNotNull null
                
                val docType = (data["documentType"] as? String ?: data["type"] as? String ?: "").trim()
                val isVideo = docType.equals("VIDEO", ignoreCase = true) ||
                        docType.equals("YouTube Resource", ignoreCase = true) ||
                        docType.equals("Videos", ignoreCase = true) ||
                        (data["youtubeUrl"] as? String)?.isNotBlank() == true

                if (!isVideo) return@mapNotNull null
                data.toVideoDetail(d.id)
            }.distinctBy { it.id }.take(3)

            if (realRelated.isNotEmpty()) {
                realRelated
            } else {
                getDummyRelatedVideos(video)
            }
        } catch (e: Exception) {
            getDummyRelatedVideos(video)
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

    private fun getDummyRelatedVideos(video: VideoDetail): List<VideoDetail> {
        val allDummyVideos = DummyData.videoRecommendations.map { getDummyVideoDetail(it.id) }.filterNotNull()
        return allDummyVideos.filter { dummy ->
            dummy.id != video.id &&
            (dummy.semester == video.semester || dummy.subject == video.subject)
        }.take(3)
    }

    fun getDummyVideoDetail(id: String): VideoDetail? {
        val recommendation = DummyData.videoRecommendations.find { it.id == id }
        val youtubeVideoId = when (id) {
            "video-0" -> "1jW01e149r8"
            "video-1" -> "wp4Hli1sOQc"
            "video-2" -> "IPvYjXCsS1U"
            "video-3" -> "vLnPwxZdW4Y"
            "video-4" -> "0IAPZzGSbME"
            "video-5" -> "HXV3zeQKqGY"
            "video-6" -> "4p15H1S-VQE"
            "video-7" -> "3QhdQBU2AK0"
            "video-8" -> "aqvDSDLOb4c"
            "video-9" -> "GwIo3gToUt0"
            else -> "1jW01e149r8"
        }
        
        if (recommendation != null) {
            return VideoDetail(
                id = id,
                title = recommendation.title,
                description = "Learn ${recommendation.subject} with ${recommendation.channelName}. This is a highly recommended video guide covering core curriculum concepts.",
                branch = "Computer Science",
                semester = "Semester 4",
                subject = recommendation.subject,
                uploaderId = "dummy-uid",
                uploaderName = recommendation.channelName,
                uploaderPhotoUrl = "",
                uploadedAt = System.currentTimeMillis() - 86400000 * 5,
                youtubeUrl = "https://www.youtube.com/watch?v=$youtubeVideoId",
                youtubeVideoId = youtubeVideoId,
                upvotes = 350 + (id.hashCode() % 150),
                downloads = 45,
                bookmarks = 0
            )
        }
        
        // Check if there is an item in feedItems that is a video
        val feedVideo = DummyData.feedItems.find { it.id == id }
        if (feedVideo != null) {
            return VideoDetail(
                id = id,
                title = feedVideo.title,
                description = feedVideo.description,
                branch = "Computer Science",
                semester = "Semester 4",
                subject = feedVideo.tags.firstOrNull() ?: "General",
                uploaderId = "dummy-uid",
                uploaderName = feedVideo.uploaderName,
                uploaderPhotoUrl = "",
                uploadedAt = System.currentTimeMillis() - 86400000 * 2,
                youtubeUrl = "https://www.youtube.com/watch?v=wp4Hli1sOQc",
                youtubeVideoId = "wp4Hli1sOQc",
                upvotes = feedVideo.upvotes,
                downloads = feedVideo.downloads,
                bookmarks = if (feedVideo.isSaved) 1 else 0
            )
        }
        
        return null
    }
}
