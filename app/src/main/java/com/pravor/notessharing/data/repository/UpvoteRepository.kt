package com.pravor.notessharing.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await

class UpvoteRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val upvotesCollection = firestore.collection("upvotes")
    private val functions = FirebaseFunctions.getInstance()

    companion object {
        private val _upvotesFlow = MutableStateFlow<Map<String, Boolean>>(emptyMap())
        val upvotesFlow: StateFlow<Map<String, Boolean>> = _upvotesFlow.asStateFlow()

        private val _upvoteCountsFlow = MutableStateFlow<Map<String, Int>>(emptyMap())
        val upvoteCountsFlow: StateFlow<Map<String, Int>> = _upvoteCountsFlow.asStateFlow()

        private val _downloadCountsFlow = MutableStateFlow<Map<String, Int>>(emptyMap())
        val downloadCountsFlow: StateFlow<Map<String, Int>> = _downloadCountsFlow.asStateFlow()

        var hasLoadedInitial = false
        private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
        private var activeUserId: String? = null
        private val documentListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()
        private val observedPathsByTag = mutableMapOf<String, Set<Pair<String, String>>>()
    }

    fun getCollectionForDocType(docType: String?): String {
        if (docType == null) return "notes"
        return when (docType.lowercase(java.util.Locale.US).trim()) {
            "notes", "note" -> "notes"
            "pyq", "pyqs" -> "pyqs"
            "assignment", "assignments" -> "assignments"
            "cheat sheet", "cheatsheet", "cheatsheets" -> "cheatsheets"
            "video", "videos", "youtube resource", "youtube" -> "videos"
            else -> "documents"
        }
    }

    fun observeVisibleDocuments(tag: String, documentPaths: List<Pair<String, String>>) {
        synchronized(observedPathsByTag) {
            if (documentPaths.isEmpty()) {
                observedPathsByTag.remove(tag)
            } else {
                observedPathsByTag[tag] = documentPaths.toSet()
            }

            val allTargets = observedPathsByTag.values.flatten().toSet()
            val targetIds = allTargets.map { it.first }.toSet()

            // 1. Remove listeners for documents that are no longer visible anywhere
            val toRemove = documentListeners.keys.filter { it !in targetIds }
            for (id in toRemove) {
                documentListeners[id]?.remove()
                documentListeners.remove(id)
            }

            // 2. Add new snapshot listeners
            for ((docId, collection) in allTargets) {
                if (docId.isBlank() || collection.isBlank()) continue
                if (docId !in documentListeners) {
                    try {
                        val listener = firestore.collection(collection).document(docId)
                            .addSnapshotListener { snapshot, error ->
                                if (error != null) {
                                    android.util.Log.e("DEBUG_DOWNLOAD", "[OBSERVER_ERROR] docId=$docId collection=$collection error=${error.message}")
                                    return@addSnapshotListener
                                }
                                if (snapshot != null && snapshot.exists()) {
                                    val count = snapshot.getLong("upvotes")?.toInt()
                                    if (count != null) {
                                        _upvoteCountsFlow.update { current ->
                                            if (current[docId] == count) current else current + (docId to count)
                                        }
                                    }
                                    val downloadCount = if (collection == "notes") {
                                        snapshot.getLong("downloadsCount")?.toInt()
                                            ?: snapshot.getLong("downloads")?.toInt()
                                    } else {
                                        snapshot.getLong("downloads")?.toInt()
                                            ?: snapshot.getLong("downloadsCount")?.toInt()
                                    }
                                    if (downloadCount != null) {
                                        _downloadCountsFlow.update { current ->
                                            if (current[docId] == downloadCount) current else current + (docId to downloadCount)
                                        }
                                    }
                                }
                            }
                        documentListeners[docId] = listener
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        }
    }

    suspend fun loadInitialUpvotesIfNeeded(userId: String): Map<String, Boolean> {
        if (!hasLoadedInitial) {
            return getUpvotes(userId)
        }
        return upvotesFlow.value
    }

    suspend fun getUpvotes(userId: String): Map<String, Boolean> {
        try {
            if (listenerRegistration == null || activeUserId != userId) {
                listenerRegistration?.remove()
                activeUserId = userId
                listenerRegistration = upvotesCollection
                    .whereEqualTo("userId", userId)
                    .addSnapshotListener { querySnapshot, error ->
                        if (error != null) return@addSnapshotListener
                        if (querySnapshot != null) {
                            val map = querySnapshot.documents.associate { doc ->
                                val docId = doc.getString("documentId") ?: ""
                                docId to true
                            }
                            if (_upvotesFlow.value != map) {
                                _upvotesFlow.value = map
                            }
                            hasLoadedInitial = true
                        }
                    }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return upvotesFlow.value
    }

    suspend fun toggleUpvote(documentId: String, collectionName: String?, currentUpvotes: Int, userId: String) {
        val wasUpvoted = _upvotesFlow.value[documentId] ?: false
        val nextUpvoted = !wasUpvoted

        // 1. Optimistic UI Update: immediately emit in-memory
        _upvotesFlow.update { current ->
            current + (documentId to nextUpvoted)
        }
        _upvoteCountsFlow.update { current ->
            val existing = current[documentId] ?: currentUpvotes
            val delta = if (nextUpvoted) 1 else -1
            current + (documentId to (existing + delta).coerceAtLeast(0))
        }

        try {
            // 2. Trigger Cloud Function in background
            val data = mapOf(
                "documentId" to documentId,
                "collectionName" to collectionName
            )
            val result = functions
                .getHttpsCallable("upvote")
                .call(data)
                .await()

            val resultData = result.data as? Map<*, *>
            val actualIsUpvoted = resultData?.get("isUpvoted") as? Boolean
            val actualUpvoteCount = (resultData?.get("upvotes") as? Number)?.toInt()

            // 3. Reconcile in-memory state with actual backend response
            if (actualIsUpvoted != null && actualUpvoteCount != null) {
                _upvotesFlow.update { current ->
                    current + (documentId to actualIsUpvoted)
                }
                _upvoteCountsFlow.update { current ->
                    current + (documentId to actualUpvoteCount)
                }
            }
        } catch (e: Exception) {
            // 4. Rollback on failure
            _upvotesFlow.update { current ->
                current + (documentId to wasUpvoted)
            }
            _upvoteCountsFlow.update { current ->
                current + (documentId to currentUpvotes)
            }
        }
    }
}
