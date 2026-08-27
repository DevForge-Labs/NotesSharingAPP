package com.pravor.notessharing.data.classroom.reminder

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pravor.notessharing.data.classroom.ClassroomAuthManager
import com.pravor.notessharing.data.classroom.ClassroomAuthState
import com.pravor.notessharing.data.classroom.ClassroomRepository
import java.util.concurrent.TimeUnit

class ClassroomSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ClassroomSyncWorker"
        private const val UNIQUE_WORK_NAME = "classroom_background_periodic_sync"

        fun enqueuePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ClassroomSyncWorker>(
                repeatInterval = 3,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
                flexTimeInterval = 30,
                flexTimeIntervalUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            Log.d(TAG, "Periodic Classroom background sync enqueued (every 3 hours).")
        }

        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            Log.d(TAG, "Periodic Classroom background sync cancelled.")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Classroom background sync worker executing...")
        val authManager = ClassroomAuthManager.getInstance(applicationContext)
        val authState = authManager.authState.value
        if (authState !is ClassroomAuthState.Connected) {
            Log.d(TAG, "Classroom not connected. Skipping background sync.")
            return Result.success()
        }

        return try {
            val repository = ClassroomRepository.getInstance(applicationContext)
            repository.syncCourses(force = false)
            ClassroomReminderScheduler.scheduleEligibleReminders(applicationContext)
            Log.d(TAG, "Classroom background sync completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Classroom background sync encountered an error", e)
            Result.retry()
        }
    }
}
