package com.pravor.notessharing.data.repository

import android.content.Context
import com.pravor.notessharing.NotesSharingApplication
import com.pravor.notessharing.data.local.AppDatabase
import com.pravor.notessharing.data.local.dao.ExploreDao
import com.pravor.notessharing.data.local.entity.ExploreItemEntity
import com.pravor.notessharing.data.mapper.toDiscoverNote
import com.pravor.notessharing.data.mapper.toExploreEntity
import com.pravor.notessharing.data.mapper.toFeedItem
import com.pravor.notessharing.data.mapper.toTrendingNote
import com.pravor.notessharing.state.ExploreContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ExploreRoomRepository(
    context: Context = NotesSharingApplication.appContext,
    private val exploreDao: ExploreDao = AppDatabase.getDatabase(context).exploreDao()
) {
    fun observeExploreContent(collegeId: String): Flow<ExploreContent?> {
        val canonicalCollegeId = com.pravor.notessharing.util.LegacyAcademicCompatibilityResolver.resolveCollegeId(collegeId)
        return exploreDao.observeExploreItems(canonicalCollegeId).map { entities ->
            if (entities.isEmpty()) return@map null
            assembleExploreContent(entities)
        }
    }

    suspend fun getCachedContent(collegeId: String): ExploreContent? = withContext(Dispatchers.IO) {
        val canonicalCollegeId = com.pravor.notessharing.util.LegacyAcademicCompatibilityResolver.resolveCollegeId(collegeId)
        val entities = exploreDao.getCachedExploreItems(canonicalCollegeId)
        if (entities.isEmpty()) return@withContext null
        assembleExploreContent(entities)
    }

    suspend fun saveExploreContent(collegeId: String, content: ExploreContent) = withContext(Dispatchers.IO) {
        val canonicalCollegeId = com.pravor.notessharing.util.LegacyAcademicCompatibilityResolver.resolveCollegeId(collegeId)
        val entities = mutableListOf<ExploreItemEntity>()

        content.popularUploads.forEach { entities.add(it.toExploreEntity(canonicalCollegeId, "POPULAR")) }
        content.notes.forEach { entities.add(it.toExploreEntity(canonicalCollegeId, "NOTES")) }
        content.examPrep.forEach { entities.add(it.toExploreEntity(canonicalCollegeId, "EXAM_PREP")) }
        content.assignments.forEach { entities.add(it.toExploreEntity(canonicalCollegeId, "ASSIGNMENTS")) }
        content.videos.forEach { entities.add(it.toExploreEntity(canonicalCollegeId, "VIDEOS")) }
        content.discoverItems.forEach { entities.add(it.toExploreEntity(canonicalCollegeId, "DISCOVER")) }

        exploreDao.upsertExploreItems(entities)
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
