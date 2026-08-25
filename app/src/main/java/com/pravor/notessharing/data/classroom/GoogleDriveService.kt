package com.pravor.notessharing.data.classroom

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

data class DriveFileMetadata(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val canDownload: Boolean
)

sealed class DriveDownloadResult {
    data class Success(val file: File) : DriveDownloadResult()
    data class Error(
        val statusCode: Int,
        val message: String,
        val reason: String?,
        val domain: String?
    ) : DriveDownloadResult()
}

class GoogleDriveService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "GoogleDriveService"
        private const val DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
    }

    suspend fun getFileMetadata(fileId: String, accessToken: String): DriveFileMetadata? = withContext(Dispatchers.IO) {
        val url = "$DRIVE_API_BASE/files/$fileId?fields=id,name,mimeType,size,capabilities(canDownload)&supportsAllDrives=true"
        Log.d(TAG, "Fetching Drive metadata: $url")

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    logGoogleApiError("Metadata Fetch", response.code, bodyString)
                    return@withContext null
                }
                val json = JSONObject(bodyString)
                val id = json.optString("id", fileId)
                val name = json.optString("name", "Document")
                val mimeType = json.optString("mimeType", "")
                val size = json.optLong("size", 0L)
                val canDownload = json.optJSONObject("capabilities")?.optBoolean("canDownload", true) ?: true

                Log.d(TAG, "Drive Metadata: ID=$id, Name='$name', MIME='$mimeType', Size=$size, CanDownload=$canDownload")
                DriveFileMetadata(id, name, mimeType, size, canDownload)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Drive metadata for $fileId", e)
            null
        }
    }

    suspend fun downloadDriveFile(
        fileId: String,
        targetFile: File,
        accessToken: String
    ): DriveDownloadResult = withContext(Dispatchers.IO) {
        val url = "$DRIVE_API_BASE/files/$fileId?alt=media&supportsAllDrives=true"
        Log.d(TAG, "Downloading Drive file $fileId from: $url")

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                Log.d(TAG, "Drive Download HTTP Status: ${response.code}")

                if (!response.isSuccessful) {
                    val bodyString = response.body?.string().orEmpty()
                    val parsedError = logGoogleApiError("Media Download", response.code, bodyString)
                    return@withContext DriveDownloadResult.Error(
                        statusCode = response.code,
                        message = parsedError.message,
                        reason = parsedError.reason,
                        domain = parsedError.domain
                    )
                }

                val body = response.body ?: return@withContext DriveDownloadResult.Error(
                    statusCode = response.code,
                    message = "Empty response body",
                    reason = null,
                    domain = null
                )

                val parentDir = targetFile.parentFile
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs()
                }

                val tempFile = File(parentDir, "${targetFile.name}.tmp")
                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (tempFile.exists() && tempFile.length() > 0) {
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    val renamed = tempFile.renameTo(targetFile)
                    Log.d(TAG, "Drive file $fileId successfully saved to: ${targetFile.absolutePath} (size=${targetFile.length()} bytes)")
                    return@withContext DriveDownloadResult.Success(targetFile)
                } else {
                    tempFile.delete()
                    return@withContext DriveDownloadResult.Error(
                        statusCode = 500,
                        message = "Could not write temporary file to disk",
                        reason = null,
                        domain = null
                    )
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network exception during Drive file download: $fileId", e)
            return@withContext DriveDownloadResult.Error(
                statusCode = 0,
                message = e.localizedMessage ?: "Network connection error",
                reason = "network_error",
                domain = "network"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error downloading Drive file: $fileId", e)
            return@withContext DriveDownloadResult.Error(
                statusCode = 0,
                message = e.localizedMessage ?: "Unexpected error",
                reason = "unknown",
                domain = "app"
            )
        }
    }

    private data class ParsedGoogleError(val message: String, val reason: String?, val domain: String?)

    private fun logGoogleApiError(operation: String, statusCode: Int, bodyString: String): ParsedGoogleError {
        var message = "HTTP $statusCode"
        var reason: String? = null
        var domain: String? = null

        try {
            if (bodyString.isNotBlank()) {
                val json = JSONObject(bodyString)
                val errorObj = json.optJSONObject("error")
                if (errorObj != null) {
                    message = errorObj.optString("message", message)
                    val errorsArr = errorObj.optJSONArray("errors")
                    if (errorsArr != null && errorsArr.length() > 0) {
                        val firstErr = errorsArr.optJSONObject(0)
                        reason = firstErr?.optString("reason")
                        domain = firstErr?.optString("domain")
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore JSON parse errors
        }

        Log.e(TAG, "[$operation Failed] Status: $statusCode | Message: $message | Reason: $reason | Domain: $domain")
        return ParsedGoogleError(message, reason, domain)
    }
}
