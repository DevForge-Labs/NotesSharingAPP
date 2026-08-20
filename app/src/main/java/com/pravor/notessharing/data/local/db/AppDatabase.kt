package com.pravor.notessharing.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pravor.notessharing.data.local.dao.ExploreDao
import com.pravor.notessharing.data.local.dao.HomeFeedDao
import com.pravor.notessharing.data.local.dao.UserProfileDao
import com.pravor.notessharing.data.local.entity.ExploreItemEntity
import com.pravor.notessharing.data.local.entity.HomeFeedItemEntity
import com.pravor.notessharing.data.local.entity.UserProfileEntity

@Database(
    entities = [
        UserProfileEntity::class,
        HomeFeedItemEntity::class,
        ExploreItemEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun homeFeedDao(): HomeFeedDao
    abstract fun exploreDao(): ExploreDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "campus_pages_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
