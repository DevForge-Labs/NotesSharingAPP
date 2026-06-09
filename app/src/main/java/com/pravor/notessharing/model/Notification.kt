package com.pravor.notessharing.model

import androidx.compose.runtime.Immutable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Immutable
data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val read: Boolean = false,
    val createdAt: Long = 0L,
    val type: String? = null,
    val targetId: String? = null
)

fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    if (diff < 0) return "Just now"
    
    val minutes = diff / (60 * 1000)
    if (minutes < 1) return "Just now"
    if (minutes < 60) return "$minutes min ago"
    
    val hours = diff / (60 * 60 * 1000)
    if (hours < 24) {
        val calNow = Calendar.getInstance()
        val calThen = Calendar.getInstance().apply { timeInMillis = timestamp }
        return if (calNow.get(Calendar.DAY_OF_YEAR) == calThen.get(Calendar.DAY_OF_YEAR)) {
            if (hours == 1L) "1 hour ago" else "$hours hours ago"
        } else {
            "Yesterday"
        }
    }
    
    val days = diff / (24 * 60 * 60 * 1000)
    if (days == 1L) return "Yesterday"
    if (days < 7) return "$days days ago"
    
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
