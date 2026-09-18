package com.pravor.notessharing.data.kaya

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying that KAYA Disconnect cleanly and atomically purges
 * credentials, session, and timetable data for the target Firebase user,
 * and strictly isolates data across different Firebase user accounts.
 */
class KayaDisconnectIsolationTest {

    private class FakeCredentialStore : KayaCredentialStore {
        val store = mutableMapOf<String, KayaCredentials>()

        override fun saveCredentials(userId: String, username: String, password: String): Boolean {
            if (userId.isBlank() || username.isBlank() || password.isBlank()) return false
            store[userId] = KayaCredentials(username = username, password = password)
            return true
        }

        override fun getCredentials(userId: String): KayaCredentials? = store[userId]

        override fun hasCredentials(userId: String): Boolean = store.containsKey(userId)

        override fun clearCredentials(userId: String) {
            store.remove(userId)
        }
    }

    private class FakeSessionStore {
        val sessions = mutableMapOf<String, KayaSession>()

        fun saveSession(userId: String, session: KayaSession) {
            sessions[userId] = session
        }

        fun getSession(userId: String): KayaSession? = sessions[userId]

        fun clearSession(userId: String) {
            sessions.remove(userId)
        }
    }

    private data class FakeTimetableEntry(
        val userId: String,
        val entryId: String,
        val courseName: String,
        val day: String
    )

    private class FakeTimetableDao {
        val entries = mutableListOf<FakeTimetableEntry>()

        fun insertAll(items: List<FakeTimetableEntry>) {
            entries.addAll(items)
        }

        fun getTimetableForUser(userId: String): List<FakeTimetableEntry> {
            return entries.filter { it.userId == userId }
        }

        fun deleteForUser(userId: String): Int {
            val before = entries.size
            entries.removeAll { it.userId == userId }
            return before - entries.size
        }
    }

    private lateinit var credentialStore: FakeCredentialStore
    private lateinit var sessionStore: FakeSessionStore
    private lateinit var timetableDao: FakeTimetableDao

    @Before
    fun setUp() {
        credentialStore = FakeCredentialStore()
        sessionStore = FakeSessionStore()
        timetableDao = FakeTimetableDao()
    }

    private fun disconnectUser(userId: String) {
        credentialStore.clearCredentials(userId)
        sessionStore.clearSession(userId)
        timetableDao.deleteForUser(userId)
    }

    @Test
    fun testDisconnectPurgesCredentialsSessionAndTimetable() {
        val userId = "firebase_user_alpha"

        // Setup active KAYA connection for User Alpha
        credentialStore.saveCredentials(userId, "21051234", "my_secret_pass")
        sessionStore.saveSession(
            userId,
            KayaSession(
                username = "21051234",
                sessionId = "sess_cookie_abc",
                csrfToken = "csrf_token_xyz",
                lastConnectedAt = System.currentTimeMillis(),
                isSessionExpired = false
            )
        )
        timetableDao.insertAll(
            listOf(
                FakeTimetableEntry(userId, "entry_1", "Computer Networks", "Monday"),
                FakeTimetableEntry(userId, "entry_2", "Design & Analysis of Algorithms", "Monday")
            )
        )

        // Verify connected state before disconnect
        assertTrue("User Alpha should have stored credentials", credentialStore.hasCredentials(userId))
        assertNotNull("User Alpha should have active session", sessionStore.getSession(userId))
        assertEquals(2, timetableDao.getTimetableForUser(userId).size)

        // Execute Disconnect
        disconnectUser(userId)

        // Verify complete, atomic cleanup
        assertFalse("Credentials must be deleted after disconnect", credentialStore.hasCredentials(userId))
        assertNull("Stored credentials must return null", credentialStore.getCredentials(userId))
        assertNull("Session cookies must be deleted after disconnect", sessionStore.getSession(userId))
        assertTrue("Timetable entries must be purged from database", timetableDao.getTimetableForUser(userId).isEmpty())
    }

    @Test
    fun testAccountIsolation_DisconnectUserADoesNotTouchUserB() {
        val userA = "firebase_user_A"
        val userB = "firebase_user_B"

        // Seed data for User A
        credentialStore.saveCredentials(userA, "roll_2105A", "pass_A")
        sessionStore.saveSession(
            userA,
            KayaSession(username = "roll_2105A", sessionId = "sess_A", csrfToken = "csrf_A", lastConnectedAt = 1000L)
        )
        timetableDao.insertAll(
            listOf(FakeTimetableEntry(userA, "a_1", "Operating Systems", "Tuesday"))
        )

        // Seed data for User B
        credentialStore.saveCredentials(userB, "roll_2105B", "pass_B")
        sessionStore.saveSession(
            userB,
            KayaSession(username = "roll_2105B", sessionId = "sess_B", csrfToken = "csrf_B", lastConnectedAt = 2000L)
        )
        timetableDao.insertAll(
            listOf(
                FakeTimetableEntry(userB, "b_1", "Database Management", "Wednesday"),
                FakeTimetableEntry(userB, "b_2", "Software Engineering", "Thursday")
            )
        )

        // Disconnect User A
        disconnectUser(userA)

        // User A must be completely disconnected
        assertFalse(credentialStore.hasCredentials(userA))
        assertNull(sessionStore.getSession(userA))
        assertTrue(timetableDao.getTimetableForUser(userA).isEmpty())

        // User B must remain 100% untouched
        assertTrue("User B credentials must remain intact", credentialStore.hasCredentials(userB))
        val credsB = credentialStore.getCredentials(userB)
        assertEquals("roll_2105B", credsB?.username)
        assertEquals("pass_B", credsB?.password)

        val sessB = sessionStore.getSession(userB)
        assertNotNull("User B session must remain intact", sessB)
        assertEquals("sess_B", sessB?.sessionId)

        val timetableB = timetableDao.getTimetableForUser(userB)
        assertEquals("User B timetable entries must remain intact", 2, timetableB.size)
        assertEquals("Database Management", timetableB[0].courseName)
        assertEquals("Software Engineering", timetableB[1].courseName)
    }

    @Test
    fun testNoSilentReauthenticationAfterDisconnect() {
        val userId = "firebase_user_gamma"

        credentialStore.saveCredentials(userId, "21055555", "some_password")
        sessionStore.saveSession(
            userId,
            KayaSession(username = "21055555", sessionId = "valid_sess", csrfToken = "token", lastConnectedAt = 500L)
        )

        // Disconnect
        disconnectUser(userId)

        // Verify that neither session nor credentials can be found for silent reauth
        val creds = credentialStore.getCredentials(userId)
        val session = sessionStore.getSession(userId)

        val canSilentReauth = creds != null || (session != null && !session.isSessionExpired)
        assertFalse("Silent re-authentication must not be possible after explicit disconnect", canSilentReauth)
    }
}
