package com.pravor.notessharing.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pravor.notessharing.data.download.DownloadDataStoreManager
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

sealed interface PdfViewingUiState {
    object Loading : PdfViewingUiState
    data class Success(val pdfFile: File) : PdfViewingUiState
    data class Error(val message: String) : PdfViewingUiState
}

class PdfViewingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<PdfViewingUiState>(PdfViewingUiState.Loading)
    val uiState: StateFlow<PdfViewingUiState> = _uiState.asStateFlow()

    fun loadPdf(context: Context, documentId: String, fileUrl: String) {
        android.util.Log.d("PDF_DEBUG", "loadPdf called")
        android.util.Log.d("PDF_DEBUG", "DocumentId=$documentId")
        android.util.Log.d("PDF_DEBUG", "FileUrl=$fileUrl")
        _uiState.value = PdfViewingUiState.Loading
        viewModelScope.launch {
            try {
                // 1. Check local attachment registry in DataStore
                val db = DownloadDataStoreManager(context.applicationContext)
                val localPath = db.getAttachmentLocalPath(fileUrl)
                if (localPath != null) {
                    val localFile = File(localPath)
                    if (localFile.exists() && localFile.length() > 0) {
                        _uiState.value = PdfViewingUiState.Success(localFile)
                        incrementViewsCount(documentId)
                        return@launch
                    }
                }

                // 2. Fall back to cache or download
                val pdfFile = getCachedPdfFile(context, documentId)
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

    private fun getCachedPdfFile(context: Context, documentId: String): File {
        val pdfsDir = File(context.cacheDir, "pdfs")
        if (!pdfsDir.exists()) {
            pdfsDir.mkdirs()
        }
        return File(pdfsDir, "$documentId.pdf")
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
                    val tmpFile = File(context.cacheDir, "${documentId}_tmp.pdf")
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
        // TODO: Analytics - Increment viewsCount when a PDF is successfully opened.
    }
}
