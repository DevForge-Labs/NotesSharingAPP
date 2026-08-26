package com.pravor.notessharing.data.classroom

import android.util.Log
import com.pravor.notessharing.domain.model.classroom.AttachmentType
import com.pravor.notessharing.domain.model.classroom.ClassroomStudentSubmission
import com.pravor.notessharing.domain.model.classroom.SubmissionAttachment
import com.pravor.notessharing.domain.model.classroom.SubmissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ClassroomProjectPermissionException(
    message: String,
    val rawResponse: String? = null
) : Exception(message)

class ClassroomSubmissionService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "ClassroomSubService"
        private const val BASE_URL = "https://classroom.googleapis.com/v1"
    }

    suspend fun getStudentSubmission(
        courseId: String,
        courseWorkId: String,
        accessToken: String
    ): Result<ClassroomStudentSubmission?> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/courses/$courseId/courseWork/$courseWorkId/studentSubmissions?userId=me"
        Log.d(TAG, "Fetching student submissions from: $url")

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                Log.d(TAG, "getStudentSubmission HTTP ${response.code}: $bodyString")

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Failed to fetch student submission (${response.code}): $bodyString")
                    )
                }

                val json = JSONObject(bodyString)
                val submissionsArray = json.optJSONArray("studentSubmissions")
                if (submissionsArray == null || submissionsArray.length() == 0) {
                    return@withContext Result.success(null)
                }

                val submissionObj = submissionsArray.getJSONObject(0)
                val submission = parseSubmission(courseId, courseWorkId, submissionObj)
                Result.success(submission)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting student submission for courseWork: $courseWorkId", e)
            Result.failure(e)
        }
    }

    suspend fun attachDriveFile(
        courseId: String,
        courseWorkId: String,
        submissionId: String,
        driveFileId: String,
        accessToken: String
    ): Result<ClassroomStudentSubmission> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/courses/$courseId/courseWork/$courseWorkId/studentSubmissions/$submissionId:modifyAttachments"
        Log.d(TAG, "Modifying attachments on submission $submissionId with Drive file $driveFileId")

        val payload = JSONObject().apply {
            val addArr = JSONArray().apply {
                val attObj = JSONObject().apply {
                    val driveObj = JSONObject().apply {
                        put("id", driveFileId)
                    }
                    put("driveFile", driveObj)
                }
                put(attObj)
            }
            put("addAttachments", addArr)
        }.toString()

        val requestBody = payload.toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Accept", "application/json")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                Log.d(TAG, "modifyAttachments HTTP ${response.code}: $bodyString")

                if (!response.isSuccessful) {
                    if (response.code == 403 && bodyString.contains("@ProjectPermissionDenied")) {
                        return@withContext Result.failure(
                            ClassroomProjectPermissionException(
                                "Google Classroom requires this assignment to be submitted through the official Classroom app.",
                                bodyString
                            )
                        )
                    }
                    return@withContext Result.failure(
                        Exception("Failed to attach Drive file to submission (${response.code}): $bodyString")
                    )
                }

                val submissionObj = JSONObject(bodyString)
                val submission = parseSubmission(courseId, courseWorkId, submissionObj)
                Result.success(submission)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception attaching Drive file to submission", e)
            Result.failure(e)
        }
    }

    suspend fun turnIn(
        courseId: String,
        courseWorkId: String,
        submissionId: String,
        accessToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/courses/$courseId/courseWork/$courseWorkId/studentSubmissions/$submissionId:turnIn"
        Log.d(TAG, "Turning in submission $submissionId for courseWork $courseWorkId")

        val emptyBody = "{}".toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Accept", "application/json")
            .post(emptyBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                Log.d(TAG, "turnIn HTTP ${response.code}: $bodyString")

                if (!response.isSuccessful) {
                    if (response.code == 403 && bodyString.contains("@ProjectPermissionDenied")) {
                        return@withContext Result.failure(
                            ClassroomProjectPermissionException(
                                "Google Classroom requires this assignment to be submitted through the official Classroom app.",
                                bodyString
                            )
                        )
                    }
                    return@withContext Result.failure(
                        Exception("Failed to turn in submission (${response.code}): $bodyString")
                    )
                }

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception turning in submission", e)
            Result.failure(e)
        }
    }

    private fun parseSubmission(
        courseId: String,
        courseWorkId: String,
        obj: JSONObject
    ): ClassroomStudentSubmission {
        val id = obj.optString("id", "")
        val userId = obj.optString("userId", "")
        val stateStr = obj.optString("state", "NEW")
        val late = obj.optBoolean("late", false)
        val assignedGrade = if (obj.has("assignedGrade")) obj.optDouble("assignedGrade") else null
        val alternateLink = obj.optString("alternateLink").takeIf { it.isNotBlank() }

        val state = when (stateStr.uppercase()) {
            "NEW" -> SubmissionState.NEW
            "CREATED" -> SubmissionState.CREATED
            "TURNED_IN" -> SubmissionState.TURNED_IN
            "RETURNED" -> SubmissionState.RETURNED
            "RECLAIMED_BY_STUDENT" -> SubmissionState.RECLAIMED_BY_STUDENT
            else -> SubmissionState.UNKNOWN
        }

        val attachments = mutableListOf<SubmissionAttachment>()
        val assignmentSubObj = obj.optJSONObject("assignmentSubmission")
        val attsArray = assignmentSubObj?.optJSONArray("attachments")
        if (attsArray != null) {
            for (i in 0 until attsArray.length()) {
                val attItem = attsArray.optJSONObject(i) ?: continue
                val driveFileObj = attItem.optJSONObject("driveFile")
                if (driveFileObj != null) {
                    val dId = driveFileObj.optString("id", "")
                    val dTitle = driveFileObj.optString("title", "Drive File")
                    val dLink = driveFileObj.optString("alternateLink", "")
                    val dThumb = driveFileObj.optString("thumbnailUrl").takeIf { it.isNotBlank() }
                    attachments.add(
                        SubmissionAttachment(
                            id = dId,
                            title = dTitle,
                            linkUrl = dLink,
                            type = AttachmentType.DRIVE_FILE,
                            thumbnailUrl = dThumb
                        )
                    )
                }
            }
        }

        return ClassroomStudentSubmission(
            id = id,
            courseId = courseId,
            courseWorkId = courseWorkId,
            userId = userId,
            state = state,
            late = late,
            assignedGrade = assignedGrade,
            attachments = attachments,
            alternateLink = alternateLink
        )
    }
}
