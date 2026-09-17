package com.pravor.notessharing.data.local.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persistent preferences for the Home Screen Timetable section.
 * Stores the dynamically measured height required to display exactly 3 rows clearly,
 * taking into account device-specific font scaling and screen density.
 */
class TimetablePreferences private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "timetable_preferences"
        private const val KEY_3_ROW_HEIGHT_DP = "key_3_row_height_dp_v2"
        private const val KEY_IS_HEIGHT_MEASURED = "key_is_height_measured_v2"
        const val DEFAULT_3_ROW_HEIGHT_DP = 182f

        @Volatile
        private var instance: TimetablePreferences? = null

        fun getInstance(context: Context): TimetablePreferences {
            return instance ?: synchronized(this) {
                instance ?: TimetablePreferences(context).also { instance = it }
            }
        }
    }

    /**
     * Returns true if a dynamically measured 3-row height has already been calculated and stored.
     */
    fun is3RowHeightStored(): Boolean {
        return prefs.getBoolean(KEY_IS_HEIGHT_MEASURED, false)
    }

    /**
     * Gets the stored 3-row height in DP, defaulting to [DEFAULT_3_ROW_HEIGHT_DP] if not yet measured.
     */
    fun getStored3RowHeight(): Float {
        return prefs.getFloat(KEY_3_ROW_HEIGHT_DP, DEFAULT_3_ROW_HEIGHT_DP)
    }

    /**
     * Persists the dynamically measured 3-row height in DP so that future app cold starts
     * render the exact required size instantly with zero layout jumps.
     */
    fun store3RowHeight(heightDp: Float) {
        if (heightDp > 50f) {
            prefs.edit()
                .putFloat(KEY_3_ROW_HEIGHT_DP, heightDp)
                .putBoolean(KEY_IS_HEIGHT_MEASURED, true)
                .apply()
        }
    }
}
