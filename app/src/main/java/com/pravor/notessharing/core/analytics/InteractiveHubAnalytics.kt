package com.pravor.notessharing.core.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.pravor.notessharing.domain.model.InteractiveHubSession

object InteractiveHubAnalytics {
    private const val TAG = "InteractiveHubAnalytics"

    private const val EVENT_IMPRESSION = "interactive_hub_impression"
    private const val EVENT_CLICKED = "interactive_hub_clicked"
    private const val EVENT_SURVEY_RESPONSE = "interactive_hub_survey_response"

    private const val PARAM_SESSION_ID = "session_id"
    private const val PARAM_SESSION_TYPE = "session_type"
    private const val PARAM_DESTINATION = "destination"
    private const val PARAM_RESPONSE = "response"

    fun logImpression(context: Context, session: InteractiveHubSession) {
        try {
            val analytics = FirebaseAnalytics.getInstance(context)
            val bundle = Bundle().apply {
                putString(PARAM_SESSION_ID, session.sessionId)
                putString(PARAM_SESSION_TYPE, session.rawType)
                putString(PARAM_DESTINATION, session.targetDestination ?: "none")
            }
            analytics.logEvent(EVENT_IMPRESSION, bundle)
            Log.d(TAG, "Logged impression for session: ${session.sessionId}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log impression: ${e.message}")
        }
    }

    fun logClick(context: Context, session: InteractiveHubSession) {
        try {
            val analytics = FirebaseAnalytics.getInstance(context)
            val bundle = Bundle().apply {
                putString(PARAM_SESSION_ID, session.sessionId)
                putString(PARAM_SESSION_TYPE, session.rawType)
                putString(PARAM_DESTINATION, session.targetDestination ?: "none")
            }
            analytics.logEvent(EVENT_CLICKED, bundle)
            Log.d(TAG, "Logged click for session: ${session.sessionId}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log click: ${e.message}")
        }
    }

    fun logSurveyResponse(context: Context, session: InteractiveHubSession, selectedOption: String) {
        try {
            val analytics = FirebaseAnalytics.getInstance(context)
            val bundle = Bundle().apply {
                putString(PARAM_SESSION_ID, session.sessionId)
                putString(PARAM_SESSION_TYPE, session.rawType)
                putString(PARAM_RESPONSE, selectedOption)
            }
            analytics.logEvent(EVENT_SURVEY_RESPONSE, bundle)
            Log.d(TAG, "Logged survey response for session: ${session.sessionId} -> $selectedOption")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log survey response: ${e.message}")
        }
    }
}
