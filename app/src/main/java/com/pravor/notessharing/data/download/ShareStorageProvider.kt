package com.pravor.notessharing.data.download

import android.content.Context
import java.io.File

interface ShareStorageProvider {
    fun getShareCacheFile(documentId: String, url: String): File
    suspend fun getDownloadedAttachmentFile(documentId: String, url: String): File?
}

class AndroidShareStorageProvider(private val context: Context) : ShareStorageProvider {
    override fun getShareCacheFile(documentId: String, url: String): File {
        val shareDir = File(context.cacheDir, "shares/$documentId")
        if (!shareDir.exists()) {
            shareDir.mkdirs()
        }
        val fileName = DownloadService.getFileNameFromUrl(url)
        return File(shareDir, fileName)
    }

    override suspend fun getDownloadedAttachmentFile(documentId: String, url: String): File? {
        val db = DownloadDataStoreManager(context)
        val downloadedAttachments = db.getDownloadedAttachments().filter { it.documentId == documentId }
        val attachment = downloadedAttachments.find { it.storagePath == url }
        if (attachment != null) {
            val file = File(attachment.localPath)
            if (file.exists()) {
                return file
            }
        }
        return null
    }
}
