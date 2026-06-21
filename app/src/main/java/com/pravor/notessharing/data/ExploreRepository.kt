package com.pravor.notessharing.data

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.BuildConfig
import com.pravor.notessharing.data.cache.TimedValueCache
import com.pravor.notessharing.model.*
import com.pravor.notessharing.state.ExploreContent
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

class ExploreRepository(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val diskCache = ExploreCacheRepository(context)

    companion object {
        private val exploreCache = TimedValueCache<ExploreContent>(5 * 60 * 1000L) // 5 minutes TTL
        
        // Request deduplication safeguards
        private val mutex = Mutex()
        private var activeFetch: Deferred<ExploreContent>? = null
        private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    init {
        // Hydrate in-memory cache from disk cache on startup (treated as expired to trigger immediate background refresh)
        if (exploreCache.getExpiredButAvailable() == null) {
            diskCache.getCache()?.let {
                exploreCache.putExpired(it)
            }
        }
    }

    fun getCachedContent(): ExploreContent? {
        return exploreCache.getExpiredButAvailable()
    }

    fun isCacheExpired(): Boolean {
        return exploreCache.isExpired()
    }

    suspend fun fetchExploreContent(): ExploreContent {
        val deferred = mutex.withLock {
            val current = activeFetch
            if (current != null && current.isActive) {
                current
            } else {
                val next = repositoryScope.async {
                    try {
                        doFetchExploreContent()
                    } finally {
                        mutex.withLock {
                            if (activeFetch === coroutineContext[Job]) {
                                activeFetch = null
                            }
                        }
                    }
                }
                activeFetch = next
                next
            }
        }
        return deferred.await()
    }

    private suspend fun doFetchExploreContent(): ExploreContent = withContext(Dispatchers.IO) {
        val collections = listOf("documents", "notes", "pyqs", "assignments", "cheatsheets", "videos")
        
        val allDocs = coroutineScope {
            val deferreds = collections.map { col ->
                async {
                    try {
                        firestore.collection(col).get().await().documents
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }
            deferreds.awaitAll().flatten()
        }.sortedWith(
            compareByDescending<com.google.firebase.firestore.DocumentSnapshot> { doc ->
                (doc.data?.get("trendingScore") as? Number)?.toDouble() ?: 0.0
            }.thenByDescending { doc ->
                doc.getLong("uploadedAt") ?: 0L
            }
        )

        val realFeed = allDocs.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            val id = data["documentId"] as? String ?: ""
            val title = data["title"] as? String ?: ""
            val uploaderId = data["uploaderId"] as? String
            if (id.isBlank() || title.isBlank() || uploaderId == "dummy-uid") return@mapNotNull null
            documentToFeedItem(data)
        }

        val bookmarkedIds = com.pravor.notessharing.bookmarks.BookmarkRepository.bookmarksFlow.value.map { it.id }.toSet()

        val realTrending = allDocs.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            val docType = (data["documentType"] as? String ?: data["type"] as? String ?: "").trim()
            val contentType = (data["contentType"] as? String ?: "").trim()
            val hasYoutubeLink = (data["hasYoutubeLink"] as? Boolean) == true || (data["hasYoutubeLink"] as? String)?.lowercase() == "true"
            val sourceType = (data["sourceType"] as? String ?: "").trim()
            val youtubeUrl = (data["youtubeUrl"] as? String ?: "").trim()
            val youtubeVideoId = (data["youtubeVideoId"] as? String ?: "").trim()
            val resourceType = (data["resourceType"] as? String ?: "").trim()
            val source = (data["source"] as? String ?: "").trim()

            val isVideo = docType.equals("VIDEO", ignoreCase = true) ||
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

            if (isVideo) return@mapNotNull null

            val id = data["documentId"] as? String ?: ""
            val title = data["title"] as? String ?: ""
            val uploaderId = data["uploaderId"] as? String
            if (id.isBlank() || title.isBlank() || uploaderId == "dummy-uid") return@mapNotNull null
            
            val subject = data["subject"] as? String ?: ""
            val displaySubjectVal = data["displaySubject"] as? String
            val downloadsCount = (data["downloadsCount"] as? Long ?: 0L).toInt()
            val upvotes = (data["upvotes"] as? Long ?: data["likesCount"] as? Long ?: 0L).toInt()
            val thumbnailUrl = (data["thumbnailUrl"] as? String)?.ifBlank { null }
                ?: (data["youtubeThumbnailUrl"] as? String)?.ifBlank { null }
            val thumbnailGenerated = data["thumbnailGenerated"] as? Boolean
            val thumbnailType = data["thumbnailType"] as? String
            val documentTypeField = data["documentType"] as? String
            val typeField = data["type"] as? String
            val examYearVal = data["examYear"] as? String
            val branchVal = data["branch"] as? String ?: ""
            val sectionDisplayVal = data["sectionDisplay"] as? String

            val resolvedIsUpvoted = com.pravor.notessharing.upvotes.UpvoteRepository.upvotesFlow.value[id] ?: false
            val resolvedUpvotes = com.pravor.notessharing.upvotes.UpvoteRepository.upvoteCountsFlow.value[id] ?: upvotes
            val trendingScore = (data["trendingScore"] as? Number)?.toDouble() ?: 0.0

            TrendingNote(
                id = id,
                title = title,
                subject = subject,
                downloadsCount = downloadsCount,
                rating = 4.5,
                upvotes = resolvedUpvotes,
                isBookmarked = bookmarkedIds.contains(id),
                thumbnailUrl = thumbnailUrl,
                thumbnailGenerated = thumbnailGenerated,
                thumbnailType = thumbnailType,
                documentType = documentTypeField ?: "",
                type = typeField,
                examYear = examYearVal,
                isUpvoted = resolvedIsUpvoted,
                branch = branchVal,
                trendingScore = trendingScore,
                displaySubject = displaySubjectVal,
                sectionDisplay = sectionDisplayVal
            )
        }

        val realVideos = allDocs.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            val docType = (data["documentType"] as? String ?: data["type"] as? String ?: "").trim()
            val contentType = (data["contentType"] as? String ?: "").trim()
            val hasYoutubeLink = (data["hasYoutubeLink"] as? Boolean) == true || (data["hasYoutubeLink"] as? String)?.lowercase() == "true"
            val sourceType = (data["sourceType"] as? String ?: "").trim()
            val youtubeUrl = (data["youtubeUrl"] as? String ?: "").trim()
            val youtubeVideoId = (data["youtubeVideoId"] as? String ?: "").trim()

            val isVideo = docType.equals("VIDEO", ignoreCase = true) ||
                    docType.equals("YouTube Resource", ignoreCase = true) ||
                    docType.equals("Videos", ignoreCase = true) ||
                    contentType.equals("VIDEO", ignoreCase = true) ||
                    hasYoutubeLink ||
                    sourceType.equals("youtube", ignoreCase = true) ||
                    sourceType.equals("video", ignoreCase = true) ||
                    youtubeUrl.isNotBlank() ||
                    youtubeVideoId.isNotBlank()

            if (!isVideo) return@mapNotNull null

            val id = data["documentId"] as? String ?: ""
            val title = data["title"] as? String ?: data["videoTitle"] as? String ?: ""
            val uploaderId = data["uploaderId"] as? String
            if (id.isBlank() || title.isBlank() || uploaderId == "dummy-uid") return@mapNotNull null
            
            val subject = data["subject"] as? String ?: ""
            val uploaderName = data["uploaderName"] as? String ?: "Anonymous"
            val upvotes = (data["upvotes"] as? Long ?: 0L).toInt()
            val bookmarks = (data["bookmarks"] as? Long ?: 0L).toInt()
            val thumbnailUrlVal = data["thumbnailUrl"] as? String
            val youtubeThumbnailUrlVal = data["youtubeThumbnailUrl"] as? String
            val semesterVal = data["semester"] as? String ?: "Semester 4"

            val resolvedIsUpvoted = com.pravor.notessharing.upvotes.UpvoteRepository.upvotesFlow.value[id] ?: false
            val resolvedUpvotes = com.pravor.notessharing.upvotes.UpvoteRepository.upvoteCountsFlow.value[id] ?: upvotes
            val resolvedIsBookmarked = bookmarkedIds.contains(id)

            VideoRecommendation(
                id = id,
                title = title,
                channelName = uploaderName,
                duration = "",
                subject = subject,
                youtubeVideoId = youtubeVideoId,
                upvotes = resolvedUpvotes,
                bookmarks = bookmarks,
                thumbnailUrl = thumbnailUrlVal,
                youtubeThumbnailUrl = youtubeThumbnailUrlVal,
                documentType = docType,
                semester = semesterVal,
                youtubeUrl = youtubeUrl,
                isUpvoted = resolvedIsUpvoted,
                isBookmarked = resolvedIsBookmarked
            )
        }

        val realDiscover = allDocs.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            val id = data["documentId"] as? String ?: ""
            val title = data["title"] as? String ?: ""
            val uploaderId = data["uploaderId"] as? String
            if (id.isBlank() || title.isBlank() || uploaderId == "dummy-uid") return@mapNotNull null
            val subject = data["subject"] as? String ?: ""
            val downloadsCount = (data["downloadsCount"] as? Long ?: 0L).toInt()
            DiscoverFeedItem.Note(
                id = id,
                title = title,
                subject = subject,
                downloadsCount = downloadsCount
            )
        }

        val freshContent = ExploreContent(
            topics = emptyList(),
            popularUploads = realFeed.distinctBy { it.id },
            trendingNotes = realTrending,
            videoRecommendations = realVideos,
            studyCollections = emptyList(),
            subjectHubs = emptyList(),
            topContributors = emptyList(),
            revisionCards = emptyList(),
            discoverItems = realDiscover.distinctBy { it.id }
        )

        // Sync with timed in-memory cache and SharedPreferences persistence fallback
        exploreCache.put(freshContent)
        diskCache.saveCache(freshContent)

        freshContent
    }

    private fun documentToFeedItem(doc: Map<String, Any>): FeedItem {
        val id = doc["documentId"] as? String ?: ""
        val uploaderName = doc["uploaderName"] as? String ?: "Anonymous"
        val initials = uploaderName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
            .ifBlank { "AN" }
        
        val uploadedAt = doc["uploadedAt"] as? Long ?: (doc["uploadTimestamp"] as? Long ?: System.currentTimeMillis())
        val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
        val uploadDate = sdf.format(java.util.Date(uploadedAt))
        
        val docType = doc["documentType"] as? String ?: (doc["type"] as? String ?: "Notes")
        val fileType = when (docType) {
            "PYQ" -> FileType.Pyq
            "Cheat Sheet" -> FileType.CheatSheet
            "Assignment" -> FileType.Notes
            "Notes" -> FileType.Notes
            "YouTube Resource", "Videos" -> FileType.Video
            else -> FileType.Pdf
        }
        
        val subject = doc["subject"] as? String ?: ""
        val displayTitle = doc["title"] as? String ?: ""

        val description = doc["description"] as? String ?: ""
        val tags = (doc["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        
        val upvotes = (doc["upvotes"] as? Long ?: (doc["likesCount"] as? Long ?: 0L)).toInt()
        val downloadsCount = (doc["downloadsCount"] as? Long ?: 0L).toInt()
        val bookmarks = (doc["bookmarks"] as? Long ?: 0L).toInt()

        val youtubeUrl = doc["youtubeUrl"] as? String
        val youtubeVideoId = doc["youtubeVideoId"] as? String
        
        val thumbnailUrl = (doc["thumbnailUrl"] as? String)?.ifBlank { null }
            ?: (doc["youtubeThumbnailUrl"] as? String)?.ifBlank { null }
        val thumbnailGenerated = doc["thumbnailGenerated"] as? Boolean
        val thumbnailType = doc["thumbnailType"] as? String

        val documentTypeField = doc["documentType"] as? String
        val typeField = doc["type"] as? String
        val subjectField = doc["subject"] as? String
        val examYearField = (doc["examYear"] ?: doc["year"])?.toString()
        val examTypeField = doc["examType"] as? String
        val sectionField = doc["section"] as? String
        val sectionDisplayField = doc["sectionDisplay"] as? String

        val upvotedMap = com.pravor.notessharing.upvotes.UpvoteRepository.upvotesFlow.value
        val upvoteCountsMap = com.pravor.notessharing.upvotes.UpvoteRepository.upvoteCountsFlow.value
        val resolvedIsUpvoted = upvotedMap[id] ?: false
        val resolvedUpvotes = upvoteCountsMap[id] ?: upvotes

        return FeedItem(
            id = id,
            uploaderName = uploaderName,
            uploaderInitials = initials,
            uploadDate = uploadDate,
            title = displayTitle,
            description = description,
            tags = tags,
            fileType = fileType,
            upvotes = resolvedUpvotes,
            comments = 0,
            downloadsCount = downloadsCount,
            isUpvoted = resolvedIsUpvoted,
            isSaved = false,
            bookmarksCount = bookmarks,
            youtubeVideoId = youtubeVideoId,
            youtubeUrl = youtubeUrl,
            thumbnailUrl = thumbnailUrl,
            thumbnailGenerated = thumbnailGenerated,
            thumbnailType = thumbnailType,
            documentType = documentTypeField,
            type = typeField,
            subject = subjectField,
            examYear = examYearField,
            examType = examTypeField,
            section = sectionField,
            sectionDisplay = sectionDisplayField
        )
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
}
