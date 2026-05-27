package com.pravor.notessharing.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreDocumentService {
    private val firestore = FirebaseFirestore.getInstance()
    private val documentsCollection = firestore.collection("documents")

    suspend fun saveDocument(doc: Map<String, Any>) {
        documentsCollection.add(doc).await()
    }
}
