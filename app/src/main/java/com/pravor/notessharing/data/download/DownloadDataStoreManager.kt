package com.pravor.notessharing.data.download

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

    suspend fun addDownload(documentId: String, attachments: List<DownloadedAttachment>) {
        context.dataStore.edit { preferences ->
            val docs = parseDownloadedDocuments(preferences[DOWNLOADED_DOCUMENTS_KEY] ?: "[]").toMutableList()
            docs.removeAll { it.documentId == documentId }
            docs.add(DownloadedDocument(documentId, System.currentTimeMillis()))
            preferences[DOWNLOADED_DOCUMENTS_KEY] = serializeDownloadedDocuments(docs)

            val atts = parseDownloadedAttachments(preferences[DOWNLOADED_ATTACHMENTS_KEY] ?: "[]").toMutableList()
            atts.removeAll { it.documentId == documentId }
            atts.addAll(attachments)
            preferences[DOWNLOADED_ATTACHMENTS_KEY] = serializeDownloadedAttachments(atts)
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
                list.add(
                    DownloadedDocument(
                        documentId = obj.getString("documentId"),
                        downloadedAt = obj.getLong("downloadedAt")
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
