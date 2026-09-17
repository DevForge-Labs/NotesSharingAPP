package com.pravor.notessharing.data.kaya

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class KayaAuthManager private constructor(
    private val context: Context,
    private val credentialStore: KayaCredentialStore = AndroidKeyStoreKayaCredentialStore(context)
) {

    companion object {
        private const val TAG = "KayaAuthManager"
        const val BASE_URL = "https://kaya.cse.kiit.ac.in"
        const val LOGIN_URL = "$BASE_URL/login/"
        const val DASHBOARD_URL = "$BASE_URL/dashboard/student/"

        private const val PREF_KEY_USERNAME = "kaya_username"
        private const val PREF_KEY_SESSION_ID = "kaya_session_id"
        private const val PREF_KEY_CSRF_TOKEN = "kaya_csrf_token"
        private const val PREF_KEY_CONNECTED_AT = "kaya_connected_at"
        private const val PREF_KEY_IS_EXPIRED = "kaya_is_expired"
        private const val GLOBAL_PREFS = "kaya_global_prefs"
        private const val PREF_LAST_CONNECTED_USER = "last_connected_user_id"

        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

        @Volatile
        private var INSTANCE: KayaAuthManager? = null

        fun getInstance(
            context: Context,
            credentialStore: KayaCredentialStore = AndroidKeyStoreKayaCredentialStore(context)
        ): KayaAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: KayaAuthManager(context.applicationContext, credentialStore).also { INSTANCE = it }
            }
        }
    }

    fun getLastConnectedUserId(): String? {
        return context.getSharedPreferences(GLOBAL_PREFS, Context.MODE_PRIVATE)
            .getString(PREF_LAST_CONNECTED_USER, null)
    }

    private fun getCurrentUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid
            ?: getLastConnectedUserId()
            ?: "anonymous"
    }

    private fun getPrefs(userId: String = getCurrentUserId()): android.content.SharedPreferences {
        return context.getSharedPreferences("kaya_session_$userId", Context.MODE_PRIVATE)
    }

    fun isConnected(userId: String = getCurrentUserId()): Boolean {
        val effectiveUserId = if (userId == "anonymous") (getLastConnectedUserId() ?: userId) else userId
        val prefs = getPrefs(effectiveUserId)
        val sessionId = prefs.getString(PREF_KEY_SESSION_ID, null)
        return !sessionId.isNullOrBlank() || hasStoredCredentials(effectiveUserId)
    }

    fun isSessionExpired(userId: String = getCurrentUserId()): Boolean {
        val prefs = getPrefs(userId)
        return prefs.getBoolean(PREF_KEY_IS_EXPIRED, false)
    }

    fun getSession(userId: String = getCurrentUserId()): KayaSession? {
        val prefs = getPrefs(userId)
        val username = prefs.getString(PREF_KEY_USERNAME, null) ?: return null
        val sessionId = prefs.getString(PREF_KEY_SESSION_ID, null) ?: return null
        val csrfToken = prefs.getString(PREF_KEY_CSRF_TOKEN, "") ?: ""
        val connectedAt = prefs.getLong(PREF_KEY_CONNECTED_AT, 0L)
        val isExpired = prefs.getBoolean(PREF_KEY_IS_EXPIRED, false)

        return KayaSession(
            username = username,
            sessionId = sessionId,
            csrfToken = csrfToken,
            lastConnectedAt = connectedAt,
            isSessionExpired = isExpired
        )
    }

    fun markSessionExpired(userId: String = getCurrentUserId()) {
        getPrefs(userId).edit().putBoolean(PREF_KEY_IS_EXPIRED, true).apply()
        Log.d(TAG, "Marked KAYA session as expired for user $userId")
    }

    fun clearSession(userId: String = getCurrentUserId()) {
        getPrefs(userId).edit().clear().apply()
        Log.d(TAG, "Cleared KAYA session for user $userId")
    }

    fun getStoredCredentials(userId: String = getCurrentUserId()): KayaCredentials? {
        return credentialStore.getCredentials(userId)
    }

    fun hasStoredCredentials(userId: String = getCurrentUserId()): Boolean {
        return credentialStore.hasCredentials(userId)
    }

    fun getStoredUsername(userId: String = getCurrentUserId()): String? {
        return credentialStore.getCredentials(userId)?.username
            ?: getPrefs(userId).getString(PREF_KEY_USERNAME, null)
    }

    /**
     * Explicitly disconnects KAYA for the specified user, purging both session
     * cookies and locally encrypted credentials.
     */
    fun disconnect(userId: String = getCurrentUserId()) {
        clearSession(userId)
        credentialStore.clearCredentials(userId)
        if (getLastConnectedUserId() == userId) {
            context.getSharedPreferences(GLOBAL_PREFS, Context.MODE_PRIVATE).edit()
                .remove(PREF_LAST_CONNECTED_USER)
                .apply()
        }
        Log.d(TAG, "Disconnected KAYA: cleared session and encrypted credentials for user $userId")
    }

    /**
     * Authenticates the user against KAYA using the verified native OkHttp flow.
     * Extracts CSRF token from /login/, posts credentials, and verifies sessionid cookie.
     * Note: The password is NEVER written to disk or logs.
     */
    suspend fun login(username: String, password: String): Result<KayaSession> = withContext(Dispatchers.IO) {
        if (username.isBlank() || password.isBlank()) {
            return@withContext Result.failure(KayaAuthException("Username and password cannot be empty."))
        }

        val cookieStore = ConcurrentHashMap<String, MutableMap<String, Cookie>>()
        val cookieJar = object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val map = cookieStore.getOrPut(url.host) { mutableMapOf() }
                for (cookie in cookies) {
                    map[cookie.name] = cookie
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host]?.values?.toList() ?: emptyList()
            }
        }

        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .followRedirects(false) // Inspect 302 redirect for successful login
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        try {
            // Step 1: GET /login/ to obtain fresh CSRF token and cookie
            val getRequest = Request.Builder()
                .url(LOGIN_URL)
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            val getResponse = client.newCall(getRequest).execute()
            val getHtml = getResponse.body?.string().orEmpty()

            if (!getResponse.isSuccessful && getResponse.code != 200) {
                return@withContext Result.failure(
                    KayaNetworkException("Unable to contact KAYA server (HTTP ${getResponse.code}).")
                )
            }

            val csrfPattern = Regex("""name=["']csrfmiddlewaretoken["']\s+value=["']([^"']+)["']""")
            val csrfTokenFromForm = csrfPattern.find(getHtml)?.groupValues?.get(1)
            val csrfCookie = cookieStore["kaya.cse.kiit.ac.in"]?.get("csrftoken")?.value ?: csrfTokenFromForm ?: ""

            if (csrfTokenFromForm.isNullOrBlank()) {
                return@withContext Result.failure(
                    KayaParseException("Could not extract security token from KAYA login page.")
                )
            }

            // Step 2: POST credentials to /login/
            val formBody = FormBody.Builder()
                .add("csrfmiddlewaretoken", csrfTokenFromForm)
                .add("username", username.trim())
                .add("password", password)
                .build()

            val postRequest = Request.Builder()
                .url(LOGIN_URL)
                .header("User-Agent", USER_AGENT)
                .header("Referer", LOGIN_URL)
                .header("Origin", BASE_URL)
                .post(formBody)
                .build()

            val postResponse = client.newCall(postRequest).execute()
            val postCode = postResponse.code
            val postBody = postResponse.body?.string().orEmpty()

            val sessionCookie = cookieStore["kaya.cse.kiit.ac.in"]?.get("sessionid")

            // Successful Django login responds with 302 redirect and sessionid cookie
            val isSuccess = sessionCookie != null || (postCode in listOf(302, 303) && postResponse.header("Location") != null)

            if (isSuccess && sessionCookie != null) {
                val userId = getCurrentUserId()
                val session = KayaSession(
                    username = username.trim(),
                    sessionId = sessionCookie.value,
                    csrfToken = csrfCookie,
                    lastConnectedAt = System.currentTimeMillis(),
                    isSessionExpired = false
                )

                // Persist session cookies locally (partitioned by Firebase UID)
                getPrefs(userId).edit()
                    .putString(PREF_KEY_USERNAME, session.username)
                    .putString(PREF_KEY_SESSION_ID, session.sessionId)
                    .putString(PREF_KEY_CSRF_TOKEN, session.csrfToken)
                    .putLong(PREF_KEY_CONNECTED_AT, session.lastConnectedAt)
                    .putBoolean(PREF_KEY_IS_EXPIRED, false)
                    .apply()

                // Securely persist credentials locally using Keystore-backed authenticated encryption
                credentialStore.saveCredentials(userId, username.trim(), password)

                context.getSharedPreferences(GLOBAL_PREFS, Context.MODE_PRIVATE).edit()
                    .putString(PREF_LAST_CONNECTED_USER, userId)
                    .apply()

                Log.d(TAG, "KAYA authentication successful for user $userId (student: ${session.username})")
                Result.success(session)
            } else {
                val isInvalidCredentials = postBody.contains("Please enter a correct username and password", ignoreCase = true) ||
                        postBody.contains("Your username and password didn't match", ignoreCase = true)

                if (isInvalidCredentials) {
                    Result.failure(KayaAuthException("Incorrect KAYA username or password."))
                } else {
                    Result.failure(KayaAuthException("Unable to sign in. Please verify your KAYA credentials and try again."))
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error during KAYA login", e)
            Result.failure(KayaNetworkException("Unable to connect to KAYA right now. Please check your internet connection and try again.", e))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during KAYA login", e)
            Result.failure(e)
        }
    }
}
