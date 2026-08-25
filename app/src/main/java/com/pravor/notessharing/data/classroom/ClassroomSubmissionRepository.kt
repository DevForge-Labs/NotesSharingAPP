package com.pravor.notessharing.data.classroom

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.pravor.notessharing.domain.model.classroom.ClassroomStudentSubmission
import com.pravor.notessharing.domain.model.classroom.SubmissionProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class SubmitAssignmentResult {
    data class Success(val submission: ClassroomStudentSubmission?) : SubmitAssignmentResult()
    data class ConsentRequired(val recoveryIntent: Intent) : SubmitAssignmentResult()
    data class Error(val message: String) : SubmitAssignmentResult()
}

class ClassroomSubmissionRepository(
    private val context: Context,
    private val authManager: ClassroomAuthManager,
    private val classroomRepository: ClassroomRepository = ClassroomRepository.getInstance(context),
    private val driveUploadService: GoogleDriveUploadService = GoogleDriveUploadService(),
    private val submissionService: ClassroomSubmissionService = ClassroomSubmissionService()
) {
    companion object {
        private const val TAG = "ClassroomSubRepo"

        @Volatile
        private var INSTANCE: ClassroomSubmissionRepository? = null

        fun getInstance(context: Context): ClassroomSubmissionRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ClassroomSubmissionRepository(
                    context.applicationContext,
                    ClassroomAuthManager.getInstance(context.applicationContext)
                ).also { INSTANCE = it }
            }
        }
    }

    suspend fun getSubmission(
        courseId: String,
        courseWorkId: String
    ): Result<ClassroomStudentSubmission?> = withContext(Dispatchers.IO) {
        val result = when (val tokenResult = authManager.getSubmissionAccessToken()) {
            is ClassroomTokenResult.Success -> {
                submissionService.getStudentSubmission(courseId, courseWorkId, tokenResult.token)
            }
            is ClassroomTokenResult.ConsentRequired -> {
                // Fall back to read-only token to at least view submission if possible
                val readToken = authManager.getClassroomAccessToken()
                if (readToken != null) {
                    submissionService.getStudentSubmission(courseId, courseWorkId, readToken)
                } else {
                    Result.failure(Exception("Classroom authorization required."))
                }
            }
            is ClassroomTokenResult.Error -> {
                Result.failure(Exception(tokenResult.message, tokenResult.cause))
            }
        }

        val sub = result.getOrNull()
        if (sub != null) {
            try {
                classroomRepository.saveSubmission(sub)
            } catch (e: Exception) {
                Log.w(TAG, "Failed caching submission to Room: ${e.message}")
            }
        }

        result
    }

    suspend fun submitAssignment(
        context: Context,
        courseId: String,
        courseWorkId: String,
        fileUri: Uri,
        fileName: String,
        mimeType: String,
        onProgress: (SubmissionProgress) -> Unit
    ): SubmitAssignmentResult = withContext(Dispatchers.IO) {
        val tokenResult = authManager.getSubmissionAccessToken()
        val token = when (tokenResult) {
            is ClassroomTokenResult.Success -> tokenResult.token
            is ClassroomTokenResult.ConsentRequired -> {
                return@withContext SubmitAssignmentResult.ConsentRequired(tokenResult.recoveryIntent)
            }
            is ClassroomTokenResult.Error -> {
                return@withContext SubmitAssignmentResult.Error(tokenResult.message)
            }
        }

        // 1. Locate student submission to get submission ID
        val subResult = submissionService.getStudentSubmission(courseId, courseWorkId, token)
        val initialSubmission = subResult.getOrNull()
        val submissionId = initialSubmission?.id
        if (submissionId.isNullOrBlank()) {
            val errorMsg = subResult.exceptionOrNull()?.message ?: "Could not find a student submission for this assignment."
            Log.e(TAG, "Failed to get student submission ID: $errorMsg")
            return@withContext SubmitAssignmentResult.Error(errorMsg)
        }

        // 2. Upload file to Google Drive using drive.file
        onProgress(SubmissionProgress.UploadingToDrive(fileName))
        val uploadResult = driveUploadService.uploadFile(
            context = context,
            fileUri = fileUri,
            customFileName = fileName,
            customMimeType = mimeType,
            accessToken = token
        )

        if (uploadResult.isFailure) {
            val errorMsg = uploadResult.exceptionOrNull()?.message ?: "Failed to upload file to Google Drive."
            Log.e(TAG, "Drive upload failed: $errorMsg")
            return@withContext SubmitAssignmentResult.Error(errorMsg)
        }

        val driveFile = uploadResult.getOrThrow()

        // 3. Attach uploaded Drive file to Classroom submission
        onProgress(SubmissionProgress.AttachingToClassroom(fileName))
        val attachResult = submissionService.attachDriveFile(
            courseId = courseId,
            courseWorkId = courseWorkId,
            submissionId = submissionId,
            driveFileId = driveFile.fileId,
            accessToken = token
        )

        if (attachResult.isFailure) {
            val errorMsg = attachResult.exceptionOrNull()?.message ?: "Failed to attach file to Classroom submission."
            Log.e(TAG, "Attach to submission failed: $errorMsg")
            return@withContext SubmitAssignmentResult.Error(errorMsg)
        }

        // 4. Turn in submission
        onProgress(SubmissionProgress.TurningIn)
        val turnInResult = submissionService.turnIn(
            courseId = courseId,
            courseWorkId = courseWorkId,
            submissionId = submissionId,
            accessToken = token
        )

        if (turnInResult.isFailure) {
            val errorMsg = turnInResult.exceptionOrNull()?.message ?: "Failed to turn in assignment."
            Log.e(TAG, "Turn in failed: $errorMsg")
            return@withContext SubmitAssignmentResult.Error(errorMsg)
        }

        onProgress(SubmissionProgress.Success("Assignment submitted successfully!"))
        // Fetch updated submission state to return and cache in Room
        val updatedSub = submissionService.getStudentSubmission(courseId, courseWorkId, token).getOrNull()
        if (updatedSub != null) {
            try {
                classroomRepository.saveSubmission(updatedSub)
            } catch (e: Exception) {
                Log.w(TAG, "Failed caching updated submission to Room: ${e.message}")
            }
        }
        SubmitAssignmentResult.Success(updatedSub)
    }
}
