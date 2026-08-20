package com.pravor.notessharing.core.util


import com.pravor.notessharing.core.config.DeveloperConfig
import java.util.concurrent.ConcurrentHashMap

/**
 * Centralized manager to enforce a pull-to-refresh cooldown across the app.
 * Keeps track of the last successful refresh timestamp to protect backend resources.
 */
object RefreshCooldownManager {
    // Thread-safe map to store the last successful refresh timestamp for each screen/context key
    private val lastRefreshTimestamps = ConcurrentHashMap<String, Long>()

    /**
     * Checks if a refresh is allowed for the given [key].
     * If allowed, updates the last refresh timestamp to current time and returns true.
     * If blocked due to cooldown, returns false.
     */
    fun tryRefresh(key: String): Boolean {
        if (DeveloperConfig.DISABLE_REFRESH_COOLDOWN) {
            return true
        }

        val currentTime = System.currentTimeMillis()
        val lastRefreshTime = lastRefreshTimestamps[key] ?: 0L
        val cooldownMs = DeveloperConfig.REFRESH_COOLDOWN_MS

        return if (currentTime - lastRefreshTime >= cooldownMs) {
            lastRefreshTimestamps[key] = currentTime
            true
        } else {
            false
        }
    }

    /**
     * Helper to run [action] only if the refresh cooldown for [key] has elapsed.
     */
    inline fun runWithCooldown(key: String, crossinline action: () -> Unit) {
        if (tryRefresh(key)) {
            action()
        }
    }
}
