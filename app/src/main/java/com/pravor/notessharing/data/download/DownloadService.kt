package com.pravor.notessharing.data.download

import android.content.Context
import com.pravor.notessharing.model.DocumentDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder

object DownloadService {

    private val okHttpClient = OkHttpClient()

    suspend fun downloadDocument(
        context: Context,
        document: DocumentDetail,
        onProgress: (Float) -> Unit = {}
    ) {
        withContext(Dispatchers.IO) {
            val manager = DownloadDataStoreManager(context)
            val docDir = File(context.filesDir, "downloads/${document.id}")
            if (!docDir.exists()) {
                docDir.mkdirs()
            }

            val urls = document.fileUrls
            if (urls.isEmpty()) {
                return@withContext
            }

            val attachments = mutableListOf<DownloadedAttachment>()
            val progressMap = mutableMapOf<String, Float>()
            urls.forEach { progressMap[it] = 0f }

            try {
                // Download each attachment sequentially
                for ((index, url) in urls.withIndex()) {
                    val rawFileName = getFileNameFromUrl(url)
                    val ext = rawFileName.substringAfterLast(".", "")
                    val base = rawFileName.substringBeforeLast(".", rawFileName)
                    var fileName = rawFileName
                    var localFile = File(docDir, fileName)

                    var counter = 1
                    while (localFile.exists()) {
                        fileName = if (ext.isNotEmpty()) "${base}_$counter.$ext" else "${base}_$counter"
                        localFile = File(docDir, fileName)
                        counter++
                    }

                    // Download the file
                    downloadFile(url, localFile) { bytesRead, totalBytes ->
                        val fileProgress = if (totalBytes > 0) bytesRead.toFloat() / totalBytes else 0f
                        progressMap[url] = fileProgress
                        
                        // Calculate average progress
                        val totalProgress = progressMap.values.sum() / urls.size
                        onProgress(totalProgress)
                    }

                    attachments.add(
                        DownloadedAttachment(
                            documentId = document.id,
                            storagePath = url,
                            localPath = localFile.absolutePath
                        )
                    )
                }

                // Prefetch document detail from Firestore to populate Firestore's offline cache
                try {
                    val collections = listOf("documents", "notes", "pyqs", "assignments", "cheatsheets")
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    for (col in collections) {
                        try {
                            firestore.collection(col).document(document.id).get(com.google.firebase.firestore.Source.SERVER)
                        } catch (e: Exception) {
                            // Ignore individual query failures
                        }
                    }
                } catch (e: Exception) {
                    // Ignore cache prefetch error
                }

                // Save to DataStore on success
                manager.addDownload(document.id, attachments)

            } catch (e: Exception) {
                // Cleanup on failure
                try {
                    docDir.deleteRecursively()
                } catch (cleanupEx: Exception) {
                    // Ignore
                }
                throw e
            }
        }
    }

    suspend fun deleteDownload(context: Context, documentId: String) {
        withContext(Dispatchers.IO) {
            val manager = DownloadDataStoreManager(context)
            
            // Delete local files
            val docDir = File(context.filesDir, "downloads/$documentId")
            if (docDir.exists()) {
                docDir.deleteRecursively()
            }

            // Remove metadata in DataStore
            manager.removeDownload(documentId)
        }
    }

    suspend fun getAttachmentLocalPath(context: Context, storagePath: String): String? {
        val manager = DownloadDataStoreManager(context)
        return manager.getAttachmentLocalPath(storagePath)
    }

    private fun downloadFile(urlStr: String, targetFile: File, onProgressUpdate: (Long, Long) -> Unit) {
        val request = Request.Builder().url(urlStr).build()
        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("Failed to download file: server returned ${response.code}")
        }

        val body = response.body ?: throw Exception("Response body is null")
        val totalBytes = body.contentLength()
        val inputStream = body.byteStream()
        
        val tmpFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
        val outputStream = FileOutputStream(tmpFile)

        val buffer = ByteArray(4096)
        var bytesRead: Int
        var totalBytesRead = 0L

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            totalBytesRead += bytesRead
            onProgressUpdate(totalBytesRead, totalBytes)
        }

        outputStream.close()
        inputStream.close()

        if (!tmpFile.renameTo(targetFile)) {
            tmpFile.copyTo(targetFile, overwrite = true)
            tmpFile.delete()
        }
    }

    fun getFileNameFromUrl(url: String): String {
        return try {
            val decoded = URLDecoder.decode(url, "UTF-8")
            val path = decoded.substringBefore("?").substringAfterLast("/")
            val name = path.ifBlank { "attachment" }
            name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        } catch (e: Exception) {
            "attachment_${System.currentTimeMillis()}"
        }
    }
}
