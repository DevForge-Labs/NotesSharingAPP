package com.pravor.notessharing.data.repository

import com.pravor.notessharing.core.util.*

import android.content.Context
import androidx.room.withTransaction
import com.pravor.notessharing.NotesSharingApplication
import com.pravor.notessharing.data.local.db.AppDatabase
import com.pravor.notessharing.data.local.dao.ExploreDao
import com.pravor.notessharing.data.local.entity.ExploreItemEntity
import com.pravor.notessharing.data.mapper.toDiscoverNote
import com.pravor.notessharing.data.mapper.toExploreEntity
import com.pravor.notessharing.data.mapper.toFeedItem
import com.pravor.notessharing.data.mapper.toTrendingNote
import com.pravor.notessharing.ui.common.ExploreContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ExploreRoomRepository(
    context: Context = NotesSharingApplication.appContext,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    private val exploreDao: ExploreDao = database.exploreDao()
) {
    fun observeExploreContent(scopeKey: String): Flow<ExploreContent?> {
        if (scopeKey.isBlank()) return kotlinx.coroutines.flow.flowOf(null)
        return exploreDao.observeExploreItems(scopeKey).map { entities ->
            if (entities.isEmpty()) return@map null
            assembleExploreContent(entities)
        }
    }

    suspend fun getCachedContent(scopeKey: String): ExploreContent? = withContext(Dispatchers.IO) {
        if (scopeKey.isBlank()) return@withContext null
        val entities = exploreDao.getCachedExploreItems(scopeKey)
        if (entities.isEmpty()) return@withContext null
        assembleExploreContent(entities)
    }

    suspend fun saveExploreContent(scopeKey: String, content: ExploreContent) = withContext(Dispatchers.IO) {
        if (scopeKey.isBlank()) return@withContext
        val entities = mutableListOf<ExploreItemEntity>()

        content.popularUploads.forEach { entities.add(it.toExploreEntity(scopeKey, "POPULAR")) }
        content.notes.forEach { entities.add(it.toExploreEntity(scopeKey, "NOTES")) }
        content.examPrep.forEach { entities.add(it.toExploreEntity(scopeKey, "EXAM_PREP")) }
        content.assignments.forEach { entities.add(it.toExploreEntity(scopeKey, "ASSIGNMENTS")) }
        content.videos.forEach { entities.add(it.toExploreEntity(scopeKey, "VIDEOS")) }
        content.discoverItems.forEach { entities.add(it.toExploreEntity(scopeKey, "DISCOVER")) }

        database.withTransaction {
            exploreDao.clearExploreItems(scopeKey)
            if (entities.isNotEmpty()) {
                exploreDao.upsertExploreItems(entities)
            }
        }
    }

    private fun assembleExploreContent(entities: List<ExploreItemEntity>): ExploreContent {
        val popular = entities.filter { it.sectionCategory == "POPULAR" }.map { it.toFeedItem() }
        val notes = entities.filter { it.sectionCategory == "NOTES" }.map { it.toTrendingNote() }
        val examPrep = entities.filter { it.sectionCategory == "EXAM_PREP" }.map { it.toTrendingNote() }
        val assignments = entities.filter { it.sectionCategory == "ASSIGNMENTS" }.map { it.toTrendingNote() }
        val videos = entities.filter { it.sectionCategory == "VIDEOS" }.map { it.toTrendingNote() }
        val discover = entities.filter { it.sectionCategory == "DISCOVER" }.map { it.toDiscoverNote() }

        return ExploreContent(
            topics = emptyList(),
            popularUploads = popular,
            notes = notes,
            examPrep = examPrep,
            assignments = assignments,
            videos = videos,
            studyCollections = emptyList(),
            subjectHubs = emptyList(),
            topContributors = emptyList(),
            revisionCards = emptyList(),
            discoverItems = discover
        )
    }
}
