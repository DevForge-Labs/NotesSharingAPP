package com.pravor.notessharing.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await

class ReportRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val reportsCollection = firestore.collection("reports")

    private val _reportedFlow = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val reportedFlow: StateFlow<Map<String, Boolean>> = _reportedFlow.asStateFlow()
    private var cachedUserId: String? = null
    
    private val activeListeners = mutableMapOf<String, ListenerRegistration>()

    companion object {
        val instance by lazy { ReportRepository() }
    }

    private fun clearListeners() {
        synchronized(activeListeners) {
            for (listener in activeListeners.values) {
                listener.remove()
            }
            activeListeners.clear()
        }
    }

    fun removeReportListener(resourceId: String) {
        synchronized(activeListeners) {
            activeListeners[resourceId]?.remove()
            activeListeners.remove(resourceId)
        }
    }

    suspend fun hasUserReported(resourceId: String, userId: String, forceRefresh: Boolean = false): Boolean {
        if (userId.isBlank() || resourceId.isBlank()) return false
        
        synchronized(this) {
            if (cachedUserId != userId) {
                clearListeners()
                _reportedFlow.value = emptyMap()
                cachedUserId = userId
            }
        }

        val docId = "${resourceId}_${userId}"

        // Start snapshot listener if not already active
        synchronized(activeListeners) {
            if (!activeListeners.containsKey(resourceId)) {
                val listener = reportsCollection.document(docId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) return@addSnapshotListener
                        if (snapshot != null) {
                            val hasActiveReport = snapshot.exists() && snapshot.getString("status") == "pending"
                            _reportedFlow.update { if (it[resourceId] == hasActiveReport) it else it + (resourceId to hasActiveReport) }
                        }
                    }
                activeListeners[resourceId] = listener
            }
        }

        if (!forceRefresh) {
            _reportedFlow.value[resourceId]?.let { return it }
        }

        return try {
            val doc = reportsCollection.document(docId).get().await()
            val hasActiveReport = doc.exists() && doc.getString("status") == "pending"
            _reportedFlow.update { it + (resourceId to hasActiveReport) }
            hasActiveReport
        } catch (e: Exception) {
            false
        }
    }

    suspend fun submitReport(
        resourceId: String,
        resourceType: String,
        resourceTitle: String,
        resourceThumbnail: String?,
        uploaderUid: String,
        uploaderName: String,
        reporterUid: String,
        reporterName: String,
        reporterEmail: String,
        reason: String,
        customMessage: String
    ): Result<Unit> {
        return try {
            val docId = "${resourceId}_${reporterUid}"
            
            // Check if already reported to prevent duplicates at the repo level
            if (hasUserReported(resourceId, reporterUid)) {
                return Result.failure(Exception("You have already reported this resource."))
            }

            // Fetch current report document if it exists to get the reportCountByUser
            val doc = reportsCollection.document(docId).get().await()
            val newUserCount = if (doc.exists()) {
                (doc.getLong("reportCountByUser") ?: 1L) + 1L
            } else {
                1L
            }

            // Fetch uploader email from users collection if we have uploaderUid
            var uploaderEmail: String? = null
            if (uploaderUid.isNotEmpty()) {
                try {
                    val uploaderSnap = firestore.collection("users").document(uploaderUid).get().await()
                    if (uploaderSnap.exists()) {
                        uploaderEmail = uploaderSnap.getString("email")
                    }
                } catch (e: Exception) {
                    // Ignore, fallback to null/empty
                }
            }

            val reportData = hashMapOf(
                "resourceId" to resourceId,
                "resourceType" to resourceType,
                "resourceTitle" to resourceTitle,
                "resourceThumbnail" to resourceThumbnail,
                "uploaderUid" to uploaderUid,
                "uploaderName" to uploaderName,
                "uploaderEmail" to (uploaderEmail ?: ""),
                "reporterUid" to reporterUid,
                "reporterName" to reporterName,
                "reporterEmail" to reporterEmail,
                "reason" to reason,
                "customMessage" to customMessage,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "resolvedAt" to null,
                "resolvedByUid" to null,
                "actionTaken" to null,
                "reportCountByUser" to newUserCount
            )
            reportsCollection.document(docId).set(reportData).await()
            
            // Optionally attempt to increment reportCount on the resource document itself
            if (resourceType.isNotEmpty()) {
                try {
                    firestore.collection(resourceType).document(resourceId)
                        .update("reportCount", FieldValue.increment(1))
                        .await()
                } catch (e: Exception) {
                    // Ignore, permission rules or invalid collection
                }
            }

            // Eagerly update cache upon successful submission
            synchronized(this) {
                if (cachedUserId != reporterUid) {
                    _reportedFlow.value = emptyMap()
                    cachedUserId = reporterUid
                }
            }
            _reportedFlow.update { it + (resourceId to true) }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

