/*
 * Copyright (c) 2026 MyCompany LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pravor.notessharing.data.mapper

import com.google.firebase.firestore.DocumentSnapshot
import com.pravor.notessharing.domain.model.DiscoverFeedItem
import com.pravor.notessharing.domain.model.FeedItem
import com.pravor.notessharing.domain.model.FileType
import com.pravor.notessharing.domain.model.ResourceType
import com.pravor.notessharing.domain.model.TrendingNote
import com.pravor.notessharing.domain.model.extractYoutubePlaylistId
import com.pravor.notessharing.domain.model.removeFileExtension
import com.pravor.notessharing.data.repository.UpvoteRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExploreMapper {

    fun documentToDiscoverNote(data: Map<String, Any?>): DiscoverFeedItem.Note? {
        val processingStatus = data["processingStatus"] as? String
        if (processingStatus == "PROCESSING" || processingStatus == "FAILED") return null

        val id = data["documentId"] as? String ?: ""
        val title = (data["title"] as? String ?: "").removeFileExtension()
        val uploaderId = data["uploaderId"] as? String
        if (id.isBlank() || title.isBlank() || uploaderId == "dummy-uid") return null
        val subject = data["subject"] as? String ?: ""
        // Discover notes are always Notes (docType="Notes"), so downloadsCount takes precedence
        val downloadsCount = ((data["downloadsCount"] ?: data["downloads"]) as? Number)?.toInt() ?: 0
        return DiscoverFeedItem.Note(
            id = id,
            title = title,
            subject = subject,
            downloadsCount = downloadsCount
        )
    }

    fun isVideoResource(data: Map<String, Any?>): Boolean {
        val resourceType = determineResourceType(data, "")
        return resourceType == ResourceType.VIDEO || resourceType == ResourceType.PLAYLIST
    }

    fun determineResourceType(data: Map<String, Any?>, collectionName: String): ResourceType {
        val docType = (data["documentType"] as? String ?: data["type"] as? String ?: "").trim()
        val contentType = (data["contentType"] as? String ?: "").trim()
        val hasYoutubeLink = (data["hasYoutubeLink"] as? Boolean) == true || (data["hasYoutubeLink"] as? String)?.lowercase() == "true"
        val sourceType = (data["sourceType"] as? String ?: "").trim()
        val youtubeUrl = (data["youtubeUrl"] as? String ?: "").trim()
        val youtubeVideoId = (data["youtubeVideoId"] as? String ?: "").trim()
        val resourceTypeField = (data["resourceType"] as? String ?: "").trim()
        val source = (data["source"] as? String ?: "").trim()

        val isVideo = collectionName.equals("videos", ignoreCase = true) ||
                docType.equals("VIDEO", ignoreCase = true) ||
                docType.equals("YouTube Resource", ignoreCase = true) ||
                docType.equals("Videos", ignoreCase = true) ||
                contentType.equals("VIDEO", ignoreCase = true) ||
                hasYoutubeLink ||
                sourceType.equals("youtube", ignoreCase = true) ||
                sourceType.equals("video", ignoreCase = true) ||
                youtubeUrl.isNotBlank() ||
                youtubeVideoId.isNotBlank() ||
                resourceTypeField.equals("VIDEO", ignoreCase = true) ||
                source.equals("YOUTUBE", ignoreCase = true)

        if (isVideo) {
            val isPlaylist = (youtubeUrl.isNotBlank() && extractYoutubePlaylistId(youtubeUrl) != null) ||
                    (data["youtubeResourceType"] as? String ?: "").trim().equals("playlist", ignoreCase = true)
            return if (isPlaylist) ResourceType.PLAYLIST else ResourceType.VIDEO
        }

        return when (collectionName.lowercase(Locale.US)) {
            "pyqs" -> ResourceType.PYQ
            "cheatsheets" -> ResourceType.CHEAT_SHEET
            "assignments" -> ResourceType.ASSIGNMENT
            else -> {
                when (docType.lowercase(Locale.US)) {
                    "pyq", "pyqs" -> ResourceType.PYQ
                    "cheat sheet", "cheatsheet", "cheatsheets" -> ResourceType.CHEAT_SHEET
                    "assignment", "assignments" -> ResourceType.ASSIGNMENT
                    else -> ResourceType.NOTE
                }
            }
        }
    }

    fun documentToTrendingNote(
        doc: DocumentSnapshot,
        bookmarkedIds: Set<String>,
        resolvedLevels: Map<String, String>? = null
    ): TrendingNote? {
        val data = doc.data ?: return null
        val processingStatus = data["processingStatus"] as? String
        if (processingStatus == "PROCESSING" || processingStatus == "FAILED") return null

        val id = data["documentId"] as? String ?: ""
        val title = (data["title"] as? String ?: data["videoTitle"] as? String ?: "").removeFileExtension()
        val uploaderId = data["uploaderId"] as? String
        if (id.isBlank() || title.isBlank() || uploaderId == "dummy-uid") return null

        val collectionName = doc.reference.parent.id
        val resourceType = determineResourceType(data, collectionName)

        val subject = data["subject"] as? String ?: ""
        val displaySubjectVal = data["displaySubject"] as? String
        val downloadsCount = if (resourceType == ResourceType.NOTE) {
            ((data["downloadsCount"] ?: data["downloads"]) as? Number)?.toInt() ?: 0
        } else {
            (data["downloadsCount"] as? Number)?.toInt() ?: 0
        }
        val upvotes = ((data["upvotes"] ?: data["likesCount"]) as? Number)?.toInt() ?: 0
        val thumbnailUrl = (data["thumbnailUrl"] as? String)?.ifBlank { null }
            ?: (data["youtubeThumbnailUrl"] as? String)?.ifBlank { null }
        val thumbnailGenerated = data["thumbnailGenerated"] as? Boolean
        val thumbnailType = data["thumbnailType"] as? String
        val documentTypeField = data["documentType"] as? String
            ?: data["type"] as? String
            ?: when (collectionName.lowercase(Locale.US)) {
                "notes" -> "Notes"
                "pyqs" -> "PYQ"
                "assignments" -> "Assignment"
                "cheatsheets" -> "CheatSheet"
                "videos" -> "Video"
                else -> "Notes"
            }
        val typeField = data["type"] as? String ?: documentTypeField
        val examYearVal = (data["examYear"] ?: data["year"])?.toString()
        val examTypeVal = data["examType"]?.toString()
        val branchVal = data["branch"] as? String ?: ""
        val sectionDisplayVal = data["sectionDisplay"] as? String
        val semesterVal = data["semester"] as? String ?: ""

        val resolvedIsUpvoted = UpvoteRepository.upvotesFlow.value[id] ?: false
        val resolvedUpvotes = UpvoteRepository.upvoteCountsFlow.value[id] ?: upvotes
        val trendingScore = (data["trendingScore"] as? Number)?.toDouble() ?: 0.0
        val uploadedAt = doc.getLong("uploadedAt") ?: 0L

        val youtubeVideoId = (data["youtubeVideoId"] as? String ?: data["youtubeId"] as? String ?: "").trim()
        val youtubeThumbnailUrl = (data["youtubeThumbnailUrl"] as? String ?: "").trim()
        val youtubeUrl = (data["youtubeUrl"] as? String ?: "").trim()
        val channelName = (data["channelName"] as? String ?: data["uploaderName"] as? String ?: "Anonymous").trim()

        val uploaderName = data["uploaderName"] as? String ?: "Anonymous"
        val uploaderPhotoUrl = data["uploaderPhotoUrl"] as? String ?: ""

        val contributorLevel = if (!uploaderId.isNullOrEmpty()) {
            if (uploaderId == "dummy-uid") {
                "Gold Contributor"
            } else {
                resolvedLevels?.get(uploaderId) ?: "Bronze Contributor"
            }
        } else {
            "Bronze Contributor"
        }

        return TrendingNote(
            id = id,
            title = title,
            subject = subject,
            downloadsCount = downloadsCount,
            rating = 4.5,
            upvotes = resolvedUpvotes,
            isBookmarked = bookmarkedIds.contains(id),
            thumbnailUrl = thumbnailUrl,
            thumbnailGenerated = thumbnailGenerated,
            thumbnailType = thumbnailType,
            description = data["description"] as? String ?: "",
            uploaderName = uploaderName,
            uploaderPhotoUrl = uploaderPhotoUrl,
            contributorLevel = contributorLevel,
            documentType = documentTypeField ?: "",
            type = typeField,
            bookmarks = (data["bookmarks"] as? Long ?: 0L).toInt(),
            examYear = examYearVal,
            examType = examTypeVal,
            semester = semesterVal,
            isUpvoted = resolvedIsUpvoted,
            branch = branchVal,
            trendingScore = trendingScore,
            displaySubject = displaySubjectVal,
            sectionDisplay = sectionDisplayVal,
            uploadedAt = uploadedAt,
            resourceType = resourceType,
            channelName = channelName,
            duration = data["duration"] as? String ?: "",
            youtubeVideoId = youtubeVideoId,
            youtubeThumbnailUrl = youtubeThumbnailUrl.ifBlank { null },
            youtubeUrl = youtubeUrl
        )
    }

    fun documentToFeedItem(doc: Map<String, Any?>): FeedItem? {
        val processingStatus = doc["processingStatus"] as? String
        if (processingStatus == "PROCESSING" || processingStatus == "FAILED") return null

        val id = doc["documentId"] as? String ?: ""
        val title = (doc["title"] as? String ?: "").removeFileExtension()
        val uploaderId = doc["uploaderId"] as? String
        if (id.isBlank() || title.isBlank() || uploaderId == "dummy-uid") return null

        val uploaderName = doc["uploaderName"] as? String ?: "Anonymous"
        val initials = uploaderName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
            .ifBlank { "AN" }

        val uploadedAt = doc["uploadedAt"] as? Long ?: (doc["uploadTimestamp"] as? Long ?: System.currentTimeMillis())
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        val uploadDate = sdf.format(Date(uploadedAt))

        val docType = doc["documentType"] as? String ?: (doc["type"] as? String ?: "Notes")
        val fileType = when (docType) {
            "PYQ" -> FileType.Pyq
            "CheatSheet", "Cheat Sheet" -> FileType.CheatSheet
            "Assignment" -> FileType.Notes
            "Notes" -> FileType.Notes
            "YouTube Resource", "Videos" -> FileType.Video
            else -> FileType.Pdf
        }

        val subject = doc["subject"] as? String ?: ""
        val displayTitle = doc["title"] as? String ?: ""

        val description = doc["description"] as? String ?: ""
        val tags = (doc["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

        val upvotes = ((doc["upvotes"] ?: doc["likesCount"]) as? Number)?.toInt() ?: 0
        val isNote = docType.trim().lowercase(Locale.US) in listOf("notes", "note")
        val downloadsCount = if (isNote) {
            ((doc["downloadsCount"] ?: doc["downloads"]) as? Number)?.toInt() ?: 0
        } else {
            (doc["downloadsCount"] as? Number)?.toInt() ?: 0
        }
        val bookmarks = (doc["bookmarks"] as? Long ?: 0L).toInt()

        val youtubeUrl = doc["youtubeUrl"] as? String
        val youtubeVideoId = doc["youtubeVideoId"] as? String

        val thumbnailUrl = (doc["thumbnailUrl"] as? String)?.ifBlank { null }
            ?: (doc["youtubeThumbnailUrl"] as? String)?.ifBlank { null }
        val thumbnailGenerated = doc["thumbnailGenerated"] as? Boolean
        val thumbnailType = doc["thumbnailType"] as? String

        val documentTypeField = doc["documentType"] as? String
        val typeField = doc["type"] as? String
        val subjectField = doc["subject"] as? String
        val examYearField = (doc["examYear"] ?: doc["year"])?.toString()
        val examTypeField = doc["examType"] as? String
        val sectionField = doc["section"] as? String
        val sectionDisplayField = doc["sectionDisplay"] as? String

        val resolvedIsUpvoted = UpvoteRepository.upvotesFlow.value[id] ?: false
        val resolvedUpvotes = UpvoteRepository.upvoteCountsFlow.value[id] ?: upvotes

        return FeedItem(
            id = id,
            uploaderName = uploaderName,
            uploaderInitials = initials,
            uploadDate = uploadDate,
            title = displayTitle,
            description = description,
            tags = tags,
            fileType = fileType,
            upvotes = resolvedUpvotes,
            comments = 0,
            downloadsCount = downloadsCount,
            isUpvoted = resolvedIsUpvoted,
            isSaved = false,
            bookmarksCount = bookmarks,
            youtubeVideoId = youtubeVideoId,
            youtubeUrl = youtubeUrl,
            thumbnailUrl = thumbnailUrl,
            thumbnailGenerated = thumbnailGenerated,
            thumbnailType = thumbnailType,
            documentType = documentTypeField,
            type = typeField,
            subject = subjectField,
            examYear = examYearField,
            examType = examTypeField,
            section = sectionField,
            sectionDisplay = sectionDisplayField
        )
    }
}
