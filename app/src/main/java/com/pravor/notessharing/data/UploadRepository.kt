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
import com.pravor.notessharing.model.firestoreValue
import com.pravor.notessharing.state.YoutubePreview
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    suspend fun uploadNotes(
        branch: String,
        semester: String,
        subject: String,
        subjectId: String = "",
        selectedFiles: List<SelectedUploadFile>,
        title: String,
        description: String = "",
        onProgress: (Float) -> Unit
    ) {
        uploadDocument(
            branch = branch,
            semester = semester,
            subject = subject,
            subjectId = subjectId,
            type = UploadType.Notes,
            selectedFiles = selectedFiles,
            youtubeUrl = null,
            youtubePreview = null,
            examYear = null,
            examType = null,
            title = title,
            description = description,
            onProgress = onProgress
        )
    }

    suspend fun uploadCheatSheet(
        branch: String,
        semester: String,
        subject: String,
        subjectId: String = "",
        selectedFiles: List<SelectedUploadFile>,
        title: String,
        description: String = "",
        onProgress: (Float) -> Unit
    ) {
        uploadDocument(
            branch = branch,
            semester = semester,
            subject = subject,
            subjectId = subjectId,
            type = UploadType.CheatSheet,
            selectedFiles = selectedFiles,
            youtubeUrl = null,
            youtubePreview = null,
            examYear = null,
            examType = null,
            title = title,
            description = description,
            onProgress = onProgress
        )
    }

    suspend fun uploadAssignment(
        branch: String,
        semester: String,
        subject: String,
        subjectId: String = "",
        selectedFiles: List<SelectedUploadFile>,
        title: String,
        description: String = "",
        section: String,
        sectionDisplay: String,
        onProgress: (Float) -> Unit
    ) {
        uploadDocument(
            branch = branch,
            semester = semester,
            subject = subject,
            subjectId = subjectId,
            type = UploadType.Assignment,
            selectedFiles = selectedFiles,
            youtubeUrl = null,
            youtubePreview = null,
            examYear = null,
            examType = null,
            title = title,
            description = description,
            section = section,
            sectionDisplay = sectionDisplay,
            onProgress = onProgress
        )
    }

    suspend fun uploadYouTubeResource(
        branch: String,
        semester: String,
        subject: String,
        subjectId: String = "",
        youtubeUrl: String,
        youtubePreview: YoutubePreview?,
        youtubeResourceType: String = "video",
        description: String = "",
        onProgress: (Float) -> Unit
    ) {
        uploadDocument(
            branch = branch,
            semester = semester,
            subject = subject,
            subjectId = subjectId,
            type = UploadType.Youtube,
            selectedFiles = emptyList(),
            youtubeUrl = youtubeUrl,
            youtubePreview = youtubePreview,
            examYear = null,
            examType = null,
            title = null,
            description = description,
            youtubeResourceType = youtubeResourceType,
            onProgress = onProgress
        )
    }

    suspend fun uploadDocument(
        branch: String,
        semester: String,
        subject: String,
        subjectId: String = "",
        type: UploadType,
        selectedFiles: List<SelectedUploadFile>,
        youtubeUrl: String?,
        youtubePreview: YoutubePreview?,
        examYear: String?,
        examType: String?,
        title: String? = null,
        description: String = "",
        section: String? = null,
        sectionDisplay: String? = null,
        youtubeResourceType: String? = null,
        onProgress: (Float) -> Unit
    ) {
        val uploaderId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val uploadedAt = System.currentTimeMillis()
        val displaySubject = subject.trim()
        val searchKey = normalizeSubject(displaySubject)

        var uploaderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Anonymous"
        var uploaderPhotoUrl = FirebaseAuth.getInstance().currentUser?.photoUrl?.toString() ?: ""
        var college: String? = null
        try {
            val userProfile = com.pravor.notessharing.firebase.FirestoreUserService().getUserProfile(uploaderId)
            if (userProfile != null) {
                if (userProfile.name.isNotBlank()) {
                    uploaderName = userProfile.name
                }
                if (userProfile.profileImageUrl.isNotBlank()) {
                    uploaderPhotoUrl = userProfile.profileImageUrl
                }
                if (userProfile.college.isNotBlank()) {
                    college = userProfile.college
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        val canonicalBranch = com.pravor.notessharing.util.LegacyAcademicCompatibilityResolver.resolveBranchId(branch)
        val canonicalCollege = college?.let {
            com.pravor.notessharing.util.LegacyAcademicCompatibilityResolver.resolveCollegeId(it)
        }

        if (type == UploadType.Youtube) {
            val documentId = UUID.randomUUID().toString()
            val isPlaylist = youtubeResourceType == "playlist"
            
            val doc = mutableMapOf<String, Any?>(
                "documentId" to documentId,
                "description" to description,
                "branch" to canonicalBranch,
                "semester" to semester,
                "subject" to displaySubject,
                "displaySubject" to displaySubject,
                "subjectId" to if (subjectId.isNotBlank()) subjectId else null,
                "searchKey" to searchKey,
                "documentType" to if (isPlaylist) "Playlist" else "Video",
                "type" to if (isPlaylist) "Playlist" else "Video",
                "uploaderId" to uploaderId,
                "uploaderName" to uploaderName,
                "uploaderPhotoUrl" to uploaderPhotoUrl,
                "uploadedAt" to uploadedAt,
                "downloadsCount" to 0,
                "upvotes" to 0,
                "bookmarks" to 0,
                "viewsCount" to 0,
                "fileUrl" to null,
                "storagePath" to null,
                "fileSize" to 0L,
                "fileExtension" to "",
                "tags" to emptyList<String>(),
                "youtubeUrl" to (youtubeUrl ?: (youtubePreview?.url ?: "")),
                "trendingScore" to 0.0
            )

            if (isPlaylist) {
                val playlistId = youtubeUrl?.let { com.pravor.notessharing.model.extractYoutubePlaylistId(it) } ?: ""
                val title = youtubePreview?.title ?: "YouTube Playlist"
                val channelName = youtubePreview?.channelTitle ?: "YouTube Channel"
                val generatedThumb = youtubePreview?.thumbnailUrl ?: ""
                
                doc["youtubeResourceType"] = "playlist"
                doc["title"] = title
                doc["playlistTitle"] = title
                doc["channelName"] = channelName
                doc["thumbnailUrl"] = generatedThumb
                doc["youtubeThumbnailUrl"] = generatedThumb
                doc["youtubeId"] = playlistId
                doc["youtubePlaylistId"] = playlistId
            } else {
                val videoId = youtubeUrl?.let { com.pravor.notessharing.model.extractYoutubeVideoId(it) } ?: ""
                val generatedThumb = if (videoId.isNotBlank()) "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else (youtubePreview?.thumbnailUrl ?: "")
                val title = youtubePreview?.title ?: "YouTube Video"
                val channelName = youtubePreview?.channelTitle ?: "YouTube Channel"

                doc["youtubeResourceType"] = "video"
                doc["title"] = title
                doc["videoTitle"] = title
                doc["channelName"] = channelName
                doc["thumbnailUrl"] = generatedThumb
                doc["youtubeThumbnailUrl"] = generatedThumb
                doc["youtubeId"] = videoId
                doc["youtubeVideoId"] = videoId
            }
            
            if (canonicalCollege != null) {
                doc["college"] = canonicalCollege
            }
            firestoreService.saveDocument(getCollectionName(type), filterNullValues(doc))
            statsService.incrementUserUploadsWithLevel(uploaderId, type.label, 1)
            onProgress(1.0f)
        } else if (type == UploadType.Pyq) {
            if (selectedFiles.size != 1) {
                throw IllegalArgumentException("PYQs support only a single PDF upload.")
            }
            val totalBytes = selectedFiles.sumOf { it.sizeBytes }
            var totalUploadedBytes = 0L

            for (file in selectedFiles) {
                val documentId = UUID.randomUUID().toString()
                val sanitizedSubject = sanitizeForStorage(subject)
                val normalizedYear = (examYear ?: "").trim()
                val normalizedExamType = when {
                    (examType ?: "").trim().lowercase(java.util.Locale.ROOT).contains("mid") -> "MidSem"
                    (examType ?: "").trim().lowercase(java.util.Locale.ROOT).contains("end") -> "EndSem"
                    else -> (examType ?: "").trim().replace(" ", "")
                }
                val pyqFileName = "$normalizedYear.$normalizedExamType.pdf"
                val storagePath = "pyqs/${semester.trim()}/$sanitizedSubject-pyq-$documentId/$pyqFileName"

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
                    "documentId" to documentId,
                    "title" to pyqFileName,
                    "description" to description,
                    "branch" to canonicalBranch,
                    "semester" to semester,
                    "subject" to displaySubject,
                    "displaySubject" to displaySubject,
                    "searchKey" to searchKey,
                    "documentType" to type.firestoreValue,
                    "type" to type.firestoreValue,
                    "uploaderId" to uploaderId,
                    "uploaderName" to uploaderName,
                    "uploaderPhotoUrl" to uploaderPhotoUrl,
                    "uploadedAt" to uploadedAt,
                    "downloadsCount" to 0,
                    "upvotes" to 0,
                    "bookmarks" to 0,
                    "viewsCount" to 0,
                    "fileUrl" to downloadUrl,
                    "downloadUrl" to downloadUrl,
                    "storagePath" to uploadedPath,
                    "storagePaths" to listOf(uploadedPath),
                    "fileSize" to file.sizeBytes,
                    "fileExtension" to "pdf",
                    "tags" to emptyList<String>(),
                    "attachmentCount" to 1,
                    "trendingScore" to 0.0
                )

                if (subjectId.isNotBlank()) {
                    doc["subjectId"] = subjectId
                }

                doc["examYear"] = normalizedYear
                doc["examType"] = normalizedExamType
                if (canonicalCollege != null) {
                    doc["college"] = canonicalCollege
                }
                firestoreService.saveDocument(getCollectionName(type), filterNullValues(doc))
            }
            statsService.incrementUserUploadsWithLevel(uploaderId, type.label, selectedFiles.size)
        } else {
            val totalBytes = selectedFiles.sumOf { it.sizeBytes }
            var totalUploadedBytes = 0L
            val downloadUrls = mutableListOf<String>()
            val storagePaths = mutableListOf<String>()
            
            val documentId = UUID.randomUUID().toString()
            
            val folderName = when (type) {
                UploadType.Notes -> "notes"
                UploadType.CheatSheet -> "cheatsheets"
                UploadType.Assignment -> "assignments"
                else -> "documents"
            }

            val sanitizedSubject = sanitizeForStorage(subject)
            val typeSlug = when (type) {
                UploadType.Notes -> "notes"
                UploadType.CheatSheet -> "cheatsheet"
                UploadType.Assignment -> "assignment"
                else -> "document"
            }
            val folderSlug = "$sanitizedSubject-$typeSlug-$documentId"

            for ((index, file) in selectedFiles.withIndex()) {
                val ext = getFileExtension(file.displayName, file.uri)
                val fileName = if (ext == "pdf") {
                    if (selectedFiles.size > 1) {
                        sanitizeFileName(file.displayName)
                    } else {
                        val base = when (type) {
                            UploadType.Assignment -> "solution"
                            UploadType.CheatSheet -> "cheatsheet"
                            else -> "notes"
                        }
                        "$base.pdf"
                    }
                } else {
                    "page${index + 1}.$ext"
                }
                
                val storagePath = "$folderName/$folderSlug/$fileName"

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
                downloadUrls.add(downloadUrl)
                storagePaths.add(uploadedPath)
            }

            val firstFileExtension = getFileExtension(selectedFiles.first().displayName, selectedFiles.first().uri)
            val isPdf = firstFileExtension == "pdf"
            val fileType = if (isPdf) "pdf" else "image"
            val mimeType = when (firstFileExtension) {
                "pdf" -> "application/pdf"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "application/octet-stream"
            }

            val formattedTitle = if (!title.isNullOrBlank()) title.trim() else subject.trim()

            val doc = mutableMapOf<String, Any>(
                    "documentId" to documentId,
                    "title" to formattedTitle,
                    "description" to description,
                    "branch" to canonicalBranch,
                    "semester" to semester,
                    "subject" to displaySubject,
                    "displaySubject" to displaySubject,
                    "searchKey" to searchKey,
                    "documentType" to type.firestoreValue,
                    "type" to type.firestoreValue,
                    "uploaderId" to uploaderId,
                    "uploaderName" to uploaderName,
                    "uploaderPhotoUrl" to uploaderPhotoUrl,
                    "uploadedAt" to uploadedAt,
                    "downloadsCount" to 0,
                    "upvotes" to 0,
                    "bookmarks" to 0,
                    "viewsCount" to 0,
                    "fileUrl" to downloadUrls.first(),
                    "downloadUrl" to downloadUrls.first(),
                    "storagePath" to storagePaths.first(),
                    "storagePaths" to storagePaths,
                    "fileSize" to totalBytes,
                    "fileExtension" to firstFileExtension,
                    "tags" to emptyList<String>(),
                    
                    // Enhanced metadata fields
                    "fileType" to fileType,
                    "mimeType" to mimeType,
                    "fileUrls" to downloadUrls,
                    "thumbnailUrl" to (if (fileType == "image") downloadUrls.first() else ""),
                    "attachmentCount" to selectedFiles.size,
                    "trendingScore" to 0.0
                )

                if (subjectId.isNotBlank()) {
                    doc["subjectId"] = subjectId
                }
            
            if (type == UploadType.Assignment) {
                if (section != null) doc["section"] = section
                if (sectionDisplay != null) doc["sectionDisplay"] = sectionDisplay
            }

            if (canonicalCollege != null) {
                doc["college"] = canonicalCollege
            }
            firestoreService.saveDocument(getCollectionName(type), filterNullValues(doc))
            statsService.incrementUserUploadsWithLevel(uploaderId, type.label, 1)
        }
    }

    private fun filterNullValues(map: Map<String, Any?>): Map<String, Any> {
        val nonNullMap = mutableMapOf<String, Any>()
        for ((key, value) in map) {
            if (value != null) {
                nonNullMap[key] = value
            }
        }
        return nonNullMap
    }

    private fun normalizeSubject(subject: String): String {
        return subject
            .trim()
            .lowercase()
            .replace(" ", "")
    }

    suspend fun getDocumentFileUrls(documentId: String): List<String> {
        return try {
            val collections = listOf("documents", "notes", "pyqs", "assignments", "cheatsheets", "videos")
            var foundData: Map<String, Any>? = null
            coroutineScope {
                val deferreds = collections.map { col ->
                    async {
                        try {
                            val snap = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection(col)
                                .document(documentId)
                                .get()
                                .await()
                            if (snap.exists()) snap.data else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                foundData = deferreds.awaitAll().firstOrNull { it != null }
            }
            if (foundData != null) {
                val fileUrls = (foundData?.get("fileUrls") as? List<*>)?.mapNotNull { it as? String }
                if (fileUrls != null && fileUrls.isNotEmpty()) {
                    return fileUrls
                }
                val singleUrl = foundData?.get("downloadUrl") as? String ?: foundData?.get("fileUrl") as? String ?: foundData?.get("youtubeUrl") as? String
                if (singleUrl != null) {
                    return listOf(singleUrl)
                }
            }
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun resolveFilesForDocument(id: String): Pair<String, List<String>> {
        // Try loading from Firestore first
        try {
            val collections = listOf("documents", "notes", "pyqs", "assignments", "cheatsheets", "videos")
            var foundData: Map<String, Any>? = null
            coroutineScope {
                val deferreds = collections.map { col ->
                    async {
                        try {
                            val snap = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection(col)
                                .document(id)
                                .get()
                                .await()
                            if (snap.exists()) snap.data else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                foundData = deferreds.awaitAll().firstOrNull { it != null }
            }
            if (foundData != null) {
                val title = foundData?.get("title") as? String ?: "Document"
                val fileUrls = (foundData?.get("fileUrls") as? List<*>)?.mapNotNull { it as? String }
                if (fileUrls != null && fileUrls.isNotEmpty()) {
                    return title to fileUrls
                }
                val singleUrl = foundData?.get("downloadUrl") as? String ?: foundData?.get("fileUrl") as? String ?: foundData?.get("youtubeUrl") as? String
                if (singleUrl != null) {
                    return title to listOf(singleUrl)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        return "Document" to emptyList()
    }

    private fun sanitizeForStorage(input: String): String {
        return input.lowercase()
            .replace("\\s+".toRegex(), "-")
            .replace("[^a-z0-9\\-]".toRegex(), "")
            .replace("-+".toRegex(), "-")
            .trim('-')
    }

    private fun sanitizeFileName(name: String): String {
        val baseName = name.substringBeforeLast('.')
        val ext = name.substringAfterLast('.', "")
        val sanitizedBase = baseName.lowercase()
            .replace("\\s+".toRegex(), "-")
            .replace("[^a-z0-9\\-]".toRegex(), "")
            .replace("-+".toRegex(), "-")
            .trim('-')
        return if (ext.isNotBlank()) "$sanitizedBase.${ext.lowercase()}" else sanitizedBase
    }

    private fun getFileExtension(displayName: String, uriString: String): String {
        val extFromDisplay = displayName.substringAfterLast('.', "").lowercase()
        if (extFromDisplay.isNotBlank() && extFromDisplay.length <= 4) {
            return extFromDisplay
        }
        val extFromUri = uriString.substringAfterLast('.', "").lowercase().substringBefore('?')
        if (extFromUri.isNotBlank() && extFromUri.length <= 4) {
            return extFromUri
        }
        return "pdf" // Default fallback
    }

    private fun getCollectionName(type: UploadType): String {
        return when (type) {
            UploadType.Notes -> "notes"
            UploadType.CheatSheet -> "cheatsheets"
            UploadType.Assignment -> "assignments"
            UploadType.Pyq -> "pyqs"
            UploadType.Youtube -> "videos"
        }
    }
}
