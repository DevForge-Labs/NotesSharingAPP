package com.pravor.notessharing.ui.features.home.timetable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableTimeUtilsTest {

    @Test
    fun testParseTimeRange_standardHyphen() {
        val result = TimetableTimeUtils.parseTimeRange("09:00 - 10:00")
        assertNotNull(result)
        assertEquals(540, result!!.first)  // 9 * 60
        assertEquals(600, result.second) // 10 * 60
    }

    @Test
    fun testParseTimeRange_enDashAndEmDash() {
        val enDashResult = TimetableTimeUtils.parseTimeRange("09:00–10:00")
        assertNotNull(enDashResult)
        assertEquals(540, enDashResult!!.first)
        assertEquals(600, enDashResult.second)

        val emDashResult = TimetableTimeUtils.parseTimeRange("08:15—09:45")
        assertNotNull(emDashResult)
        assertEquals(495, emDashResult!!.first)
        assertEquals(585, emDashResult.second)
    }

    @Test
    fun testParseTimeRange_toKeyword() {
        val result = TimetableTimeUtils.parseTimeRange("9:00 to 11:00")
        assertNotNull(result)
        assertEquals(540, result!!.first)
        assertEquals(660, result.second)
    }

    @Test
    fun testParseTimeRange_invalidOrInvertedRanges() {
        assertNull(TimetableTimeUtils.parseTimeRange(""))
        assertNull(TimetableTimeUtils.parseTimeRange("invalid"))
        // Inverted time (end before start)
        assertNull(TimetableTimeUtils.parseTimeRange("10:00 - 09:00"))
        // Equal start and end
        assertNull(TimetableTimeUtils.parseTimeRange("09:00 - 09:00"))
        // Invalid hour
        assertNull(TimetableTimeUtils.parseTimeRange("25:00 - 26:00"))
    }

    @Test
    fun testIsClassActive_exactMinuteBoundaries() {
        val timeStr = "09:00 - 10:00"

        // 1 minute before start -> inactive
        assertFalse(
            TimetableTimeUtils.isClassActive(
                timeString = timeStr,
                entryDay = "Monday",
                selectedDay = "Monday",
                currentDay = "Monday",
                currentMinutes = 539 // 08:59
            )
        )

        // Exact start time -> active
        assertTrue(
            TimetableTimeUtils.isClassActive(
                timeString = timeStr,
                entryDay = "Monday",
                selectedDay = "Monday",
                currentDay = "Monday",
                currentMinutes = 540 // 09:00
            )
        )

        // Mid-class -> active
        assertTrue(
            TimetableTimeUtils.isClassActive(
                timeString = timeStr,
                entryDay = "Monday",
                selectedDay = "Monday",
                currentDay = "Monday",
                currentMinutes = 570 // 09:30
            )
        )

        // Last minute of class -> active
        assertTrue(
            TimetableTimeUtils.isClassActive(
                timeString = timeStr,
                entryDay = "Monday",
                selectedDay = "Monday",
                currentDay = "Monday",
                currentMinutes = 599 // 09:59
            )
        )

        // Exact end time -> inactive (next class starts)
        assertFalse(
            TimetableTimeUtils.isClassActive(
                timeString = timeStr,
                entryDay = "Monday",
                selectedDay = "Monday",
                currentDay = "Monday",
                currentMinutes = 600 // 10:00
            )
        )
    }

    @Test
    fun testIsClassActive_selectedDayNotToday_returnsFalse() {
        val timeStr = "09:00 - 10:00"

        // Viewing Tuesday while today is Monday -> false even if class is 9-10
        assertFalse(
            TimetableTimeUtils.isClassActive(
                timeString = timeStr,
                entryDay = "Tuesday",
                selectedDay = "Tuesday",
                currentDay = "Monday",
                currentMinutes = 550
            )
        )
    }

    @Test
    fun testIsClassActive_entryDayNotToday_returnsFalse() {
        val timeStr = "09:00 - 10:00"

        assertFalse(
            TimetableTimeUtils.isClassActive(
                timeString = timeStr,
                entryDay = "Tuesday",
                selectedDay = "Monday",
                currentDay = "Monday",
                currentMinutes = 550
            )
        )
    }

    @Test
    fun testIsClassActive_caseInsensitiveAndShortDays() {
        val timeStr = "09:00 - 10:00"

        assertTrue(
            TimetableTimeUtils.isClassActive(
                timeString = timeStr,
                entryDay = "mon",
                selectedDay = "Monday",
                currentDay = "MONDAY",
                currentMinutes = 550
            )
        )
    }

    @Test
    fun testNavigationSwipeBounds_firstAndLastDays() {
        val availableDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

        // First day: swipe right (previous) has no previous day
        val mondayIndex = availableDays.indexOf("Monday")
        assertTrue(mondayIndex == 0)
        assertFalse(mondayIndex > 0)

        // Middle day: swipe left (next) and swipe right (prev) work
        val wednesdayIndex = availableDays.indexOf("Wednesday")
        assertTrue(wednesdayIndex in 0 until availableDays.lastIndex)
        assertEquals("Thursday", availableDays[wednesdayIndex + 1])
        assertTrue(wednesdayIndex > 0)
        assertEquals("Tuesday", availableDays[wednesdayIndex - 1])

        // Last day: swipe left (next) has no next day
        val saturdayIndex = availableDays.indexOf("Saturday")
        assertTrue(saturdayIndex == availableDays.lastIndex)
        assertFalse(saturdayIndex in 0 until availableDays.lastIndex)
    }

    @Test
    fun testSundayNormalizationAndDetection() {
        assertEquals("Sunday", TimetableTimeUtils.normalizeDay("sun"))
        assertEquals("Sunday", TimetableTimeUtils.normalizeDay("Sunday"))
        assertEquals("Sunday", TimetableTimeUtils.normalizeDay("SUN"))
        assertTrue(TimetableTimeUtils.isSameDay("Sunday", "sun"))
        assertFalse(TimetableTimeUtils.isSameDay("Sunday", "Monday"))
    }

    @Test
    fun testCalculateInitialListIndexForDay_allCases() {
        val entries = listOf(
            TimetableRowItem(id = "1", subjectName = "CN", rawTime = "08:00 - 09:00", startTimeMinutes = 480, endTimeMinutes = 540, day = "Monday"),
            TimetableRowItem(id = "2", subjectName = "CNL 1", rawTime = "09:00 - 10:00", startTimeMinutes = 540, endTimeMinutes = 600, day = "Monday"),
            TimetableRowItem(id = "3", subjectName = "CNL 2", rawTime = "10:00 - 11:00", startTimeMinutes = 600, endTimeMinutes = 660, day = "Monday"),
            TimetableRowItem(id = "4", subjectName = "DAA", rawTime = "11:00 - 12:00", startTimeMinutes = 660, endTimeMinutes = 720, day = "Monday"),
            TimetableRowItem(id = "5", subjectName = "IPA", rawTime = "12:00 - 13:00", startTimeMinutes = 720, endTimeMinutes = 780, day = "Monday"),
            TimetableRowItem(id = "6", subjectName = "SVP", rawTime = "13:00 - 14:00", startTimeMinutes = 780, endTimeMinutes = 840, day = "Monday")
        )

        // 1. Different day -> always 0
        val tuesdayIndex = TimetableTimeUtils.calculateInitialListIndexForDay(
            day = "Tuesday",
            entries = entries,
            currentDay = "Monday",
            currentMinutes = 680 // 11:20
        )
        assertEquals(0, tuesdayIndex)

        // 2. Before first class (07:30 = 450) -> start at 0
        val beforeFirst = TimetableTimeUtils.calculateInitialListIndexForDay(
            day = "Monday",
            entries = entries,
            currentDay = "Monday",
            currentMinutes = 450
        )
        assertEquals(0, beforeFirst)

        // 3. Current class in first 3 rows (09:30 = 570) -> start at 0
        val inFirst3 = TimetableTimeUtils.calculateInitialListIndexForDay(
            day = "Monday",
            entries = entries,
            currentDay = "Monday",
            currentMinutes = 570
        )
        assertEquals(0, inFirst3)

        // 4. Current class at index 3 (DAA at 11:20 = 680) -> start at 3
        val atIndex3 = TimetableTimeUtils.calculateInitialListIndexForDay(
            day = "Monday",
            entries = entries,
            currentDay = "Monday",
            currentMinutes = 680
        )
        assertEquals(3, atIndex3)

        // 5. Current class at index 4 (IPA at 12:30 = 750) -> bounded by maxTopIndex (6 - 3 = 3)
        val atIndex4 = TimetableTimeUtils.calculateInitialListIndexForDay(
            day = "Monday",
            entries = entries,
            currentDay = "Monday",
            currentMinutes = 750
        )
        assertEquals(3, atIndex4)

        // 6. After final class (15:00 = 900) -> end of timetable (index 3)
        val afterLast = TimetableTimeUtils.calculateInitialListIndexForDay(
            day = "Monday",
            entries = entries,
            currentDay = "Monday",
            currentMinutes = 900
        )
        assertEquals(3, afterLast)

        // 7. Small list with <= 3 entries -> always 0
        val smallEntries = entries.take(2)
        val smallIndex = TimetableTimeUtils.calculateInitialListIndexForDay(
            day = "Monday",
            entries = smallEntries,
            currentDay = "Monday",
            currentMinutes = 570
        )
        assertEquals(0, smallIndex)
    }
}
