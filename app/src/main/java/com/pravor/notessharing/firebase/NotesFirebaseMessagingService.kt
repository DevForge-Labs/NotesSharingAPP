package com.pravor.notessharing.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pravor.notessharing.MainActivity

class NotesFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_SERVICE", "Refreshed FCM token received: $token")
        
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            Log.d("FCM_SERVICE", "Attempting to update refreshed token in Firestore for authenticated user: $uid")
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("FCM_SERVICE", "Successfully updated token in Firestore for user: $uid")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM_SERVICE", "Failed to update token in Firestore for user: $uid", e)
                }
        } else {
            Log.d("FCM_SERVICE", "No authenticated user currently logged in. Token will be saved on next login.")
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d("FCM_SERVICE", "onMessageReceived: Message from = ${remoteMessage.from}")
        Log.d("FCM_SERVICE", "onMessageReceived: Message ID = ${remoteMessage.messageId}")
        Log.d("FCM_SERVICE", "onMessageReceived: Message data payload = ${remoteMessage.data}")

        // 1. Read notification title and body
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "NotesSharing Update"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""

        // 2. Read optional data payload fields
        val docId = remoteMessage.data["document_id"]
        val videoId = remoteMessage.data["video_id"]
        val deepLink = remoteMessage.data["deepLink"]
        val notificationId = remoteMessage.data["notificationId"] ?: remoteMessage.data["notification_id"]

        Log.d("FCM_SERVICE", "Parsed fields - Title: '$title', Body: '$body', docId: '$docId', videoId: '$videoId', deepLink: '$deepLink', notificationId: '$notificationId'")

        // 3. Display the notification
        showNotification(title, body, docId, videoId, deepLink, notificationId)
    }

    private fun showNotification(
        title: String,
        body: String,
        docId: String?,
        videoId: String?,
        deepLink: String?,
        notificationId: String?
    ) {
        val channelId = "general_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (Android 8.0 / API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "General Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channels for receiving general notes updates and notifications"
            }
            notificationManager.createNotificationChannel(channel)
            Log.d("FCM_SERVICE", "Notification channel '$channelId' verified/created.")
        }

        // Prepare Intent targeting MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!docId.isNullOrBlank()) {
                putExtra("document_id", docId)
            }
            if (!videoId.isNullOrBlank()) {
                putExtra("video_id", videoId)
            }
            if (!deepLink.isNullOrBlank()) {
                putExtra("deepLink", deepLink)
            }
            if (!notificationId.isNullOrBlank()) {
                putExtra("notification_id", notificationId)
            }
        }

        // Add FLAG_IMMUTABLE for compatibility with Android 12+
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(), // Unique request code to prevent overwriting
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(applicationInfo.icon) // Fallback to application icon
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val systemNotificationId = if (!notificationId.isNullOrBlank()) {
            notificationId.hashCode()
        } else {
            System.currentTimeMillis().toInt()
        }
        Log.d("FCM_SERVICE", "Displaying system notification: ID = $systemNotificationId (from raw ID: '$notificationId')")
        notificationManager.notify(systemNotificationId, builder.build())
    }
}
