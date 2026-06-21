package com.pravor.notessharing.data

import android.content.Context
import com.pravor.notessharing.data.mapper.ExploreMapper
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
        }.sortedWith(ExploreRankingUtils.documentSnapshotComparator)

        val realFeed = allDocs.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            ExploreMapper.documentToFeedItem(data)
        }

        val bookmarkedIds = com.pravor.notessharing.bookmarks.BookmarkRepository.bookmarksFlow.value.map { it.id }.toSet()

        val allResources = allDocs.mapNotNull { doc ->
            ExploreMapper.documentToTrendingNote(doc, bookmarkedIds)
        }

        // Sort all resources deterministically using our single ranking utility
        val sortedResources = ExploreRankingUtils.sortResources(allResources)

        val notesList = ExploreRankingUtils.filterNotes(sortedResources)
        val examPrepList = ExploreRankingUtils.filterExamPrep(sortedResources)
        val assignmentsList = ExploreRankingUtils.filterAssignments(sortedResources)
        val videosList = ExploreRankingUtils.filterVideos(sortedResources)

        val realDiscover = allDocs.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            ExploreMapper.documentToDiscoverNote(data)
        }

        val freshContent = ExploreContent(
            topics = emptyList(),
            popularUploads = realFeed.distinctBy { it.id },
            notes = notesList,
            examPrep = examPrepList,
            assignments = assignmentsList,
            videos = videosList,
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

}
