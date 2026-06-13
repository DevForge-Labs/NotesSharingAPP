package com.pravor.notessharing.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.model.FeedItem
import com.pravor.notessharing.model.FileType
import org.json.JSONObject

class RecentlyOpenedRepository(context: Context) {
    private val preferences = context.getSharedPreferences("recently_opened", Context.MODE_PRIVATE)

    private fun getUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
    }

    fun saveLastOpened(
        id: String,
        type: String, // "video" or "document"
        title: String,
        subject: String,
        youtubeVideoId: String?,
        uploaderName: String,
        thumbnailUrl: String? = null,
        thumbnailGenerated: Boolean? = null,
        thumbnailType: String? = null,
        thumbnailUrls: List<String> = emptyList(),
        documentType: String? = null,
        typeField: String? = null,
        examYear: String? = null,
        section: String? = null,
        sectionDisplay: String? = null,
        youtubeThumbnailUrl: String? = null
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
            .put("documentType", documentType ?: "")
            .put("typeField", typeField ?: "")
            .put("examYear", examYear ?: "")
            .put("section", section ?: "")
            .put("sectionDisplay", sectionDisplay ?: "")
            .put("youtubeThumbnailUrl", youtubeThumbnailUrl ?: "")
        
        val thumbnailUrlsArray = org.json.JSONArray()
        thumbnailUrls.forEach { thumbnailUrlsArray.put(it) }
        json.put("thumbnailUrls", thumbnailUrlsArray)
        
        val key = "${KEY_LAST_OPENED}_${getUserId()}"
        preferences.edit().putString(key, json.toString()).apply()
    }

    fun getLastOpened(): FeedItem? {
        val key = "${KEY_LAST_OPENED}_${getUserId()}"
        val raw = preferences.getString(key, null) ?: return null
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
            val youtubeThumbnailUrl = json.optString("youtubeThumbnailUrl").ifBlank { null }
            val thumbnailGenerated = if (json.has("thumbnailGenerated")) json.getBoolean("thumbnailGenerated") else null
            val thumbnailType = json.optString("thumbnailType").ifBlank { null }
            
            val thumbnailUrlsArray = json.optJSONArray("thumbnailUrls")
            val thumbnailUrlsList = mutableListOf<String>()
            if (thumbnailUrlsArray != null) {
                for (i in 0 until thumbnailUrlsArray.length()) {
                    thumbnailUrlsList.add(thumbnailUrlsArray.getString(i))
                }
            } else if (!thumbnailUrl.isNullOrBlank()) {
                thumbnailUrlsList.add(thumbnailUrl)
            }

            val documentType = json.optString("documentType").ifBlank { null }
            val typeField = json.optString("typeField").ifBlank { null }
            val examYear = json.optString("examYear").ifBlank { null }
            val section = json.optString("section").ifBlank { null }
            val sectionDisplay = json.optString("sectionDisplay").ifBlank { null }

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
                downloadsCount = 0,
                isUpvoted = false,
                isSaved = false,
                bookmarksCount = 0,
                youtubeVideoId = youtubeVideoId,
                youtubeUrl = null,
                thumbnailUrl = thumbnailUrl,
                thumbnailGenerated = thumbnailGenerated,
                thumbnailType = thumbnailType,
                thumbnailUrls = thumbnailUrlsList,
                documentType = documentType,
                type = typeField,
                subject = subject,
                examYear = examYear,
                section = section,
                sectionDisplay = sectionDisplay,
                youtubeThumbnailUrl = youtubeThumbnailUrl
            )
        } catch (e: Exception) {
            null
        }
    }

    fun clearLastOpened() {
        val key = "${KEY_LAST_OPENED}_${getUserId()}"
        preferences.edit().remove(key).apply()
    }

    companion object {
        private const val KEY_LAST_OPENED = "last_opened_item"
    }
}
