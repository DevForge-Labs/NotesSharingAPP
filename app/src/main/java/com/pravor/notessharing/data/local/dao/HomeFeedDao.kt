package com.pravor.notessharing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pravor.notessharing.data.local.entity.HomeFeedItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeFeedDao {
    @Query("SELECT * FROM home_feed_items WHERE collegeId = :collegeId ORDER BY upvotes DESC, uploadedAtMs DESC")
    fun observeHomeFeed(collegeId: String): Flow<List<HomeFeedItemEntity>>

    @Query("SELECT * FROM home_feed_items WHERE collegeId = :collegeId ORDER BY upvotes DESC, uploadedAtMs DESC")
    suspend fun getCachedHomeFeed(collegeId: String): List<HomeFeedItemEntity>

    @Query("SELECT * FROM home_feed_items ORDER BY upvotes DESC, uploadedAtMs DESC")
    fun observeAllHomeFeed(): Flow<List<HomeFeedItemEntity>>

    @Query("SELECT * FROM home_feed_items ORDER BY upvotes DESC, uploadedAtMs DESC")
    suspend fun getAllCachedHomeFeed(): List<HomeFeedItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFeedItems(items: List<HomeFeedItemEntity>): List<Long>

    @Query("DELETE FROM home_feed_items WHERE collegeId = :collegeId AND cachedAtMs < :cutoffMs")
    suspend fun deleteStaleItems(collegeId: String, cutoffMs: Long): Int

    @Query("DELETE FROM home_feed_items WHERE collegeId = :collegeId")
    suspend fun clearHomeFeed(collegeId: String): Int

    @Query("DELETE FROM home_feed_items")
    suspend fun clearAllHomeFeed(): Int
}

