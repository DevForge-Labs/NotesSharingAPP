package com.pravor.notessharing.data.repository


import com.pravor.notessharing.domain.util.ExploreRankingUtils
import com.pravor.notessharing.data.local.cache.*
import com.pravor.notessharing.domain.model.*

import com.pravor.notessharing.core.util.*

import android.content.Context
import com.pravor.notessharing.data.mapper.ExploreMapper
import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.BuildConfig
import com.pravor.notessharing.data.local.cache.TimedValueCache
import com.pravor.notessharing.ui.common.ExploreContent
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

    fun observeExploreContent(scopeKey: String): Flow<ExploreContent?> {
        return roomRepository.observeExploreContent(scopeKey)
    }

    suspend fun getCachedContent(scopeKey: String): ExploreContent? {
        if (scopeKey.isBlank()) return null
        val cache = memoryCaches.getOrPut(scopeKey) { TimedValueCache(30 * 1000L) }
        val inMemory = cache.getExpiredButAvailable()
        if (inMemory != null) return inMemory
        
        val fromRoom = roomRepository.getCachedContent(scopeKey)
        if (fromRoom != null) {
            cache.putExpired(fromRoom)
        }
        return fromRoom
    }

    fun isCacheExpired(scopeKey: String): Boolean {
        if (scopeKey.isBlank()) return true
        val cache = memoryCaches[scopeKey] ?: return true
        return cache.isExpired()
    }

    suspend fun fetchExploreContent(scope: AcademicScope): ExploreContent {
        if (!scope.isCollegeValid) {
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
                        doFetchExploreContent(scope)
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

    private suspend fun doFetchExploreContent(scope: AcademicScope): ExploreContent = withContext(Dispatchers.IO) {
        val collections = listOf("notes", "pyqs", "assignments", "cheatsheets", "videos")
        val canonicalCollegeId = scope.canonicalCollegeId
        
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

        val eligibleDocs = allDocs.filter { doc ->
            val data = doc.data ?: return@filter false
            scope.isDocumentPermitted(
                docCollege = data["college"] as? String ?: canonicalCollegeId,
                docBranch = data["branch"] as? String,
                docSemester = data["semester"] as? String,
                docSubjectId = data["subjectId"] as? String,
                docSubjectName = (data["displaySubject"] as? String)?.takeIf { it.isNotBlank() } ?: (data["subject"] as? String)
            )
        }

        val realFeed = eligibleDocs.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            ExploreMapper.documentToFeedItem(data)
        }

        val bookmarkedIds = com.pravor.notessharing.data.repository.BookmarkRepository.bookmarksFlow.value.map { it.id }.toSet()

        val allResources = eligibleDocs.mapNotNull { doc ->
            ExploreMapper.documentToTrendingNote(doc, bookmarkedIds)
        }

        val sortedResources = ExploreRankingUtils.sortResources(allResources)

        val notesList = ExploreRankingUtils.filterNotes(sortedResources)
        val examPrepList = ExploreRankingUtils.filterExamPrep(sortedResources)
        val assignmentsList = ExploreRankingUtils.filterAssignments(sortedResources)
        val videosList = ExploreRankingUtils.filterVideos(sortedResources)

        val realDiscover = eligibleDocs.mapNotNull { doc ->
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

        // Sync with timed in-memory cache and Room DB persistence scoped to exact academic context
        memoryCaches.getOrPut(scope.scopeKey) { TimedValueCache(30 * 1000L) }.put(freshContent)
        roomRepository.saveExploreContent(scope.scopeKey, freshContent)

        freshContent
    }
}
