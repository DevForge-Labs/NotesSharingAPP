package com.pravor.notessharing.ui.features.documentViewing

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import com.pravor.notessharing.data.local.preferences.*

import com.pravor.notessharing.domain.model.*
import com.pravor.notessharing.data.repository.*
import com.pravor.notessharing.core.util.*

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pravor.notessharing.data.local.preferences.DownloadDataStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

import kotlinx.coroutines.Job
import java.security.MessageDigest
import java.net.URLDecoder

sealed interface PdfViewingUiState {
    object Loading : PdfViewingUiState
    data class Success(val pdfFile: File) : PdfViewingUiState
    data class Error(val message: String) : PdfViewingUiState
}

class PdfViewingViewModel(
    private val viewTrackingRepository: com.pravor.notessharing.data.repository.ViewTrackingRepository = com.pravor.notessharing.data.repository.ViewTrackingRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<PdfViewingUiState>(PdfViewingUiState.Loading)
    val uiState: StateFlow<PdfViewingUiState> = _uiState.asStateFlow()
    private var hasIncremented = false
    private var currentDocumentId: String? = null
    private var currentFileUrl: String? = null
    private var loadJob: Job? = null

    companion object {
        fun computePdfCacheFileName(documentId: String, fileUrl: String): String {
            val safeDocId = documentId.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "unknown_doc" }
            val decodedUrl = try {
                URLDecoder.decode(fileUrl, "UTF-8")
            } catch (e: Exception) {
                fileUrl
            }
            val pathWithoutQuery = decodedUrl.substringBefore("?")
            val rawFileName = pathWithoutQuery.substringAfterLast("/").substringBeforeLast(".pdf")
            val cleanFileName = rawFileName.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(32)

            val sha256 = MessageDigest.getInstance("SHA-256")
                .digest(decodedUrl.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
                .take(16)

            return if (cleanFileName.isNotBlank()) {
                "${safeDocId}_${cleanFileName}_${sha256}.pdf"
            } else {
                "${safeDocId}_${sha256}.pdf"
            }
        }

        fun getCachedPdfFile(context: Context, documentId: String, fileUrl: String): File {
            val pdfsDir = File(context.cacheDir, "pdfs")
            if (!pdfsDir.exists()) {
                pdfsDir.mkdirs()
            }
            val fileName = computePdfCacheFileName(documentId, fileUrl)
            return File(pdfsDir, fileName)
        }
    }

    fun loadPdf(context: Context, documentId: String, fileUrl: String) {
        android.util.Log.d("PDF_DEBUG", "loadPdf called")
        android.util.Log.d("PDF_DEBUG", "DocumentId=$documentId")
        android.util.Log.d("PDF_DEBUG", "FileUrl=$fileUrl")

        loadJob?.cancel()

        if (documentId != currentDocumentId || fileUrl != currentFileUrl) {
            hasIncremented = false
            currentDocumentId = documentId
            currentFileUrl = fileUrl
        }

        _uiState.value = PdfViewingUiState.Loading

        loadJob = viewModelScope.launch {
            try {
                // 1. Check local attachment registry in DataStore
                val db = DownloadDataStoreManager(context.applicationContext)
                val localPath = db.getAttachmentLocalPath(documentId, fileUrl)
                if (localPath != null) {
                    val localFile = File(localPath)
                    if (localFile.exists() && localFile.length() > 0) {
                        _uiState.value = PdfViewingUiState.Success(localFile)
                        incrementViewsCount(documentId)
                        return@launch
                    } else {
                        db.removeDownload(documentId)
                        _uiState.value = PdfViewingUiState.Error("This download is no longer available on your device.")
                        return@launch
                    }
                }

                // 1.5. Check if fileUrl is a direct local absolute path
                if (fileUrl.startsWith("/") || fileUrl.startsWith("file:") || fileUrl.contains(":\\") || fileUrl.contains(":/")) {
                    val cleanPath = fileUrl.removePrefix("file://").removePrefix("file:")
                    val localDirect = File(cleanPath)
                    if (localDirect.exists() && localDirect.length() > 0) {
                        _uiState.value = PdfViewingUiState.Success(localDirect)
                        incrementViewsCount(documentId)
                        return@launch
                    }
                }

                val pdfFile = getCachedPdfFile(context, documentId, fileUrl)
                android.util.Log.d("PDF_DEBUG", "Checking cache")
                android.util.Log.d("PDF_DEBUG", "CachePath=${pdfFile.absolutePath}")
                android.util.Log.d("PDF_DEBUG", "CacheExists=${pdfFile.exists()}")
                if (pdfFile.exists() && pdfFile.length() > 0) {
                    _uiState.value = PdfViewingUiState.Success(pdfFile)
                    incrementViewsCount(documentId)
                } else {
                    downloadPdfAndCache(context, documentId, fileUrl, pdfFile)
                }
            } catch (e: Exception) {
                _uiState.value = PdfViewingUiState.Error(e.localizedMessage ?: "Unknown error occurred.")
            }
        }
    }

    private suspend fun downloadPdfAndCache(
        context: Context,
        documentId: String,
        fileUrl: String,
        targetFile: File
    ) {
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("PDF_DEBUG", "Starting download")
                android.util.Log.d("PDF_DEBUG", "URL=$fileUrl")
                val url = URL(fileUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.requestMethod = "GET"
                connection.connect()

                android.util.Log.d("PDF_DEBUG", "ResponseCode=${connection.responseCode}")
                android.util.Log.d("PDF_DEBUG", "ResponseMessage=${connection.responseMessage}")
                android.util.Log.d("PDF_DEBUG", "ContentLength=${connection.contentLength}")
                android.util.Log.d("PDF_DEBUG", "ContentType=${connection.contentType}")

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val tmpFile = File(context.cacheDir, "${targetFile.name}.tmp")
                    val outputStream = FileOutputStream(tmpFile)

                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }

                    outputStream.close()
                    inputStream.close()

                    // Rename temp file to target file atomically
                    if (tmpFile.renameTo(targetFile)) {
                        android.util.Log.d("PDF_DEBUG", "PDF cached successfully")
                        android.util.Log.d("PDF_DEBUG", "CachedFile=${targetFile.absolutePath}")
                        android.util.Log.d("PDF_DEBUG", "FileSize=${targetFile.length()}")
                        _uiState.value = PdfViewingUiState.Success(targetFile)
                        incrementViewsCount(documentId)
                    } else {
                        // Fallback rename if atomic rename fails
                        tmpFile.copyTo(targetFile, overwrite = true)
                        tmpFile.delete()
                        android.util.Log.d("PDF_DEBUG", "PDF cached successfully")
                        android.util.Log.d("PDF_DEBUG", "CachedFile=${targetFile.absolutePath}")
                        android.util.Log.d("PDF_DEBUG", "FileSize=${targetFile.length()}")
                        _uiState.value = PdfViewingUiState.Success(targetFile)
                        incrementViewsCount(documentId)
                    }
                } else {
                    _uiState.value = PdfViewingUiState.Error("Server returned code ${connection.responseCode}")
                }
            } catch (e: Exception) {
                _uiState.value = PdfViewingUiState.Error("Download failed: ${e.localizedMessage}")
            }
        }
    }

    private fun incrementViewsCount(documentId: String) {
        if (hasIncremented) return
        hasIncremented = true
        com.pravor.notessharing.core.analytics.AnalyticsManager.logContentViewFile(
            contentId = documentId,
            contentType = "document",
            fileFormat = "pdf",
            viewerType = "in_app_pdf"
        )
        viewModelScope.launch {
            viewTrackingRepository.incrementViewCount(documentId)
        }
    }
}
