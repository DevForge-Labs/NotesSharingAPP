package com.pravor.notessharing.data.mapper

import com.pravor.notessharing.data.local.entity.ExploreItemEntity
import com.pravor.notessharing.domain.model.DiscoverFeedItem
import com.pravor.notessharing.domain.model.FeedItem
import com.pravor.notessharing.domain.model.FileType
import com.pravor.notessharing.domain.model.TrendingNote
import org.json.JSONArray

fun ExploreItemEntity.toFeedItem(): FeedItem {
    val tagsList = try {
        val array = JSONArray(tagsJson)
        (0 until array.length()).map { array.getString(it) }
    } catch (e: Exception) {
        emptyList()
    }
    val urlsList = try {
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
        comments = commentsCount,
        downloadsCount = downloadsCount,
        isUpvoted = false,
        isSaved = false,
        bookmarksCount = bookmarksCount,
        youtubeVideoId = youtubeVideoId,
        youtubeUrl = youtubeUrl,
        thumbnailUrl = thumbnailUrl,
        youtubeThumbnailUrl = youtubeThumbnailUrl,
        thumbnailGenerated = thumbnailGenerated,
        thumbnailType = thumbnailType,
        thumbnailUrls = urlsList,
        documentType = documentType,
        type = type,
        subject = subject,
        examYear = examYear,
        examType = examType,
        section = section,
        sectionDisplay = sectionDisplay
    )
}

fun ExploreItemEntity.toTrendingNote(): TrendingNote {
    val docTypeLower = documentType?.trim()?.lowercase(java.util.Locale.US) ?: ""
    val typeLower = type?.trim()?.lowercase(java.util.Locale.US) ?: ""
    val isVideo = docTypeLower in listOf("video", "videos", "youtube resource", "playlist", "playlists") ||
            (!youtubeVideoId.isNullOrBlank()) || (!youtubeUrl.isNullOrBlank())
    val isPyq = docTypeLower in listOf("pyq", "pyqs", "past paper", "past papers", "question paper", "question papers", "exam prep", "previous year question", "previous year questions", "exam paper", "exam papers", "midsem", "endsem", "mid sem", "end sem") ||
            typeLower in listOf("pyq", "pyqs", "past paper", "past papers", "question paper", "question papers", "exam prep", "previous year question", "previous year questions", "exam paper", "exam papers", "midsem", "endsem", "mid sem", "end sem") ||
            docTypeLower.contains("pyq") || typeLower.contains("pyq") ||
            !examYear.isNullOrBlank() || !examType.isNullOrBlank()
    val isCheatSheet = docTypeLower in listOf("cheat sheet", "cheatsheet", "cheatsheets", "cheat_sheet", "formula sheet", "formula sheets", "formulasheet") ||
            typeLower in listOf("cheat sheet", "cheatsheet", "cheatsheets", "cheat_sheet", "formula sheet", "formula sheets", "formulasheet") ||
            docTypeLower.contains("cheat") || typeLower.contains("cheat")
    val isAssignment = docTypeLower in listOf("assignment", "assignments", "homework", "lab assignment", "lab report") ||
            typeLower in listOf("assignment", "assignments", "homework", "lab assignment", "lab report") ||
            docTypeLower.contains("assignment") || typeLower.contains("assignment")

    val resolvedResourceType = when {
        isVideo -> com.pravor.notessharing.domain.model.ResourceType.VIDEO
        isPyq -> com.pravor.notessharing.domain.model.ResourceType.PYQ
        isCheatSheet -> com.pravor.notessharing.domain.model.ResourceType.CHEAT_SHEET
        isAssignment -> com.pravor.notessharing.domain.model.ResourceType.ASSIGNMENT
        else -> com.pravor.notessharing.domain.model.ResourceType.NOTE
    }

    return TrendingNote(
        id = id,
        title = title,
        subject = subject,
        downloadsCount = downloadsCount,
        rating = rating,
        upvotes = upvotes,
        isBookmarked = false,
        thumbnailUrl = thumbnailUrl,
        thumbnailGenerated = thumbnailGenerated,
        thumbnailType = thumbnailType,
        description = description,
        uploaderName = uploaderName,
        uploaderPhotoUrl = uploaderPhotoUrl,
        contributorLevel = contributorLevel,
        documentType = documentType ?: type ?: "notes",
        type = type ?: "notes",
        bookmarks = bookmarksCount,
        examYear = examYear,
        examType = examType,
        semester = semester ?: "",
        isUpvoted = false,
        branch = branch,
        trendingScore = trendingScore,
        displaySubject = displaySubject,
        sectionDisplay = sectionDisplay,
        uploadedAt = uploadedAtMs,
        resourceType = resolvedResourceType,
        youtubeVideoId = youtubeVideoId ?: "",
        youtubeThumbnailUrl = youtubeThumbnailUrl,
        youtubeUrl = youtubeUrl ?: ""
    )
}

fun ExploreItemEntity.toDiscoverNote(): DiscoverFeedItem {
    return DiscoverFeedItem.Note(
        id = id,
        title = title,
        subject = subject,
        downloadsCount = downloadsCount
    )
}

