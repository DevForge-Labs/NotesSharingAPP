package com.pravor.notessharing.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

class ViewTrackingRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private val sessionViewedIds = ConcurrentHashMap.newKeySet<String>()

        fun clearSessionCache() {
            sessionViewedIds.clear()
        }
    }

    suspend fun incrementViewCount(resourceId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        if (currentUid.isEmpty() || resourceId.isEmpty()) return

        // 1. Check in-memory session cache first
        if (sessionViewedIds.contains(resourceId)) {
            Log.d("VIEW_TRACKING", "Session cache hit for resource $resourceId. Skipping.")
            return
        }

        try {
            // 2. Resolve collection and documentType using DocumentDetailRepository if needed
            val docDetail = DocumentDetailRepository().getDocument(resourceId)
            if (docDetail == null) {
                Log.e("VIEW_TRACKING", "Could not resolve document details for resource $resourceId.")
                return
            }

            val collection = docDetail.collection
            val resourceType = docDetail.documentType

            // 3. Perform atomic transaction (internally verifies viewed status safely)
            performAtomicIncrementTransaction(resourceId, collection, resourceType, currentUid)
        } catch (e: Exception) {
            Log.e("VIEW_TRACKING", "Failed to update view tracking for $resourceId: ${e.message}")
        }
    }

    suspend fun incrementViewCountDirect(resourceId: String, collection: String, resourceType: String) {
        val currentUid = auth.currentUser?.uid ?: return
        if (currentUid.isEmpty() || resourceId.isEmpty()) return

        // 1. Check in-memory session cache first
        if (sessionViewedIds.contains(resourceId)) {
            Log.d("VIEW_TRACKING", "Session cache hit for resource $resourceId. Skipping.")
            return
        }

        try {
            // 2. Perform atomic transaction directly (internally verifies viewed status safely)
            performAtomicIncrementTransaction(resourceId, collection, resourceType, currentUid)
        } catch (e: Exception) {
            Log.e("VIEW_TRACKING", "Failed to update direct view tracking for $resourceId: ${e.message}")
        }
    }

    private suspend fun performAtomicIncrementTransaction(
        resourceId: String,
        collection: String,
        resourceType: String,
        userId: String
    ) {
        val viewedDocRef = firestore.collection("users")
            .document(userId)
            .collection("viewedResources")
            .document(resourceId)

        val resourceDocRef = firestore.collection(collection)
            .document(resourceId)

        firestore.runTransaction { transaction ->
            val viewedSnapshot = transaction.get(viewedDocRef)
            if (viewedSnapshot.exists()) {
                return@runTransaction null
            }

            // Mark as viewed
            val viewedData = mapOf(
                "resourceId" to resourceId,
                "resourceType" to resourceType,
                "viewedAt" to FieldValue.serverTimestamp()
            )
            transaction.set(viewedDocRef, viewedData)

            // Increment viewsCount and update lastViewedAt on resource
            transaction.update(
                resourceDocRef,
                mapOf(
                    "viewsCount" to FieldValue.increment(1),
                    "lastViewedAt" to FieldValue.serverTimestamp()
                )
            )
            null
        }.await()

        sessionViewedIds.add(resourceId)
        Log.d("VIEW_TRACKING", "Successfully completed atomic view tracking transaction for $resourceId.")
    }
}
