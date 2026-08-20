package com.pravor.notessharing.data.service

import com.pravor.notessharing.data.local.preferences.*

import com.pravor.notessharing.data.service.*

import com.pravor.notessharing.domain.model.*

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pravor.notessharing.MainActivity
import com.pravor.notessharing.R
import com.pravor.notessharing.domain.model.DocumentDetail
import com.pravor.notessharing.viewmodel.DownloadState
import com.pravor.notessharing.firebase.FirestoreDocumentService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class DownloadForegroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private val activeJobMap = ConcurrentHashMap<String, Job>()
    private val activeDocuments = ConcurrentHashMap<String, DocumentDetail>()
    private val activeProgress = ConcurrentHashMap<String, Int>()
    private var firstNotificationId: Int? = null
    private val firestoreService = FirestoreDocumentService()

    private fun getNotificationId(docId: String): Int {
        val rawHash = docId.hashCode()
        val posId = if (rawHash == Int.MIN_VALUE) 1 else Math.abs(rawHash)
        return if (posId == 0) 1 else posId
    }

    companion object {
        const val CHANNEL_ID = "notes_sharing_downloads"
        const val ACTION_START_DOWNLOAD = "com.pravor.notessharing.action.START_DOWNLOAD"
        
        const val EXTRA_DOC_ID = "extra_doc_id"
        const val EXTRA_DOC_TITLE = "extra_doc_title"
        const val EXTRA_DOC_TYPE = "extra_doc_type"
        const val EXTRA_UPLOADER_ID = "extra_uploader_id"
        const val EXTRA_FILE_URLS = "extra_file_urls"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "DownloadForegroundService onCreate: Service initialized. Preparing notification channel.")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "DownloadForegroundService onStartCommand: action=${intent?.action}")
        if (intent != null && intent.action == ACTION_START_DOWNLOAD) {
            val docId = intent.getStringExtra(EXTRA_DOC_ID) ?: return START_NOT_STICKY
            val title = intent.getStringExtra(EXTRA_DOC_TITLE) ?: "Document"
            val fileUrls = intent.getStringArrayListExtra(EXTRA_FILE_URLS) ?: emptyList<String>()
            val docType = intent.getStringExtra(EXTRA_DOC_TYPE) ?: "Notes"
            val uploaderId = intent.getStringExtra(EXTRA_UPLOADER_ID) ?: ""

            Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "DownloadForegroundService onStartCommand: Starting download process for \"$title\" (ID: $docId)")

            // Reconstruct a lightweight DocumentDetail
            val document = DocumentDetail(
                id = docId,
                title = title,
                description = "",
                branch = "",
                semester = "",
                subject = "",
                documentType = docType,
                uploaderId = uploaderId,
                uploaderName = "",
                uploaderPhotoUrl = "",
                uploadedAt = 0L,
                downloadsCount = 0,
                upvotes = 0,
                bookmarks = 0,
                fileUrls = fileUrls,
                fileSize = 0L,
                fileExtension = "",
                fileType = "",
                attachmentCount = fileUrls.size
            )

            startDocumentDownload(document)
        }
        return START_NOT_STICKY
    }

    private fun isNotificationPreferenceEnabled(): Boolean {
        val prefs = applicationContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("notifications_enabled", true)
        Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "isNotificationPreferenceEnabled: Checked SharedPreferences: $enabled")
        return enabled
    }

    private fun isDownloadsNotificationEnabled(): Boolean {
        val prefs = applicationContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val masterEnabled = prefs.getBoolean("notifications_enabled", true)
        val downloadsEnabled = prefs.getBoolean("downloads_enabled", true)
        val enabled = masterEnabled && downloadsEnabled
        Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "isDownloadsNotificationEnabled: Checked SharedPreferences: $enabled (master=$masterEnabled, downloads=$downloadsEnabled)")
        return enabled
    }

    private fun hasNotificationPermission(): Boolean {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "hasNotificationPermission: POST_NOTIFICATIONS granted: $hasPermission")
        return hasPermission
    }

    private fun startDocumentDownload(document: DocumentDetail) {
        val docId = document.id
        
        // Prevent duplicate downloads
        if (activeJobMap.containsKey(docId)) {
            Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "startDocumentDownload: Job already active for ID=$docId. Ignoring request.")
            return
        }

        val notificationId = getNotificationId(docId)
        val prefEnabled = isNotificationPreferenceEnabled()
        val permissionGranted = hasNotificationPermission()
        
        Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "startDocumentDownload: docId=$docId, title=\"${document.title}\", prefEnabled=$prefEnabled, permissionGranted=$permissionGranted, assigned notificationId=$notificationId")

        activeDocuments[docId] = document
        activeProgress[docId] = 0

        // Show initial progress notification
        if (prefEnabled) {
            val notification = buildProgressNotification(document, 0)
            
            // startForeground on the first notification
            synchronized(this) {
                if (firstNotificationId == null) {
                    firstNotificationId = notificationId
                    Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "startForeground: Calling startForeground for ID=$docId (notificationId=$notificationId)")
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            startForeground(
                                notificationId,
                                notification,
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                            )
                        } else {
                            startForeground(notificationId, notification)
                        }
                    } catch (e: Exception) {
                        Log.e("DOWNLOAD_NOTIFICATION_DEBUG", "startForeground failed: ${e.message}", e)
                    }
                } else {
                    Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "startDocumentDownload: Service already in foreground. Posting new download notificationId=$notificationId")
                    if (isDownloadsNotificationEnabled()) {
                        try {
                            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.notify(notificationId, notification)
                        } catch (e: Exception) {
                            Log.e("DOWNLOAD_NOTIFICATION_DEBUG", "notify failed in startDocumentDownload: ${e.message}", e)
                        }
                    }
                }
            }
        } else {
            Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "startDocumentDownload: Notifications are disabled in app settings. Skipping startForeground notification.")
        }

        DownloadTracker.updateState(docId, DownloadState.Downloading(0f))

        val job = serviceScope.launch {
            try {
                Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "Download Coroutine started: docId=$docId")
                DownloadService.downloadDocument(applicationContext, document) { progress ->
                    val percentage = (progress * 100).toInt()
                    Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "Download progress tick: docId=$docId, progress=$progress ($percentage%)")
                    
                    activeProgress[docId] = percentage
                    
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    if (isDownloadsNotificationEnabled()) {
                        val updatedNotification = buildProgressNotification(document, percentage)
                        synchronized(this@DownloadForegroundService) {
                            if (firstNotificationId == null) {
                                firstNotificationId = notificationId
                                Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "Promoting service to foreground during progress update for ID=$docId")
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        startForeground(
                                            notificationId,
                                            updatedNotification,
                                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                                        )
                                    } else {
                                        startForeground(notificationId, updatedNotification)
                                    }
                                } catch (e: Exception) {
                                    Log.e("DOWNLOAD_NOTIFICATION_DEBUG", "startForeground promotion failed: ${e.message}", e)
                                }
                            } else {
                                try {
                                    notificationManager.notify(notificationId, updatedNotification)
                                    Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "Progress notification updated: ID=$notificationId, percentage=$percentage%")
                                } catch (e: Exception) {
                                    Log.e("DOWNLOAD_NOTIFICATION_DEBUG", "notify failed during progress update: ${e.message}", e)
                                }
                            }
                        }
                    } else {
                        Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "Notifications disabled mid-download. Demoting service from foreground.")
                        try {
                            stopForeground(true)
                        } catch (e: Exception) {
                            Log.e("DOWNLOAD_NOTIFICATION_DEBUG", "stopForeground failed: ${e.message}", e)
                        }
                        synchronized(this@DownloadForegroundService) {
                            if (firstNotificationId == notificationId) {
                                firstNotificationId = null
                            }
                        }
                    }
                    
                    // Update Progress Tracker
                    DownloadTracker.updateState(docId, DownloadState.Downloading(progress))
                }

                // Download Success
                Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "Download completed successfully: docId=$docId")
                DownloadTracker.updateState(docId, DownloadState.Downloaded)
                
                try {
                    val collection = getCollectionName(document.documentType)
                    Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "Incrementing download count in Firestore: docId=$docId, collection=$collection")
                    firestoreService.incrementDownloadCount(collection, docId)
                    Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "Firestore download count incremented successfully for docId=$docId")
                    
                    val newCount = document.downloadsCount + 1
                    DownloadCountTracker.updateDownloadCount(docId, newCount)
                } catch (e: Exception) {
                    Log.e("DOWNLOAD_NOTIFICATION_DEBUG", "Failed to increment download count in Firestore for docId=$docId: ${e.message}", e)
                }
                
                if (isDownloadsNotificationEnabled()) {
                    val successNotification = buildSuccessNotification(document)
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    try {
                        notificationManager.notify(notificationId, successNotification)
                        Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "Success notification posted: ID=$notificationId")
                    } catch (e: Exception) {
                        Log.e("DOWNLOAD_NOTIFICATION_DEBUG", "Failed to post success notification: ${e.message}", e)
                    }
                } else {
                    Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "Success notification skipped (toggled off). Removing foreground notifications.")
                    try {
                        stopForeground(true)
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(notificationId)
                    } catch (e: Exception) {
                        Log.e("DOWNLOAD_NOTIFICATION_DEBUG", "Failed to cancel notification on success: ${e.message}", e)
                    }
                }

            } catch (e: Exception) {
                Log.e("DOWNLOAD_NOTIFICATION_DEBUG", "Download failed with exception: docId=$docId, error=${e.message}", e)
                // Download Failure
                val db = DownloadDataStoreManager(applicationContext)
                val isDownloadedLocally = db.isDocumentDownloaded(docId)
                val finalState = if (isDownloadedLocally) DownloadState.Downloaded else DownloadState.NotDownloaded
                DownloadTracker.updateState(docId, finalState)

                if (isDownloadsNotificationEnabled()) {
                    val failureNotification = buildFailureNotification(document, e.localizedMessage ?: "Connection error")
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    try {
                        notificationManager.notify(notificationId, failureNotification)
                        Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "Failure notification posted: ID=$notificationId")
                    } catch (ex: Exception) {
                        Log.e("DOWNLOAD_NOTIFICATION_DEBUG", "Failed to post failure notification: ${ex.message}", ex)
                    }
                } else {
                    Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "Failure notification skipped (toggled off). Removing foreground notifications.")
                    try {
                        stopForeground(true)
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(notificationId)
                    } catch (ex: Exception) {
                        Log.e("DOWNLOAD_NOTIFICATION_DEBUG", "Failed to cancel notification on failure: ${ex.message}", ex)
                    }
                }
            } finally {
                activeJobMap.remove(docId)
                activeDocuments.remove(docId)
                activeProgress.remove(docId)
                Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "Download job finished. Cleaning up active job list: docId=$docId")
                checkAndStopService(notificationId)
            }
        }

        activeJobMap[docId] = job
    }

    private fun checkAndStopService(completedNotificationId: Int) {
        synchronized(this) {
            Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "checkAndStopService: completedNotificationId=$completedNotificationId, activeJobsRemaining=${activeJobMap.size}")
            if (activeJobMap.isEmpty()) {
                // If the first foreground job is finished, stopForeground
                if (firstNotificationId == completedNotificationId) {
                    Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "checkAndStopService: Last active foreground job finished. stopForeground(false) and stopSelf().")
                    try {
                        stopForeground(false)
                    } catch (e: Exception) {
                        Log.e("DOWNLOAD_NOTIFICATION_DEBUG", "stopForeground failed in checkAndStopService: ${e.message}", e)
                    }
                    firstNotificationId = null
                }
                stopSelf()
            } else if (firstNotificationId == completedNotificationId) {
                // Shift foreground focus to another active download
                val nextActiveDocId = activeJobMap.keys.firstOrNull()
                if (nextActiveDocId != null) {
                    val nextNotificationId = getNotificationId(nextActiveDocId)
                    firstNotificationId = nextNotificationId
                    Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "checkAndStopService: Shifting foreground focus to notificationId=$nextNotificationId")
                    
                    val nextDoc = activeDocuments[nextActiveDocId]
                    val nextProg = activeProgress[nextActiveDocId] ?: 0
                    if (nextDoc != null) {
                        val nextNotification = buildProgressNotification(nextDoc, nextProg)
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                startForeground(
                                    nextNotificationId,
                                    nextNotification,
                                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                                )
                            } else {
                                startForeground(nextNotificationId, nextNotification)
                            }
                        } catch (e: Exception) {
                            Log.e("DOWNLOAD_NOTIFICATION_DEBUG", "Failed to startForeground during focus shift: ${e.message}", e)
                        }
                    }
                } else {
                    Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "checkAndStopService: Active job map not empty but next ID resolves null. stopForeground(false) and stopSelf().")
                    try {
                        stopForeground(false)
                    } catch (e: Exception) {
                        Log.e("DOWNLOAD_NOTIFICATION_DEBUG", "stopForeground failed in checkAndStopService fallback: ${e.message}", e)
                    }
                    firstNotificationId = null
                    stopSelf()
                }
            }
        }
    }

    private fun buildProgressNotification(document: DocumentDetail, progressPercent: Int): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("document_id", document.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            getNotificationId(document.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(document.title)
            .setContentText("Downloading... $progressPercent%")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setProgress(100, progressPercent, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun buildSuccessNotification(document: DocumentDetail): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("document_id", document.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            getNotificationId(document.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download Complete")
            .setContentText(document.title)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(false)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    private fun buildFailureNotification(document: DocumentDetail, errorMsg: String): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("document_id", document.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            getNotificationId(document.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Retry Intent
        val retryIntent = Intent(this, DownloadForegroundService::class.java).apply {
            action = ACTION_START_DOWNLOAD
            putExtra(EXTRA_DOC_ID, document.id)
            putExtra(EXTRA_DOC_TITLE, document.title)
            putExtra(EXTRA_DOC_TYPE, document.documentType)
            putExtra(EXTRA_UPLOADER_ID, document.uploaderId)
            putStringArrayListExtra(EXTRA_FILE_URLS, java.util.ArrayList(document.fileUrls))
        }
        val retryPendingIntent = PendingIntent.getService(
            this,
            getNotificationId(document.id) + 100000,
            retryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download Failed")
            .setContentText(document.title)
            .setSubText(errorMsg)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(false)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Retry", retryPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Document Downloads",
                importance
            ).apply {
                description = "Shows progress of documents being downloaded offline"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "createNotificationChannel: Channel created. ID=$CHANNEL_ID, Importance=$importance")
            
            // Verify creation
            val checkChannel = manager.getNotificationChannel(CHANNEL_ID)
            if (checkChannel != null) {
                Log.d("DOWNLOAD_NOTIFICATION_DEBUG", "createNotificationChannel verification: Channel exists in manager. ID=${checkChannel.id}, Importance=${checkChannel.importance}")
            } else {
                Log.e("DOWNLOAD_NOTIFICATION_DEBUG", "createNotificationChannel verification: Failed to locate channel after creation!")
            }
        }
    }

    private fun getCollectionName(documentType: String): String {
        return when (documentType.trim().lowercase()) {
            "assignment", "assignments" -> "assignments"
            "notes" -> "notes"
            "pyq", "pyqs" -> "pyqs"
            "cheat sheet", "cheatsheet", "cheatsheets" -> "cheatsheets"
            "videos", "youtube resource" -> "videos"
            else -> "notes"
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }
}

object DownloadTracker {
    private val activeDownloads = ConcurrentHashMap<String, MutableStateFlow<DownloadState>>()
    private val _activeDownloadsCount = MutableStateFlow(0)
    val activeDownloadsCount: StateFlow<Int> = _activeDownloadsCount.asStateFlow()

    fun getDownloadStateFlow(documentId: String): StateFlow<DownloadState> {
        return activeDownloads.getOrPut(documentId) {
            MutableStateFlow(DownloadState.NotDownloaded)
        }.asStateFlow()
    }

    fun updateState(documentId: String, state: DownloadState) {
        activeDownloads.getOrPut(documentId) {
            MutableStateFlow(DownloadState.NotDownloaded)
        }.value = state
        updateCount()
    }

    fun isDownloading(documentId: String): Boolean {
        return activeDownloads[documentId]?.value is DownloadState.Downloading
    }

    private fun updateCount() {
        _activeDownloadsCount.value = activeDownloads.values.count { it.value is DownloadState.Downloading }
    }
}
