package com.pravor.notessharing.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

object GoogleAuthHelper {
    private const val TAG = "AuthAudit"
    private const val EXPECTED_WEB_CLIENT_ID = "1093598842915-7un8eep0q7323t57tgjb4f034mtrvprq.apps.googleusercontent.com"

    fun getVerifiedWebClientId(context: Context): String {
        Log.d(TAG, "[GoogleAuth] Loading Web Client ID dynamically from resources...")
        val resourceId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resourceId == 0) {
            val errorMsg = "Resource 'default_web_client_id' not found in package resources. Ensure google-services.json is correctly configured and applied."
            Log.e(TAG, "[GoogleAuth] $errorMsg")
            throw IllegalStateException(errorMsg)
        }
        val resolvedWebClientId = context.getString(resourceId)
        Log.d(TAG, "[GoogleAuth] Dynamically loaded Web Client ID: '$resolvedWebClientId'")

        if (resolvedWebClientId != EXPECTED_WEB_CLIENT_ID) {
            val errorMsg = "Web Client ID verification mismatch! Expected: '$EXPECTED_WEB_CLIENT_ID', Loaded: '$resolvedWebClientId'. Ensure google-services.json is up-to-date."
            Log.e(TAG, "[GoogleAuth] $errorMsg")
            throw IllegalStateException(errorMsg)
        }
        
        Log.d(TAG, "[GoogleAuth] Web Client ID successfully verified matches Firebase Web Client ID.")
        return resolvedWebClientId
    }

    @Volatile
    private var isSigningIn = false

    private fun findActivity(context: Context): android.app.Activity? {
        var currentContext = context
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is android.app.Activity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }

    suspend fun performGoogleSignIn(
        context: Context,
        onTokenReceived: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isSigningIn) {
            Log.w(TAG, "[GoogleAuth] Sign-in request ignored: already in progress.")
            return
        }
        isSigningIn = true
        try {
            Log.d(TAG, "[GoogleAuth] Starting Google Sign-In with Credential Manager...")
            val webClientId = getVerifiedWebClientId(context)
            val credentialManager = CredentialManager.create(context)
            val activityContext = findActivity(context)
            val launchContext = activityContext ?: context

            Log.d(TAG, "[GoogleAuth] Configuring GetGoogleIdOption with server client ID...")
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Prompt accounts chooser
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            Log.d(TAG, "[GoogleAuth] Building GetCredentialRequest...")
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            Log.d(TAG, "[GoogleAuth] Launching Credential Manager UI picker...")
            val result = credentialManager.getCredential(launchContext, request)
            val credential = result.credential

            Log.d(TAG, "[GoogleAuth] Credential type retrieved: '${credential.type}'")
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                Log.d(TAG, "[GoogleAuth] Parsing Google ID token credential...")
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                
                if (idToken.isNullOrBlank()) {
                    Log.e(TAG, "[GoogleAuth] Received empty Google ID Token.")
                    onError("Unable to start Google Sign-In. Please try again.")
                } else {
                    Log.d(TAG, "[GoogleAuth] ID Token successfully retrieved. Passing to ViewModel.")
                    onTokenReceived(idToken)
                }
            } else {
                Log.e(TAG, "[GoogleAuth] Unexpected credential type: '${credential.type}'")
                onError("Unable to start Google Sign-In. Please try again.")
            }
        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            Log.d(TAG, "[GoogleAuth] Google Sign-In cancelled by user.")
        } catch (e: GetCredentialException) {
            Log.e(TAG, "[GoogleAuth] Credential Manager failed: ${e.message} (Type: ${e::class.java.simpleName})", e)
            onError("Unable to start Google Sign-In. Please try again.")
        } catch (e: Exception) {
            Log.e(TAG, "[GoogleAuth] Google Sign-In Exception: ${e.message} (Type: ${e::class.java.simpleName})", e)
            onError("Unable to start Google Sign-In. Please try again.")
        } finally {
            isSigningIn = false
        }
    }
}
