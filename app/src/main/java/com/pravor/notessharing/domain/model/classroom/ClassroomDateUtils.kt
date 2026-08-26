package com.pravor.notessharing.domain.model.classroom

import java.util.Calendar

object ClassroomDateUtils {

    /**
     * Parses a raw due date string (e.g. "28/2/2026, 23:59" or "28/2/2026") into epoch milliseconds.
     * Returns null if rawDue is missing or unparseable.
     */
    fun parseDueDateTimeToEpochMillis(rawDue: String?): Long? {
        if (rawDue.isNullOrBlank()) return null
        return try {
            val clean = rawDue.removePrefix("Due ").trim()
            val parts = clean.split(",")
            val datePart = parts[0].trim()
            val dmy = datePart.split("/")
            if (dmy.size == 3) {
                val day = dmy[0].toIntOrNull() ?: return null
                val month = dmy[1].toIntOrNull() ?: return null // 1-12
                val year = dmy[2].toIntOrNull() ?: return null

                var hour = 23
                var minute = 59
                var second = 59
                if (parts.size > 1) {
                    val timeParts = parts[1].trim().split(":")
                    if (timeParts.size >= 2) {
                        hour = timeParts[0].trim().toIntOrNull() ?: 23
                        minute = timeParts[1].trim().toIntOrNull() ?: 59
                        second = 0
                    }
                }
                val calendar = Calendar.getInstance()
                calendar.set(year, month - 1, day, hour, minute, second)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats a raw due date string into "Due MMM d, yyyy · h:mm a" cleanly.
     */
    fun formatDueDateTime(rawDue: String?): String? {
        if (rawDue.isNullOrBlank()) return null
        return try {
            val clean = rawDue.removePrefix("Due ").trim()
            val parts = clean.split(",")
            val datePart = parts[0].trim()
            val timePart = if (parts.size > 1) parts[1].trim() else null

            val dmy = datePart.split("/")
            if (dmy.size == 3) {
                val day = dmy[0].toIntOrNull()
                val month = dmy[1].toIntOrNull()
                val year = dmy[2].toIntOrNull()
                if (day != null && month != null && year != null) {
                    val months = arrayOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                    val monthStr = if (month in 1..12) months[month] else month.toString()
                    val formattedDate = "$monthStr $day, $year"

                    val formattedTime = if (timePart != null) {
                        val hm = timePart.split(":")
                        if (hm.size >= 2) {
                            val hours = hm[0].toIntOrNull() ?: 0
                            val mins = hm[1].toIntOrNull() ?: 0
                            val amPm = if (hours >= 12) "PM" else "AM"
                            val h12 = if (hours % 12 == 0) 12 else if (hours > 12) hours - 12 else hours
                            String.format("%d:%02d %s", h12, mins, amPm)
                        } else timePart
                    } else null

                    if (formattedTime != null) {
                        "Due $formattedDate · $formattedTime"
                    } else {
                        "Due $formattedDate"
                    }
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
