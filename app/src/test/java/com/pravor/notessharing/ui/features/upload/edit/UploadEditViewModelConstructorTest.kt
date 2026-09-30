package com.pravor.notessharing.ui.features.upload.edit

import android.app.Application
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

class UploadEditViewModelConstructorTest {

    @Test
    fun testUploadEditViewModelExposesApplicationConstructor() {
        val constructor = UploadEditViewModel::class.java.getConstructor(Application::class.java)

        assertNotNull("UploadEditViewModel must have a public constructor(Application)", constructor)
        assertTrue("Constructor must be public", Modifier.isPublic(constructor.modifiers))
    }
}
