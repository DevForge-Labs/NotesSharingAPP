package com.pravor.notessharing.core.update

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.pravor.notessharing.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reusable manager for Google Play In-App Updates.
 * Handles update checks, flexible background downloads, install completion,
 * session throttling, and error recovery.
 */
class InAppUpdateManager private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(appContext)

    private val _updateState = MutableStateFlow<InAppUpdateState>(InAppUpdateState.Idle)
    val updateState: StateFlow<InAppUpdateState> = _updateState.asStateFlow()

    @Volatile
    private var isSessionDismissed = false

    @Volatile
    private var isChecking = false

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytes = state.totalBytesToDownload()
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[IN_APP_UPDATE] Update download progress: $bytesDownloaded / $totalBytes bytes")
                }
                _updateState.value = InAppUpdateState.Downloading(bytesDownloaded, totalBytes)
            }
            InstallStatus.DOWNLOADED -> {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[IN_APP_UPDATE] Update downloaded and ready to install")
                }
                _updateState.value = InAppUpdateState.Downloaded
            }
            InstallStatus.INSTALLED -> {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[IN_APP_UPDATE] Update installation completed")
                }
                _updateState.value = InAppUpdateState.Idle
            }
            InstallStatus.FAILED -> {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "[IN_APP_UPDATE] Update download failed with error code: ${state.installErrorCode()}")
                }
                _updateState.value = InAppUpdateState.Failed("Update download failed (code: ${state.installErrorCode()})")
            }
            InstallStatus.CANCELED -> {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[IN_APP_UPDATE] Update download cancelled")
                }
                _updateState.value = InAppUpdateState.Idle
            }
            else -> {
                // Other transient statuses (PENDING, INSTALLING, UNKNOWN)
            }
        }
    }

    init {
        try {
            appUpdateManager.registerListener(installStateUpdatedListener)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "[IN_APP_UPDATE] Failed to register InstallStateUpdatedListener: ${e.message}", e)
            }
        }
    }

    /**
     * Checks for available Google Play updates asynchronously.
     * Safe against exceptions (e.g. Play Store missing, network down, Play Services disabled).
     *
     * @param isForced Set to true if the check is for a mandatory update (bypasses session dismissal).
     */
    fun checkForUpdate(isForced: Boolean = false) {
        if (isSessionDismissed && !isForced) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[IN_APP_UPDATE] Skipping check: Update was dismissed for the current session")
            }
            return
        }

        val currentState = _updateState.value
        if (currentState is InAppUpdateState.Downloading || currentState is InAppUpdateState.Downloaded) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[IN_APP_UPDATE] Skipping check: Update is already in progress or downloaded")
            }
            return
        }

        if (isChecking) {
            return
        }

        isChecking = true
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[IN_APP_UPDATE] Update check started")
        }

        try {
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { appUpdateInfo ->
                    isChecking = false
                    handleAppUpdateInfo(appUpdateInfo, isForced)
                }
                .addOnFailureListener { exception ->
                    isChecking = false
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "[IN_APP_UPDATE] Update check failed: ${exception.message}", exception)
                    }
                    // Fail silently for user, remain in current state
                }
        } catch (e: Exception) {
            isChecking = false
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "[IN_APP_UPDATE] Unexpected error checking for update: ${e.message}", e)
            }
        }
    }

    private fun handleAppUpdateInfo(appUpdateInfo: AppUpdateInfo, isForced: Boolean) {
        val availability = appUpdateInfo.updateAvailability()
        val installStatus = appUpdateInfo.installStatus()
        val isFlexibleAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
        val isImmediateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
        val availableVersionCode = appUpdateInfo.availableVersionCode()

        if (installStatus == InstallStatus.DOWNLOADED) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[IN_APP_UPDATE] Update is already downloaded and ready to install")
            }
            _updateState.value = InAppUpdateState.Downloaded
            return
        }

        if (availability == UpdateAvailability.UPDATE_AVAILABLE && (isFlexibleAllowed || isImmediateAllowed)) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[IN_APP_UPDATE] Update available: availableVersionCode=$availableVersionCode, flexible=$isFlexibleAllowed, immediate=$isImmediateAllowed")
            }
            if (!isSessionDismissed || isForced) {
                _updateState.value = InAppUpdateState.UpdateAvailable(
                    appUpdateInfo = appUpdateInfo,
                    isForced = isForced
                )
            }
        } else if (availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[IN_APP_UPDATE] Developer triggered update in progress")
            }
            if (installStatus == InstallStatus.DOWNLOADED) {
                _updateState.value = InAppUpdateState.Downloaded
            }
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[IN_APP_UPDATE] No update available (availability=$availability)")
            }
            if (_updateState.value !is InAppUpdateState.Downloaded && _updateState.value !is InAppUpdateState.Downloading) {
                _updateState.value = InAppUpdateState.Idle
            }
        }
    }

    /**
     * Starts the Google Play In-App Update flow (FLEXIBLE mode).
     */
    fun startFlexibleUpdate(
        activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>,
        appUpdateInfo: AppUpdateInfo
    ) {
        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[IN_APP_UPDATE] Update flow started (type=FLEXIBLE)")
            }
            val updateOptions = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activityResultLauncher,
                updateOptions
            )
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "[IN_APP_UPDATE] Failed to start flexible update flow: ${e.message}", e)
            }
            _updateState.value = InAppUpdateState.Failed("Failed to start update: ${e.message}")
        }
    }

    /**
     * Starts the Google Play In-App Update flow (IMMEDIATE mode for future forced updates).
     */
    fun startImmediateUpdate(
        activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>,
        appUpdateInfo: AppUpdateInfo
    ) {
        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[IN_APP_UPDATE] Update flow started (type=IMMEDIATE)")
            }
            val updateOptions = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activityResultLauncher,
                updateOptions
            )
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "[IN_APP_UPDATE] Failed to start immediate update flow: ${e.message}", e)
            }
            _updateState.value = InAppUpdateState.Failed("Failed to start immediate update: ${e.message}")
        }
    }

    /**
     * Completes a downloaded flexible update by installing it and restarting the application.
     */
    fun completeUpdate() {
        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[IN_APP_UPDATE] Complete update installation requested")
            }
            appUpdateManager.completeUpdate()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "[IN_APP_UPDATE] completeUpdate failed: ${e.message}", e)
            }
        }
    }

    /**
     * Dismisses the update dialog for the current app session when the user taps "Later".
     */
    fun dismissUpdateForSession() {
        isSessionDismissed = true
        if (_updateState.value is InAppUpdateState.UpdateAvailable) {
            _updateState.value = InAppUpdateState.Idle
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[IN_APP_UPDATE] Update dismissed for current session")
        }
    }

    /**
     * Handles Activity result codes from the update flow.
     */
    fun onActivityResult(resultCode: Int) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[IN_APP_UPDATE] Update flow accepted by user")
                }
            }
            Activity.RESULT_CANCELED -> {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[IN_APP_UPDATE] Update flow cancelled by user")
                }
                dismissUpdateForSession()
            }
            ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "[IN_APP_UPDATE] In-app update failed with result code: $resultCode")
                }
                _updateState.value = InAppUpdateState.Failed("Update flow failed")
            }
            else -> {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[IN_APP_UPDATE] Update result code: $resultCode")
                }
            }
        }
    }

    /**
     * Synchronizes update state when the hosting Activity resumes.
     */
    fun onResumeCheck(activity: Activity) {
        try {
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { info ->
                    if (info.installStatus() == InstallStatus.DOWNLOADED) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "[IN_APP_UPDATE] onResumeCheck: Update is downloaded and ready to install")
                        }
                        _updateState.value = InAppUpdateState.Downloaded
                    } else if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "[IN_APP_UPDATE] onResumeCheck: Developer triggered update in progress")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "[IN_APP_UPDATE] onResumeCheck failed: ${e.message}", e)
                    }
                }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "[IN_APP_UPDATE] onResumeCheck unexpected exception: ${e.message}", e)
            }
        }
    }

    /**
     * Unregisters the install listener when tearing down.
     */
    fun unregisterListener() {
        try {
            appUpdateManager.unregisterListener(installStateUpdatedListener)
        } catch (e: Exception) {
            // Ignore
        }
    }

    companion object {
        private const val TAG = "InAppUpdateManager"

        @Volatile
        private var instance: InAppUpdateManager? = null

        fun getInstance(context: Context): InAppUpdateManager {
            return instance ?: synchronized(this) {
                instance ?: InAppUpdateManager(context).also { instance = it }
            }
        }
    }
}
