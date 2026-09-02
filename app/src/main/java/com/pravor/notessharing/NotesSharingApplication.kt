package com.pravor.notessharing

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlinx.coroutines.launch

class NotesSharingApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024L) // 50 MB
                    .build()
            }
            .respectCacheHeaders(false) // Bypass short-lived cache-control headers from Firebase Storage
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

        // Initialize Classroom reminders and periodic sync
        try {
            com.pravor.notessharing.data.classroom.reminder.ClassroomReminderReceiver.createNotificationChannel(this)
            com.pravor.notessharing.data.classroom.reminder.ClassroomSyncWorker.enqueuePeriodicSync(this)
            com.pravor.notessharing.data.classroom.reminder.ClassroomReminderScheduler.reconcileReminders(this)
        } catch (e: Exception) {
            android.util.Log.e("Application", "Failed to initialize Classroom reminder services", e)
        }

        // Initialize Centralized Subject Catalog in background (snapshot listener and Room cache)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                com.pravor.notessharing.data.repository.SubjectCatalogRepository.getInstance(applicationContext)
            } catch (e: Exception) {
                android.util.Log.w("Application", "Background subject catalog initialization skipped/failed", e)
            }
        }
    }

    companion object {
        lateinit var appContext: android.content.Context
            private set
    }
}
