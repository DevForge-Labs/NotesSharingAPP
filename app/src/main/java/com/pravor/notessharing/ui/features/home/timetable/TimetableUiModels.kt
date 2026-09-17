package com.pravor.notessharing.ui.features.home.timetable

import androidx.compose.runtime.Immutable

@Immutable
data class TimetableRowItem(
    val id: String,
    val subjectName: String,
    val rawTime: String,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val period: String? = null,
    val room: String? = null,
    val type: String? = null,
    val faculty: String? = null,
    val day: String = "Monday",
    val isCurrentClass: Boolean = false
) {
    /**
     * Secondary constructor for backwards compatibility with call sites passing `time`.
     */
    constructor(
        id: String,
        subjectName: String,
        time: String,
        period: String? = null,
        room: String? = null,
        type: String? = null,
        faculty: String? = null,
        day: String = "Monday",
        isCurrentClass: Boolean = false,
        startTimeMinutes: Int? = null,
        endTimeMinutes: Int? = null
    ) : this(
        id = id,
        subjectName = subjectName,
        rawTime = time,
        startTimeMinutes = startTimeMinutes,
        endTimeMinutes = endTimeMinutes,
        period = period,
        room = room,
        type = type,
        faculty = faculty,
        day = day,
        isCurrentClass = isCurrentClass
    )

    /**
     * Formats this row's time range dynamically based on 12-hour or 24-hour clock preference.
     */
    fun formattedTime(is24Hour: Boolean, locale: java.util.Locale = java.util.Locale.getDefault()): String {
        return TimetableTimeFormatter.format(this, is24Hour, locale)
    }

    /**
     * Backwards-compatible accessor for existing code that reads `.time`.
     */
    val time: String
        get() = rawTime
}

sealed interface TimetableSectionUiState {
    /**
     * First-time / empty state when no KAYA account is connected.
     * Shows the "Connect your KAYA account" CTA.
     */
    data object NotConnected : TimetableSectionUiState

    /**
     * Subtle non-blocking loading state when KAYA connection or sync is being attempted.
     */
    data object Connecting : TimetableSectionUiState

    /**
     * Timetable entries are available and rendered.
     */
    data class Success(
        val entries: List<TimetableRowItem>,
        val allEntries: List<TimetableRowItem> = entries,
        val selectedDay: String = "Monday",
        val availableDays: List<String> = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"),
        val isSyncing: Boolean = false,
        val isSessionExpired: Boolean = false
    ) : TimetableSectionUiState {
        fun entriesForDay(day: String): List<TimetableRowItem> {
            return allEntries.filter { it.day.equals(day, ignoreCase = true) }
        }
    }

    /**
     * Connection or authentication failure state with retry CTA.
     */
    data class Error(
        val message: String
    ) : TimetableSectionUiState
}
