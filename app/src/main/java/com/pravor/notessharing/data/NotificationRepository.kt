package com.pravor.notessharing.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.pravor.notessharing.model.Notification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()
    
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()
    
    private var listenerRegistration: ListenerRegistration? = null
    private var activeUserId: String? = null
    
    fun startObserving(userId: String) {
        if (activeUserId == userId && listenerRegistration != null) return
        
        stopObserving()
        activeUserId = userId
        
        listenerRegistration = firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    android.util.Log.e("NotificationRepo", "Error listening to notifications", error)
                    return@addSnapshotListener
                }
                if (querySnapshot != null) {
                    val list = querySnapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        
                        val rawCreatedAt = data["createdAt"]
                        val createdAtVal = when (rawCreatedAt) {
                            is com.google.firebase.Timestamp -> rawCreatedAt.toDate().time
                            is Long -> rawCreatedAt
                            is Number -> rawCreatedAt.toLong()
                            else -> 0L
                        }
                        
                        Notification(
                            id = doc.id,
                            title = data["title"] as? String ?: "",
                            message = data["message"] as? String ?: (data["body"] as? String ?: ""),
                            read = data["read"] as? Boolean ?: false,
                            createdAt = createdAtVal,
                            type = data["type"] as? String ?: (data["targetType"] as? String),
                            targetId = data["targetId"] as? String
                        )
                    }.distinctBy { it.id }
                    _notifications.value = list
                    _unreadCount.value = list.count { !it.read }
                }
            }
    }
    
    fun stopObserving() {
        listenerRegistration?.remove()
        listenerRegistration = null
        activeUserId = null
        _notifications.value = emptyList()
        _unreadCount.value = 0
    }
    
    suspend fun markAsRead(userId: String, notificationId: String) {
        try {
            firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .document(notificationId)
                .update("read", true)
                .await()
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepo", "Error marking notification as read", e)
        }
    }
    
    suspend fun markAllAsRead(userId: String) {
        try {
            val unreadDocs = firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .whereEqualTo("read", false)
                .get()
                .await()
                
            if (unreadDocs.isEmpty) return
            
            val batch = firestore.batch()
            for (doc in unreadDocs.documents) {
                batch.update(doc.reference, "read", true)
            }
            batch.commit().await()
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepo", "Error marking all notifications as read", e)
        }
    }

    suspend fun delete(userId: String, notificationId: String) {
        try {
            firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .document(notificationId)
                .delete()
                .await()
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepo", "Error deleting notification", e)
        }
    }

    suspend fun clearAll(userId: String) {
        try {
            val docs = firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .get()
                .await()
            if (docs.isEmpty) return
            val batch = firestore.batch()
            for (doc in docs.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepo", "Error clearing notifications", e)
        }
    }
}
