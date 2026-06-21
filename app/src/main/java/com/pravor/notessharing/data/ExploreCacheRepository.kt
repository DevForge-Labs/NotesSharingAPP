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

            // 3. notes
            val notesArray = JSONArray()
            content.notes.forEach {
                notesArray.put(serializeTrendingNote(it))
            }
            json.put("notes", notesArray)

            // 4. examPrep
            val examPrepArray = JSONArray()
            content.examPrep.forEach {
                examPrepArray.put(serializeTrendingNote(it))
            }
            json.put("examPrep", examPrepArray)

            // 5. assignments
            val assignmentsArray = JSONArray()
            content.assignments.forEach {
                assignmentsArray.put(serializeTrendingNote(it))
            }
            json.put("assignments", assignmentsArray)

            // 6. videos
            val videosArray = JSONArray()
            content.videos.forEach {
                videosArray.put(serializeTrendingNote(it))
            }
            json.put("videos", videosArray)

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

            // 3. notes
            val notesList = mutableListOf<TrendingNote>()
            val notesArray = json.optJSONArray("notes")
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    notesList.add(deserializeTrendingNote(notesArray.getJSONObject(i)))
                }
            }

            // 4. examPrep
            val examPrepList = mutableListOf<TrendingNote>()
            val examPrepArray = json.optJSONArray("examPrep")
            if (examPrepArray != null) {
                for (i in 0 until examPrepArray.length()) {
                    examPrepList.add(deserializeTrendingNote(examPrepArray.getJSONObject(i)))
                }
            }

            // 5. assignments
            val assignmentsList = mutableListOf<TrendingNote>()
            val assignmentsArray = json.optJSONArray("assignments")
            if (assignmentsArray != null) {
                for (i in 0 until assignmentsArray.length()) {
                    assignmentsList.add(deserializeTrendingNote(assignmentsArray.getJSONObject(i)))
                }
            }

            // 6. videos
            val videosList = mutableListOf<TrendingNote>()
            val videosArray = json.optJSONArray("videos")
            if (videosArray != null) {
                for (i in 0 until videosArray.length()) {
                    videosList.add(deserializeTrendingNote(videosArray.getJSONObject(i)))
                }
            }

            // 7. studyCollections
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

            // 8. subjectHubs
            val hubsList = mutableListOf<String>()
            val hubsArray = json.optJSONArray("subjectHubs")
            if (hubsArray != null) {
                for (i in 0 until hubsArray.length()) {
                    hubsList.add(hubsArray.getString(i))
                }
            }

            // 9. topContributors
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

            // 10. revisionCards
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

            // 11. discoverItems
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
                notes = notesList,
                examPrep = examPrepList,
                assignments = assignmentsList,
                videos = videosList,
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
            put("downloadsCount", item.downloadsCount)
            put("isUpvoted", item.isUpvoted)
            put("isSaved", item.isSaved)
            put("bookmarksCount", item.bookmarksCount)
            put("youtubeVideoId", item.youtubeVideoId ?: "")
            put("youtubeUrl", item.youtubeUrl ?: "")
            put("thumbnailUrl", item.thumbnailUrl ?: "")
            put("youtubeThumbnailUrl", item.youtubeThumbnailUrl ?: "")
            put("thumbnailGenerated", item.thumbnailGenerated ?: false)
            put("thumbnailType", item.thumbnailType ?: "")
            val thumbnailUrlsArray = JSONArray()
            item.thumbnailUrls.forEach { thumbnailUrlsArray.put(it) }
            put("thumbnailUrls", thumbnailUrlsArray)
            put("documentType", item.documentType ?: "")
            put("type", item.type ?: "")
            put("subject", item.subject ?: "")
            put("examYear", item.examYear ?: "")
            put("examType", item.examType ?: "")
            put("section", item.section ?: "")
            put("sectionDisplay", item.sectionDisplay ?: "")
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
            downloadsCount = obj.getInt("downloadsCount"),
            isUpvoted = obj.getBoolean("isUpvoted"),
            isSaved = obj.getBoolean("isSaved"),
            bookmarksCount = obj.optInt("bookmarksCount", 0),
            youtubeVideoId = obj.optString("youtubeVideoId").ifBlank { null },
            youtubeUrl = obj.optString("youtubeUrl").ifBlank { null },
            thumbnailUrl = obj.optString("thumbnailUrl").ifBlank { null },
            thumbnailGenerated = if (obj.has("thumbnailGenerated")) obj.getBoolean("thumbnailGenerated") else null,
            thumbnailType = obj.optString("thumbnailType").ifBlank { null },
            thumbnailUrls = run {
                val array = obj.optJSONArray("thumbnailUrls")
                val list = mutableListOf<String>()
                if (array != null) {
                    for (j in 0 until array.length()) {
                        list.add(array.getString(j))
                    }
                } else {
                    val single = obj.optString("thumbnailUrl")
                    if (single.isNotBlank()) list.add(single)
                }
                list
            },
            documentType = obj.optString("documentType").ifBlank { null },
            type = obj.optString("type").ifBlank { null },
            subject = obj.optString("subject").ifBlank { null },
            examYear = obj.optString("examYear").ifBlank { null },
            examType = obj.optString("examType").ifBlank { null },
            section = obj.optString("section").ifBlank { null },
            sectionDisplay = obj.optString("sectionDisplay").ifBlank { null },
            youtubeThumbnailUrl = obj.optString("youtubeThumbnailUrl").ifBlank { null }
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
                    put("downloadsCount", item.downloadsCount)
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
                downloadsCount = obj.getInt("downloadsCount")
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

    private fun serializeTrendingNote(note: TrendingNote): JSONObject {
        return JSONObject().apply {
            put("id", note.id)
            put("title", note.title)
            put("subject", note.subject)
            put("downloadsCount", note.downloadsCount)
            put("rating", note.rating)
            put("upvotes", note.upvotes)
            put("isBookmarked", note.isBookmarked)
            put("thumbnailUrl", note.thumbnailUrl ?: "")
            put("thumbnailGenerated", note.thumbnailGenerated ?: false)
            put("thumbnailType", note.thumbnailType ?: "")
            put("description", note.description)
            put("uploaderName", note.uploaderName)
            put("uploaderPhotoUrl", note.uploaderPhotoUrl)
            put("contributorLevel", note.contributorLevel)
            put("documentType", note.documentType)
            put("type", note.type ?: "")
            put("bookmarks", note.bookmarks)
            put("examYear", note.examYear ?: "")
            put("examType", note.examType ?: "")
            put("semester", note.semester)
            put("isUpvoted", note.isUpvoted)
            put("branch", note.branch)
            put("trendingScore", note.trendingScore)
            put("displaySubject", note.displaySubject ?: "")
            put("sectionDisplay", note.sectionDisplay ?: "")
            put("uploadedAt", note.uploadedAt)
            put("resourceType", note.resourceType.name)
            put("channelName", note.channelName)
            put("duration", note.duration)
            put("youtubeVideoId", note.youtubeVideoId)
            put("youtubeThumbnailUrl", note.youtubeThumbnailUrl ?: "")
            put("youtubeUrl", note.youtubeUrl)
        }
    }

    private fun deserializeTrendingNote(obj: JSONObject): TrendingNote {
        val resourceTypeName = obj.optString("resourceType", ResourceType.NOTE.name)
        val resourceType = try {
            ResourceType.valueOf(resourceTypeName)
        } catch (e: Exception) {
            ResourceType.NOTE
        }

        return TrendingNote(
            id = obj.getString("id"),
            title = obj.getString("title"),
            subject = obj.getString("subject"),
            downloadsCount = obj.getInt("downloadsCount"),
            rating = obj.getDouble("rating"),
            upvotes = obj.getInt("upvotes"),
            isBookmarked = obj.getBoolean("isBookmarked"),
            thumbnailUrl = obj.optString("thumbnailUrl").ifBlank { null },
            thumbnailGenerated = if (obj.has("thumbnailGenerated")) obj.getBoolean("thumbnailGenerated") else null,
            thumbnailType = obj.optString("thumbnailType").ifBlank { null },
            description = obj.optString("description"),
            uploaderName = obj.optString("uploaderName"),
            uploaderPhotoUrl = obj.optString("uploaderPhotoUrl"),
            contributorLevel = obj.optString("contributorLevel"),
            documentType = obj.optString("documentType", ""),
            type = obj.optString("type").ifBlank { null },
            bookmarks = obj.optInt("bookmarks", 0),
            examYear = obj.optString("examYear").ifBlank { null },
            examType = obj.optString("examType").ifBlank { null },
            semester = obj.optString("semester", ""),
            isUpvoted = obj.optBoolean("isUpvoted", false),
            branch = obj.optString("branch", ""),
            trendingScore = obj.optDouble("trendingScore", 0.0),
            displaySubject = obj.optString("displaySubject").ifBlank { null },
            sectionDisplay = obj.optString("sectionDisplay").ifBlank { null },
            uploadedAt = obj.optLong("uploadedAt", 0L),
            resourceType = resourceType,
            channelName = obj.optString("channelName", ""),
            duration = obj.optString("duration", ""),
            youtubeVideoId = obj.optString("youtubeVideoId", ""),
            youtubeThumbnailUrl = obj.optString("youtubeThumbnailUrl").ifBlank { null },
            youtubeUrl = obj.optString("youtubeUrl", "")
        )
    }
}
