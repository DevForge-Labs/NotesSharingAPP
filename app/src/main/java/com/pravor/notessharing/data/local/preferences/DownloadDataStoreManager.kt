package com.pravor.notessharing.data.local.preferences


import com.pravor.notessharing.domain.model.*

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "downloads_metadata")

class DownloadDataStoreManager(private val context: Context) {

    companion object {
        private val DOWNLOADED_DOCUMENTS_KEY = stringPreferencesKey("downloaded_documents")
        private val DOWNLOADED_ATTACHMENTS_KEY = stringPreferencesKey("downloaded_attachments")
    }

    // Get all downloaded documents as Flow
    val downloadedDocumentsFlow: Flow<List<DownloadedDocument>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[DOWNLOADED_DOCUMENTS_KEY] ?: "[]"
        parseDownloadedDocuments(jsonStr)
    }

    // Get all downloaded attachments as Flow
    val downloadedAttachmentsFlow: Flow<List<DownloadedAttachment>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[DOWNLOADED_ATTACHMENTS_KEY] ?: "[]"
        parseDownloadedAttachments(jsonStr)
    }

    suspend fun getDownloadedDocuments(): List<DownloadedDocument> {
        return downloadedDocumentsFlow.first()
    }

    suspend fun getDownloadedAttachments(): List<DownloadedAttachment> {
        return downloadedAttachmentsFlow.first()
    }

    suspend fun addDownload(
        document: com.pravor.notessharing.domain.model.DocumentDetail,
        contributorLevel: String,
        attachments: List<DownloadedAttachment>,
        localThumbnailPath: String? = null
    ) {
        context.dataStore.edit { preferences ->
            val docs = parseDownloadedDocuments(preferences[DOWNLOADED_DOCUMENTS_KEY] ?: "[]").toMutableList()
            docs.removeAll { it.documentId == document.id }
            docs.add(
                DownloadedDocument(
                    documentId = document.id,
                    downloadedAt = System.currentTimeMillis(),
                    title = document.title,
                    subject = document.subject,
                    uploaderName = document.uploaderName,
                    uploaderContributorLevel = contributorLevel,
                    documentType = document.documentType,
                    fileUrls = document.fileUrls,
                    thumbnailUrl = document.thumbnailUrl,
                    examYear = document.examYear,
                    examType = document.examType,
                    sectionDisplay = document.sectionDisplay,
                    upvotes = document.upvotes,
                    downloadsCount = document.downloadsCount,
                    localThumbnailPath = localThumbnailPath
                )
            )
            preferences[DOWNLOADED_DOCUMENTS_KEY] = serializeDownloadedDocuments(docs)

            val atts = parseDownloadedAttachments(preferences[DOWNLOADED_ATTACHMENTS_KEY] ?: "[]").toMutableList()
            atts.removeAll { it.documentId == document.id }
            atts.addAll(attachments)
            preferences[DOWNLOADED_ATTACHMENTS_KEY] = serializeDownloadedAttachments(atts)
        }
        try {
            com.pravor.notessharing.core.widget.WidgetUpdateManager.updateAllWidgets(context)
        } catch (ex: Exception) {
            android.util.Log.e("DownloadDataStoreManager", "Widget update error: ${ex.message}", ex)
        }
    }

    suspend fun removeDownload(documentId: String) {
        context.dataStore.edit { preferences ->
            val docs = parseDownloadedDocuments(preferences[DOWNLOADED_DOCUMENTS_KEY] ?: "[]").toMutableList()
            docs.removeAll { it.documentId == documentId }
            preferences[DOWNLOADED_DOCUMENTS_KEY] = serializeDownloadedDocuments(docs)

            val atts = parseDownloadedAttachments(preferences[DOWNLOADED_ATTACHMENTS_KEY] ?: "[]").toMutableList()
            atts.removeAll { it.documentId == documentId }
            preferences[DOWNLOADED_ATTACHMENTS_KEY] = serializeDownloadedAttachments(atts)
        }
        try {
            com.pravor.notessharing.core.widget.WidgetUpdateManager.updateAllWidgets(context)
        } catch (ex: Exception) {
            android.util.Log.e("DownloadDataStoreManager", "Widget update error: ${ex.message}", ex)
        }
    }

    suspend fun isDocumentDownloaded(documentId: String): Boolean {
        return getDownloadedDocuments().any { it.documentId == documentId }
    }

    fun isDocumentDownloadedFlow(documentId: String): Flow<Boolean> {
        return downloadedDocumentsFlow.map { docs ->
            docs.any { it.documentId == documentId }
        }
    }

    suspend fun getAttachmentLocalPath(storagePath: String): String? {
        return getDownloadedAttachments().firstOrNull { it.storagePath == storagePath }?.localPath
    }

    // JSON Helper Methods using org.json (built-in Android SDK)
    private fun parseDownloadedDocuments(jsonStr: String): List<DownloadedDocument> {
        val list = mutableListOf<DownloadedDocument>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                
                val fileUrlsArray = obj.optJSONArray("fileUrls")
                val fileUrlsList = mutableListOf<String>()
                if (fileUrlsArray != null) {
                    for (j in 0 until fileUrlsArray.length()) {
                        fileUrlsList.add(fileUrlsArray.getString(j))
                    }
                }

                list.add(
                    DownloadedDocument(
                        documentId = obj.getString("documentId"),
                        downloadedAt = obj.getLong("downloadedAt"),
                        title = obj.optString("title", ""),
                        subject = obj.optString("subject", ""),
                        uploaderName = obj.optString("uploaderName", "Anonymous"),
                        uploaderContributorLevel = obj.optString("uploaderContributorLevel", "Bronze Contributor"),
                        documentType = obj.optString("documentType", "Notes"),
                        fileUrls = fileUrlsList,
                        thumbnailUrl = obj.optString("thumbnailUrl", "").ifEmpty { null },
                        examYear = obj.optString("examYear", "").ifEmpty { null },
                        examType = obj.optString("examType", "").ifEmpty { null },
                        sectionDisplay = obj.optString("sectionDisplay", "").ifEmpty { null },
                        upvotes = obj.optInt("upvotes", 0),
                        downloadsCount = obj.optInt("downloadsCount", 0),
                        localThumbnailPath = obj.optString("localThumbnailPath", "").ifEmpty { null }
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun serializeDownloadedDocuments(list: List<DownloadedDocument>): String {
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("documentId", item.documentId)
            obj.put("downloadedAt", item.downloadedAt)
            obj.put("title", item.title)
            obj.put("subject", item.subject)
            obj.put("uploaderName", item.uploaderName)
            obj.put("uploaderContributorLevel", item.uploaderContributorLevel)
            obj.put("documentType", item.documentType)
            
            val fileUrlsArray = JSONArray()
            item.fileUrls.forEach { fileUrlsArray.put(it) }
            obj.put("fileUrls", fileUrlsArray)
            
            obj.put("thumbnailUrl", item.thumbnailUrl ?: "")
            obj.put("examYear", item.examYear ?: "")
            obj.put("examType", item.examType ?: "")
            obj.put("sectionDisplay", item.sectionDisplay ?: "")
            obj.put("upvotes", item.upvotes)
            obj.put("downloadsCount", item.downloadsCount)
            obj.put("localThumbnailPath", item.localThumbnailPath ?: "")
            
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    private fun parseDownloadedAttachments(jsonStr: String): List<DownloadedAttachment> {
        val list = mutableListOf<DownloadedAttachment>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    DownloadedAttachment(
                        documentId = obj.getString("documentId"),
                        storagePath = obj.getString("storagePath"),
                        localPath = obj.getString("localPath")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun serializeDownloadedAttachments(list: List<DownloadedAttachment>): String {
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("documentId", item.documentId)
            obj.put("storagePath", item.storagePath)
            obj.put("localPath", item.localPath)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }
}
