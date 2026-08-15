package com.pravor.notessharing.data

import android.content.Context
import com.pravor.notessharing.data.mapper.ExploreMapper
import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.BuildConfig
import com.pravor.notessharing.data.cache.TimedValueCache
import com.pravor.notessharing.data.repository.ExploreRoomRepository
import com.pravor.notessharing.model.*
import com.pravor.notessharing.state.ExploreContent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

class ExploreRepository(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val roomRepository = ExploreRoomRepository(context)

    companion object {
        private val memoryCaches = java.util.concurrent.ConcurrentHashMap<String, TimedValueCache<ExploreContent>>()
        
        // Request deduplication safeguards
        private val mutex = Mutex()
        private var activeFetch: Deferred<ExploreContent>? = null
        private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    fun observeExploreContent(collegeId: String): Flow<ExploreContent?> {
        return roomRepository.observeExploreContent(collegeId)
    }

    suspend fun getCachedContent(collegeId: String): ExploreContent? {
        val canonical = com.pravor.notessharing.util.LegacyAcademicCompatibilityResolver.resolveCollegeId(collegeId)
        if (canonical.isBlank()) return null
        val cache = memoryCaches.getOrPut(canonical) { TimedValueCache(5 * 60 * 1000L) }
        val inMemory = cache.getExpiredButAvailable()
        if (inMemory != null) return inMemory
        
        val fromRoom = roomRepository.getCachedContent(canonical)
        if (fromRoom != null) {
            cache.putExpired(fromRoom)
        }
        return fromRoom
    }

    fun isCacheExpired(collegeId: String): Boolean {
        val canonical = com.pravor.notessharing.util.LegacyAcademicCompatibilityResolver.resolveCollegeId(collegeId)
        if (canonical.isBlank()) return true
        val cache = memoryCaches[canonical] ?: return true
        return cache.isExpired()
    }

    suspend fun fetchExploreContent(collegeId: String): ExploreContent {
        val canonicalCollegeId = com.pravor.notessharing.util.LegacyAcademicCompatibilityResolver.resolveCollegeId(collegeId)
        if (canonicalCollegeId.isBlank()) {
            return ExploreContent(
                topics = emptyList(),
                popularUploads = emptyList(),
                notes = emptyList(),
                examPrep = emptyList(),
                assignments = emptyList(),
                videos = emptyList(),
                studyCollections = emptyList(),
                subjectHubs = emptyList(),
                topContributors = emptyList(),
                revisionCards = emptyList(),
                discoverItems = emptyList()
            )
        }
        val deferred = mutex.withLock {
            val current = activeFetch
            if (current != null && current.isActive) {
                current
            } else {
                val next = repositoryScope.async {
                    try {
                        doFetchExploreContent(canonicalCollegeId)
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

    private suspend fun doFetchExploreContent(canonicalCollegeId: String): ExploreContent = withContext(Dispatchers.IO) {
        val collections = listOf("documents", "notes", "pyqs", "assignments", "cheatsheets", "videos")
        
        val allDocs = coroutineScope {
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

        // Sync with timed in-memory cache and Room DB persistence
        memoryCaches.getOrPut(canonicalCollegeId) { TimedValueCache(5 * 60 * 1000L) }.put(freshContent)
        roomRepository.saveExploreContent(canonicalCollegeId, freshContent)

        freshContent
    }
}
