package com.pravor.notessharing.data.local.preferences


import android.content.Context

enum class NotificationCategory {
    DOWNLOADS,
    PERSONAL,
    CONTENT_ALERTS,
    ANNOUNCEMENTS,
    WEEKLY_DIGEST,
    EXAM_ALERTS,
    TRENDING_RESOURCES
}

object NotificationCategoryResolver {
    fun resolve(type: String?, title: String?, body: String?): NotificationCategory {
        val typeLower = type?.lowercase() ?: ""
        val titleLower = title?.lowercase() ?: ""
        val bodyLower = body?.lowercase() ?: ""

        // 1. Prioritize matching structured notification type
        if (typeLower.isNotEmpty()) {
            when {
                typeLower == "resource_deleted" -> return NotificationCategory.PERSONAL
                
                typeLower.contains("personal") || typeLower.contains("contributor") || 
                    typeLower.contains("moderation") || typeLower.contains("account") || 
                    typeLower.contains("uploader") || typeLower.contains("level") || 
                    typeLower.contains("achievement") -> return NotificationCategory.PERSONAL
                
                typeLower.contains("exam") || typeLower.contains("revision") || 
                    typeLower.contains("prep") -> return NotificationCategory.EXAM_ALERTS
                
                typeLower.contains("digest") || typeLower.contains("weekly") -> return NotificationCategory.WEEKLY_DIGEST
                
                typeLower.contains("announcement") || typeLower.contains("system") || 
                    typeLower.contains("maintenance") -> return NotificationCategory.ANNOUNCEMENTS
                
                typeLower.contains("trending") || typeLower.contains("popular") -> return NotificationCategory.TRENDING_RESOURCES
                
                typeLower.contains("content") || typeLower.contains("new") || 
                    typeLower.contains("upload") || typeLower.contains("document") -> return NotificationCategory.CONTENT_ALERTS
            }
        }

        // 2. Fall back to title/body keyword matching only if structured type is absent/unrecognized
        when {
            titleLower.contains("bookmark") || titleLower.contains("removed") || 
                titleLower.contains("deleted") || titleLower.contains("contributor") || 
                titleLower.contains("achievement") || titleLower.contains("account") || 
                titleLower.contains("moderation") || titleLower.contains("uploader") ||
                bodyLower.contains("bookmark") || bodyLower.contains("removed") || 
                bodyLower.contains("deleted") || bodyLower.contains("contributor") || 
                bodyLower.contains("achievement") || bodyLower.contains("account") || 
                bodyLower.contains("moderation") -> return NotificationCategory.PERSONAL

            titleLower.contains("exam") || titleLower.contains("revision") || 
                titleLower.contains("prep") || titleLower.contains("pyq") ||
                bodyLower.contains("exam") || bodyLower.contains("revision") || 
                bodyLower.contains("prep") -> return NotificationCategory.EXAM_ALERTS

            titleLower.contains("weekly") || titleLower.contains("digest") ||
                bodyLower.contains("weekly") || bodyLower.contains("digest") -> return NotificationCategory.WEEKLY_DIGEST

            titleLower.contains("trending") || titleLower.contains("popular") ||
                bodyLower.contains("trending") || bodyLower.contains("popular") -> return NotificationCategory.TRENDING_RESOURCES

            titleLower.contains("new") || titleLower.contains("uploaded") || 
                titleLower.contains("available") || titleLower.contains("note") || 
                titleLower.contains("assignment") || titleLower.contains("cheatsheet") || 
                titleLower.contains("video") || bodyLower.contains("uploaded") || 
                bodyLower.contains("available") -> return NotificationCategory.CONTENT_ALERTS

            titleLower.contains("announcement") || titleLower.contains("maintenance") || 
                titleLower.contains("update") || titleLower.contains("platform") ||
                bodyLower.contains("announcement") || bodyLower.contains("maintenance") || 
                bodyLower.contains("update") -> return NotificationCategory.ANNOUNCEMENTS
        }

        // Default fallback
        return NotificationCategory.ANNOUNCEMENTS
    }
}

class NotificationPreferences(private val context: Context) {
    private val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun isMasterEnabled(): Boolean = sharedPrefs.getBoolean("notifications_enabled", true)
    fun isDownloadsEnabled(): Boolean = sharedPrefs.getBoolean("downloads_enabled", true)
    fun isPersonalEnabled(): Boolean = sharedPrefs.getBoolean("pref_notifications_personal", true)
    fun isContentAlertsEnabled(): Boolean = sharedPrefs.getBoolean("pref_notifications_content_alerts", true)
    fun isAnnouncementsEnabled(): Boolean = sharedPrefs.getBoolean("pref_notifications_announcements", true)
    fun isWeeklyDigestEnabled(): Boolean = sharedPrefs.getBoolean("pref_notifications_weekly_digest", true)
    fun isExamAlertsEnabled(): Boolean = sharedPrefs.getBoolean("pref_notifications_exam_alerts", true)
    fun isTrendingResourcesEnabled(): Boolean = sharedPrefs.getBoolean("pref_notifications_trending_resources", true)

    fun shouldShowSystemNotification(category: NotificationCategory): Boolean {
        if (!isMasterEnabled()) return false
        return when (category) {
            NotificationCategory.DOWNLOADS -> isDownloadsEnabled()
            NotificationCategory.PERSONAL -> isPersonalEnabled()
            NotificationCategory.CONTENT_ALERTS -> isContentAlertsEnabled()
            NotificationCategory.ANNOUNCEMENTS -> isAnnouncementsEnabled()
            NotificationCategory.WEEKLY_DIGEST -> isWeeklyDigestEnabled()
            NotificationCategory.EXAM_ALERTS -> isExamAlertsEnabled()
            NotificationCategory.TRENDING_RESOURCES -> isTrendingResourcesEnabled()
        }
    }
}
