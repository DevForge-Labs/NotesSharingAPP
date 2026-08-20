package com.pravor.notessharing.data.mapper

import com.pravor.notessharing.data.local.entity.HomeFeedItemEntity
import com.pravor.notessharing.domain.model.FeedItem
import com.pravor.notessharing.domain.model.FileType
import org.json.JSONArray

fun HomeFeedItemEntity.toDomainModel(): FeedItem {
    val tagsList = try {
        val array = JSONArray(tagsJson)
        (0 until array.length()).map { array.getString(it) }
    } catch (e: Exception) {
        emptyList()
    }

    val thumbnailUrlsList = try {
        val array = JSONArray(thumbnailUrlsJson)
        (0 until array.length()).map { array.getString(it) }
    } catch (e: Exception) {
        emptyList()
    }

    val fileTypeEnum = try {
        FileType.valueOf(fileTypeName)
    } catch (e: Exception) {
        FileType.Pdf
    }

    return FeedItem(
        id = id,
        uploaderName = uploaderName,
        uploaderInitials = uploaderInitials,
        uploadDate = uploadDate,
        title = title,
        description = description,
        tags = tagsList,
        fileType = fileTypeEnum,
        upvotes = upvotes,
        comments = comments,
        downloadsCount = downloadsCount,
        isUpvoted = false,
        isSaved = isSaved,
        bookmarksCount = bookmarksCount,
        youtubeVideoId = youtubeVideoId,
        youtubeUrl = youtubeUrl,
        thumbnailUrl = thumbnailUrl,
        youtubeThumbnailUrl = youtubeThumbnailUrl,
        thumbnailGenerated = thumbnailGenerated,
        thumbnailType = thumbnailType,
        thumbnailUrls = thumbnailUrlsList,
        documentType = documentType,
        type = type,
        subject = subject,
        examYear = examYear,
        examType = examType,
        section = section,
        sectionDisplay = sectionDisplay
    )
}

fun FeedItem.toEntity(collegeId: String, cachedAtMs: Long = System.currentTimeMillis()): HomeFeedItemEntity {
    val tagsArray = JSONArray()
    tags.forEach { tagsArray.put(it) }

    val thumbnailUrlsArray = JSONArray()
    thumbnailUrls.forEach { thumbnailUrlsArray.put(it) }

    val uploadedMs = try {
        uploadDate.toLongOrNull() ?: 0L
    } catch (e: Exception) {
        0L
    }

    return HomeFeedItemEntity(
        id = id,
        uploaderName = uploaderName,
        uploaderInitials = uploaderInitials,
        uploadDate = uploadDate,
        title = title,
        description = description,
        tagsJson = tagsArray.toString(),
        fileTypeName = fileType.name,
        upvotes = upvotes,
        comments = comments,
        downloadsCount = downloadsCount,
        isSaved = isSaved,
        bookmarksCount = bookmarksCount,
        youtubeVideoId = youtubeVideoId,
        youtubeUrl = youtubeUrl,
        thumbnailUrl = thumbnailUrl,
        youtubeThumbnailUrl = youtubeThumbnailUrl,
        thumbnailGenerated = thumbnailGenerated,
        thumbnailType = thumbnailType,
        thumbnailUrlsJson = thumbnailUrlsArray.toString(),
        documentType = documentType,
        type = type,
        subject = subject,
        examYear = examYear,
        examType = examType,
        section = section,
        sectionDisplay = sectionDisplay,
        collegeId = collegeId,
        cachedAtMs = cachedAtMs,
        uploadedAtMs = uploadedMs
    )
}
