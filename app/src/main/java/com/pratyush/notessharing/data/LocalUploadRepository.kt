package com.pratyush.notessharing.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.pratyush.notessharing.model.SelectedUploadFile
import com.pratyush.notessharing.model.UploadFileSource
import com.pratyush.notessharing.model.UploadItem
import com.pratyush.notessharing.model.UploadType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class LocalUploadRepository(private val context: Context) {
    private val preferences = context.getSharedPreferences("local_uploads", Context.MODE_PRIVATE)

    fun resolveSelectedFile(uri: Uri, source: UploadFileSource): SelectedUploadFile {
        val metadata = queryMetadata(uri)
        return SelectedUploadFile(
            uri = uri.toString(),
            displayName = metadata.first,
            sizeBytes = metadata.second,
            source = source
        )
    }

    fun saveUpload(
        branch: String,
        year: String,
        subject: String,
        type: UploadType,
        selectedFiles: List<SelectedUploadFile>,
        youtubeUrl: String?
    ): UploadItem {
        val id = UUID.randomUUID().toString()
        val uploadDir = File(context.filesDir, "uploads/$id").apply { mkdirs() }
        val localUris = when (type) {
            UploadType.Pdf,
            UploadType.Images -> selectedFiles.mapIndexed { index, file ->
                copyIntoLocalUpload(uri = Uri.parse(file.uri), uploadDir = uploadDir, fallbackName = "${type.label}-$index")
            }
            UploadType.Youtube -> emptyList()
        }

        val item = UploadItem(
            id = id,
            branch = branch,
            year = year,
            subject = subject.trim(),
            type = type,
            uriList = localUris,
            youtubeUrl = youtubeUrl?.trim()?.ifBlank { null },
            timestamp = System.currentTimeMillis(),
            totalSizeBytes = selectedFiles.sumOf { it.sizeBytes }
        )
        persist(item)
        return item
    }

    private fun queryMetadata(uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected file"
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
            }
        }
        if (size <= 0L) {
            size = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
        }
        return name to size.coerceAtLeast(0L)
    }

    private fun copyIntoLocalUpload(uri: Uri, uploadDir: File, fallbackName: String): String {
        val name = queryMetadata(uri).first.ifBlank { fallbackName }.sanitizeFileName()
        val destination = uniqueFile(uploadDir, name)
        context.contentResolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
        return Uri.fromFile(destination).toString()
    }

    private fun uniqueFile(directory: File, name: String): File {
        var candidate = File(directory, name)
        if (!candidate.exists()) return candidate
        val base = name.substringBeforeLast('.', name)
        val extension = name.substringAfterLast('.', "")
        var index = 1
        while (candidate.exists()) {
            val nextName = if (extension.isBlank()) "$base-$index" else "$base-$index.$extension"
            candidate = File(directory, nextName)
            index++
        }
        return candidate
    }

    private fun persist(item: UploadItem) {
        val uploads = JSONArray(preferences.getString(KEY_UPLOADS, "[]"))
        uploads.put(item.toJson())
        preferences.edit().putString(KEY_UPLOADS, uploads.toString()).apply()
    }

    private fun UploadItem.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("branch", branch)
        .put("year", year)
        .put("subject", subject)
        .put("type", type.name)
        .put("uriList", JSONArray(uriList))
        .put("youtubeUrl", youtubeUrl)
        .put("timestamp", timestamp)
        .put("totalSizeBytes", totalSizeBytes)

    private fun String.sanitizeFileName(): String =
        replace(Regex("[\\\\/:*?\"<>|]"), "_")

    private companion object {
        const val KEY_UPLOADS = "uploads"
    }
}
