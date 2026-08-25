package com.pravor.notessharing.data.classroom

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import org.json.JSONObject
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class DriveUploadResult(
    val fileId: String,
    val fileName: String,
    val mimeType: String,
    val alternateLink: String? = null
)

class GoogleDriveUploadService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "DriveUploadService"
        private const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&supportsAllDrives=true"
    }

    suspend fun uploadFile(
        context: Context,
        fileUri: Uri,
        customFileName: String? = null,
        customMimeType: String? = null,
        accessToken: String
    ): Result<DriveUploadResult> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val resolvedName = customFileName ?: getFileName(context, fileUri) ?: "submission_document"
            val resolvedMimeType = customMimeType ?: contentResolver.getType(fileUri) ?: "application/octet-stream"

            val metadataJson = JSONObject().apply {
                put("name", resolvedName)
                put("mimeType", resolvedMimeType)
            }.toString()

            val metadataPart = RequestBody.create(
                "application/json; charset=UTF-8".toMediaTypeOrNull(),
                metadataJson
            )

            val mediaPart = object : RequestBody() {
                override fun contentType() = resolvedMimeType.toMediaTypeOrNull()

                override fun writeTo(sink: BufferedSink) {
                    val inputStream: InputStream? = contentResolver.openInputStream(fileUri)
                    if (inputStream == null) {
                        throw java.io.IOException("Cannot open input stream for URI: $fileUri")
                    }
                    inputStream.use { stream ->
                        sink.writeAll(stream.source())
                    }
                }
            }

            val multipartBody = MultipartBody.Builder()
                .setType("multipart/related".toMediaTypeOrNull() ?: MultipartBody.FORM)
                .addPart(metadataPart)
                .addPart(mediaPart)
                .build()

            val request = Request.Builder()
                .url(UPLOAD_URL)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(multipartBody)
                .build()

            Log.d(TAG, "Starting Google Drive upload for file: '$resolvedName' ($resolvedMimeType)")
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                Log.d(TAG, "Drive upload HTTP ${response.code}: $bodyString")

                if (!response.isSuccessful) {
                    val errorMessage = parseErrorMessage(bodyString, response.code)
                    return@withContext Result.failure(Exception("Drive upload failed (${response.code}): $errorMessage"))
                }

                val json = JSONObject(bodyString)
                val fileId = json.optString("id")
                if (fileId.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("Drive API did not return a valid file ID."))
                }

                val alternateLink = json.optString("webViewLink").takeIf { it.isNotBlank() }
                    ?: "https://drive.google.com/file/d/$fileId/view"

                Log.d(TAG, "Drive upload successful. File ID: $fileId")
                Result.success(
                    DriveUploadResult(
                        fileId = fileId,
                        fileName = resolvedName,
                        mimeType = resolvedMimeType,
                        alternateLink = alternateLink
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception uploading file to Google Drive", e)
            Result.failure(e)
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            result = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resolve file display name from content resolver", e)
            }
        }
        if (result == null) {
            result = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return result
    }

    private fun parseErrorMessage(bodyString: String, statusCode: Int): String {
        try {
            if (bodyString.isNotBlank()) {
                val json = JSONObject(bodyString)
                val errorObj = json.optJSONObject("error")
                val msg = errorObj?.optString("message")
                if (!msg.isNullOrBlank()) return msg
            }
        } catch (e: Exception) {
            // Ignore
        }
        return when (statusCode) {
            401 -> "Authorization expired. Please re-authenticate."
            403 -> "Drive upload permission denied. Ensure drive.file scope is granted."
            else -> "Upload error (HTTP $statusCode)"
        }
    }
}
