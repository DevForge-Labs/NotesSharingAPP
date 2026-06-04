package com.pravor.notessharing.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class FirestoreDocumentService {
    private val firestore = FirebaseFirestore.getInstance()
    suspend fun saveDocument(collectionName: String, doc: Map<String, Any>) {
        val docId = doc["documentId"] as? String
        val collection = firestore.collection(collectionName)
        if (docId != null) {
            collection.document(docId).set(doc).await()
        } else {
            collection.add(doc).await()
        }
    }

    suspend fun incrementDownloadCount(collection: String, documentId: String) {
        val documentRef = firestore.collection(collection).document(documentId)
        documentRef.update(
            mapOf(
                "downloads" to FieldValue.increment(1),
                "downloadsCount" to FieldValue.increment(1)
            )
        ).await()
    }
}
