package com.pravor.notessharing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pravor.notessharing.ui.navigation.NotesSharingApp
import com.pravor.notessharing.ui.theme.NotesSharingTheme
import com.pravor.notessharing.viewmodel.AppSettingsViewModel

import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Log
import java.security.MessageDigest

class MainActivity : ComponentActivity() {
    private val appSettingsViewModel: AppSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        logAppSignatures()
        setContent {
            val appSettings by appSettingsViewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()

            NotesSharingTheme(darkTheme = appSettings.darkModeEnabled) {
                NotesSharingApp(
                    appSettings = appSettings,
                    onDarkModeChange = appSettingsViewModel::setDarkMode,
                    onThemePreferenceChange = { preference ->
                        appSettingsViewModel.setThemePreference(preference, systemDark)
                    },
                    onNotificationsChange = appSettingsViewModel::setNotifications
                )
            }
        }
    }

    private fun logAppSignatures() {
        try {
            val packageName = this.packageName
            val packageManager = this.packageManager
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                packageInfo.signatures
            }

            signatures?.forEach { signature ->
                val sha1 = getSignatureHash(signature, "SHA-1")
                val sha256 = getSignatureHash(signature, "SHA-256")
                Log.d("AuthAudit", "=== NotesSharing APP SIGNATURE AUDIT ===")
                Log.d("AuthAudit", "Package Name: $packageName")
                Log.d("AuthAudit", "Actual SHA-1 Signature: $sha1")
                Log.d("AuthAudit", "Actual SHA-256 Signature: $sha256")
                Log.d("AuthAudit", "========================================")
            }
        } catch (e: Exception) {
            Log.e("AuthAudit", "Failed to retrieve signatures", e)
        }
    }

    private fun getSignatureHash(signature: Signature, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        val digest = md.digest(signature.toByteArray())
        return digest.joinToString(":") { String.format("%02x", it) }.uppercase()
    }
}
