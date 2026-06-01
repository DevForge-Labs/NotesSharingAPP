package com.pravor.notessharing.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

sealed interface ImageViewingUiState {
    object Loading : ImageViewingUiState
    data class Success(val imageFile: File) : ImageViewingUiState
    data class Error(val message: String) : ImageViewingUiState
}

class ImageViewingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ImageViewingUiState>(ImageViewingUiState.Loading)
    val uiState: StateFlow<ImageViewingUiState> = _uiState.asStateFlow()

    fun loadImage(context: Context, documentId: String, imageUrl: String) {
        _uiState.value = ImageViewingUiState.Loading
        viewModelScope.launch {
            try {
                val imageFile = getCachedImageFile(context, documentId, imageUrl)
                if (imageFile.exists() && imageFile.length() > 0) {
                    _uiState.value = ImageViewingUiState.Success(imageFile)
                } else {
                    downloadImageAndCache(context, documentId, imageUrl, imageFile)
                }
            } catch (e: Exception) {
                _uiState.value = ImageViewingUiState.Error(e.localizedMessage ?: "Unknown error occurred.")
            }
        }
    }

    private fun getCachedImageFile(context: Context, documentId: String, imageUrl: String): File {
        val imagesDir = File(context.cacheDir, "images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val ext = imageUrl.substringBefore("?").substringAfterLast(".", "jpg")
        val sanitizedExt = if (ext.length in 2..4) ext else "jpg"
        return File(imagesDir, "${documentId}.$sanitizedExt")
    }

    private suspend fun downloadImageAndCache(
        context: Context,
        documentId: String,
        imageUrl: String,
        targetFile: File
    ) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val tmpFile = File(context.cacheDir, "${documentId}_tmp_img")
                    val outputStream = FileOutputStream(tmpFile)

                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }

                    outputStream.close()
                    inputStream.close()

                    if (tmpFile.renameTo(targetFile)) {
                        _uiState.value = ImageViewingUiState.Success(targetFile)
                    } else {
                        tmpFile.copyTo(targetFile, overwrite = true)
                        tmpFile.delete()
                        _uiState.value = ImageViewingUiState.Success(targetFile)
                    }
                } else {
                    _uiState.value = ImageViewingUiState.Error("Server returned code ${connection.responseCode}")
                }
            } catch (e: Exception) {
                _uiState.value = ImageViewingUiState.Error("Download failed: ${e.localizedMessage}")
            }
        }
    }
}
