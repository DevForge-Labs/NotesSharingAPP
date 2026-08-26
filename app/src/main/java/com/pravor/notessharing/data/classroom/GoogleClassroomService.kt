package com.pravor.notessharing.data.classroom

import android.util.Log
import com.pravor.notessharing.domain.model.classroom.AttachmentType
import com.pravor.notessharing.domain.model.classroom.ClassroomAnnouncement
import com.pravor.notessharing.domain.model.classroom.ClassroomAttachment
import com.pravor.notessharing.domain.model.classroom.ClassroomCourse
import com.pravor.notessharing.domain.model.classroom.ClassroomCourseWork
import com.pravor.notessharing.domain.model.classroom.ClassroomMaterial
import com.pravor.notessharing.domain.model.classroom.CourseState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class ClassroomException(message: String, cause: Throwable? = null) : Exception(message, cause)
class ClassroomAuthException(message: String = "Classroom authorization expired or invalid.") : ClassroomException(message)
class ClassroomConsentRequiredException(val consentIntent: android.content.Intent, val scope: String = "https://www.googleapis.com/auth/drive.readonly", message: String = "Google Drive authorization consent required.") : ClassroomException(message)
class ClassroomForbiddenException(message: String = "Classroom access denied. Ensure Google Classroom API is enabled.") : ClassroomException(message)
class ClassroomNetworkException(message: String = "Network error. Please check your internet connection.", cause: Throwable? = null) : ClassroomException(message, cause)
class ClassroomApiException(val statusCode: Int, message: String) : ClassroomException("Classroom API Error ($statusCode): $message")

