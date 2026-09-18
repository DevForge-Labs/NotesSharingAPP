package com.pravor.notessharing.ui.features.profile

import android.app.Application
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * Verifies that ProfileViewModel exposes the exact constructor required by AndroidViewModelFactory
 * to prevent NoSuchMethodException crashes when created via Compose viewModel().
 */
class ProfileViewModelConstructorTest {

    @Test
    fun testProfileViewModelExposesApplicationConstructor() {
        // AndroidViewModelFactory strictly searches for constructor(Application::class.java)
        val constructor = ProfileViewModel::class.java.getConstructor(Application::class.java)

        assertNotNull("ProfileViewModel must have a public constructor(Application)", constructor)
        assertTrue("Constructor must be public", Modifier.isPublic(constructor.modifiers))
    }

    @Test
    fun testProfileViewModelExposesNoArgConstructor() {
        // Optional parameterless constructor fallback
        val noArgConstructor = ProfileViewModel::class.java.getConstructor()

        assertNotNull("ProfileViewModel must have a parameterless constructor fallback", noArgConstructor)
        assertTrue("Constructor must be public", Modifier.isPublic(noArgConstructor.modifiers))
    }

    @Test
    fun testProfileViewModelExposesFullConstructor() {
        val fullConstructor = ProfileViewModel::class.java.getConstructor(
            Application::class.java,
            com.pravor.notessharing.data.repository.ProfileRepository::class.java,
            com.pravor.notessharing.data.repository.KayaTimetableRepository::class.java
        )

        assertNotNull("ProfileViewModel must have a full constructor for testing/DI", fullConstructor)
        assertTrue("Constructor must be public", Modifier.isPublic(fullConstructor.modifiers))
    }
}
