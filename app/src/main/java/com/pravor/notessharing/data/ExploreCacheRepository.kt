package com.pravor.notessharing.data

import android.content.Context
import com.pravor.notessharing.model.*
import com.pravor.notessharing.state.ExploreContent
import org.json.JSONArray
import org.json.JSONObject

class ExploreCacheRepository(context: Context) {
    private val preferences = context.getSharedPreferences("explore_cache", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_EXPLORE_CONTENT = "cached_explore_content"
    }

    fun saveCache(content: ExploreContent) {
        try {
            val json = JSONObject()
            
            // 1. topics
            val topicsArray = JSONArray()
            content.topics.forEach {
                topicsArray.put(JSONObject().apply {
                    put("id", it.id)
                    put("title", it.title)
                    put("subtitle", it.subtitle)
                })
            }
            json.put("topics", topicsArray)

            // 2. popularUploads (FeedItem)
            val popularArray = JSONArray()
            content.popularUploads.forEach {
                popularArray.put(serializeFeedItem(it))
            }
            json.put("popularUploads", popularArray)

            // 3. trendingNotes
            val trendingArray = JSONArray()
            content.trendingNotes.forEach {
                trendingArray.put(JSONObject().apply {
                    put("id", it.id)
                    put("title", it.title)
                    put("subject", it.subject)
                    put("downloads", it.downloads)
                    put("rating", it.rating)
                    put("upvotes", it.upvotes)
                    put("isBookmarked", it.isBookmarked)
                    put("thumbnailUrl", it.thumbnailUrl ?: "")
                    put("thumbnailGenerated", it.thumbnailGenerated ?: false)
                    put("thumbnailType", it.thumbnailType ?: "")
                })
            }
            json.put("trendingNotes", trendingArray)

            // 4. videoRecommendations
            val videosArray = JSONArray()
            content.videoRecommendations.forEach {
                videosArray.put(JSONObject().apply {
                    put("id", it.id)
                    put("title", it.title)
                    put("channelName", it.channelName)
                    put("duration", it.duration)
                    put("subject", it.subject)
                    put("youtubeVideoId", it.youtubeVideoId)
                    put("upvotes", it.upvotes)
                    put("bookmarks", it.bookmarks)
                })
            }
            json.put("videoRecommendations", videosArray)

            // 5. studyCollections
            val collectionsArray = JSONArray()
            content.studyCollections.forEach {
                collectionsArray.put(JSONObject().apply {
                    put("id", it.id)
                    put("title", it.title)
                    put("notes", it.notes)
                    put("pyqs", it.pyqs)
                    put("playlists", it.playlists)
                    put("cheatSheets", it.cheatSheets)
                })
            }
            json.put("studyCollections", collectionsArray)

            // 6. subjectHubs
            val hubsArray = JSONArray()
            content.subjectHubs.forEach { hubsArray.put(it) }
            json.put("subjectHubs", hubsArray)

            // 7. topContributors
            val contributorsArray = JSONArray()
            content.topContributors.forEach {
                contributorsArray.put(JSONObject().apply {
                    put("id", it.id)
                    put("name", it.name)
                    put("initials", it.initials)
                    put("uploads", it.uploads)
                    put("rating", it.rating)
                })
            }
            json.put("topContributors", contributorsArray)

            // 8. revisionCards
            val revisionArray = JSONArray()
            content.revisionCards.forEach {
                val pointsArray = JSONArray()
                it.points.forEach { pt -> pointsArray.put(pt) }
                revisionArray.put(JSONObject().apply {
                    put("id", it.id)
                    put("title", it.title)
                    put("points", pointsArray)
                })
            }
            json.put("revisionCards", revisionArray)

            // 9. discoverItems
            val discoverArray = JSONArray()
            content.discoverItems.forEach {
                discoverArray.put(serializeDiscoverItem(it))
            }
            json.put("discoverItems", discoverArray)

            preferences.edit().putString(KEY_EXPLORE_CONTENT, json.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCache(): ExploreContent? {
        val raw = preferences.getString(KEY_EXPLORE_CONTENT, null) ?: return null
        return try {
            val json = JSONObject(raw)

            // 1. topics
            val topicsList = mutableListOf<TrendingTopic>()
            val topicsArray = json.optJSONArray("topics")
            if (topicsArray != null) {
                for (i in 0 until topicsArray.length()) {
                    val obj = topicsArray.getJSONObject(i)
                    topicsList.add(TrendingTopic(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        subtitle = obj.getString("subtitle")
                    ))
                }
            }

            // 2. popularUploads
            val popularList = mutableListOf<FeedItem>()
            val popularArray = json.optJSONArray("popularUploads")
            if (popularArray != null) {
                for (i in 0 until popularArray.length()) {
                    popularList.add(deserializeFeedItem(popularArray.getJSONObject(i)))
                }
            }

            // 3. trendingNotes
            val trendingList = mutableListOf<TrendingNote>()
            val trendingArray = json.optJSONArray("trendingNotes")
            if (trendingArray != null) {
                for (i in 0 until trendingArray.length()) {
                    val obj = trendingArray.getJSONObject(i)
                    trendingList.add(TrendingNote(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        subject = obj.getString("subject"),
                        downloads = obj.getInt("downloads"),
                        rating = obj.getDouble("rating"),
                        upvotes = obj.getInt("upvotes"),
                        isBookmarked = obj.getBoolean("isBookmarked"),
                        thumbnailUrl = obj.optString("thumbnailUrl").ifBlank { null },
                        thumbnailGenerated = if (obj.has("thumbnailGenerated")) obj.getBoolean("thumbnailGenerated") else null,
                        thumbnailType = obj.optString("thumbnailType").ifBlank { null }
                    ))
                }
            }

            // 4. videoRecommendations
            val videosList = mutableListOf<VideoRecommendation>()
            val videosArray = json.optJSONArray("videoRecommendations")
            if (videosArray != null) {
                for (i in 0 until videosArray.length()) {
                    val obj = videosArray.getJSONObject(i)
                    videosList.add(VideoRecommendation(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        channelName = obj.getString("channelName"),
                        duration = obj.getString("duration"),
                        subject = obj.getString("subject"),
                        youtubeVideoId = obj.getString("youtubeVideoId"),
                        upvotes = obj.optInt("upvotes", 0),
                        bookmarks = obj.optInt("bookmarks", 0)
                    ))
                }
            }

            // 5. studyCollections
            val collectionsList = mutableListOf<StudyCollection>()
            val collectionsArray = json.optJSONArray("studyCollections")
            if (collectionsArray != null) {
                for (i in 0 until collectionsArray.length()) {
                    val obj = collectionsArray.getJSONObject(i)
                    collectionsList.add(StudyCollection(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        notes = obj.getInt("notes"),
                        pyqs = obj.getInt("pyqs"),
                        playlists = obj.getInt("playlists"),
                        cheatSheets = obj.getInt("cheatSheets")
                    ))
                }
            }

            // 6. subjectHubs
            val hubsList = mutableListOf<String>()
            val hubsArray = json.optJSONArray("subjectHubs")
            if (hubsArray != null) {
                for (i in 0 until hubsArray.length()) {
                    hubsList.add(hubsArray.getString(i))
                }
            }

            // 7. topContributors
            val contributorsList = mutableListOf<Contributor>()
            val contributorsArray = json.optJSONArray("topContributors")
            if (contributorsArray != null) {
                for (i in 0 until contributorsArray.length()) {
                    val obj = contributorsArray.getJSONObject(i)
                    contributorsList.add(Contributor(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        initials = obj.getString("initials"),
                        uploads = obj.getInt("uploads"),
                        rating = obj.getDouble("rating")
                    ))
                }
            }

            // 8. revisionCards
            val revisionList = mutableListOf<RevisionCard>()
            val revisionArray = json.optJSONArray("revisionCards")
            if (revisionArray != null) {
                for (i in 0 until revisionArray.length()) {
                    val obj = revisionArray.getJSONObject(i)
                    val pts = mutableListOf<String>()
                    val ptsArray = obj.getJSONArray("points")
                    for (j in 0 until ptsArray.length()) {
                        pts.add(ptsArray.getString(j))
                    }
                    revisionList.add(RevisionCard(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        points = pts
                    ))
                }
            }

            // 9. discoverItems
            val discoverList = mutableListOf<DiscoverFeedItem>()
            val discoverArray = json.optJSONArray("discoverItems")
            if (discoverArray != null) {
                for (i in 0 until discoverArray.length()) {
                    val obj = discoverArray.getJSONObject(i)
                    deserializeDiscoverItem(obj)?.let { discoverList.add(it) }
                }
            }

            ExploreContent(
                topics = topicsList,
                popularUploads = popularList,
                trendingNotes = trendingList,
                videoRecommendations = videosList,
                studyCollections = collectionsList,
                subjectHubs = hubsList,
                topContributors = contributorsList,
                revisionCards = revisionList,
                discoverItems = discoverList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun serializeFeedItem(item: FeedItem): JSONObject {
        return JSONObject().apply {
            put("id", item.id)
            put("uploaderName", item.uploaderName)
            put("uploaderInitials", item.uploaderInitials)
            put("uploadDate", item.uploadDate)
            put("title", item.title)
            put("description", item.description)
            val tagsArray = JSONArray()
            item.tags.forEach { tagsArray.put(it) }
            put("tags", tagsArray)
            put("fileType", item.fileType.name)
            put("upvotes", item.upvotes)
            put("comments", item.comments)
            put("downloads", item.downloads)
            put("isUpvoted", item.isUpvoted)
            put("isSaved", item.isSaved)
            put("bookmarksCount", item.bookmarksCount)
            put("youtubeVideoId", item.youtubeVideoId ?: "")
            put("youtubeUrl", item.youtubeUrl ?: "")
            put("thumbnailUrl", item.thumbnailUrl ?: "")
            put("thumbnailGenerated", item.thumbnailGenerated ?: false)
            put("thumbnailType", item.thumbnailType ?: "")
        }
    }

    private fun deserializeFeedItem(obj: JSONObject): FeedItem {
        val tagsArray = obj.getJSONArray("tags")
        val tagsList = mutableListOf<String>()
        for (i in 0 until tagsArray.length()) {
            tagsList.add(tagsArray.getString(i))
        }
        val fileType = try {
            FileType.valueOf(obj.getString("fileType"))
        } catch (e: Exception) {
            FileType.Pdf
        }

        return FeedItem(
            id = obj.getString("id"),
            uploaderName = obj.getString("uploaderName"),
            uploaderInitials = obj.getString("uploaderInitials"),
            uploadDate = obj.getString("uploadDate"),
            title = obj.getString("title"),
            description = obj.getString("description"),
            tags = tagsList,
            fileType = fileType,
            upvotes = obj.getInt("upvotes"),
            comments = obj.getInt("comments"),
            downloads = obj.getInt("downloads"),
            isUpvoted = obj.getBoolean("isUpvoted"),
            isSaved = obj.getBoolean("isSaved"),
            bookmarksCount = obj.optInt("bookmarksCount", 0),
            youtubeVideoId = obj.optString("youtubeVideoId").ifBlank { null },
            youtubeUrl = obj.optString("youtubeUrl").ifBlank { null },
            thumbnailUrl = obj.optString("thumbnailUrl").ifBlank { null },
            thumbnailGenerated = if (obj.has("thumbnailGenerated")) obj.getBoolean("thumbnailGenerated") else null,
            thumbnailType = obj.optString("thumbnailType").ifBlank { null }
        )
    }

    private fun serializeDiscoverItem(item: DiscoverFeedItem): JSONObject {
        return JSONObject().apply {
            when (item) {
                is DiscoverFeedItem.Note -> {
                    put("discType", "Note")
                    put("id", item.id)
                    put("title", item.title)
                    put("subject", item.subject)
                    put("downloads", item.downloads)
                }
                is DiscoverFeedItem.Video -> {
                    put("discType", "Video")
                    put("id", item.id)
                    put("title", item.title)
                    put("channelName", item.channelName)
                    put("duration", item.duration)
                }
                is DiscoverFeedItem.Collection -> {
                    put("discType", "Collection")
                    put("id", item.id)
                    put("title", item.title)
                    put("resourceCount", item.resourceCount)
                }
                is DiscoverFeedItem.ContributorPost -> {
                    put("discType", "ContributorPost")
                    put("id", item.id)
                    put("name", item.name)
                    put("initials", item.initials)
                    put("message", item.message)
                }
            }
        }
    }

    private fun deserializeDiscoverItem(obj: JSONObject): DiscoverFeedItem? {
        return when (obj.optString("discType")) {
            "Note" -> DiscoverFeedItem.Note(
                id = obj.getString("id"),
                title = obj.getString("title"),
                subject = obj.getString("subject"),
                downloads = obj.getInt("downloads")
            )
            "Video" -> DiscoverFeedItem.Video(
                id = obj.getString("id"),
                title = obj.getString("title"),
                channelName = obj.getString("channelName"),
                duration = obj.getString("duration")
            )
            "Collection" -> DiscoverFeedItem.Collection(
                id = obj.getString("id"),
                title = obj.getString("title"),
                resourceCount = obj.getInt("resourceCount")
            )
            "ContributorPost" -> DiscoverFeedItem.ContributorPost(
                id = obj.getString("id"),
                name = obj.getString("name"),
                initials = obj.getString("initials"),
                message = obj.getString("message")
            )
            else -> null
        }
    }
}