fun FeedItem.toExploreEntity(collegeId: String, sectionCategory: String): ExploreItemEntity {
    val tagsArray = JSONArray()
    tags.forEach { tagsArray.put(it) }
    val urlsArray = JSONArray()
    thumbnailUrls.forEach { urlsArray.put(it) }

    val uploadedMs = try {
        uploadDate.toLongOrNull() ?: 0L
    } catch (e: Exception) {
        0L
    }

    return ExploreItemEntity(
        compositeId = "$sectionCategory-$id",
        id = id,
        collegeId = collegeId,
        sectionCategory = sectionCategory,
        title = title,
        description = description,
        uploaderName = uploaderName,
        uploaderInitials = uploaderInitials,
        uploadDate = uploadDate,
        subject = subject ?: "",
        subjectId = "",
        fileTypeName = fileType.name,
        documentType = documentType,
        type = type,
        thumbnailUrl = thumbnailUrl,
        youtubeVideoId = youtubeVideoId,
        youtubeUrl = youtubeUrl,
        youtubeThumbnailUrl = youtubeThumbnailUrl,
        thumbnailGenerated = thumbnailGenerated,
        thumbnailType = thumbnailType,
        tagsJson = tagsArray.toString(),
        thumbnailUrlsJson = urlsArray.toString(),
        upvotes = upvotes,
        commentsCount = comments,
        downloadsCount = downloadsCount,
        bookmarksCount = bookmarksCount,
        section = section,
        sectionDisplay = sectionDisplay,
        examYear = examYear,
        examType = examType,
        semester = null,
        trendingScore = 0.0,
        displaySubject = null,
        branch = "",
        uploaderPhotoUrl = "",
        contributorLevel = "Bronze Contributor",
        rating = 4.5,
        uploadedAtMs = uploadedMs
    )
}

fun TrendingNote.toExploreEntity(collegeId: String, sectionCategory: String): ExploreItemEntity {
    val urlsArray = JSONArray()
    if (!thumbnailUrl.isNullOrBlank()) urlsArray.put(thumbnailUrl)
    if (!youtubeThumbnailUrl.isNullOrBlank()) urlsArray.put(youtubeThumbnailUrl)

    return ExploreItemEntity(
        compositeId = "$sectionCategory-$id",
        id = id,
        collegeId = collegeId,
        sectionCategory = sectionCategory,
        title = title,
        description = description,
        uploaderName = uploaderName,
        uploaderInitials = "",
        uploadDate = "",
        subject = subject,
        subjectId = "",
        fileTypeName = FileType.Pdf.name,
        documentType = documentType,
        type = type,
        thumbnailUrl = thumbnailUrl,
        youtubeVideoId = youtubeVideoId,
        youtubeUrl = youtubeUrl,
        youtubeThumbnailUrl = youtubeThumbnailUrl,
        thumbnailGenerated = thumbnailGenerated,
        thumbnailType = thumbnailType,
        tagsJson = "[]",
        thumbnailUrlsJson = urlsArray.toString(),
        upvotes = upvotes,
        commentsCount = 0,
        downloadsCount = downloadsCount,
        bookmarksCount = bookmarks,
        section = null,
        sectionDisplay = sectionDisplay,
        examYear = examYear,
        examType = examType,
        semester = semester,
        trendingScore = trendingScore,
        displaySubject = displaySubject,
        branch = branch,
        uploaderPhotoUrl = uploaderPhotoUrl,
        contributorLevel = contributorLevel,
        rating = rating,
        uploadedAtMs = uploadedAt
    )
}

fun DiscoverFeedItem.toExploreEntity(collegeId: String, sectionCategory: String): ExploreItemEntity {
    val itemTitle = when (this) {
        is DiscoverFeedItem.Note -> title
        is DiscoverFeedItem.Video -> title
        is DiscoverFeedItem.Collection -> title
        is DiscoverFeedItem.ContributorPost -> name
    }
    val itemSubject = when (this) {
        is DiscoverFeedItem.Note -> subject
        else -> ""
    }
    val itemDownloads = when (this) {
        is DiscoverFeedItem.Note -> downloadsCount
        else -> 0
    }

    return ExploreItemEntity(
        compositeId = "$sectionCategory-$id",
        id = id,
        collegeId = collegeId,
        sectionCategory = sectionCategory,
        title = itemTitle,
        description = "",
        uploaderName = "",
        uploaderInitials = "",
        uploadDate = "",
        subject = itemSubject,
        subjectId = "",
        fileTypeName = FileType.Pdf.name,
        documentType = "notes",
        type = "notes",
        thumbnailUrl = null,
        youtubeVideoId = null,
        youtubeUrl = null,
        youtubeThumbnailUrl = null,
        thumbnailGenerated = null,
        thumbnailType = null,
        tagsJson = "[]",
        thumbnailUrlsJson = "[]",
        upvotes = 0,
        commentsCount = 0,
        downloadsCount = itemDownloads,
        bookmarksCount = 0,
        section = null,
        sectionDisplay = null,
        examYear = null,
        examType = null,
        semester = null,
        uploadedAtMs = 0L
    )
}
