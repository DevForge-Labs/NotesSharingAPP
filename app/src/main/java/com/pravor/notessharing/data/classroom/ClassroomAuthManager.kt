package com.pravor.notessharing.data.classroom

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

sealed class ClassroomTokenResult {
    data class Success(val token: String) : ClassroomTokenResult()
    data class ConsentRequired(val recoveryIntent: Intent) : ClassroomTokenResult()
    data class Error(val message: String, val cause: Throwable? = null) : ClassroomTokenResult()
}

data class ClassroomSession(
    val firebaseUid: String?,
    val classroomAccount: String?,
    val authState: ClassroomAuthState
)

class ClassroomAuthManager(private val context: Context) {

    companion object {
        private const val TAG = "ClassroomAuth"

        val SCOPES = listOf(
            "https://www.googleapis.com/auth/classroom.courses.readonly",
            "https://www.googleapis.com/auth/classroom.courseworkmaterials.readonly",
            "https://www.googleapis.com/auth/classroom.coursework.me.readonly",
            "https://www.googleapis.com/auth/classroom.announcements.readonly"
        )

        val SUBMISSION_SCOPES = listOf(
            "https://www.googleapis.com/auth/classroom.coursework.me",
            "https://www.googleapis.com/auth/drive.file"
        )

        val ALL_SCOPES = (SCOPES + SUBMISSION_SCOPES).distinct()

        private const val PREF_KEY_EMAIL = "connected_email"
        private const val PREF_KEY_NAME = "connected_display_name"
        private const val PREF_KEY_PHOTO = "connected_photo_url"
        private const val PREF_KEY_CONNECTED_AT = "connected_at"

        @Volatile
        private var INSTANCE: ClassroomAuthManager? = null

        fun getInstance(context: Context): ClassroomAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ClassroomAuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val _authState = MutableStateFlow<ClassroomAuthState>(ClassroomAuthState.Disconnected)
    val authState: StateFlow<ClassroomAuthState> = _authState.asStateFlow()

    val currentSessionFlow: Flow<ClassroomSession> = _authState.map { state ->
        val uid = getFirebaseUid()
        val account = (state as? ClassroomAuthState.Connected)?.account?.email
        ClassroomSession(uid, account, state)
    }

    private val firebaseAuthListener = FirebaseAuth.AuthStateListener { auth ->
        val uid = auth.currentUser?.uid
        Log.d(TAG, "FirebaseAuth state changed. Current UID: $uid")
        refreshAuthState()
    }

    init {
        try {
            FirebaseAuth.getInstance().addAuthStateListener(firebaseAuthListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register FirebaseAuth listener", e)
        }
        refreshAuthState()
    }

    private fun getFirebaseUid(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    private fun getPrefs(): android.content.SharedPreferences? {
        val uid = getFirebaseUid() ?: return null
        return context.getSharedPreferences("classroom_auth_$uid", Context.MODE_PRIVATE)
    }

    fun refreshAuthState() {
        val prefs = getPrefs()
        if (prefs == null) {
            _authState.value = ClassroomAuthState.Disconnected
            return
        }

        val email = prefs.getString(PREF_KEY_EMAIL, null)
        if (email.isNullOrBlank()) {
            _authState.value = ClassroomAuthState.Disconnected
            return
        }

        val displayName = prefs.getString(PREF_KEY_NAME, null)
        val photoUrl = prefs.getString(PREF_KEY_PHOTO, null)
        val connectedAt = prefs.getLong(PREF_KEY_CONNECTED_AT, 0L)

        _authState.value = ClassroomAuthState.Connected(
            ClassroomAccount(
                email = email,
                displayName = displayName,
                photoUrl = photoUrl,
                connectedAt = connectedAt
            )
        )
    }

    private fun getSignInClient(): GoogleSignInClient {
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()

        SCOPES.forEach { scopeUri ->
            gsoBuilder.requestScopes(Scope(scopeUri))
        }

        return GoogleSignIn.getClient(context, gsoBuilder.build())
    }

    fun getSignInIntent(): Intent {
        val client = getSignInClient()
        return client.signInIntent
    }

    fun handleSignInResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val email = account.email

            if (email.isNullOrBlank()) {
                _authState.value = ClassroomAuthState.Error("No email associated with this Google account.")
                return
            }

            val prefs = getPrefs()
            if (prefs == null) {
                _authState.value = ClassroomAuthState.Error("Please log in to Campus Pages before connecting Classroom.")
                return
            }

            prefs.edit()
                .putString(PREF_KEY_EMAIL, email)
                .putString(PREF_KEY_NAME, account.displayName)
                .putString(PREF_KEY_PHOTO, account.photoUrl?.toString())
                .putLong(PREF_KEY_CONNECTED_AT, System.currentTimeMillis())
                .apply()

            _authState.value = ClassroomAuthState.Connected(
                ClassroomAccount(
                    email = email,
                    displayName = account.displayName,
                    photoUrl = account.photoUrl?.toString(),
                    connectedAt = System.currentTimeMillis()
                )
            )

            Log.d(TAG, "Google Classroom connected successfully for email: $email")
            com.pravor.notessharing.data.classroom.reminder.ClassroomSyncWorker.enqueuePeriodicSync(context)
            com.pravor.notessharing.data.classroom.reminder.ClassroomReminderScheduler.reconcileReminders(context)
        } catch (e: ApiException) {
            Log.e(TAG, "Google Sign-In failed with status code: ${e.statusCode}", e)
            if (e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED || 
                e.statusCode == 12501 || 
                e.statusCode == 16 || 
                e.statusCode == CommonStatusCodes.CANCELED
            ) {
                Log.d(TAG, "Google Sign-In was cancelled by user.")
                refreshAuthState()
            } else {
                _authState.value = ClassroomAuthState.Error("Google Sign-In was cancelled or failed (${e.statusCode}).")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error handling Google Sign-In result", e)
            _authState.value = ClassroomAuthState.Error(e.message ?: "Sign-in failed. Please try again.")
        }
    }

    suspend fun getClassroomAccessToken(): String? = withContext(Dispatchers.IO) {
        val prefs = getPrefs() ?: return@withContext null
        val email = prefs.getString(PREF_KEY_EMAIL, null) ?: return@withContext null

        val scopeString = "oauth2:" + SCOPES.joinToString(" ")

        try {
            val account = Account(email, "com.google")
            val token = GoogleAuthUtil.getToken(context, account, scopeString)
            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve OAuth token for Classroom: ${e.message}", e)
            null
        }
    }

    suspend fun getSubmissionAccessToken(): ClassroomTokenResult = withContext(Dispatchers.IO) {
        val prefs = getPrefs() ?: return@withContext ClassroomTokenResult.Error("Please log in to Campus Pages.")
        val email = prefs.getString(PREF_KEY_EMAIL, null) ?: return@withContext ClassroomTokenResult.Error("No connected Google Classroom account.")

        val scopeString = "oauth2:" + ALL_SCOPES.joinToString(" ")

        try {
            val account = Account(email, "com.google")
            val token = GoogleAuthUtil.getToken(context, account, scopeString)
            if (token.isNullOrBlank()) {
                ClassroomTokenResult.Error("Received empty OAuth token.")
            } else {
                ClassroomTokenResult.Success(token)
            }
        } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
            Log.d(TAG, "Incremental authorization consent required for submission scopes", e)
            val recoveryIntent = e.intent
            if (recoveryIntent != null) {
                ClassroomTokenResult.ConsentRequired(recoveryIntent)
            } else {
                ClassroomTokenResult.Error("Consent required but no recovery intent provided.", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve submission OAuth token: ${e.message}", e)
            ClassroomTokenResult.Error(e.message ?: "Failed to retrieve authorization token.", e)
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        val prefs = getPrefs()
        prefs?.edit()?.clear()?.apply()

        try {
            val client = getSignInClient()
            client.signOut()
            client.revokeAccess()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to completely revoke GoogleSignInClient access", e)
        }

        com.pravor.notessharing.data.classroom.reminder.ClassroomReminderScheduler.cancelAllReminders(context)
        com.pravor.notessharing.data.classroom.reminder.ClassroomSyncWorker.cancelPeriodicSync(context)

        _authState.value = ClassroomAuthState.Disconnected
        Log.d(TAG, "Google Classroom account disconnected.")
    }
}
