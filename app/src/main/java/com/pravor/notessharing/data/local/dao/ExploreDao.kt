package com.pravor.notessharing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pravor.notessharing.data.local.entity.ExploreItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExploreDao {
    @Query("SELECT * FROM explore_items WHERE collegeId = :collegeId ORDER BY upvotes DESC, uploadedAtMs DESC")
    fun observeExploreItems(collegeId: String): Flow<List<ExploreItemEntity>>

    @Query("SELECT * FROM explore_items WHERE collegeId = :collegeId AND sectionCategory = :sectionCategory ORDER BY upvotes DESC, uploadedAtMs DESC")
    fun observeSectionItems(collegeId: String, sectionCategory: String): Flow<List<ExploreItemEntity>>

    @Query("SELECT * FROM explore_items WHERE collegeId = :collegeId ORDER BY upvotes DESC, uploadedAtMs DESC")
    suspend fun getCachedExploreItems(collegeId: String): List<ExploreItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExploreItems(items: List<ExploreItemEntity>): List<Long>

    @Query("DELETE FROM explore_items WHERE collegeId = :collegeId")
    suspend fun clearExploreItems(collegeId: String): Int

    @Query("DELETE FROM explore_items WHERE collegeId = :collegeId AND cachedAtMs < :cutoffMs")
    suspend fun deleteStaleItems(collegeId: String, cutoffMs: Long): Int
}
