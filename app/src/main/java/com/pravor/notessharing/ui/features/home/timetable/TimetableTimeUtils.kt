package com.pravor.notessharing.ui.features.home.timetable

import java.util.Calendar
import java.util.Locale

object TimetableTimeUtils {

    private val TIME_RANGE_REGEX = Regex(
        """(\d{1,2}):(\d{2})\s*(?:[-–—]|to)\s*(\d{1,2}):(\d{2})""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Parses a timetable slot time string (e.g. "09:00 - 10:00", "09:00–10:00", "9:00 to 10:00")
     * into start and end minutes from midnight: Pair(startMinutes, endMinutes).
     *
     * Returns null if the format is invalid or end time does not come after start time.
     */
    fun parseTimeRange(timeStr: String): Pair<Int, Int>? {
        if (timeStr.isBlank()) return null

        val match = TIME_RANGE_REGEX.find(timeStr) ?: return null
        val startH = match.groupValues[1].toIntOrNull() ?: return null
        val startM = match.groupValues[2].toIntOrNull() ?: return null
        val endH = match.groupValues[3].toIntOrNull() ?: return null
        val endM = match.groupValues[4].toIntOrNull() ?: return null

        if (startH !in 0..23 || startM !in 0..59 || endH !in 0..23 || endM !in 0..59) {
            return null
        }

        val startMinutes = startH * 60 + startM
        val endMinutes = endH * 60 + endM

        if (endMinutes <= startMinutes) {
            return null
        }

        return Pair(startMinutes, endMinutes)
    }

    /**
     * Normalizes a day name to standard full name (e.g. "mon" -> "Monday").
     */
    fun normalizeDay(day: String): String {
        val lower = day.trim().lowercase(Locale.ROOT)
        return when {
            lower.startsWith("mon") -> "Monday"
            lower.startsWith("tue") -> "Tuesday"
            lower.startsWith("wed") -> "Wednesday"
            lower.startsWith("thu") -> "Thursday"
            lower.startsWith("fri") -> "Friday"
            lower.startsWith("sat") -> "Saturday"
            lower.startsWith("sun") -> "Sunday"
            else -> day.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
    }

    /**
     * Returns true if two day representations refer to the same day of the week.
     */
    fun isSameDay(dayA: String, dayB: String): Boolean {
        if (dayA.isBlank() || dayB.isBlank()) return false
        return normalizeDay(dayA).equals(normalizeDay(dayB), ignoreCase = true)
    }

    /**
     * Determines whether a class is currently active.
     *
     * Conditions:
     * 1. The selected tab must match today (if user is viewing another day, no class is active).
     * 2. The entry's day must match today.
     * 3. currentMinutes must be >= classStartTime and < classEndTime.
     */
    fun isClassActive(
        timeString: String,
        entryDay: String,
        selectedDay: String,
        currentDay: String,
        currentMinutes: Int
    ): Boolean {
        if (!isSameDay(selectedDay, currentDay)) return false
        if (!isSameDay(entryDay, currentDay)) return false

        val range = parseTimeRange(timeString) ?: return false
        return currentMinutes >= range.first && currentMinutes < range.second
    }

    /**
     * Determines whether a class is currently active using structured start/end minutes from midnight.
     */
    fun isClassActive(
        startMinutes: Int,
        endMinutes: Int,
        entryDay: String,
        selectedDay: String,
        currentDay: String,
        currentMinutes: Int
    ): Boolean {
        if (!isSameDay(selectedDay, currentDay)) return false
        if (!isSameDay(entryDay, currentDay)) return false
        return currentMinutes >= startMinutes && currentMinutes < endMinutes
    }

    /**
     * Determines whether a TimetableRowItem is currently active, prioritizing structured times.
     */
    fun isClassActive(
        item: TimetableRowItem,
        selectedDay: String,
        currentDay: String,
        currentMinutes: Int
    ): Boolean {
        if (item.startTimeMinutes != null && item.endTimeMinutes != null) {
            return isClassActive(
                startMinutes = item.startTimeMinutes,
                endMinutes = item.endTimeMinutes,
                entryDay = item.day,
                selectedDay = selectedDay,
                currentDay = currentDay,
                currentMinutes = currentMinutes
            )
        }
        val range = parseTimeRange(item.rawTime) ?: return false
        return isClassActive(
            startMinutes = range.first,
            endMinutes = range.second,
            entryDay = item.day,
            selectedDay = selectedDay,
            currentDay = currentDay,
            currentMinutes = currentMinutes
        )
    }

    /**
     * Returns the current day name (e.g. "Monday", "Tuesday") from a Calendar instance in device local timezone.
     */
    fun getCurrentDayName(calendar: Calendar = Calendar.getInstance()): String {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Monday"
        }
    }

    /**
     * Returns the current minutes from midnight (0..1439) in device local timezone.
     */
    fun getCurrentMinutes(calendar: Calendar = Calendar.getInstance()): Int {
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    /**
     * Calculates the initial visible list index for a day's timetable so that:
     * - Only today's list is positioned based on the current clock / active class.
     * - Other days always start at the top (index 0).
     * - If the active class is in the first 3 rows, starts at index 0.
     * - If the active class is further down, initializes directly with the active class visible at the top,
     *   bounded by maxTopIndex so the list does not scroll past the end.
     * - Before the first class: starts at top (index 0).
     * - After the last class: shows the end of the timetable naturally.
     * - Between classes (break): positions at the upcoming class.
     */
    fun calculateInitialListIndexForDay(
        day: String,
        entries: List<TimetableRowItem>,
        currentDay: String = getCurrentDayName(),
        currentMinutes: Int = getCurrentMinutes()
    ): Int {
        if (entries.isEmpty()) return 0

        // Only today's list is positioned based on current clock / active class
        if (!isSameDay(day, currentDay)) {
            return 0
        }

        val maxTopIndex = (entries.size - 3).coerceAtLeast(0)

        // 1. Check for currently active class
        val activeIndex = entries.indexOfFirst {
            it.isCurrentClass || isClassActive(it, day, currentDay, currentMinutes)
        }
        if (activeIndex >= 0) {
            // Already in first 3 rows: start at the top
            if (activeIndex < 3) return 0
            return activeIndex.coerceAtMost(maxTopIndex)
        }

        // 2. Edge cases when no class is currently active
        val firstClassStart = entries.first().startTimeMinutes
        val lastClassEnd = entries.last().endTimeMinutes

        // Before the first class: start at top
        if (firstClassStart != null && currentMinutes < firstClassStart) {
            return 0
        }

        // After the final class: show end of timetable
        if (lastClassEnd != null && currentMinutes >= lastClassEnd) {
            return maxTopIndex
        }

        // In-between classes (e.g. break): position at upcoming class
        val nextIndex = entries.indexOfFirst { (it.startTimeMinutes ?: 0) > currentMinutes }
        if (nextIndex >= 0) {
            if (nextIndex < 3) return 0
            return nextIndex.coerceAtMost(maxTopIndex)
        }

        return 0
    }
}
