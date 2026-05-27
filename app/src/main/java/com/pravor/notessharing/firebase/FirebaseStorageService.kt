package com.pravor.notessharing.firebase

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FirebaseStorageService(private val context: Context) {
    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadFile(
        uriStr: String,
        storagePath: String,
        onProgress: (Float) -> Unit
    ): Pair<String, String> {
        val uri = Uri.parse(uriStr)
        val ref = storage.reference.child(storagePath)
        
        val uploadTask = ref.putFile(uri)
        
        uploadTask.addOnProgressListener { taskSnapshot ->
            val totalBytes = taskSnapshot.totalByteCount
            val progress = if (totalBytes > 0) {
                taskSnapshot.bytesTransferred.toFloat() / totalBytes.toFloat()
            } else {
                0f
            }
            onProgress(progress)
        }
        
        uploadTask.await()
        val downloadUrl = ref.downloadUrl.await().toString()
        return Pair(storagePath, downloadUrl)
    }
}
