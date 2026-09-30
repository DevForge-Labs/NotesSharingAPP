package com.pravor.notessharing.ui.features.upload.edit

import com.pravor.notessharing.domain.model.EditableAttachment
import com.pravor.notessharing.domain.model.UploadFileSource
import com.pravor.notessharing.domain.model.UploadType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadEditUiStateTest {

    private fun createRemoteAttachment(
        id: String,
        displayName: String = "file.pdf",
        sizeBytes: Long = 1024L,
        isPdf: Boolean = true,
        isImage: Boolean = false
    ): EditableAttachment.ExistingRemote {
        return EditableAttachment.ExistingRemote(
            id = id,
            displayName = displayName,
            sizeBytes = sizeBytes,
            isPdf = isPdf,
            isImage = isImage,
            downloadUrl = "https://firebasestorage.googleapis.com/$id",
            storagePath = "notes/$id"
        )
    }

    private fun createLocalAttachment(
        id: String,
        displayName: String = "new_file.pdf",
        sizeBytes: Long = 2048L,
        isPdf: Boolean = true,
        isImage: Boolean = false,
        source: UploadFileSource = UploadFileSource.DocumentPicker
    ): EditableAttachment.NewlyAddedLocal {
        return EditableAttachment.NewlyAddedLocal(
            id = id,
            displayName = displayName,
            sizeBytes = sizeBytes,
            isPdf = isPdf,
            isImage = isImage,
            uri = "content://media/external/files/$id",
            source = source
        )
    }

    @Test
    fun testInitialStateHasNoChangesAndCannotSave() {
        val remote1 = createRemoteAttachment("doc1")
        val state = UploadEditUiState(
            isLoading = false,
            existingAttachments = listOf(remote1)
        )

        assertFalse(state.hasChanges)
        assertEquals(1, state.allActiveAttachments.size)
        assertEquals(1024L, state.totalSizeBytes)
        assertFalse("Cannot save without changes", state.canSave)
    }

    @Test
    fun testRemovingExistingAttachmentMarksChangesAndCanSaveIfNonEmpty() {
        val remote1 = createRemoteAttachment("doc1")
        val remote2 = createRemoteAttachment("doc2")
        val state = UploadEditUiState(
            isLoading = false,
            existingAttachments = listOf(remote1, remote2),
            removedAttachmentIds = setOf("doc1")
        )

        assertTrue(state.hasChanges)
        assertEquals(1, state.activeExistingAttachments.size)
        assertEquals("doc2", state.activeExistingAttachments[0].id)
        assertTrue("Can save since 1 attachment remains and changes exist", state.canSave)
    }

    @Test
    fun testCannotSaveWhenAllAttachmentsAreRemoved() {
        val remote1 = createRemoteAttachment("doc1")
        val state = UploadEditUiState(
            isLoading = false,
            existingAttachments = listOf(remote1),
            removedAttachmentIds = setOf("doc1")
        )

        assertTrue(state.hasChanges)
        assertTrue(state.allActiveAttachments.isEmpty())
        assertFalse("Cannot save when 0 attachments remain", state.isValidForType)
        assertFalse("Save must be disabled when 0 attachments remain", state.canSave)
    }

    @Test
    fun testAddingNewAttachmentEnablesCanSave() {
        val remote1 = createRemoteAttachment("doc1")
        val local1 = createLocalAttachment("local1")
        val state = UploadEditUiState(
            isLoading = false,
            existingAttachments = listOf(remote1),
            newlyAddedAttachments = listOf(local1)
        )

        assertTrue(state.hasChanges)
        assertEquals(2, state.allActiveAttachments.size)
        assertEquals(1024L + 2048L, state.totalSizeBytes)
        assertTrue(state.canSave)
    }

    @Test
    fun testPyqUploadTypeEnforcesStrictSinglePdfRule() {
        val pdf1 = createRemoteAttachment("pyq1", "pyq2023.pdf", 1000L, isPdf = true)
        val pdf2 = createLocalAttachment("pyq2", "pyq2024.pdf", 1000L, isPdf = true)
        val image1 = createLocalAttachment("img1", "photo.png", 500L, isPdf = false, isImage = true, source = UploadFileSource.Gallery)

        // Single PDF is valid
        val validPyqState = UploadEditUiState(
            isLoading = false,
            uploadType = UploadType.Pyq,
            existingAttachments = listOf(pdf1)
        )
        assertTrue(validPyqState.isValidForType)

        // Multiple PDFs is invalid for PYQ
        val multiPdfPyqState = UploadEditUiState(
            isLoading = false,
            uploadType = UploadType.Pyq,
            existingAttachments = listOf(pdf1),
            newlyAddedAttachments = listOf(pdf2)
        )
        assertFalse(multiPdfPyqState.isValidForType)
        assertFalse(multiPdfPyqState.canSave)

        // Non-PDF is invalid for PYQ
        val nonPdfPyqState = UploadEditUiState(
            isLoading = false,
            uploadType = UploadType.Pyq,
            newlyAddedAttachments = listOf(image1)
        )
        assertFalse(nonPdfPyqState.isValidForType)
        assertFalse(nonPdfPyqState.canSave)
    }

    @Test
    fun testSavingInProgressOrErrorMessageDisablesSave() {
        val remote1 = createRemoteAttachment("doc1")
        val local1 = createLocalAttachment("local1")

        val savingState = UploadEditUiState(
            isLoading = false,
            existingAttachments = listOf(remote1),
            newlyAddedAttachments = listOf(local1),
            isSaving = true
        )
        assertFalse(savingState.canSave)

        val errorState = UploadEditUiState(
            isLoading = false,
            existingAttachments = listOf(remote1),
            newlyAddedAttachments = listOf(local1),
            errorMessage = "Some error occurred"
        )
        assertFalse(errorState.canSave)
    }
}
