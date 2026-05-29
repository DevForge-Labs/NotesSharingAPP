package com.pravor.notessharing.firebase

import com.google.firebase.firestore.FirebaseFirestore
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
}
