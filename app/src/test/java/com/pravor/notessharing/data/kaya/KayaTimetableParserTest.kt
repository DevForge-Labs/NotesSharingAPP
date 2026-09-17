package com.pravor.notessharing.data.kaya

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class KayaTimetableParserTest {

    @Test
    fun testSuccessfulParsingOfChips() {
        val html = """
            <html>
            <head><title>Student Dashboard | KAYA</title></head>
            <body>
                <div class="timetable-grid">
                    <div data-dashboard-tt-chip
                         data-entry-id="entry-101"
                         data-course="CS3001"
                         data-course-name="Operating Systems"
                         data-section="CSE-3"
                         data-section-short="3"
                         data-faculty="Dr. John Doe"
                         data-faculty-code="JD01"
                         data-day="mon"
                         data-time="P1 | 08:00 - 09:00"
                         data-room="LH-101"
                         data-full-room="Lecture Hall 101"
                         data-type="Theory">
                         Operating Systems
                    </div>
                    <div data-dashboard-tt-chip
                         data-entry-id="entry-102"
                         data-course="CS3002"
                         data-course-name="Database Management"
                         data-section="CSE-3"
                         data-section-short="3"
                         data-faculty="Prof. Jane Smith"
                         data-faculty-code="JS02"
                         data-day="Tuesday"
                         data-time="P2 | 09:05 - 10:05"
                         data-room="LH-102"
                         data-full-room="Lecture Hall 102"
                         data-type="Lab">
                         Database Management
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val slots = KayaTimetableParser.parseDashboardHtml(html)

        assertEquals(2, slots.size)

        val slot1 = slots[0]
        assertEquals("entry-101", slot1.entryId)
        assertEquals("CS3001", slot1.courseCode)
        assertEquals("Operating Systems", slot1.courseName)
        assertEquals("Monday", slot1.day)
        assertEquals("P1", slot1.period)
        assertEquals("08:00 - 09:00", slot1.time)
        assertEquals("Lecture Hall 101", slot1.room)
        assertEquals("Theory", slot1.type)
        assertEquals("Dr. John Doe", slot1.faculty)
        assertEquals(1, slot1.slotOrder)

        val slot2 = slots[1]
        assertEquals("entry-102", slot2.entryId)
        assertEquals("CS3002", slot2.courseCode)
        assertEquals("Database Management", slot2.courseName)
        assertEquals("Tuesday", slot2.day)
        assertEquals("P2", slot2.period)
        assertEquals("09:05 - 10:05", slot2.time)
        assertEquals("Lecture Hall 102", slot2.room)
        assertEquals("Lab", slot2.type)
        assertEquals(2, slot2.slotOrder)
    }

    @Test
    fun testEntireRoomExtractionWithoutRoomPrefix() {
        val html = """
            <div data-dashboard-tt-chip
                 data-entry-id="entry-room-test"
                 data-course="CS1234"
                 data-course-name="Algorithms"
                 data-day="Thu"
                 data-time="P2 | 09:00 - 10:00"
                 data-room="B122"
                 data-full-room="Room C25-B122"
                 data-type="CORE">
            </div>
        """.trimIndent()

        val slots = KayaTimetableParser.parseDashboardHtml(html)
        assertEquals(1, slots.size)
        val slot = slots[0]
        // Entire room is fetched, and "Room " string is dropped!
        assertEquals("C25-B122", slot.room)
    }

    @Test
    fun testSessionExpiredDetectionWithLoginForm() {
        val loginHtml = """
            <html>
            <head><title>Login | KAYA</title></head>
            <body>
                <form id="loginForm" method="post" action="/login/">
                    <input type="hidden" name="csrfmiddlewaretoken" value="dummy_csrf_token" />
                    <input type="text" name="username" />
                    <input type="password" name="password" />
                </form>
            </body>
            </html>
        """.trimIndent()

        try {
            KayaTimetableParser.parseDashboardHtml(loginHtml)
            fail("Expected KayaSessionExpiredException was not thrown")
        } catch (e: KayaSessionExpiredException) {
            assertTrue(e.message?.contains("expired", ignoreCase = true) == true)
        }
    }

    @Test
    fun testEmptyHtmlThrowsParseException() {
        try {
            KayaTimetableParser.parseDashboardHtml("")
            fail("Expected KayaParseException for empty HTML")
        } catch (e: KayaParseException) {
            assertNotNull(e.message)
        }
    }

    @Test
    fun testNoChipsThrowsParseException() {
        val emptyDashboard = """
            <html>
            <head><title>Dashboard</title></head>
            <body><div>Welcome, Student!</div></body>
            </html>
        """.trimIndent()

        try {
            KayaTimetableParser.parseDashboardHtml(emptyDashboard)
            fail("Expected KayaParseException when no chips exist")
        } catch (e: KayaParseException) {
            assertTrue(e.message?.contains("No timetable chips", ignoreCase = true) == true)
        }
    }

    @Test
    fun testFallbackCourseCodeWhenNameBlank() {
        val html = """
            <div data-dashboard-tt-chip
                 data-entry-id="entry-fallback"
                 data-course="CS9999"
                 data-course-name=""
                 data-day="wed"
                 data-time="11:00 - 12:00"
                 data-room=""
                 data-full-room="Lab 4"
                 data-type="Practical">
            </div>
        """.trimIndent()

        val slots = KayaTimetableParser.parseDashboardHtml(html)
        assertEquals(1, slots.size)
        val slot = slots[0]
        assertEquals("CS9999", slot.courseName)
        assertEquals("Wednesday", slot.day)
        assertEquals("", slot.period)
        assertEquals("11:00 - 12:00", slot.time)
        assertEquals("Lab 4", slot.room)
        assertEquals(660, slot.slotOrder)
    }
}
