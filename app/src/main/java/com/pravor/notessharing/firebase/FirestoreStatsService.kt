package com.pravor.notessharing.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.model.UploadType
import com.pravor.notessharing.model.dbField
import com.pravor.notessharing.model.calculateLevel
import kotlinx.coroutines.tasks.await

class FirestoreStatsService {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun incrementUserUploadsWithLevel(uid: String, type: UploadType, count: Int = 1) {
        val docRef = usersCollection.document(uid)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            
            val typeField = type.dbField

            if (!snapshot.exists()) {
                val initMap = linkedMapOf<String, Any>(
                    "uid" to uid,
                    "totalUploads" to count.toLong(),
                    "contributorLevel" to calculateLevel(count).toLong(),
                    "bookmarks" to 0L,
                    "upvotes" to 0L,
                    "branch" to "Computer Science",
                    "createdAt" to System.currentTimeMillis()
                )
                
                // Initialize all type fields for consistency
                val allTypeFields = listOf("pyqUploads", "notesUploads", "cheatSheetUploads", "assignmentUploads", "youtubeResourceUploads")
                for (field in allTypeFields) {
                    if (field == typeField) {
                        initMap[field] = count.toLong()
                    } else {
                        initMap[field] = 0L
                    }
                }
                transaction.set(docRef, initMap)
            } else {
                val currentUploads = snapshot.getLong("totalUploads") ?: 0L
                val newUploads = currentUploads + count
                
                val currentTypeUploads = snapshot.getLong(typeField) ?: 0L
                
                val updatesMap = mutableMapOf<String, Any>(
                    "totalUploads" to newUploads,
                    "contributorLevel" to calculateLevel(newUploads.toInt()).toLong(),
                    typeField to currentTypeUploads + count
                )
                
                // Safe migration: set missing type stats to 0L if they don't exist
                val allTypeFields = listOf("pyqUploads", "notesUploads", "cheatSheetUploads", "assignmentUploads", "youtubeResourceUploads")
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
