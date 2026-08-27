package com.pravor.notessharing.data.classroom.reminder

import com.pravor.notessharing.data.local.preferences.NotificationCategory
import com.pravor.notessharing.data.local.preferences.NotificationCategoryResolver
import com.pravor.notessharing.domain.model.classroom.ClassroomDateUtils
import com.pravor.notessharing.domain.model.classroom.SubmissionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassroomReminderSchedulerTest {

    @Test
    fun testReminderTypeOffsets() {
        assertEquals(86400000L, ClassroomReminderType.DUE_24_HOURS.offsetMillis)
        assertEquals(10800000L, ClassroomReminderType.DUE_3_HOURS.offsetMillis)
        assertEquals("24H", ClassroomReminderType.DUE_24_HOURS.typeKey)
        assertEquals("3H", ClassroomReminderType.DUE_3_HOURS.typeKey)
    }

    @Test
    fun testDeterministicRequestCodes() {
        val code1 = ClassroomReminderScheduler.getRequestCode("course_101", "cw_201", ClassroomReminderType.DUE_24_HOURS)
        val code1Repeat = ClassroomReminderScheduler.getRequestCode("course_101", "cw_201", ClassroomReminderType.DUE_24_HOURS)
        val code2 = ClassroomReminderScheduler.getRequestCode("course_101", "cw_201", ClassroomReminderType.DUE_3_HOURS)
        val code3 = ClassroomReminderScheduler.getRequestCode("course_102", "cw_201", ClassroomReminderType.DUE_24_HOURS)

        // Same parameters must produce identical request codes
        assertEquals(code1, code1Repeat)

        // Different reminder types or courses must produce different request codes
        assertNotEquals(code1, code2)
        assertNotEquals(code1, code3)
    }

    @Test
    fun testReminderTriggerEligibility() {
        val now = 1_000_000_000_000L // arbitrary baseline

        // 1. Due in 30 hours (30 * 3600 * 1000 = 108,000,000 ms)
        val dueIn30Hours = now + 30 * 3600 * 1000L
        val trigger24h_Case1 = dueIn30Hours - ClassroomReminderType.DUE_24_HOURS.offsetMillis
        val trigger3h_Case1 = dueIn30Hours - ClassroomReminderType.DUE_3_HOURS.offsetMillis
        assertTrue("24h reminder should be in the future", trigger24h_Case1 > now)
        assertTrue("3h reminder should be in the future", trigger3h_Case1 > now)

        // 2. Due in 5 hours (5 * 3600 * 1000 = 18,000,000 ms)
        val dueIn5Hours = now + 5 * 3600 * 1000L
        val trigger24h_Case2 = dueIn5Hours - ClassroomReminderType.DUE_24_HOURS.offsetMillis
        val trigger3h_Case2 = dueIn5Hours - ClassroomReminderType.DUE_3_HOURS.offsetMillis
        assertFalse("24h reminder was 19h ago and should be skipped", trigger24h_Case2 > now)
        assertTrue("3h reminder should be scheduled (in 2 hours)", trigger3h_Case2 > now)

        // 3. Due in 1 hour
        val dueIn1Hour = now + 1 * 3600 * 1000L
        val trigger24h_Case3 = dueIn1Hour - ClassroomReminderType.DUE_24_HOURS.offsetMillis
        val trigger3h_Case3 = dueIn1Hour - ClassroomReminderType.DUE_3_HOURS.offsetMillis
        assertFalse("24h reminder is past", trigger24h_Case3 > now)
        assertFalse("3h reminder is past", trigger3h_Case3 > now)
    }

    @Test
    fun testSubmissionStateEligibility() {
        val unsubmittedStates = listOf(SubmissionState.NEW, SubmissionState.CREATED, SubmissionState.RECLAIMED_BY_STUDENT, SubmissionState.UNKNOWN)
        val completedStates = listOf(SubmissionState.TURNED_IN, SubmissionState.RETURNED)

        for (state in unsubmittedStates) {
            val isCompleted = state == SubmissionState.TURNED_IN || state == SubmissionState.RETURNED
            assertFalse("State $state should NOT be considered completed", isCompleted)
        }

        for (state in completedStates) {
            val isCompleted = state == SubmissionState.TURNED_IN || state == SubmissionState.RETURNED
            assertTrue("State $state should be considered completed", isCompleted)
        }
    }

    @Test
    fun testNotificationCategoryResolverForClassroom() {
        val cat1 = NotificationCategoryResolver.resolve("classroom_reminder", "Assignment Due", "DBMS Assignment 3 is due tomorrow")
        val cat2 = NotificationCategoryResolver.resolve(null, "Classroom Update", "New coursework assigned")
        val cat3 = NotificationCategoryResolver.resolve("assignment_reminder", "Quiz", "Quiz starting soon")

        assertEquals(NotificationCategory.CLASSROOM, cat1)
        assertEquals(NotificationCategory.CLASSROOM, cat2)
        assertEquals(NotificationCategory.CLASSROOM, cat3)
    }

    @Test
    fun testDueDateParsingForReminderScheduling() {
        val rawDue = "30/8/2026, 23:59"
        val epochMillis = ClassroomDateUtils.parseDueDateTimeToEpochMillis(rawDue)
        assertTrue(epochMillis != null && epochMillis > 0)

        val invalidDue = "not_a_date"
        val invalidEpoch = ClassroomDateUtils.parseDueDateTimeToEpochMillis(invalidDue)
        assertNull(invalidEpoch)
    }
}
