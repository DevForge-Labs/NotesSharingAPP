package com.pravor.notessharing.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreStatsService {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun incrementUserUploadsWithLevel(uid: String, typeString: String, count: Int = 1) {
        val docRef = usersCollection.document(uid)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            
            val typeField = when (typeString.lowercase().replace(" ", "")) {
                "pyq" -> "pyqUploads"
                "notes" -> "notesUploads"
                "cheatsheet" -> "cheatSheetUploads"
                "assignment" -> "assignmentUploads"
                "youtuberesource" -> "youtubeUploads"
                else -> null
            }

            if (!snapshot.exists()) {
                val initMap = mutableMapOf<String, Any>(
                    "uploads" to count.toLong(),
                    "contributorLevel" to when {
                        count in 0..4 -> 1L
                        count in 5..14 -> 2L
                        count in 15..29 -> 3L
                        count in 30..49 -> 4L
                        else -> 5L
                    },
                    "bookmarks" to 0L,
                    "upvotes" to 0L,
                    "notesUploaded" to 0L,
                    "branch" to "Computer Science",
                    "createdAt" to System.currentTimeMillis()
                )
                if (typeField != null) {
                    initMap[typeField] = count.toLong()
                }
                
                // Initialize all type fields for consistency
                val allTypeFields = listOf("pyqUploads", "notesUploads", "cheatSheetUploads", "assignmentUploads", "youtubeUploads")
                for (field in allTypeFields) {
                    if (field != typeField) {
                        initMap[field] = 0L
                    }
                }
                transaction.set(docRef, initMap)
            } else {
                val currentUploads = snapshot.getLong("uploads") ?: 0L
                val newUploads = currentUploads + count
                
                val currentTypeUploads = if (typeField != null) {
                    snapshot.getLong(typeField) ?: 0L
                } else {
                    0L
                }
                
                val newLevel = when {
                    newUploads in 0..4 -> 1L
                    newUploads in 5..14 -> 2L
                    newUploads in 15..29 -> 3L
                    newUploads in 30..49 -> 4L
                    else -> 5L
                }
                
                val updatesMap = mutableMapOf<String, Any>(
                    "uploads" to newUploads,
                    "contributorLevel" to newLevel
                )
                if (typeField != null) {
                    updatesMap[typeField] = currentTypeUploads + count
                }
                
                // Safe migration: set missing type stats to 0L
                val allTypeFields = listOf("pyqUploads", "notesUploads", "cheatSheetUploads", "assignmentUploads", "youtubeUploads")
                for (field in allTypeFields) {
                    if (field != typeField && snapshot.getLong(field) == null) {
                        updatesMap[field] = 0L
                    }
                }
                
                transaction.update(docRef, updatesMap)
            }
        }.await()
    }
}
