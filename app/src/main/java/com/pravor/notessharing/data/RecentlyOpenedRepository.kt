package com.pravor.notessharing.data

import android.content.Context
import com.pravor.notessharing.model.FeedItem
import com.pravor.notessharing.model.FileType
import org.json.JSONObject

class RecentlyOpenedRepository(context: Context) {
    private val preferences = context.getSharedPreferences("recently_opened", Context.MODE_PRIVATE)

    fun saveLastOpened(
        id: String,
        type: String, // "video" or "document"
        title: String,
        subject: String,
        youtubeVideoId: String?,
        uploaderName: String,
        thumbnailUrl: String? = null,
        thumbnailGenerated: Boolean? = null,
        thumbnailType: String? = null
    ) {
        val json = JSONObject()
            .put("id", id)
            .put("type", type)
            .put("title", title)
            .put("subject", subject)
            .put("youtubeVideoId", youtubeVideoId ?: "")
            .put("timestamp", System.currentTimeMillis())
            .put("uploaderName", uploaderName)
            .put("thumbnailUrl", thumbnailUrl ?: "")
            .put("thumbnailGenerated", thumbnailGenerated ?: false)
            .put("thumbnailType", thumbnailType ?: "")
        
        preferences.edit().putString(KEY_LAST_OPENED, json.toString()).apply()
    }

    fun getLastOpened(): FeedItem? {
        val raw = preferences.getString(KEY_LAST_OPENED, null) ?: return null
        return try {
            val json = JSONObject(raw)
            val id = json.getString("id")
            val type = json.getString("type")
            val title = json.getString("title")
            val subject = json.getString("subject")
            val youtubeVideoId = json.optString("youtubeVideoId").ifBlank { null }
            val timestamp = json.getLong("timestamp")
            val uploaderName = json.optString("uploaderName", "Anonymous")
            
            val thumbnailUrl = json.optString("thumbnailUrl").ifBlank { null }
            val thumbnailGenerated = if (json.has("thumbnailGenerated")) json.getBoolean("thumbnailGenerated") else null
            val thumbnailType = json.optString("thumbnailType").ifBlank { null }

            val fileType = if (type == "video") FileType.Video else FileType.Pdf
            
            val initials = uploaderName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercase() }
                .joinToString("")
                .ifBlank { "AN" }

            FeedItem(
                id = id,
                uploaderName = uploaderName,
                uploaderInitials = initials,
                uploadDate = timestamp.toString(), // Store raw timestamp to compute relative time in UI
                title = title,
                description = subject,
                tags = emptyList(),
                fileType = fileType,
                upvotes = 0,
                comments = 0,
                downloads = 0,
                isUpvoted = false,
                isSaved = false,
                bookmarksCount = 0,
                youtubeVideoId = youtubeVideoId,
                youtubeUrl = null,
                thumbnailUrl = thumbnailUrl,
                thumbnailGenerated = thumbnailGenerated,
                thumbnailType = thumbnailType
            )
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val KEY_LAST_OPENED = "last_opened_item"
    }
}
