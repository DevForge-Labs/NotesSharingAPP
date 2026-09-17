package com.pravor.notessharing.data.kaya

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KayaCredentialStoreTest {

    private class InMemoryKayaCredentialStore : KayaCredentialStore {
        private val store = mutableMapOf<String, KayaCredentials>()

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

    private lateinit var credentialStore: KayaCredentialStore

    @Before
    fun setUp() {
        credentialStore = InMemoryKayaCredentialStore()
    }

    @Test
    fun testSaveAndRetrieveCredentials() {
        val userId = "firebase_user_123"
        val saved = credentialStore.saveCredentials(userId, "22051000", "super_secret_kaya_pass")

        assertTrue("Credentials should be saved successfully", saved)
        assertTrue("hasCredentials should be true", credentialStore.hasCredentials(userId))

        val retrieved = credentialStore.getCredentials(userId)
        assertNotNull("Retrieved credentials should not be null", retrieved)
        assertEquals("22051000", retrieved?.username)
        assertEquals("super_secret_kaya_pass", retrieved?.password)
    }

    @Test
    fun testAccountPartitioning_UserIsolation() {
        val userA = "user_A"
        val userB = "user_B"

        credentialStore.saveCredentials(userA, "roll_A", "pass_A")
        credentialStore.saveCredentials(userB, "roll_B", "pass_B")

        val credsA = credentialStore.getCredentials(userA)
        val credsB = credentialStore.getCredentials(userB)

        assertEquals("roll_A", credsA?.username)
        assertEquals("pass_A", credsA?.password)

        assertEquals("roll_B", credsB?.username)
        assertEquals("pass_B", credsB?.password)

        // Clearing user A must NOT touch user B
        credentialStore.clearCredentials(userA)
        assertFalse(credentialStore.hasCredentials(userA))
        assertNull(credentialStore.getCredentials(userA))

        assertTrue(credentialStore.hasCredentials(userB))
        assertNotNull(credentialStore.getCredentials(userB))
        assertEquals("roll_B", credentialStore.getCredentials(userB)?.username)
    }

    @Test
    fun testBlankCredentialsHandling() {
        assertFalse(credentialStore.saveCredentials("", "user", "pass"))
        assertFalse(credentialStore.saveCredentials("uid", "", "pass"))
        assertFalse(credentialStore.saveCredentials("uid", "user", ""))
        assertNull(credentialStore.getCredentials("non_existent"))
    }
}
