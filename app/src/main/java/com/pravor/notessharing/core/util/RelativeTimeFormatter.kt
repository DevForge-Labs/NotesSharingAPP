package com.pravor.notessharing.core.util

fun formatRelativeTime(timestampStr: String, isVideo: Boolean): String {
    val timestamp = timestampStr.toLongOrNull() ?: return "Recently viewed"
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val action = if (isVideo) "watched" else "opened"
    
    if (diff < 0) return "Opened just now"
    
    val minutes = diff / (1000 * 60)
    if (minutes < 1) return "Opened just now"
    if (minutes < 60) return "Opened $minutes min ago"
    
    val hours = minutes / 60
    if (hours < 24) return "Last $action ${hours}h ago"
    
    val days = hours / 24
    if (days == 1L) return "Last $action yesterday"
    if (days < 7) return "Last $action $days days ago"
    
    val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
    return "Last $action on ${sdf.format(java.util.Date(timestamp))}"
}
