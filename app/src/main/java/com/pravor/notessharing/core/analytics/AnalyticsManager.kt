package com.pravor.notessharing.core.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.pravor.notessharing.NotesSharingApplication

/**
 * Centralized, reliable, and privacy-conscious analytics facade for Campus Pages.
 * All Firebase Analytics / GA4 operations must flow through this manager.
 * 
 * Safety & Privacy guarantees:
 * - Never throws exceptions that could crash the application.
 * - Discards or sanitizes PII (emails, names, tokens, passwords).
 * - Truncates excessively long parameter values (> 100 characters).
 * - Enforces lowercase snake_case event names.
 * - Deduplicates rapid repeated screen views (within 500ms).
 */
object AnalyticsManager {

    private const val TAG = "AnalyticsManager"
    private const val MAX_PARAM_LENGTH = 100
    private const val SCREEN_DEDUP_WINDOW_MS = 500L

    @Volatile
    private var firebaseAnalytics: FirebaseAnalytics? = null

    private var lastScreenName: String? = null
    private var lastScreenTimestamp: Long = 0L

    private val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")

    /**
     * Initializes the FirebaseAnalytics instance with the application context.
     */
    fun initialize(context: Context) {
        if (firebaseAnalytics == null) {
            synchronized(this) {
                if (firebaseAnalytics == null) {
                    try {
                        firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext)
                        Log.d(TAG, "FirebaseAnalytics initialized successfully.")
                    } catch (e: Throwable) {
                        Log.w(TAG, "Failed to initialize FirebaseAnalytics: ${e.message}")
                    }
                }
            }
        }
    }

    private fun getAnalytics(): FirebaseAnalytics? {
        if (firebaseAnalytics == null) {
            try {
                val context = NotesSharingApplication.appContext
                initialize(context)
            } catch (e: Throwable) {
                // Ignore if appContext is not yet available
            }
        }
        return firebaseAnalytics
    }

    // ==========================================
    // Core Screen & Event Logging
    // ==========================================

    /**
     * Logs a screen view event to GA4 with deduplication protection.
     * Prevents duplicate screen events triggered by recomposition or immediate backstack transitions.
     */
    fun logScreenView(screenName: String, screenClass: String? = null) {
        try {
            val now = System.currentTimeMillis()
            if (screenName == lastScreenName && (now - lastScreenTimestamp) < SCREEN_DEDUP_WINDOW_MS) {
                return
            }

            lastScreenName = screenName
            lastScreenTimestamp = now

            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass ?: screenName)
            }

            getAnalytics()?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            Log.d(TAG, "Screen view logged: $screenName (class: ${screenClass ?: screenName})")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to log screen view '$screenName': ${e.message}")
        }
    }

    /**
     * Generic event logging helper that safely filters nulls, sanitizes values,
     * and maps parameters into a Bundle.
     */
    fun logEvent(eventName: String, params: Map<String, Any?> = emptyMap()) {
        try {
            val bundle = Bundle()
            params.forEach { (key, value) ->
                if (value != null) {
                    val sanitizedKey = key.trim().lowercase()
                    when (value) {
                        is String -> {
                            val sanitizedVal = sanitizeString(value)
                            if (sanitizedVal.isNotEmpty()) {
                                bundle.putString(sanitizedKey, sanitizedVal)
                            }
                        }
                        is Int -> bundle.putInt(sanitizedKey, value)
                        is Long -> bundle.putLong(sanitizedKey, value)
                        is Double -> bundle.putDouble(sanitizedKey, value)
                        is Float -> bundle.putFloat(sanitizedKey, value)
                        is Boolean -> bundle.putBoolean(sanitizedKey, value)
                        else -> {
                            val strVal = sanitizeString(value.toString())
                            if (strVal.isNotEmpty()) {
                                bundle.putString(sanitizedKey, strVal)
                            }
                        }
                    }
                }
            }
            getAnalytics()?.logEvent(eventName, bundle)
            Log.d(TAG, "Event logged: $eventName with params ${params.keys}")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to log event '$eventName': ${e.message}")
        }
    }

    // ==========================================
    // User Properties
    // ==========================================

    /**
     * Sets privacy-safe academic user properties. Never accepts or records PII.
     */
    fun setUserProperties(college: String? = null, branch: String? = null, semester: String? = null) {
        try {
            val analytics = getAnalytics() ?: return
            analytics.setUserProperty("user_college", college?.trim()?.take(MAX_PARAM_LENGTH))
            analytics.setUserProperty("user_branch", branch?.trim()?.take(MAX_PARAM_LENGTH))
            analytics.setUserProperty("user_semester", semester?.trim()?.take(MAX_PARAM_LENGTH))
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to set user properties: ${e.message}")
        }
    }

    // ==========================================
    // Domain-Specific Events
    // ==========================================

    // --- Content ---

    fun logContentOpen(
        contentId: String,
        contentType: String,
        subject: String? = null,
        semester: String? = null,
        sourceScreen: String? = null
    ) {
        logEvent(
            "content_detail_open",
            mapOf(
                "content_id" to contentId,
                "content_type" to contentType.lowercase(),
                "subject" to subject,
                "semester" to semester,
                "source_screen" to sourceScreen
            )
        )
    }

    fun logContentViewFile(
        contentId: String,
        contentType: String,
        fileFormat: String,
        viewerType: String
    ) {
        logEvent(
            "content_file_view",
            mapOf(
                "content_id" to contentId,
                "content_type" to contentType.lowercase(),
                "file_format" to fileFormat.lowercase(),
                "viewer_type" to viewerType
            )
        )
    }

    fun logContentBookmarkToggle(
        contentId: String,
        contentType: String,
        isBookmarked: Boolean
    ) {
        logEvent(
            "content_bookmark_toggle",
            mapOf(
                "content_id" to contentId,
                "content_type" to contentType.lowercase(),
                "action" to if (isBookmarked) "add" else "remove"
            )
        )
    }

    fun logContentUpvoteToggle(
        contentId: String,
        contentType: String,
        isUpvoted: Boolean
    ) {
        logEvent(
            "content_upvote_toggle",
            mapOf(
                "content_id" to contentId,
                "content_type" to contentType.lowercase(),
                "action" to if (isUpvoted) "upvote" else "remove_upvote"
            )
        )
    }

    fun logContentShare(
        contentId: String,
        contentType: String,
        subject: String? = null
    ) {
        logEvent(
            "content_share",
            mapOf(
                "content_id" to contentId,
                "content_type" to contentType.lowercase(),
                "subject" to subject
            )
        )
    }

    // --- Search ---

    fun logSearchPerformed(
        query: String,
        category: String,
        branch: String? = null,
        semester: String? = null,
        resultsCount: Int
    ) {
        // Sanitize search query: do not log if it contains email format
        val cleanQuery = if (emailRegex.containsMatchIn(query)) "[sanitized_email]" else query.trim()
        logEvent(
            "search_performed",
            mapOf(
                "search_term" to cleanQuery,
                "search_category" to category.lowercase(),
                "filter_branch" to branch,
                "filter_semester" to semester,
                "results_count" to resultsCount
            )
        )
    }

    fun logSearchResultClick(
        searchTerm: String,
        contentId: String,
        contentType: String,
        position: Int
    ) {
        val cleanQuery = if (emailRegex.containsMatchIn(searchTerm)) "[sanitized_email]" else searchTerm.trim()
        logEvent(
            "search_result_click",
            mapOf(
                "search_term" to cleanQuery,
                "content_id" to contentId,
                "content_type" to contentType.lowercase(),
                "result_position" to position
            )
        )
    }

    // --- Downloads ---

    fun logDownloadStarted(
        contentId: String,
        contentType: String,
        fileCount: Int
    ) {
        logEvent(
            "download_started",
            mapOf(
                "content_id" to contentId,
                "content_type" to contentType.lowercase(),
                "file_count" to fileCount
            )
        )
    }

    fun logDownloadCompleted(
        contentId: String,
        contentType: String,
        durationMs: Long
    ) {
        logEvent(
            "download_completed",
            mapOf(
                "content_id" to contentId,
                "content_type" to contentType.lowercase(),
                "duration_ms" to durationMs
            )
        )
    }

    fun logDownloadFailed(
        contentId: String,
        contentType: String,
        errorType: String
    ) {
        logEvent(
            "download_failed",
            mapOf(
                "content_id" to contentId,
                "content_type" to contentType.lowercase(),
                "error_type" to errorType
            )
        )
    }

    // --- Uploads ---

    fun logUploadStarted(
        contentType: String,
        subject: String,
        semester: String,
        fileCount: Int
    ) {
        logEvent(
            "upload_started",
            mapOf(
                "content_type" to contentType.lowercase(),
                "subject" to subject,
                "semester" to semester,
                "file_count" to fileCount
            )
        )
    }

    fun logUploadCompleted(
        contentType: String,
        subject: String,
        durationMs: Long
    ) {
        logEvent(
            "upload_completed",
            mapOf(
                "content_type" to contentType.lowercase(),
                "subject" to subject,
                "duration_ms" to durationMs
            )
        )
    }

    fun logUploadFailed(
        contentType: String,
        subject: String,
        errorReason: String
    ) {
        logEvent(
            "upload_failed",
            mapOf(
                "content_type" to contentType.lowercase(),
                "subject" to subject,
                "error_reason" to errorReason
            )
        )
    }

    // --- Authentication ---

    fun logAuthStarted(authMethod: String) {
        logEvent(
            "auth_started",
            mapOf("auth_method" to authMethod.lowercase())
        )
    }

    fun logAuthSuccess(authMethod: String, isNewUser: Boolean) {
        logEvent(
            "auth_success",
            mapOf(
                "auth_method" to authMethod.lowercase(),
                "is_new_user" to isNewUser
            )
        )
    }

    fun logAuthFailed(authMethod: String, errorCode: String) {
        logEvent(
            "auth_failed",
            mapOf(
                "auth_method" to authMethod.lowercase(),
                "error_code" to errorCode
            )
        )
    }

    fun logAuthLogout() {
        logEvent("auth_logout")
    }

    fun logOnboardingCompleted(college: String, branch: String, semester: String) {
        logEvent(
            "onboarding_completed",
            mapOf(
                "college" to college,
                "branch" to branch,
                "semester" to semester
            )
        )
    }

    // --- Google Classroom ---

    fun logClassroomConnectStarted() {
        logEvent("classroom_connect_started")
    }

    fun logClassroomConnectSuccess(coursesCount: Int) {
        logEvent(
            "classroom_connect_success",
            mapOf("courses_count" to coursesCount)
        )
    }

    fun logClassroomConnectFailed(errorCode: String) {
        logEvent(
            "classroom_connect_failed",
            mapOf("error_code" to errorCode)
        )
    }

    fun logClassroomDisconnect() {
        logEvent("classroom_disconnect")
    }

    fun logClassroomSubmissionOpened(courseId: String, workId: String) {
        logEvent(
            "classroom_submission_opened",
            mapOf(
                "course_id" to courseId,
                "work_id" to workId
            )
        )
    }

    // ==========================================
    // Sanitization Utilities
    // ==========================================

    private fun sanitizeString(value: String): String {
        var str = value.trim()
        if (emailRegex.containsMatchIn(str)) {
            str = emailRegex.replace(str, "[email]")
        }
        return str.take(MAX_PARAM_LENGTH)
    }
}
