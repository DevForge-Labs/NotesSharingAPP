package com.pravor.notessharing.data.local.preferences

import android.content.Context
import android.content.SharedPreferences

class InteractiveHubPreferences(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "interactive_hub_preferences"
        private const val PREFIX_ANSWERED = "answered_survey_"

        @Volatile
        private var instance: InteractiveHubPreferences? = null

        fun getInstance(context: Context): InteractiveHubPreferences {
            return instance ?: synchronized(this) {
                instance ?: InteractiveHubPreferences(context).also { instance = it }
            }
        }
    }

    /**
     * Check if user on this device has already answered a specific survey session
     */
    fun isSurveyAnswered(sessionId: String): Boolean {
        if (sessionId.isBlank()) return false
        return prefs.contains(PREFIX_ANSWERED + sessionId)
    }

    /**
     * Get the option selected by the user for a specific survey session
     */
    fun getSurveyAnswer(sessionId: String): String? {
        if (sessionId.isBlank()) return null
        return prefs.getString(PREFIX_ANSWERED + sessionId, null)
    }

    /**
     * Record the user's vote locally so the survey is permanently marked as completed
     * across app restarts for this specific sessionId.
     */
    fun recordSurveyAnswer(sessionId: String, option: String) {
        if (sessionId.isBlank()) return
        prefs.edit()
            .putString(PREFIX_ANSWERED + sessionId, option)
            .apply()
    }
}
