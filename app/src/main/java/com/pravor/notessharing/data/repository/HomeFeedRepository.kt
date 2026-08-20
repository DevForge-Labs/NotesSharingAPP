package com.pravor.notessharing.data.repository

import com.pravor.notessharing.core.util.*

import android.content.Context
import com.pravor.notessharing.NotesSharingApplication
import com.pravor.notessharing.data.local.db.AppDatabase
import com.pravor.notessharing.data.local.dao.HomeFeedDao
import com.pravor.notessharing.data.mapper.toDomainModel
import com.pravor.notessharing.data.mapper.toEntity
import com.pravor.notessharing.domain.model.FeedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HomeFeedRepository(
    context: Context = NotesSharingApplication.appContext,
    private val homeFeedDao: HomeFeedDao = AppDatabase.getDatabase(context).homeFeedDao()
) {
    /**
     * Observes Home Feed items directly from Room Database.
     */
    fun observeHomeFeed(collegeId: String): Flow<List<FeedItem>> {
        return if (collegeId.isBlank()) {
            homeFeedDao.observeAllHomeFeed().map { list -> list.map { it.toDomainModel() } }
        } else {
            val canonicalCollegeId = com.pravor.notessharing.core.util.LegacyAcademicCompatibilityResolver.resolveCollegeId(collegeId)
            homeFeedDao.observeHomeFeed(canonicalCollegeId).map { list -> list.map { it.toDomainModel() } }
        }
    }

    suspend fun getCachedHomeFeed(collegeId: String): List<FeedItem> = withContext(Dispatchers.IO) {
        val canonicalCollegeId = com.pravor.notessharing.core.util.LegacyAcademicCompatibilityResolver.resolveCollegeId(collegeId)
        val entities = if (canonicalCollegeId.isBlank()) {
            homeFeedDao.getCachedHomeFeed("")
        } else {
            homeFeedDao.getCachedHomeFeed(canonicalCollegeId)
        }
        entities.map { it.toDomainModel() }
    }

    suspend fun saveHomeFeed(collegeId: String, items: List<FeedItem>) = withContext(Dispatchers.IO) {
        val canonicalCollegeId = com.pravor.notessharing.core.util.LegacyAcademicCompatibilityResolver.resolveCollegeId(collegeId)
        val entities = items.map { it.toEntity(collegeId = canonicalCollegeId) }
        homeFeedDao.upsertFeedItems(entities)
    }

    suspend fun purgeStaleFeedItems(collegeId: String, maxAgeMs: Long = 7 * 24 * 60 * 60 * 1000L) = withContext(Dispatchers.IO) {
        val canonicalCollegeId = com.pravor.notessharing.core.util.LegacyAcademicCompatibilityResolver.resolveCollegeId(collegeId)
        val cutoffMs = System.currentTimeMillis() - maxAgeMs
        homeFeedDao.deleteStaleItems(canonicalCollegeId, cutoffMs)
    }
}
