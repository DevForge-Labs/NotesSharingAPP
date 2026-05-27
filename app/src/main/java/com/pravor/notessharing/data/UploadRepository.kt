package com.pravor.notessharing.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.firebase.FirebaseStorageService
import com.pravor.notessharing.firebase.FirestoreDocumentService
import com.pravor.notessharing.model.SelectedUploadFile
import com.pravor.notessharing.model.UploadFileSource
import com.pravor.notessharing.model.UploadType
import com.pravor.notessharing.state.YoutubePreview
import java.util.UUID

class UploadRepository(private val context: Context) {
    private val storageService = FirebaseStorageService(context)
    private val firestoreService = FirestoreDocumentService()
    private val statsService = com.pravor.notessharing.firebase.FirestoreStatsService()

    fun resolveSelectedFile(uri: Uri, source: UploadFileSource): SelectedUploadFile {
        val metadata = queryMetadata(uri)
        return SelectedUploadFile(
            uri = uri.toString(),
            displayName = metadata.first,
            sizeBytes = metadata.second,
            source = source
        )
    }

    private fun queryMetadata(uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected file"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        if (size <= 0L) {
            try {
                size = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
            } catch (e: Exception) {
                // Ignore
            }
        }
        return name to size.coerceAtLeast(0L)
    }

    suspend fun uploadDocument(
        branch: String,
        semester: String,
        subject: String,
        type: UploadType,
        selectedFiles: List<SelectedUploadFile>,
        youtubeUrl: String?,
        youtubePreview: YoutubePreview?,
        examYear: String?,
        examType: String?,
        onProgress: (Float) -> Unit
    ) {
        val uploaderId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val uploadedAt = System.currentTimeMillis()
        val displaySubject = subject.trim()
        val searchKey = normalizeSubject(displaySubject)

        if (type == UploadType.Youtube) {
            val preview = youtubePreview ?: throw Exception("Invalid YouTube video details.")
            val doc = mapOf(
                "title" to preview.title,
                "displaySubject" to displaySubject,
                "searchKey" to searchKey,
                "branch" to branch,
                "semester" to semester,
                "type" to type.label,
                "youtubeUrl" to (youtubeUrl ?: preview.url),
                "thumbnailUrl" to preview.thumbnailUrl,
                "videoTitle" to preview.title,
                "channelTitle" to preview.channelTitle,
                "uploaderId" to uploaderId,
                "uploadedAt" to uploadedAt,
                "downloads" to 0,
                "bookmarks" to 0,
                "upvotes" to 0
            )
            firestoreService.saveDocument(doc)
            statsService.incrementUserUploadsWithLevel(uploaderId, type.label, 1)
            onProgress(1.0f)
        } else {
            val totalBytes = selectedFiles.sumOf { it.sizeBytes }
            var totalUploadedBytes = 0L

            for (file in selectedFiles) {
                val cleanFileName = Uri.parse(file.uri).lastPathSegment?.substringAfterLast('/') ?: "file_${UUID.randomUUID()}"
                
                val folderName = when (type) {
                    UploadType.Pyq -> "pyqs"
                    UploadType.Notes -> "notes"
                    UploadType.CheatSheet -> "cheatsheets"
                    UploadType.Assignment -> "assignments"
                    else -> "documents"
                }
                
                val storagePath = "$folderName/$semester/$cleanFileName"

                val (uploadedPath, downloadUrl) = storageService.uploadFile(file.uri, storagePath) { fileProgress ->
                    val fileUploadedBytes = (fileProgress * file.sizeBytes).toLong()
                    val overallProgress = if (totalBytes > 0) {
                        (totalUploadedBytes + fileUploadedBytes).toFloat() / totalBytes.toFloat()
                    } else {
                        1.0f
                    }
                    onProgress(overallProgress.coerceIn(0f, 1f))
                }

                totalUploadedBytes += file.sizeBytes

                val doc = mutableMapOf<String, Any>(
                    "title" to file.displayName,
                    "displaySubject" to displaySubject,
                    "searchKey" to searchKey,
                    "branch" to branch,
                    "semester" to semester,
                    "type" to type.label,
                    "storagePath" to uploadedPath,
                    "downloadUrl" to downloadUrl,
                    "fileSize" to file.sizeBytes,
                    "uploaderId" to uploaderId,
                    "uploadedAt" to uploadedAt,
                    "downloads" to 0,
                    "bookmarks" to 0,
                    "upvotes" to 0
                )

                if (type == UploadType.Pyq) {
                    doc["examYear"] = examYear ?: ""
                    doc["examType"] = examType ?: ""
                }

                firestoreService.saveDocument(doc)
            }
            statsService.incrementUserUploadsWithLevel(uploaderId, type.label, selectedFiles.size)
        }
    }

    private fun normalizeSubject(subject: String): String {
        return subject
            .trim()
            .lowercase()
            .replace(" ", "")
    }
}