class GoogleClassroomService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "ClassroomService"
        private const val BASE_URL = "https://classroom.googleapis.com/v1"
    }

    suspend fun listCourses(accessToken: String): List<ClassroomCourse> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/courses?courseStates=ACTIVE"
        Log.d(TAG, "Fetching active courses from: $url")
        val bodyString = executeGetRequest(url, accessToken)
        parseCoursesJson(bodyString)
    }

    suspend fun getCourse(courseId: String, accessToken: String): ClassroomCourse? = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/courses/$courseId"
        Log.d(TAG, "Fetching course details for: $courseId")
        try {
            val bodyString = executeGetRequest(url, accessToken)
            parseSingleCourseJson(bodyString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get single course $courseId", e)
            null
        }
    }

    suspend fun listCourseMaterials(courseId: String, accessToken: String): List<ClassroomMaterial> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/courses/$courseId/courseWorkMaterials?courseWorkMaterialStates=PUBLISHED"
        Log.d(TAG, "Fetching course materials from: $url")
        val bodyString = executeGetRequest(url, accessToken)
        parseMaterialsJson(bodyString)
    }

    suspend fun listAnnouncements(courseId: String, accessToken: String): List<ClassroomAnnouncement> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/courses/$courseId/announcements?announcementStates=PUBLISHED"
        Log.d(TAG, "Fetching course announcements from: $url")
        val bodyString = executeGetRequest(url, accessToken)
        parseAnnouncementsJson(bodyString)
    }

    suspend fun listCourseWork(courseId: String, accessToken: String): List<ClassroomCourseWork> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/courses/$courseId/courseWork?courseWorkStates=PUBLISHED"
        Log.d(TAG, "Fetching coursework assignments from: $url")
        val bodyString = executeGetRequest(url, accessToken)
        parseCourseWorkJson(bodyString)
    }

    suspend fun listCourseTeachers(courseId: String, accessToken: String): List<com.pravor.notessharing.domain.model.classroom.ClassroomTeacher> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/courses/$courseId/teachers"
        try {
            val bodyString = executeGetRequest(url, accessToken)
            parseTeachersJson(bodyString)
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch teachers for course $courseId: ${e.message}")
            emptyList()
        }
    }

    private fun executeGetRequest(url: String, accessToken: String): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                Log.d(TAG, "HTTP ${response.code} for $url")

                when (response.code) {
                    200 -> return bodyString
                    401 -> throw ClassroomAuthException()
                    403 -> throw ClassroomForbiddenException()
                    else -> throw ClassroomApiException(response.code, response.message)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network failure calling $url", e)
            throw ClassroomNetworkException(cause = e)
        }
    }

    private fun parseCoursesJson(jsonString: String): List<ClassroomCourse> {
        if (jsonString.isBlank()) return emptyList()
        val jsonObject = JSONObject(jsonString)
        val coursesArray = jsonObject.optJSONArray("courses") ?: return emptyList()
        val courses = mutableListOf<ClassroomCourse>()

        for (i in 0 until coursesArray.length()) {
            val item = coursesArray.optJSONObject(i) ?: continue
            val course = parseCourseObject(item)
            if (course != null) {
                courses.add(course)
            }
        }
        return courses
    }

    private fun parseTeachersJson(jsonString: String): List<com.pravor.notessharing.domain.model.classroom.ClassroomTeacher> {
        if (jsonString.isBlank()) return emptyList()
        val jsonObject = JSONObject(jsonString)
        val array = jsonObject.optJSONArray("teachers") ?: return emptyList()
        val teachers = mutableListOf<com.pravor.notessharing.domain.model.classroom.ClassroomTeacher>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val userId = item.optString("userId", "")
            val profile = item.optJSONObject("profile")
            val nameObj = profile?.optJSONObject("name")
            val fullName = nameObj?.optString("fullName")?.takeIf { it.isNotBlank() }
                ?: profile?.optString("name")?.takeIf { it.isNotBlank() }
                ?: ""
            val photoUrl = profile?.optString("photoUrl")?.takeIf { it.isNotBlank() }
                ?.let { if (it.startsWith("//")) "https:$it" else it }

            if (fullName.isNotBlank()) {
                teachers.add(
                    com.pravor.notessharing.domain.model.classroom.ClassroomTeacher(
                        id = userId,
                        name = fullName,
                        photoUrl = photoUrl
                    )
                )
            }
        }
        return teachers
    }

    private fun parseSingleCourseJson(jsonString: String): ClassroomCourse? {
        if (jsonString.isBlank()) return null
        val item = JSONObject(jsonString)
        return parseCourseObject(item)
    }

    private fun parseCourseObject(item: JSONObject): ClassroomCourse? {
        val id = item.optString("id", "")
        val name = item.optString("name", "")
        if (id.isBlank() || name.isBlank()) return null

        val section = item.optString("section").takeIf { it.isNotBlank() }
        val descriptionHeading = item.optString("descriptionHeading").takeIf { it.isNotBlank() }
        val description = item.optString("description").takeIf { it.isNotBlank() }
        val room = item.optString("room").takeIf { it.isNotBlank() }
        val enrollmentCode = item.optString("enrollmentCode").takeIf { it.isNotBlank() }
        val alternateLink = item.optString("alternateLink").takeIf { it.isNotBlank() }
        val stateStr = item.optString("courseState", "ACTIVE")

        val state = when (stateStr.uppercase()) {
            "ACTIVE" -> CourseState.ACTIVE
            "ARCHIVED" -> CourseState.ARCHIVED
            "PROVISIONED" -> CourseState.PROVISIONED
            "DECLINED" -> CourseState.DECLINED
            "SUSPENDED" -> CourseState.SUSPENDED
            else -> CourseState.UNKNOWN
        }

        return ClassroomCourse(
            id = id,
            name = name,
            section = section,
            descriptionHeading = descriptionHeading,
            description = description,
            room = room,
            enrollmentCode = enrollmentCode,
            alternateLink = alternateLink,
            state = state
        )
    }

    private fun parseMaterialsJson(jsonString: String): List<ClassroomMaterial> {
        if (jsonString.isBlank()) return emptyList()
        val jsonObject = JSONObject(jsonString)
        val array = jsonObject.optJSONArray("courseWorkMaterial") ?: return emptyList()
        val materials = mutableListOf<ClassroomMaterial>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optString("id", "")
            val title = item.optString("title", "Untitled Material")
            val description = item.optString("description").takeIf { it.isNotBlank() }
            val creationTime = item.optString("creationTime").takeIf { it.isNotBlank() }
            val updateTime = item.optString("updateTime").takeIf { it.isNotBlank() }
            val alternateLink = item.optString("alternateLink").takeIf { it.isNotBlank() }
            val attachments = parseAttachments(item.optJSONArray("materials"))

            materials.add(
                ClassroomMaterial(
                    id = id,
                    title = title,
                    description = description,
                    creationTime = creationTime,
                    updateTime = updateTime,
                    alternateLink = alternateLink,
                    attachments = attachments
                )
            )
        }
        return materials
    }

    private fun parseAnnouncementsJson(jsonString: String): List<ClassroomAnnouncement> {
        if (jsonString.isBlank()) return emptyList()
        val jsonObject = JSONObject(jsonString)
        val array = jsonObject.optJSONArray("announcements") ?: return emptyList()
        val list = mutableListOf<ClassroomAnnouncement>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optString("id", "")
            val text = item.optString("text", "")
            if (text.isBlank()) continue

            val creationTime = item.optString("creationTime").takeIf { it.isNotBlank() }
            val updateTime = item.optString("updateTime").takeIf { it.isNotBlank() }
            val alternateLink = item.optString("alternateLink").takeIf { it.isNotBlank() }
            val attachments = parseAttachments(item.optJSONArray("materials"))

            list.add(
                ClassroomAnnouncement(
                    id = id,
                    text = text,
                    creationTime = creationTime,
                    updateTime = updateTime,
                    alternateLink = alternateLink,
                    attachments = attachments
                )
            )
        }
        return list
    }

    private fun parseCourseWorkJson(jsonString: String): List<ClassroomCourseWork> {
        if (jsonString.isBlank()) return emptyList()
        val jsonObject = JSONObject(jsonString)
        val array = jsonObject.optJSONArray("courseWork") ?: return emptyList()
        val list = mutableListOf<ClassroomCourseWork>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optString("id", "")
            val title = item.optString("title", "Untitled Assignment")
            val description = item.optString("description").takeIf { it.isNotBlank() }
            val creationTime = item.optString("creationTime").takeIf { it.isNotBlank() }
            val alternateLink = item.optString("alternateLink").takeIf { it.isNotBlank() }
            val associatedWithDeveloper = item.optBoolean("associatedWithDeveloper", false)
            val attachments = parseAttachments(item.optJSONArray("materials"))

            var dueFormatted: String? = null
            val dueDateObj = item.optJSONObject("dueDate")
            if (dueDateObj != null) {
                val year = dueDateObj.optInt("year")
                val month = dueDateObj.optInt("month")
                val day = dueDateObj.optInt("day")
                val dueTimeObj = item.optJSONObject("dueTime")
                val timeStr = if (dueTimeObj != null) {
                    val hours = dueTimeObj.optInt("hours", 0)
                    val minutes = dueTimeObj.optInt("minutes", 0)
                    String.format(", %02d:%02d", hours, minutes)
                } else ""
                dueFormatted = "$day/$month/$year$timeStr"
            }

            list.add(
                ClassroomCourseWork(
                    id = id,
                    title = title,
                    description = description,
                    dueFormatted = dueFormatted,
                    creationTime = creationTime,
                    alternateLink = alternateLink,
                    associatedWithDeveloper = associatedWithDeveloper,
                    attachments = attachments
                )
            )
        }
        return list
    }

    private fun parseAttachments(materialsArray: JSONArray?): List<ClassroomAttachment> {
        if (materialsArray == null || materialsArray.length() == 0) return emptyList()
        val attachments = mutableListOf<ClassroomAttachment>()

        for (i in 0 until materialsArray.length()) {
            val matObj = materialsArray.optJSONObject(i) ?: continue

            // 1. Drive File
            val driveFileObj = matObj.optJSONObject("driveFile")?.optJSONObject("driveFile")
            if (driveFileObj != null) {
                val title = driveFileObj.optString("title", "Drive File")
                val linkUrl = driveFileObj.optString("alternateLink", "")
                val thumbnailUrl = driveFileObj.optString("thumbnailUrl").takeIf { it.isNotBlank() }
                if (linkUrl.isNotBlank()) {
                    attachments.add(
                        ClassroomAttachment(
                            title = title,
                            linkUrl = linkUrl,
                            type = AttachmentType.DRIVE_FILE,
                            thumbnailUrl = thumbnailUrl
                        )
                    )
                }
                continue
            }

            // 2. YouTube Video
            val ytObj = matObj.optJSONObject("youtubeVideo")
            if (ytObj != null) {
                val title = ytObj.optString("title", "YouTube Video")
                val linkUrl = ytObj.optString("alternateLink", "")
                val thumbnailUrl = ytObj.optString("thumbnailUrl").takeIf { it.isNotBlank() }
                if (linkUrl.isNotBlank()) {
                    attachments.add(
                        ClassroomAttachment(
                            title = title,
                            linkUrl = linkUrl,
                            type = AttachmentType.YOUTUBE,
                            thumbnailUrl = thumbnailUrl
                        )
                    )
                }
                continue
            }

            // 3. Link
            val linkObj = matObj.optJSONObject("link")
            if (linkObj != null) {
                val url = linkObj.optString("url", "")
                val isFormUrl = url.contains("docs.google.com/forms", ignoreCase = true) ||
                        url.contains("forms.gle", ignoreCase = true) ||
                        url.contains("forms.google.com", ignoreCase = true)
                val defaultTitle = if (isFormUrl) "Google Form" else "Web Link"
                val title = linkObj.optString("title", defaultTitle)
                val thumbnailUrl = linkObj.optString("thumbnailUrl").takeIf { it.isNotBlank() }
                if (url.isNotBlank()) {
                    attachments.add(
                        ClassroomAttachment(
                            title = title.ifBlank { defaultTitle },
                            linkUrl = url,
                            type = if (isFormUrl) AttachmentType.FORM else AttachmentType.LINK,
                            thumbnailUrl = thumbnailUrl
                        )
                    )
                }
                continue
            }

            // 4. Form
            val formObj = matObj.optJSONObject("form")
            if (formObj != null) {
                val title = formObj.optString("title", "Google Form")
                val formUrl = formObj.optString("formUrl", "")
                val thumbnailUrl = formObj.optString("thumbnailUrl").takeIf { it.isNotBlank() }
                if (formUrl.isNotBlank()) {
                    attachments.add(
                        ClassroomAttachment(
                            title = title,
                            linkUrl = formUrl,
                            type = AttachmentType.FORM,
                            thumbnailUrl = thumbnailUrl
                        )
                    )
                }
            }
        }

        return attachments
    }
}
