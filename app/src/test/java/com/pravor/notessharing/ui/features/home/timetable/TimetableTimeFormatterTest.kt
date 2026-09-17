package com.pravor.notessharing.ui.features.home.timetable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class TimetableTimeFormatterTest {

    private val testLocale = Locale.US

    @Test
    fun testFormatTimeRange_12HourClock() {
        // 09:00 AM to 10:00 AM
        val morning = TimetableTimeFormatter.formatTimeRange(
            startMinutes = 540,
            endMinutes = 600,
            is24Hour = false,
            locale = testLocale
        )
        assertEquals("09:00 AM – 10:00 AM", morning)

        // 10:00 AM to 11:00 AM
        val lateMorning = TimetableTimeFormatter.formatTimeRange(
            startMinutes = 600,
            endMinutes = 660,
            is24Hour = false,
            locale = testLocale
        )
        assertEquals("10:00 AM – 11:00 AM", lateMorning)

        // 12:00 PM to 01:00 PM (noon transition)
        val afternoon = TimetableTimeFormatter.formatTimeRange(
            startMinutes = 720,
            endMinutes = 780,
            is24Hour = false,
            locale = testLocale
        )
        assertEquals("12:00 PM – 01:00 PM", afternoon)

        // Midnight to 01:00 AM
        val midnight = TimetableTimeFormatter.formatTimeRange(
            startMinutes = 0,
            endMinutes = 60,
            is24Hour = false,
            locale = testLocale
        )
        assertEquals("12:00 AM – 01:00 AM", midnight)
    }

    @Test
    fun testFormatTimeRange_24HourClock() {
        // 09:00 to 10:00
        val morning = TimetableTimeFormatter.formatTimeRange(
            startMinutes = 540,
            endMinutes = 600,
            is24Hour = true,
            locale = testLocale
        )
        assertEquals("09:00 – 10:00", morning)

        // 10:00 to 11:00
        val lateMorning = TimetableTimeFormatter.formatTimeRange(
            startMinutes = 600,
            endMinutes = 660,
            is24Hour = true,
            locale = testLocale
        )
        assertEquals("10:00 – 11:00", lateMorning)

        // 12:00 to 13:00
        val afternoon = TimetableTimeFormatter.formatTimeRange(
            startMinutes = 720,
            endMinutes = 780,
            is24Hour = true,
            locale = testLocale
        )
        assertEquals("12:00 – 13:00", afternoon)

        // 00:00 to 01:00
        val midnight = TimetableTimeFormatter.formatTimeRange(
            startMinutes = 0,
            endMinutes = 60,
            is24Hour = true,
            locale = testLocale
        )
        assertEquals("00:00 – 01:00", midnight)
    }

    @Test
    fun testFormatTimetableRowItem_dynamicAdapting() {
        val item = TimetableRowItem(
            id = "1",
            subjectName = "Software Engineering",
            rawTime = "09:00 - 10:00",
            startTimeMinutes = 540,
            endTimeMinutes = 600,
            day = "Monday"
        )

        assertEquals("09:00 AM – 10:00 AM", item.formattedTime(is24Hour = false))
        assertEquals("09:00 – 10:00", item.formattedTime(is24Hour = true))
    }

    @Test
    fun testFormatTimetableRowItem_fallbackToParsingRawTime() {
        val item = TimetableRowItem(
            id = "2",
            subjectName = "Computer Networks",
            rawTime = "12:00 - 13:00",
            startTimeMinutes = null,
            endTimeMinutes = null,
            day = "Monday"
        )

        assertEquals("12:00 PM – 01:00 PM", item.formattedTime(is24Hour = false))
        assertEquals("12:00 – 13:00", item.formattedTime(is24Hour = true))
    }

    @Test
    fun testActiveClassDetection_independentOfFormat() {
        val item = TimetableRowItem(
            id = "3",
            subjectName = "Operating Systems",
            rawTime = "09:00 - 10:00",
            startTimeMinutes = 540,
            endTimeMinutes = 600,
            day = "Monday"
        )

        // Active at 09:30 (570 min)
        val isActiveDuringClass = TimetableTimeUtils.isClassActive(
            item = item,
            selectedDay = "Monday",
            currentDay = "Monday",
            currentMinutes = 570
        )
        assertTrue(isActiveDuringClass)

        // Inactive at 10:05 (605 min)
        val isInactiveAfterClass = TimetableTimeUtils.isClassActive(
            item = item,
            selectedDay = "Monday",
            currentDay = "Monday",
            currentMinutes = 605
        )
        assertFalse(isInactiveAfterClass)

        // Inactive when viewing another day
        val isInactiveAnotherDay = TimetableTimeUtils.isClassActive(
            item = item,
            selectedDay = "Tuesday",
            currentDay = "Monday",
            currentMinutes = 570
        )
        assertFalse(isInactiveAnotherDay)
    }
}
