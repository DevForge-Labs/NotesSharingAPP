package com.pravor.notessharing.data.repository

import com.pravor.notessharing.core.util.*

import android.content.Context
import com.pravor.notessharing.NotesSharingApplication
import com.pravor.notessharing.data.local.db.AppDatabase
import com.pravor.notessharing.data.local.dao.HomeFeedDao
import com.pravor.notessharing.data.mapper.toDomainModel
import com.pravor.notessharing.data.mapper.toEntity
import com.pravor.notessharing.domain.model.FeedItem
import com.pravor.notessharing.domain.model.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HomeFeedRepository(
    context: Context = NotesSharingApplication.appContext,
    private val homeFeedDao: HomeFeedDao = AppDatabase.getDatabase(context).homeFeedDao()
) {
    /**
     * Observes Home Feed items directly from Room Database, excluding any video/playlist resources.
     */
    fun observeHomeFeed(scopeKey: String): Flow<List<FeedItem>> {
        val rawFlow = if (scopeKey.isBlank()) {
            homeFeedDao.observeAllHomeFeed()
        } else {
            homeFeedDao.observeHomeFeed(scopeKey)
        }
        return rawFlow.map { list ->
            list.map { it.toDomainModel() }.filter { item -> isEligibleHomeFeedItem(item) }
        }
    }

    suspend fun getCachedHomeFeed(scopeKey: String): List<FeedItem> = withContext(Dispatchers.IO) {
        val entities = if (scopeKey.isBlank()) {
            homeFeedDao.getCachedHomeFeed("")
        } else {
            homeFeedDao.getCachedHomeFeed(scopeKey)
        }
        entities.map { it.toDomainModel() }.filter { item -> isEligibleHomeFeedItem(item) }
    }

    suspend fun saveHomeFeed(scopeKey: String, items: List<FeedItem>) = withContext(Dispatchers.IO) {
        if (scopeKey.isBlank()) return@withContext
        val eligibleItems = items.filter { item -> isEligibleHomeFeedItem(item) }
        val entities = eligibleItems.map { it.toEntity(collegeId = scopeKey) }
        homeFeedDao.upsertFeedItems(entities)
    }

    private fun isEligibleHomeFeedItem(item: FeedItem): Boolean {
        val isVideoType = item.fileType == FileType.Video
        val hasYoutubeId = !item.youtubeVideoId.isNullOrBlank()
        val hasYoutubeUrl = !item.youtubeUrl.isNullOrBlank()
        val hasYoutubeThumbnail = !item.youtubeThumbnailUrl.isNullOrBlank()
        val docTypeVideo = item.documentType.equals("VIDEO", ignoreCase = true) ||
                item.documentType.equals("YouTube Resource", ignoreCase = true) ||
                item.documentType.equals("Videos", ignoreCase = true) ||
                item.documentType.equals("Video", ignoreCase = true)
        val typeVideo = item.type.equals("VIDEO", ignoreCase = true) ||
                item.type.equals("YouTube Resource", ignoreCase = true) ||
                item.type.equals("Videos", ignoreCase = true) ||
                item.type.equals("Video", ignoreCase = true)

        return !isVideoType && !hasYoutubeId && !hasYoutubeUrl && !hasYoutubeThumbnail && !docTypeVideo && !typeVideo
    }

    suspend fun purgeStaleFeedItems(scopeKey: String, maxAgeMs: Long = 7 * 24 * 60 * 60 * 1000L) = withContext(Dispatchers.IO) {
        if (scopeKey.isBlank()) return@withContext
        val cutoffMs = System.currentTimeMillis() - maxAgeMs
        homeFeedDao.deleteStaleItems(scopeKey, cutoffMs)
    }
}
