package com.pravor.notessharing.data.kaya

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class KayaRemoteDataSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val TAG = "KayaRemoteDataSource"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
    }

    /**
     * Fetches the personalized student dashboard HTML using the authenticated session cookies.
     * Throws [KayaSessionExpiredException] if the session is invalid or has expired.
     * Throws [KayaNetworkException] if connection fails.
     */
    suspend fun fetchDashboardHtml(session: KayaSession): String = withContext(Dispatchers.IO) {
        val cookieHeader = "sessionid=${session.sessionId}; csrftoken=${session.csrfToken}"

        val request = Request.Builder()
            .url(KayaAuthManager.DASHBOARD_URL)
            .header("User-Agent", USER_AGENT)
            .header("Referer", KayaAuthManager.LOGIN_URL)
            .header("Cookie", cookieHeader)
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            val code = response.code
            val body = response.body?.string().orEmpty()

            // Check for redirect back to login page or unauthenticated status
            val finalUrl = response.request.url.encodedPath
            val isRedirectedToLogin = finalUrl.contains("/login", ignoreCase = true)
            val isLoginPageBody = body.contains("id=\"loginForm\"", ignoreCase = true) ||
                    body.contains("login-page", ignoreCase = true)

            if (code in listOf(401, 403) || isRedirectedToLogin || isLoginPageBody) {
                Log.w(TAG, "KAYA session expired or invalid (HTTP $code, path $finalUrl)")
                throw KayaSessionExpiredException("Your KAYA session has expired. Please reconnect.")
            }

            if (!response.isSuccessful) {
                throw KayaNetworkException("KAYA server returned an error (HTTP $code).")
            }

            body
        } catch (e: KayaException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Network error while fetching KAYA dashboard", e)
            throw KayaNetworkException("Unable to connect to KAYA right now. Please check your internet connection and try again.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error fetching KAYA dashboard", e)
            throw e
        }
    }
}
