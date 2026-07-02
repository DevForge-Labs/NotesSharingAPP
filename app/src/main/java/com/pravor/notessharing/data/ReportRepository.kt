package com.pravor.notessharing.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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

    companion object {
        val instance by lazy { ReportRepository() }
    }

    suspend fun hasUserReported(resourceId: String, userId: String): Boolean {
        if (userId.isBlank() || resourceId.isBlank()) return false
        
        synchronized(this) {
            if (cachedUserId != userId) {
                _reportedFlow.value = emptyMap()
                cachedUserId = userId
            }
        }

        _reportedFlow.value[resourceId]?.let { return it }

        return try {
            val docId = "${resourceId}_${userId}"
            val doc = reportsCollection.document(docId).get().await()
            val exists = doc.exists()
            _reportedFlow.update { it + (resourceId to exists) }
            exists
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
                "actionTaken" to null
            )
            reportsCollection.document(docId).set(reportData).await()
            
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

