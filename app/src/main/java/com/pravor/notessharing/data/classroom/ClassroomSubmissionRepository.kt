package com.pravor.notessharing.data.classroom

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.pravor.notessharing.domain.model.classroom.ClassroomStudentSubmission
import com.pravor.notessharing.domain.model.classroom.SubmissionProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface SubmitAssignmentResult {
    data class Success(val submission: ClassroomStudentSubmission?) : SubmitAssignmentResult
    data class ConsentRequired(val recoveryIntent: Intent) : SubmitAssignmentResult
    data class ProjectPermissionDenied(
        val message: String = "Google Classroom requires this assignment to be submitted through the official Classroom app.",
        val alternateLink: String? = null
    ) : SubmitAssignmentResult
    data class AuthenticationError(val message: String) : SubmitAssignmentResult
    data class NetworkError(val message: String) : SubmitAssignmentResult
    data class UploadError(val message: String) : SubmitAssignmentResult
    data class Error(val message: String) : SubmitAssignmentResult
}

sealed interface MarkExternalAssignmentResult {
    data object TurnedIn : MarkExternalAssignmentResult
    data object ProjectPermissionDenied : MarkExternalAssignmentResult
    data class ConsentRequired(val recoveryIntent: Intent) : MarkExternalAssignmentResult
    data class AuthenticationError(val message: String) : MarkExternalAssignmentResult
    data class NetworkError(val message: String) : MarkExternalAssignmentResult
    data class Error(val message: String) : MarkExternalAssignmentResult
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
                return@withContext SubmitAssignmentResult.AuthenticationError(tokenResult.message)
            }
        }

        // 1. Locate student submission to get submission ID
        val subResult = submissionService.getStudentSubmission(courseId, courseWorkId, token)
        val initialSubmission = subResult.getOrNull()
        val submissionId = initialSubmission?.id
        if (submissionId.isNullOrBlank()) {
            val errorMsg = subResult.exceptionOrNull()?.message ?: "Could not find a student submission for this assignment."
            Log.e(TAG, "Failed to get student submission ID: $errorMsg")
            return@withContext SubmitAssignmentResult.Error("Could not locate assignment submission. Please check your internet connection.")
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
            return@withContext SubmitAssignmentResult.UploadError("Couldn't upload '$fileName' to Google Drive. Please check your connection and try again.")
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
            val exception = attachResult.exceptionOrNull()
            Log.e(TAG, "Attach to submission failed", exception)

            if (exception is ClassroomProjectPermissionException || exception?.message?.contains("@ProjectPermissionDenied") == true) {
                return@withContext SubmitAssignmentResult.ProjectPermissionDenied(
                    message = "Google Classroom requires this assignment to be submitted through the official Classroom app.",
                    alternateLink = initialSubmission.alternateLink
                )
            }
            return@withContext SubmitAssignmentResult.Error("Unable to attach file to Classroom submission. Please try again.")
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
            val exception = turnInResult.exceptionOrNull()
            Log.e(TAG, "Turn in failed", exception)

            if (exception is ClassroomProjectPermissionException || exception?.message?.contains("@ProjectPermissionDenied") == true) {
                return@withContext SubmitAssignmentResult.ProjectPermissionDenied(
                    message = "Google Classroom requires this assignment to be submitted through the official Classroom app.",
                    alternateLink = initialSubmission.alternateLink
                )
            }
            return@withContext SubmitAssignmentResult.Error("Unable to turn in assignment. Please try again.")
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

    suspend fun turnInExternalAssignment(
        courseId: String,
        courseWorkId: String
    ): MarkExternalAssignmentResult = withContext(Dispatchers.IO) {
        val tokenResult = authManager.getSubmissionAccessToken()
        val token = when (tokenResult) {
            is ClassroomTokenResult.Success -> tokenResult.token
            is ClassroomTokenResult.ConsentRequired -> {
                return@withContext MarkExternalAssignmentResult.ConsentRequired(tokenResult.recoveryIntent)
            }
            is ClassroomTokenResult.Error -> {
                return@withContext MarkExternalAssignmentResult.AuthenticationError(tokenResult.message)
            }
        }

        // 1. Get submission ID
        val subResult = submissionService.getStudentSubmission(courseId, courseWorkId, token)
        val initialSubmission = subResult.getOrNull()
        val submissionId = initialSubmission?.id
        if (submissionId.isNullOrBlank()) {
            val errorMsg = subResult.exceptionOrNull()?.message ?: "Could not locate assignment submission."
            Log.e(TAG, "Failed to get student submission ID for external turnIn: $errorMsg")
            return@withContext MarkExternalAssignmentResult.Error("Could not connect to Classroom. Check your internet connection.")
        }

        // 2. Attempt real turnIn API call
        val turnInResult = submissionService.turnIn(
            courseId = courseId,
            courseWorkId = courseWorkId,
            submissionId = submissionId,
            accessToken = token
        )

        if (turnInResult.isFailure) {
            val exception = turnInResult.exceptionOrNull()
            Log.w(TAG, "External assignment turnIn failed: ${exception?.message}", exception)

            if (exception is ClassroomProjectPermissionException || exception?.message?.contains("@ProjectPermissionDenied") == true) {
                return@withContext MarkExternalAssignmentResult.ProjectPermissionDenied
            }
            return@withContext MarkExternalAssignmentResult.Error("Couldn't update the Google Classroom submission. Please try again.")
        }

        // If turnIn succeeded, update Room submission cache with the real Google Classroom state
        val updatedSub = submissionService.getStudentSubmission(courseId, courseWorkId, token).getOrNull()
        if (updatedSub != null) {
            try {
                classroomRepository.saveSubmission(updatedSub)
            } catch (e: Exception) {
                Log.w(TAG, "Failed caching updated submission to Room: ${e.message}")
            }
        }
        // Also remove any stale local manual completion since real Google Classroom state is now TURNED_IN
        classroomRepository.deleteManualCompletion(courseWorkId)

        MarkExternalAssignmentResult.TurnedIn
    }
}
