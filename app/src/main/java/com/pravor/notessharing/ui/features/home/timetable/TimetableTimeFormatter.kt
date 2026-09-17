package com.pravor.notessharing.ui.features.home.timetable

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object TimetableTimeFormatter {

    const val RANGE_SEPARATOR = " – " // En-dash with spaces

    /**
     * Determines whether the user prefers 24-hour time format according to Android device settings.
     */
    fun is24HourFormat(context: Context): Boolean {
        return DateFormat.is24HourFormat(context)
    }

    /**
     * Formats a single point in time (in minutes from midnight) into a locale-aware 12-hour or 24-hour string.
     * Examples:
     * - 12-hour: "09:00 AM", "12:00 PM", "01:00 PM"
     * - 24-hour: "09:00", "12:00", "13:00"
     */
    fun formatTime(
        minutesFromMidnight: Int,
        is24Hour: Boolean,
        locale: Locale = Locale.getDefault()
    ): String {
        val calendar = Calendar.getInstance(locale).apply {
            set(Calendar.HOUR_OF_DAY, minutesFromMidnight / 60)
            set(Calendar.MINUTE, minutesFromMidnight % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
        val sdf = SimpleDateFormat(pattern, locale)
        return sdf.format(calendar.time).uppercase(locale)
    }

    /**
     * Formats a start and end minute range into a clean string.
     * Examples:
     * - 12-hour: "09:00 AM – 10:00 AM"
     * - 24-hour: "09:00 – 10:00"
     */
    fun formatTimeRange(
        startMinutes: Int,
        endMinutes: Int,
        is24Hour: Boolean,
        locale: Locale = Locale.getDefault()
    ): String {
        val startStr = formatTime(startMinutes, is24Hour, locale)
        val endStr = formatTime(endMinutes, is24Hour, locale)
        return "$startStr$RANGE_SEPARATOR$endStr"
    }

    /**
     * Formats a TimetableRowItem based on its internal structured times, falling back to rawTime if needed.
     */
    fun format(
        item: TimetableRowItem,
        is24Hour: Boolean,
        locale: Locale = Locale.getDefault()
    ): String {
        if (item.startTimeMinutes != null && item.endTimeMinutes != null) {
            return formatTimeRange(item.startTimeMinutes, item.endTimeMinutes, is24Hour, locale)
        }
        val parsed = TimetableTimeUtils.parseTimeRange(item.rawTime)
        if (parsed != null) {
            return formatTimeRange(parsed.first, parsed.second, is24Hour, locale)
        }
        return item.rawTime
    }
}
