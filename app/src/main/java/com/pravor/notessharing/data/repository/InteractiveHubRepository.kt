package com.pravor.notessharing.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.pravor.notessharing.data.local.preferences.InteractiveHubPreferences
import com.pravor.notessharing.domain.model.ActiveInteractiveHubConfig
import com.pravor.notessharing.domain.model.InteractiveHubResponse
import com.pravor.notessharing.domain.model.InteractiveHubSession
import com.pravor.notessharing.domain.model.InteractiveHubType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class InteractiveHubRepository(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val preferences = InteractiveHubPreferences.getInstance(context)

    companion object {
        private const val TAG = "InteractiveHubRepo"
        private const val APP_CONFIG_COLLECTION = "app_config"
        private const val ACTIVE_CONFIG_DOC = "active_interactive_hub"
        private const val RESPONSES_COLLECTION = "interactive_hub_responses"

        // In-memory cache with 5-minute TTL to completely eliminate repeated Firestore reads
        // across screen recompositions, tab switching, and rapid navigation.
        private var cachedSession: InteractiveHubSession? = null
        private var lastFetchTimeMillis: Long = 0L
        private const val CACHE_TTL_MILLIS = 5 * 60 * 1000L // 5 minutes

        @Volatile
        private var instance: InteractiveHubRepository? = null

        fun getInstance(context: Context): InteractiveHubRepository {
            return instance ?: synchronized(this) {
                instance ?: InteractiveHubRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Fetch the currently applicable Interactive Hub session.
     * Uses single document read on app_config/active_interactive_hub with in-memory TTL caching.
     * Guaranteed 0 continuous listeners and 0 historical collection scans.
     */
    suspend fun getActiveSession(forceRefresh: Boolean = false): InteractiveHubSession? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 1. Check in-memory TTL cache unless forceRefresh (e.g. pull-to-refresh) is requested
        if (!forceRefresh && (now - lastFetchTimeMillis) < CACHE_TTL_MILLIS && cachedSession != null) {
            val session = cachedSession
            if (session != null && session.isCurrentlyActive(now)) {
                // If it's a survey, verify user hasn't voted
                if (session.hubType == InteractiveHubType.SURVEY && isSurveyCompletedByUser(session.sessionId)) {
                    return@withContext null
                }
                return@withContext session
            }
        }

        try {
            // 2. Fetch the single active config document
            val docRef = firestore.collection(APP_CONFIG_COLLECTION).document(ACTIVE_CONFIG_DOC)
            val snapshot = docRef.get().await()

            if (!snapshot.exists()) {
                Log.d(TAG, "No active_interactive_hub document found.")
                cachedSession = null
                lastFetchTimeMillis = now
                return@withContext null
            }

            var config: ActiveInteractiveHubConfig? = null
            try {
                config = snapshot.toObject(ActiveInteractiveHubConfig::class.java)
            } catch (e: Exception) {
                Log.w(TAG, "toObject failed on ActiveInteractiveHubConfig: ${e.message}", e)
            }

            var session = config?.session
            val isActive = config?.isActive ?: (snapshot.getBoolean("isActive") == true)

            // Fallback manual parse if toObject failed or returned null session
            if (session == null && isActive) {
                val sessionMap = snapshot.get("session") as? Map<*, *>
                if (sessionMap != null) {
                    session = InteractiveHubSession(
                        sessionId = sessionMap["sessionId"] as? String ?: "",
                        title = sessionMap["title"] as? String ?: "",
                        body = sessionMap["body"] as? String ?: "",
                        type = sessionMap["type"] as? String ?: "ANNOUNCEMENT",
                        ctaText = sessionMap["ctaText"] as? String,
                        targetDestination = sessionMap["targetDestination"] as? String,
                        surveyOptions = (sessionMap["surveyOptions"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                        status = sessionMap["status"] as? String ?: "DRAFT",
                        startMode = sessionMap["startMode"] as? String ?: "MANUAL",
                        startTime = (sessionMap["startTime"] as? Number)?.toLong(),
                        endTime = (sessionMap["endTime"] as? Number)?.toLong(),
                        repeatable = sessionMap["repeatable"] as? Boolean ?: true,
                        createdAt = (sessionMap["createdAt"] as? Number)?.toLong() ?: 0L,
                        updatedAt = (sessionMap["updatedAt"] as? Number)?.toLong() ?: 0L,
                        createdBy = sessionMap["createdBy"] as? String ?: ""
                    )
                }
            }

            if (!isActive || session == null || !session.isCurrentlyActive(now)) {
                Log.d(TAG, "Interactive hub session not active or null: isActive=$isActive, session=$session")
                cachedSession = null
                lastFetchTimeMillis = now
                return@withContext null
            }

            // 3. For Survey campaigns: verify if user already voted (both locally and remote if first launch)
            if (session.hubType == InteractiveHubType.SURVEY) {
                if (isSurveyCompletedByUser(session.sessionId)) {
                    Log.d(TAG, "Survey ${session.sessionId} already completed by user. Hiding from Home.")
                    cachedSession = null
                    lastFetchTimeMillis = now
                    return@withContext null
                }
            }

            Log.d(TAG, "Successfully loaded active Interactive Hub session: ${session.sessionId} - ${session.title}")
            cachedSession = session
            lastFetchTimeMillis = now
            return@withContext session
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching active Interactive Hub session: ${e.message}", e)
            // Fail gracefully without crashing or throwing
            return@withContext cachedSession?.takeIf { it.isCurrentlyActive(now) }
        }
    }

    /**
     * Checks whether the current user has already submitted a response for this session.
     * Checks local SharedPreferences first (0 Firestore read cost).
     * If not found locally and user is authenticated, queries document once to check across devices.
     */
    suspend fun isSurveyCompletedByUser(sessionId: String): Boolean {
        if (sessionId.isBlank()) return false

        // 1. Check local device preferences (0 Firestore reads)
        if (preferences.isSurveyAnswered(sessionId)) {
            return true
        }

        // 2. Fallback: Check Firestore document once for authenticated user
        val uid = auth.currentUser?.uid ?: return false
        val responseDocId = "${sessionId}_${uid}"

        return try {
            val docSnap = firestore.collection(RESPONSES_COLLECTION).document(responseDocId).get().await()
            if (docSnap.exists()) {
                val answer = docSnap.getString("response") ?: "VOTED"
                // Cache locally so we never need to check Firestore again on future app launches
                preferences.recordSurveyAnswer(sessionId, answer)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking remote survey response status: ${e.message}")
            false
        }
    }

    /**
     * Submit user's survey response:
     * 1. Immediately saves locally to SharedPreferences for instant, optimistic UI completion.
     * 2. Writes deterministic document ID {sessionId}_{userId} to Firestore to enforce uniqueness.
     */
    suspend fun submitSurveyResponse(sessionId: String, option: String): Boolean = withContext(NonCancellable + Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: "anonymous_${android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "guest"}"
        val responseDocId = "${sessionId}_${uid}"

        // 1. Record in local preferences immediately
        preferences.recordSurveyAnswer(sessionId, option)

        // Invalidate cached session so it will not be returned to this user again
        if (cachedSession?.sessionId == sessionId) {
            cachedSession = null
        }

        // 2. Persist to Firestore with deterministic docId
        return@withContext try {
            val responseData = hashMapOf(
                "responseId" to responseDocId,
                "sessionId" to sessionId,
                "userId" to uid,
                "response" to option,
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection(RESPONSES_COLLECTION)
                .document(responseDocId)
                .set(responseData, SetOptions.merge())
                .await()

            Log.d(TAG, "Survey response recorded in Firestore: $responseDocId ($option)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist survey response to Firestore: ${e.message}", e)
            // Even if offline/error, local preference already recorded it so user UX is not blocked
            true
        }
    }

    fun clearCache() {
        cachedSession = null
        lastFetchTimeMillis = 0L
    }
}
