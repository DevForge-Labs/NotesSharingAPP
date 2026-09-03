package com.pravor.notessharing.data.repository

import com.pravor.notessharing.domain.util.ExploreRankingUtils
import com.pravor.notessharing.domain.model.*
import com.pravor.notessharing.core.util.*

import android.content.Context
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.data.mapper.ExploreMapper
import com.pravor.notessharing.domain.model.TrendingNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class TrendingFeedRepository(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val roomRepository = ExploreRoomRepository(context)

    private val _trendingNotes = MutableStateFlow<List<TrendingNote>>(emptyList())
    val trendingNotes: StateFlow<List<TrendingNote>> = _trendingNotes.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var allScoredCandidates: List<TrendingNote> = emptyList()
    private var displayedCount = 0

    companion object {
        private const val PAGE_SIZE = 15
    }

    suspend fun refresh(scope: AcademicScope) {
        if (!scope.isCollegeValid) {
            _trendingNotes.value = emptyList()
            return
        }

        // Hydrate from Room DB cache immediately
        if (_trendingNotes.value.isEmpty()) {
            val cached = roomRepository.getCachedTrendingNotes(scope.scopeKey)
            if (cached.isNotEmpty()) {
                _trendingNotes.value = cached
                allScoredCandidates = cached
                displayedCount = cached.size
            }
        }

        if (_isRefreshing.value) return
        _isRefreshing.value = true
        try {
            val freshNotes = fetchAllTrendingFromFirestore(scope)
            allScoredCandidates = freshNotes
            _trendingNotes.value = freshNotes
            displayedCount = freshNotes.size
            roomRepository.saveTrendingNotes(scope.scopeKey, freshNotes)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isRefreshing.value = false
        }
    }

    suspend fun loadMore(scope: AcademicScope) {
        if (!scope.isCollegeValid || _isLoadingMore.value) return
        if (_trendingNotes.value.size >= allScoredCandidates.size) return

        _isLoadingMore.value = true
        try {
            _trendingNotes.value = allScoredCandidates
            displayedCount = allScoredCandidates.size
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isLoadingMore.value = false
        }
    }

    private suspend fun fetchAllTrendingFromFirestore(scope: AcademicScope): List<TrendingNote> = withContext(Dispatchers.IO) {
        val canonicalCollegeId = scope.canonicalCollegeId

        val allDocs = try {
            firestore.collection("notes")
                .whereEqualTo("college", canonicalCollegeId)
                .get()
                .await()
                .documents
        } catch (e: Exception) {
            try {
                firestore.collection("notes").get().await().documents
            } catch (e2: Exception) {
                emptyList()
            }
        }

        val nonVideoDocs = allDocs.filter { doc ->
            val data = doc.data ?: return@filter false
            val docId = doc.id
            val idField = data["id"] as? String
            val docIdField = data["documentId"] as? String
            val title = data["title"] as? String
            val uploaderId = data["uploaderId"] as? String

            val isIdBlank = docId.isBlank() && (idField.isNullOrBlank()) && (docIdField.isNullOrBlank())
            val isTitleBlank = title.isNullOrBlank()
            val isDummyUploader = uploaderId == "dummy-uid"

            val isGenuineNote = ExploreMapper.determineResourceType(data, "notes") == ResourceType.NOTE

            isGenuineNote && !isIdBlank && !isTitleBlank && !isDummyUploader
        }

        // Filter by scope
        var scopedDocs = nonVideoDocs.filter { doc ->
            val data = doc.data ?: return@filter false
            scope.isDocumentPermitted(
                docCollege = data["college"] as? String ?: canonicalCollegeId,
                docBranch = data["branch"] as? String,
                docSemester = data["semester"] as? String,
                docSubjectId = data["subjectId"] as? String,
                docSubjectName = (data["displaySubject"] as? String)?.takeIf { it.isNotBlank() } ?: (data["subject"] as? String)
            )
        }

        // Fallback: If user's specific semester/branch has very few docs (< 5), include college-wide docs
        if (scopedDocs.size < 5 && nonVideoDocs.isNotEmpty()) {
            val fallbackDocs = nonVideoDocs.filter { doc ->
                val data = doc.data ?: return@filter false
                val docCollege = data["college"] as? String
                docCollege == null || LegacyAcademicCompatibilityResolver.resolveCollegeId(docCollege) == canonicalCollegeId
            }
            scopedDocs = (scopedDocs + fallbackDocs).distinctBy { it.id }
        }

        val sortedDocs = scopedDocs.sortedWith(ExploreRankingUtils.documentSnapshotComparator)

        val detailRepository = DocumentDetailRepository()
        val uploaderIds = sortedDocs.mapNotNull { doc ->
            val uploaderId = (doc.data ?: emptyMap<String, Any>())["uploaderId"] as? String
            if (!uploaderId.isNullOrBlank() && uploaderId != "dummy-uid") uploaderId else null
        }.distinct()

        val resolvedLevels = coroutineScope {
            uploaderIds.map { uid ->
                async {
                    val level = detailRepository.getUploaderContributorLevel(uid) ?: "Bronze Contributor"
                    uid to level
                }
            }.awaitAll().toMap()
        }

        val bookmarkedIds = BookmarkRepository.bookmarksFlow.value.map { it.id }.toSet()

        val mappedNotes = sortedDocs.mapNotNull { doc ->
            ExploreMapper.documentToTrendingNote(doc, bookmarkedIds, resolvedLevels)
        }.filter { it.resourceType == ResourceType.NOTE }

        mappedNotes
    }
}

