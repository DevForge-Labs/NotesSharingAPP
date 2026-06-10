package com.pravor.notessharing.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NotificationPreferencesUiState(
    val masterEnabled: Boolean = true,
    val downloads: Boolean = true,
    val personal: Boolean = true,
    val contentAlerts: Boolean = true,
    val announcements: Boolean = true,
    val weeklyDigest: Boolean = true,
    val examAlerts: Boolean = true,
    val trendingResources: Boolean = true,
    val showDisableConfirmationDialog: Boolean = false
) {
    val enabledCount: Int
        get() = listOf(
            downloads, personal, contentAlerts, announcements,
            weeklyDigest, examAlerts, trendingResources
        ).count { it }
}

class NotificationPreferencesViewModel(application: Application) : AndroidViewModel(application) {
    
    private val sharedPrefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(NotificationPreferencesUiState())
    val uiState: StateFlow<NotificationPreferencesUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        _uiState.update {
            it.copy(
                masterEnabled = sharedPrefs.getBoolean("notifications_enabled", true),
                downloads = sharedPrefs.getBoolean("downloads_enabled", true),
                personal = sharedPrefs.getBoolean("pref_notifications_personal", true),
                contentAlerts = sharedPrefs.getBoolean("pref_notifications_content_alerts", true),
                announcements = sharedPrefs.getBoolean("pref_notifications_announcements", true),
                weeklyDigest = sharedPrefs.getBoolean("pref_notifications_weekly_digest", true),
                examAlerts = sharedPrefs.getBoolean("pref_notifications_exam_alerts", true),
                trendingResources = sharedPrefs.getBoolean("pref_notifications_trending_resources", true)
            )
        }
    }

    fun toggleCategory(categoryKey: String, enabled: Boolean) {
        sharedPrefs.edit().putBoolean(categoryKey, enabled).apply()
        when (categoryKey) {
            "downloads_enabled" -> _uiState.update { it.copy(downloads = enabled) }
            "pref_notifications_personal" -> _uiState.update { it.copy(personal = enabled) }
            "pref_notifications_content_alerts" -> _uiState.update { it.copy(contentAlerts = enabled) }
            "pref_notifications_announcements" -> _uiState.update { it.copy(announcements = enabled) }
            "pref_notifications_weekly_digest" -> _uiState.update { it.copy(weeklyDigest = enabled) }
            "pref_notifications_exam_alerts" -> _uiState.update { it.copy(examAlerts = enabled) }
            "pref_notifications_trending_resources" -> _uiState.update { it.copy(trendingResources = enabled) }
        }
    }

    fun toggleMaster(enabled: Boolean) {
        if (!enabled) {
            // Turning OFF the master toggle requires confirmation
            _uiState.update { it.copy(showDisableConfirmationDialog = true) }
        } else {
            // Turning ON enables all categories immediately
            setAllCategories(true)
        }
    }

    fun confirmDisableMaster() {
        // Disables all categories
        setAllCategories(false)
        _uiState.update { it.copy(showDisableConfirmationDialog = false) }
    }

    fun dismissDisableDialog() {
        _uiState.update { it.copy(showDisableConfirmationDialog = false) }
    }

    private fun setAllCategories(enabled: Boolean) {
        sharedPrefs.edit().apply {
            putBoolean("notifications_enabled", enabled)
            putBoolean("downloads_enabled", enabled)
            putBoolean("pref_notifications_personal", enabled)
            putBoolean("pref_notifications_content_alerts", enabled)
            putBoolean("pref_notifications_announcements", enabled)
            putBoolean("pref_notifications_weekly_digest", enabled)
            putBoolean("pref_notifications_exam_alerts", enabled)
            putBoolean("pref_notifications_trending_resources", enabled)
        }.apply()

        _uiState.update {
            it.copy(
                masterEnabled = enabled,
                downloads = enabled,
                personal = enabled,
                contentAlerts = enabled,
                announcements = enabled,
                weeklyDigest = enabled,
                examAlerts = enabled,
                trendingResources = enabled
            )
        }
    }
}
