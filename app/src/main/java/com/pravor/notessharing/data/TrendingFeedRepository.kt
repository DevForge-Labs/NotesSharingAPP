package com.pravor.notessharing.data

import android.content.Context
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pravor.notessharing.data.mapper.ExploreMapper
import com.pravor.notessharing.model.TrendingNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache

class TrendingFeedRepository(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val preferences = context.getSharedPreferences("trending_feed_cache", Context.MODE_PRIVATE)

    private val _trendingNotes = MutableStateFlow<List<TrendingNote>>(emptyList())
    val trendingNotes: StateFlow<List<TrendingNote>> = _trendingNotes.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val lastSnapshots = mutableMapOf<String, DocumentSnapshot>()
    private val isCollectionEnd = mutableMapOf<String, Boolean>()

    companion object {
        private const val KEY_FEED = "cached_trending_notes"
        private const val CACHE_LIMIT = 100
        private const val PAGE_SIZE = 10
    }

    init {
        // Load cache immediately
        _trendingNotes.value = getCachedNotes()

        // Coil image loader is initialized globally in NotesSharingApplication
        // to ensure caching is enabled immediately at app startup (for the Home screen).
    }

    private fun getCachedNotes(): List<TrendingNote> {
        val raw = preferences.getString(KEY_FEED, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<TrendingNote>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(deserializeTrendingNote(obj))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveCachedNotes(notes: List<TrendingNote>) {
        try {
            val array = JSONArray()
            notes.take(CACHE_LIMIT).forEach {
                array.put(serializeTrendingNote(it))
            }
            preferences.edit().putString(KEY_FEED, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun serializeTrendingNote(note: TrendingNote): JSONObject {
        return JSONObject().apply {
            put("id", note.id)
            put("title", note.title)
            put("subject", note.subject)
            put("downloadsCount", note.downloadsCount)
            put("rating", note.rating)
            put("upvotes", note.upvotes)
            put("isBookmarked", note.isBookmarked)
            put("thumbnailUrl", note.thumbnailUrl ?: "")
            put("thumbnailGenerated", note.thumbnailGenerated ?: false)
            put("thumbnailType", note.thumbnailType ?: "")
            put("description", note.description)
            put("uploaderName", note.uploaderName)
            put("uploaderPhotoUrl", note.uploaderPhotoUrl)
            put("contributorLevel", note.contributorLevel)
            put("documentType", note.documentType)
            put("type", note.type ?: "")
            put("bookmarks", note.bookmarks)
            put("semester", note.semester)
            put("examYear", note.examYear ?: "")
            put("examType", note.examType ?: "")
            put("trendingScore", note.trendingScore)
            put("displaySubject", note.displaySubject ?: "")
        }
    }

    private fun deserializeTrendingNote(obj: JSONObject): TrendingNote {
        return TrendingNote(
            id = obj.getString("id"),
            title = obj.getString("title"),
            subject = obj.getString("subject"),
            downloadsCount = obj.getInt("downloadsCount"),
            rating = obj.getDouble("rating"),
            upvotes = obj.getInt("upvotes"),
            isBookmarked = obj.getBoolean("isBookmarked"),
            thumbnailUrl = obj.optString("thumbnailUrl").ifBlank { null },
            thumbnailGenerated = if (obj.has("thumbnailGenerated")) obj.getBoolean("thumbnailGenerated") else null,
            thumbnailType = obj.optString("thumbnailType").ifBlank { null },
            description = obj.optString("description"),
            uploaderName = obj.optString("uploaderName"),
            uploaderPhotoUrl = obj.optString("uploaderPhotoUrl"),
            contributorLevel = obj.optString("contributorLevel"),
            documentType = obj.optString("documentType", ""),
            type = obj.optString("type").ifBlank { null },
            bookmarks = obj.optInt("bookmarks", 0),
            semester = obj.optString("semester", ""),
            examYear = obj.optString("examYear").ifBlank { null },
            examType = obj.optString("examType").ifBlank { null },
            trendingScore = obj.optDouble("trendingScore", 0.0),
            displaySubject = obj.optString("displaySubject").ifBlank { null }
        )
    }

    suspend fun refresh() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        try {
            val newNotes = fetchPageFromFirestore(isRefresh = true)
            _trendingNotes.value = newNotes
            saveCachedNotes(newNotes)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isRefreshing.value = false
        }
    }

    suspend fun loadMore() {
        if (_isLoadingMore.value || isAllCollectionsEnded()) return
        _isLoadingMore.value = true
        try {
            val nextNotes = fetchPageFromFirestore(isRefresh = false)
            if (nextNotes.isNotEmpty()) {
                val merged = (_trendingNotes.value + nextNotes).distinctBy { it.id }
                _trendingNotes.value = merged
                saveCachedNotes(merged)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isLoadingMore.value = false
        }
    }

    private fun isAllCollectionsEnded(): Boolean {
        val collections = listOf("notes", "pyqs", "assignments", "cheatsheets", "documents")
        return collections.all { isCollectionEnd[it] == true }
    }

    private suspend fun fetchPageFromFirestore(isRefresh: Boolean): List<TrendingNote> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (com.pravor.notessharing.BuildConfig.DEBUG) {
            android.util.Log.d("PERF", "[PERF] Trending page fetch START thread=${Thread.currentThread().name}")
        }
        val collections = listOf("notes", "pyqs", "assignments", "cheatsheets", "documents")

        if (isRefresh) {
            lastSnapshots.clear()
            isCollectionEnd.clear()
        }

        val allCandidates = mutableListOf<Pair<DocumentSnapshot, String>>()

        val candidatesStartTime = System.currentTimeMillis()
        coroutineScope {
            val deferreds = collections.map { col ->
                async {
                    if (isCollectionEnd[col] == true) return@async Pair(emptyList<DocumentSnapshot>(), null)
                    try {
                        var query = firestore.collection(col)
                            .orderBy("trendingScore", Query.Direction.DESCENDING)
                            .limit(PAGE_SIZE.toLong())

                        val lastSnap = lastSnapshots[col]
                        if (lastSnap != null) {
                            query = query.startAfter(lastSnap)
                        }

                        val firestoreQueryStartTime = System.currentTimeMillis()
                        if (com.pravor.notessharing.BuildConfig.DEBUG) {
                            android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query START collection=$col thread=${Thread.currentThread().name}")
                        }
                        val snap = query.get().await()
                        if (com.pravor.notessharing.BuildConfig.DEBUG) {
                            val firestoreQueryDuration = System.currentTimeMillis() - firestoreQueryStartTime
                            android.util.Log.d("FIRESTORE", "[FIRESTORE] Firestore query END collection=$col duration=${firestoreQueryDuration}ms docs=${snap.size()} thread=${Thread.currentThread().name}")
                        }

                        if (snap.isEmpty) {
                            isCollectionEnd[col] = true
                        }
                        
                        val docs = snap.documents
                        val nonVideoDocs = docs.filter { doc ->
                            val data = doc.data ?: return@filter false
                            val docId = doc.id
                            val idField = data["id"] as? String
                            val docIdField = data["documentId"] as? String
                            val title = data["title"] as? String
                            val uploaderId = data["uploaderId"] as? String

                            val isIdBlank = docId.isBlank() || 
                                    (idField != null && idField.isBlank()) || 
                                    (docIdField != null && docIdField.isBlank())
                            val isTitleBlank = title.isNullOrBlank()
                            val isDummyUploader = uploaderId == "dummy-uid"

                            !ExploreMapper.isVideoResource(data) && !isIdBlank && !isTitleBlank && !isDummyUploader
                        }

                        val advanceCursorTo = if (nonVideoDocs.isEmpty() && docs.isNotEmpty()) {
                            docs.last()
                        } else {
                            null
                        }

                        Pair(nonVideoDocs, advanceCursorTo)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Pair(emptyList<DocumentSnapshot>(), null)
                    }
                }
            }
            val results = deferreds.awaitAll()
            results.forEachIndexed { index, (docs, advanceCursorTo) ->
                val col = collections[index]
                if (advanceCursorTo != null) {
                    lastSnapshots[col] = advanceCursorTo
                }
                docs.forEach { doc ->
                    allCandidates.add(Pair(doc, col))
                }
            }
        }
        if (com.pravor.notessharing.BuildConfig.DEBUG) {
            val candidatesDuration = System.currentTimeMillis() - candidatesStartTime
            android.util.Log.d("PERF", "[PERF] Trending stage=FetchCandidates duration=${candidatesDuration}ms")
        }

        val sortingStartTime = System.currentTimeMillis()
        // Sort all candidates by trendingScore descending, then by uploadedAt descending using the centralized comparator
        allCandidates.sortWith { a, b ->
            ExploreRankingUtils.documentSnapshotComparator.compare(a.first, b.first)
        }

        // Take the top PAGE_SIZE (10)
        val selected = allCandidates.take(PAGE_SIZE)

        // Update the last snapshot for each collection based on what we actually took
        selected.forEach { (doc, col) ->
            lastSnapshots[col] = doc
        }
        if (com.pravor.notessharing.BuildConfig.DEBUG) {
            val sortingDuration = System.currentTimeMillis() - sortingStartTime
            android.util.Log.d("PERF", "[PERF] Trending stage=Sorting duration=${sortingDuration}ms")
        }

        // Now map the selected DocumentSnapshots to TrendingNote
        val mappingStartTime = System.currentTimeMillis()
        if (com.pravor.notessharing.BuildConfig.DEBUG) {
            android.util.Log.d("PERF", "[PERF] MainThreadWork START operation=Trending feed assembly thread=${Thread.currentThread().name}")
        }

        val detailRepository = DocumentDetailRepository()

        // Extract unique uploader IDs to eliminate duplicate fetches
        val uploaderIds = selected.mapNotNull { (doc, _) ->
            val uploaderId = (doc.data ?: emptyMap<String, Any>())["uploaderId"] as? String
            if (!uploaderId.isNullOrBlank() && uploaderId != "dummy-uid") uploaderId else null
        }.distinct()

        val uniqueContributorCount = uploaderIds.size
        var cacheHits = 0
        var cacheMisses = 0
        var userFetchCount = 0

        val contributorStartTime = System.currentTimeMillis()
        val resolvedLevels = coroutineScope {
            uploaderIds.map { uid ->
                async {
                    val level = detailRepository.getUploaderContributorLevel(uid) ?: "Bronze Contributor"
                    uid to level
                }
            }.awaitAll().toMap()
        }
        val totalContributorLevelDuration = System.currentTimeMillis() - contributorStartTime

        // Add requested contributor resolution logging
        if (com.pravor.notessharing.BuildConfig.DEBUG) {
            android.util.Log.d("PERF", "[PERF] uniqueContributorCount=$uniqueContributorCount userFetchCount=$userFetchCount cacheHits=$cacheHits cacheMisses=$cacheMisses")
            android.util.Log.d("PERF", "[PERF] ResolveContributorLevels uniqueContributors=$uniqueContributorCount")
            android.util.Log.d("PERF", "[PERF] ResolveContributorLevels userFetches=$userFetchCount")
            android.util.Log.d("PERF", "[PERF] ResolveContributorLevels cacheHits=$cacheHits")
        }

        val mappedNotes = selected.mapNotNull { (doc, _) ->
            ExploreMapper.documentToTrendingNote(doc, emptySet(), resolvedLevels)
        }
        if (com.pravor.notessharing.BuildConfig.DEBUG) {
            val mappingTotalDuration = System.currentTimeMillis() - mappingStartTime
            android.util.Log.d("PERF", "[PERF] MainThreadWork END operation=Trending feed assembly duration=${mappingTotalDuration}ms thread=${Thread.currentThread().name}")
            android.util.Log.d("PERF", "[PERF] Trending stage=ResolveContributorLevels duration=${totalContributorLevelDuration}ms")
            android.util.Log.d("PERF", "[PERF] Trending stage=FetchDocumentDetails duration=0ms")
            android.util.Log.d("PERF", "[PERF] Trending stage=ScoreCalculation duration=0ms")
            android.util.Log.d("PERF", "[PERF] Trending stage=Mapping duration=${mappingTotalDuration - totalContributorLevelDuration}ms")

            val duration = System.currentTimeMillis() - startTime
            android.util.Log.d("PERF", "[PERF] Trending TOTAL duration=${duration}ms")
        }
        mappedNotes
    }
}
