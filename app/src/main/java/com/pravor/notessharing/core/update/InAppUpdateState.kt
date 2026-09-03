package com.pravor.notessharing.core.update

import com.google.android.play.core.appupdate.AppUpdateInfo

/**
 * Represents the observable UI / lifecycle state of the Google Play In-App Updates system.
 */
sealed class InAppUpdateState {
    /**
     * Default initial state: no active update flow or check in progress.
     */
    data object Idle : InAppUpdateState()

    /**
     * An update is available on Google Play.
     * @param appUpdateInfo Play Core metadata containing update availability, version, and supported types.
     * @param isForced Set to true if future policy requires a mandatory/immediate update.
     */
    data class UpdateAvailable(
        val appUpdateInfo: AppUpdateInfo,
        val isForced: Boolean = false
    ) : InAppUpdateState()

    /**
     * A flexible update is actively downloading in the background.
     * @param bytesDownloaded Total bytes downloaded so far.
     * @param totalBytesToDownload Total expected size of the update in bytes.
     */
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytesToDownload: Long
    ) : InAppUpdateState()

    /**
     * A flexible update has finished downloading and is ready to be installed via completeUpdate().
     */
    data object Downloaded : InAppUpdateState()

    /**
     * The update check or installation flow encountered a non-fatal failure.
     * @param message Human-readable error description for debugging.
     */
    data class Failed(val message: String) : InAppUpdateState()
}
