package com.pravor.notessharing.data.kaya

import org.jsoup.Jsoup
import java.util.Locale

object KayaTimetableParser {

    /**
     * Parses the student dashboard HTML and extracts all [data-dashboard-tt-chip] timetable elements.
     * Throws [KayaSessionExpiredException] if the HTML represents a redirected login page.
     * Throws [KayaParseException] if no valid timetable elements could be extracted.
     */
    fun parseDashboardHtml(html: String): List<KayaParsedSlot> {
        if (html.isBlank()) {
            throw KayaParseException("Empty dashboard response received from KAYA.")
        }

        val doc = Jsoup.parse(html)

        // Check if the server redirected or returned a login page
        val isLoginPage = doc.select("#loginForm").isNotEmpty() ||
                doc.title().contains("Login", ignoreCase = true) ||
                doc.select("input[name=csrfmiddlewaretoken]").isNotEmpty() && doc.select("[data-dashboard-tt-chip]").isEmpty()

        if (isLoginPage) {
            throw KayaSessionExpiredException("Your KAYA session has expired. Please reconnect.")
        }

        val chips = doc.select("[data-dashboard-tt-chip]")
        if (chips.isEmpty()) {
            throw KayaParseException("No timetable chips found in the KAYA dashboard.")
        }

        val parsedSlots = mutableListOf<KayaParsedSlot>()

        for (chip in chips) {
            val entryId = chip.attr("data-entry-id").trim()
            val courseCode = chip.attr("data-course").trim()
            val rawCourseName = chip.attr("data-course-name").trim()
            val courseName = if (rawCourseName.isNotBlank()) rawCourseName else courseCode

            val section = chip.attr("data-section").trim()
            val sectionShort = chip.attr("data-section-short").trim()
            val faculty = chip.attr("data-faculty").trim()
            val facultyCode = chip.attr("data-faculty-code").trim()
            val rawDay = chip.attr("data-day").trim()
            val day = normalizeDay(rawDay)

            val rawTime = chip.attr("data-time").trim()
            val (period, time) = splitPeriodAndTime(rawTime)

            val rawRoom = chip.attr("data-room").trim()
            val rawFullRoom = chip.attr("data-full-room").trim()
            val entireRoom = (rawFullRoom.ifBlank { rawRoom })
                .replace(Regex("""^(?i)room\s*[-:]*\s*"""), "")
                .trim()
            val type = chip.attr("data-type").trim()

            // Skip entirely empty / corrupt chips
            if (entryId.isBlank() && courseCode.isBlank() && courseName.isBlank()) {
                continue
            }

            val effectiveEntryId = entryId.ifBlank { "${day}_${period}_${courseCode}".hashCode().toString() }
            val slotOrder = calculateSlotOrder(period, time)

            parsedSlots.add(
                KayaParsedSlot(
                    entryId = effectiveEntryId,
                    courseCode = courseCode,
                    courseName = courseName,
                    section = section,
                    sectionShort = sectionShort,
                    faculty = faculty,
                    facultyCode = facultyCode,
                    day = day,
                    period = period,
                    time = time,
                    room = entireRoom,
                    fullRoom = rawFullRoom.ifBlank { rawRoom },
                    type = type,
                    slotOrder = slotOrder
                )
            )
        }

        if (parsedSlots.isEmpty()) {
            throw KayaParseException("Parsed 0 valid timetable slots from dashboard chips.")
        }

        return parsedSlots
    }

    private fun normalizeDay(rawDay: String): String {
        val lower = rawDay.lowercase(Locale.ROOT)
        return when {
            lower.startsWith("mon") -> "Monday"
            lower.startsWith("tue") -> "Tuesday"
            lower.startsWith("wed") -> "Wednesday"
            lower.startsWith("thu") -> "Thursday"
            lower.startsWith("fri") -> "Friday"
            lower.startsWith("sat") -> "Saturday"
            lower.startsWith("sun") -> "Sunday"
            rawDay.isNotBlank() -> rawDay.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            else -> "Monday"
        }
    }

    private fun splitPeriodAndTime(rawTime: String): Pair<String, String> {
        return if (rawTime.contains("|")) {
            val p = rawTime.substringBefore("|").trim()
            val t = rawTime.substringAfter("|").trim()
            Pair(p, t)
        } else {
            Pair("", rawTime)
        }
    }

    private fun calculateSlotOrder(period: String, time: String): Int {
        // Try extracting numeric digit from period (e.g. "P1" -> 1, "P2" -> 2)
        val periodDigits = period.filter { it.isDigit() }.toIntOrNull()
        if (periodDigits != null) return periodDigits

        // Fallback to parsing start hour from time (e.g. "08:00 - 09:00" -> 800)
        val hourMatch = Regex("""(\d{1,2}):(\d{2})""").find(time)
        if (hourMatch != null) {
            val h = hourMatch.groupValues[1].toIntOrNull() ?: 0
            val m = hourMatch.groupValues[2].toIntOrNull() ?: 0
            return h * 60 + m
        }

        return 999
    }
}
