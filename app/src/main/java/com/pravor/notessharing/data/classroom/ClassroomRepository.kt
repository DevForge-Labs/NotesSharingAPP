package com.pravor.notessharing.data.classroom

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.data.local.db.AppDatabase
import com.pravor.notessharing.data.local.entity.ClassroomAnnouncementEntity
import com.pravor.notessharing.data.local.entity.ClassroomAttachmentEntity
import com.pravor.notessharing.data.local.entity.ClassroomCourseEntity
import com.pravor.notessharing.data.local.entity.ClassroomCourseWorkEntity
import com.pravor.notessharing.data.local.entity.ClassroomHiddenCourseEntity
import com.pravor.notessharing.data.local.entity.ClassroomMaterialEntity
import com.pravor.notessharing.domain.model.classroom.AttachmentType
import com.pravor.notessharing.domain.model.classroom.ClassroomAnnouncement
import com.pravor.notessharing.domain.model.classroom.ClassroomAttachment
import com.pravor.notessharing.domain.model.classroom.ClassroomCourse
import com.pravor.notessharing.domain.model.classroom.ClassroomCourseWork
import com.pravor.notessharing.domain.model.classroom.ClassroomMaterial
import com.pravor.notessharing.domain.model.classroom.ClassroomTeacher
import com.pravor.notessharing.domain.model.classroom.CourseState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ClassroomRepository(
    private val context: Context,
    private val authManager: ClassroomAuthManager,
    private val classroomService: GoogleClassroomService = GoogleClassroomService()
) {

    companion object {
        private const val TAG = "ClassroomRepo"

        @Volatile
        private var INSTANCE: ClassroomRepository? = null

        fun getInstance(context: Context): ClassroomRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ClassroomRepository(
                    context.applicationContext,
                    ClassroomAuthManager.getInstance(context.applicationContext)
                ).also { INSTANCE = it }
            }
        }
    }

    private val classroomDao = AppDatabase.getDatabase(context).classroomDao()

    private fun getUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
    }

    private fun getClassroomAccount(): String {
        return (authManager.authState.value as? ClassroomAuthState.Connected)?.account?.email ?: "default"
    }

    private fun isPotentialTeacherName(str: String): Boolean {
        val trimmed = str.trim()
        if (trimmed.length in 3..50 && !trimmed.contains("http") && !trimmed.contains("\n")) {
            if (trimmed.startsWith("Dr.", ignoreCase = true) ||
                trimmed.startsWith("Prof", ignoreCase = true) ||
                trimmed.startsWith("Mr.", ignoreCase = true) ||
                trimmed.startsWith("Ms.", ignoreCase = true) ||
                trimmed.startsWith("Mrs.", ignoreCase = true) ||
                trimmed.split(" ").size in 2..4) {
                return true
            }
        }
        return false
    }

    // --- 1. Courses ---

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeCourses(): Flow<List<ClassroomCourse>> {
        return authManager.currentSessionFlow.flatMapLatest { session ->
            val uid = session.firebaseUid
            val account = session.classroomAccount
            if (uid.isNullOrBlank() || account.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                classroomDao.observeCourses(uid, account).map { entities ->
                    entities.map { entity ->
                        val teacher = if (!entity.teacherName.isNullOrBlank()) {
                            ClassroomTeacher(
                                id = entity.teacherId.orEmpty(),
                                name = entity.teacherName,
                                photoUrl = entity.teacherPhotoUrl
                            )
                        } else if (!entity.descriptionHeading.isNullOrBlank() && isPotentialTeacherName(entity.descriptionHeading)) {
                            ClassroomTeacher(
                                id = "",
                                name = entity.descriptionHeading,
                                photoUrl = null
                            )
                        } else {
                            null
                        }

                        ClassroomCourse(
                            id = entity.courseId,
                            name = entity.name,
                            section = entity.section,
                            descriptionHeading = entity.descriptionHeading,
                            description = entity.description,
                            room = entity.room,
                            enrollmentCode = entity.enrollmentCode,
                            alternateLink = entity.alternateLink,
                            state = try { CourseState.valueOf(entity.state) } catch (e: Exception) { CourseState.ACTIVE },
                            teacher = teacher
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeHiddenCourseIds(): Flow<Set<String>> {
        return authManager.currentSessionFlow.flatMapLatest { session ->
            val uid = session.firebaseUid
            val account = session.classroomAccount
            if (uid.isNullOrBlank() || account.isNullOrBlank()) {
                flowOf(emptySet())
            } else {
                classroomDao.observeHiddenCourseIds(uid, account).map { list: List<String> -> list.toSet() }
            }
        }
    }

    suspend fun setHiddenCourseIds(hiddenIds: Set<String>) = withContext(Dispatchers.IO) {
        val userId = getUserId()
        val account = getClassroomAccount()
        classroomDao.clearHiddenCoursesForAccount(userId, account)
        if (hiddenIds.isNotEmpty()) {
            val entities = hiddenIds.map { courseId ->
                ClassroomHiddenCourseEntity(
                    id = "${userId}_${account}_$courseId",
                    userId = userId,
                    classroomAccount = account,
                    courseId = courseId
                )
            }
            classroomDao.hideCourses(entities)
        }
        Log.d(TAG, "Saved ${hiddenIds.size} hidden course preferences for account: $account")
    }

    suspend fun clearClassroomDataForCurrentAccount() = withContext(Dispatchers.IO) {
        val userId = getUserId()
        val account = getClassroomAccount()
        ClassroomSyncManager.clearForAccount(userId, account)
        classroomDao.clearAccountCourses(userId, account)
        classroomDao.clearHiddenCoursesForAccount(userId, account)
    }

    suspend fun getCourse(courseId: String): ClassroomCourse? = withContext(Dispatchers.IO) {
        val userId = getUserId()
        val local = classroomDao.getCourse(courseId, userId)
        if (local != null) {
            val teacher = if (!local.teacherName.isNullOrBlank()) {
                ClassroomTeacher(
                    id = local.teacherId.orEmpty(),
                    name = local.teacherName,
                    photoUrl = local.teacherPhotoUrl
                )
            } else if (!local.descriptionHeading.isNullOrBlank() && isPotentialTeacherName(local.descriptionHeading)) {
                ClassroomTeacher(
                    id = "",
                    name = local.descriptionHeading,
                    photoUrl = null
                )
            } else {
                null
            }

            ClassroomCourse(
                id = local.courseId,
                name = local.name,
                section = local.section,
                descriptionHeading = local.descriptionHeading,
                description = local.description,
                room = local.room,
                enrollmentCode = local.enrollmentCode,
                alternateLink = local.alternateLink,
                state = try { CourseState.valueOf(local.state) } catch (e: Exception) { CourseState.ACTIVE },
                teacher = teacher
            )
        } else {
            val token = authManager.getClassroomAccessToken() ?: return@withContext null
            classroomService.getCourse(courseId, token)
        }
    }

    suspend fun syncCourses(force: Boolean = false): Result<List<ClassroomCourse>> = withContext(Dispatchers.IO) {
        val userId = getUserId()
        val account = getClassroomAccount()
        val syncKey = "${userId}_${account}_courses"

        if (!force && ClassroomSyncManager.isFresh(syncKey, ClassroomSyncManager.COURSES_TTL_MS)) {
            Log.d(TAG, "Courses are fresh (< 5m). Skipping network fetch.")
            return@withContext Result.success(emptyList())
        }

        ClassroomSyncManager.runCoalesced(syncKey) {
            val token = authManager.getClassroomAccessToken() ?: return@runCoalesced Result.failure(
                ClassroomAuthException("No active Google Classroom authorization found.")
            )

            try {
                val remoteCourses = classroomService.listCourses(token)
                val localEntities = classroomDao.getCoursesForAccount(userId, account)
                val localMap = localEntities.associateBy { it.courseId }

                // Concurrently fetch teachers for active courses
                val teachersByCourseId = remoteCourses.map { course ->
                    async {
                        val teachers = classroomService.listCourseTeachers(course.id, token)
                        course.id to teachers.firstOrNull()
                    }
                }.awaitAll().toMap()

                val remoteIds = remoteCourses.map { it.id }.toSet()
                val entitiesToUpsert = mutableListOf<ClassroomCourseEntity>()

                for (c in remoteCourses) {
                    val local = localMap[c.id]
                    val teacher = teachersByCourseId[c.id]
                    val resolvedTeacherName = teacher?.name ?: c.descriptionHeading?.takeIf { isPotentialTeacherName(it) }
                    val resolvedTeacherPhoto = teacher?.photoUrl
                    val resolvedTeacherId = teacher?.id

                    val isChanged = local == null ||
                            local.name != c.name ||
                            local.section != c.section ||
                            local.descriptionHeading != c.descriptionHeading ||
                            local.description != c.description ||
                            local.room != c.room ||
                            local.alternateLink != c.alternateLink ||
                            local.state != c.state.name ||
                            local.teacherName != resolvedTeacherName ||
                            local.teacherPhotoUrl != resolvedTeacherPhoto

                    if (isChanged) {
                        entitiesToUpsert.add(
                            ClassroomCourseEntity(
                                id = "${userId}_${c.id}",
                                courseId = c.id,
                                userId = userId,
                                classroomAccount = account,
                                name = c.name,
                                section = c.section,
                                descriptionHeading = c.descriptionHeading,
                                description = c.description,
                                room = c.room,
                                enrollmentCode = c.enrollmentCode,
                                alternateLink = c.alternateLink,
                                state = c.state.name,
                                teacherId = resolvedTeacherId,
                                teacherName = resolvedTeacherName,
                                teacherPhotoUrl = resolvedTeacherPhoto,
                                lastSyncedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }

                if (remoteCourses.isEmpty()) {
                    if (localEntities.isNotEmpty()) {
                        classroomDao.clearAccountCourses(userId, account)
                        Log.d(TAG, "No active courses found; cleared ${localEntities.size} cached courses for $account.")
                    }
                } else {
                    if (entitiesToUpsert.isNotEmpty()) {
                        classroomDao.upsertCourses(entitiesToUpsert)
                        Log.d(TAG, "Upserted ${entitiesToUpsert.size} changed/new courses into Room.")
                    } else {
                        Log.d(TAG, "All ${remoteCourses.size} courses unchanged; skipped Room writes.")
                    }

                    val staleIds = localEntities.map { it.courseId }.filter { it !in remoteIds }
                    if (staleIds.isNotEmpty()) {
                        classroomDao.deleteStaleCourses(userId, account, remoteCourses.map { it.id })
                        Log.d(TAG, "Deleted ${staleIds.size} stale courses from Room.")
                    }
                }

                ClassroomSyncManager.markSynced(syncKey)
                Result.success(remoteCourses)
            } catch (e: ClassroomAuthException) {
                authManager.refreshAuthState()
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- 2. Materials ---

    fun observeMaterials(courseId: String): Flow<List<ClassroomMaterial>> {
        val userId = getUserId()
        return combine(
            classroomDao.observeMaterials(courseId, userId),
            classroomDao.observeAttachmentsForCourse(courseId, userId)
        ) { materialsEntities, attachmentsEntities ->
            val attachmentsByParent = attachmentsEntities.groupBy { it.parentId }
            materialsEntities.map { mat ->
                val atts = attachmentsByParent[mat.materialId].orEmpty().map { att ->
                    ClassroomAttachment(
                        title = att.title,
                        linkUrl = att.linkUrl,
                        type = try { AttachmentType.valueOf(att.type) } catch (e: Exception) { AttachmentType.UNKNOWN },
                        thumbnailUrl = att.thumbnailUrl
                    )
                }
                ClassroomMaterial(
                    id = mat.materialId,
                    title = mat.title,
                    description = mat.description,
                    creationTime = mat.creationTime,
                    updateTime = mat.updateTime,
                    alternateLink = mat.alternateLink,
                    attachments = atts
                )
            }
        }
    }

    suspend fun syncMaterials(courseId: String, force: Boolean = false): Result<List<ClassroomMaterial>> = withContext(Dispatchers.IO) {
        val userId = getUserId()
        val account = getClassroomAccount()
        val syncKey = "${userId}_${account}_materials_${courseId}"

        if (!force && ClassroomSyncManager.isFresh(syncKey)) {
            Log.d(TAG, "Materials for course $courseId are fresh (< 3m). Skipping network fetch.")
            return@withContext Result.success(emptyList())
        }

        ClassroomSyncManager.runCoalesced(syncKey) {
            val token = authManager.getClassroomAccessToken() ?: return@runCoalesced Result.failure(
                ClassroomAuthException("No active Google Classroom authorization.")
            )

            try {
                val remoteMaterials = classroomService.listCourseMaterials(courseId, token)
                val localMaterials = classroomDao.getMaterialsForCourse(courseId, userId).associateBy { it.materialId }
                val localAttachments = classroomDao.getAttachmentsForCourse(courseId, userId).groupBy { it.parentId }

                val remoteIds = remoteMaterials.map { it.id }.toSet()
                val matEntitiesToUpsert = mutableListOf<ClassroomMaterialEntity>()
                val attEntitiesToUpsert = mutableListOf<ClassroomAttachmentEntity>()

                for (m in remoteMaterials) {
                    val local = localMaterials[m.id]
                    val existingAtts = localAttachments[m.id].orEmpty()

                    val isContentChanged = local == null ||
                            local.updateTime != m.updateTime ||
                            local.title != m.title ||
                            local.description != m.description ||
                            local.alternateLink != m.alternateLink ||
                            areAttachmentsChanged(existingAtts, m.attachments)

                    if (isContentChanged) {
                        matEntitiesToUpsert.add(
                            ClassroomMaterialEntity(
                                id = "${userId}_${m.id}",
                                materialId = m.id,
                                courseId = courseId,
                                userId = userId,
                                title = m.title,
                                description = m.description,
                                creationTime = m.creationTime,
                                updateTime = m.updateTime,
                                alternateLink = m.alternateLink,
                                lastSyncedAt = System.currentTimeMillis()
                            )
                        )
                        m.attachments.forEachIndexed { index, att ->
                            attEntitiesToUpsert.add(
                                ClassroomAttachmentEntity(
                                    id = "${userId}_${m.id}_$index",
                                    parentId = m.id,
                                    parentType = "MATERIAL",
                                    courseId = courseId,
                                    userId = userId,
                                    title = att.title,
                                    linkUrl = att.linkUrl,
                                    type = att.type.name,
                                    driveFileId = extractDriveFileId(att.linkUrl),
                                    thumbnailUrl = att.thumbnailUrl
                                )
                            )
                        }
                    }
                }

                if (matEntitiesToUpsert.isNotEmpty()) {
                    classroomDao.upsertMaterials(matEntitiesToUpsert)
                    classroomDao.upsertAttachments(attEntitiesToUpsert)
                    Log.d(TAG, "Upserted ${matEntitiesToUpsert.size} changed/new materials into Room.")
                } else {
                    Log.d(TAG, "All ${remoteMaterials.size} materials unchanged; skipped Room writes.")
                }

                val staleIds = localMaterials.keys.filter { it !in remoteIds }
                if (staleIds.isNotEmpty()) {
                    classroomDao.deleteStaleMaterials(courseId, userId, remoteMaterials.map { it.id })
                    classroomDao.deleteStaleAttachments(courseId, userId, remoteMaterials.map { it.id })
                    Log.d(TAG, "Deleted ${staleIds.size} stale materials from Room.")
                }

                ClassroomSyncManager.markSynced(syncKey)
                Result.success(remoteMaterials)
            } catch (e: ClassroomAuthException) {
                authManager.refreshAuthState()
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- 3. Announcements ---

    fun observeAnnouncements(courseId: String): Flow<List<ClassroomAnnouncement>> {
        val userId = getUserId()
        return combine(
            classroomDao.observeAnnouncements(courseId, userId),
            classroomDao.observeAttachmentsForCourse(courseId, userId)
        ) { annEntities, attachmentsEntities ->
            val attachmentsByParent = attachmentsEntities.groupBy { it.parentId }
            annEntities.map { ann ->
                val atts = attachmentsByParent[ann.announcementId].orEmpty().map { att ->
                    ClassroomAttachment(
                        title = att.title,
                        linkUrl = att.linkUrl,
                        type = try { AttachmentType.valueOf(att.type) } catch (e: Exception) { AttachmentType.UNKNOWN },
                        thumbnailUrl = att.thumbnailUrl
                    )
                }
                ClassroomAnnouncement(
                    id = ann.announcementId,
                    text = ann.text,
                    creationTime = ann.creationTime,
                    updateTime = ann.updateTime,
                    alternateLink = ann.alternateLink,
                    attachments = atts
                )
            }
        }
    }

    suspend fun syncAnnouncements(courseId: String, force: Boolean = false): Result<List<ClassroomAnnouncement>> = withContext(Dispatchers.IO) {
        val userId = getUserId()
        val account = getClassroomAccount()
        val syncKey = "${userId}_${account}_announcements_${courseId}"

        if (!force && ClassroomSyncManager.isFresh(syncKey)) {
            Log.d(TAG, "Announcements for course $courseId are fresh (< 3m). Skipping network fetch.")
            return@withContext Result.success(emptyList())
        }

        ClassroomSyncManager.runCoalesced(syncKey) {
            val token = authManager.getClassroomAccessToken() ?: return@runCoalesced Result.failure(
                ClassroomAuthException("No active Google Classroom authorization.")
            )

            try {
                val remoteAnnouncements = classroomService.listAnnouncements(courseId, token)
                val localAnnouncements = classroomDao.getAnnouncementsForCourse(courseId, userId).associateBy { it.announcementId }
                val localAttachments = classroomDao.getAttachmentsForCourse(courseId, userId).groupBy { it.parentId }

                val remoteIds = remoteAnnouncements.map { it.id }.toSet()
                val annEntitiesToUpsert = mutableListOf<ClassroomAnnouncementEntity>()
                val attEntitiesToUpsert = mutableListOf<ClassroomAttachmentEntity>()

                for (a in remoteAnnouncements) {
                    val local = localAnnouncements[a.id]
                    val existingAtts = localAttachments[a.id].orEmpty()

                    val isContentChanged = local == null ||
                            local.updateTime != a.updateTime ||
                            local.text != a.text ||
                            local.alternateLink != a.alternateLink ||
                            areAttachmentsChanged(existingAtts, a.attachments)

                    if (isContentChanged) {
                        annEntitiesToUpsert.add(
                            ClassroomAnnouncementEntity(
                                id = "${userId}_${a.id}",
                                announcementId = a.id,
                                courseId = courseId,
                                userId = userId,
                                text = a.text,
                                creationTime = a.creationTime,
                                updateTime = a.updateTime,
                                alternateLink = a.alternateLink,
                                lastSyncedAt = System.currentTimeMillis()
                            )
                        )
                        a.attachments.forEachIndexed { index, att ->
                            attEntitiesToUpsert.add(
                                ClassroomAttachmentEntity(
                                    id = "${userId}_${a.id}_$index",
                                    parentId = a.id,
                                    parentType = "ANNOUNCEMENT",
                                    courseId = courseId,
                                    userId = userId,
                                    title = att.title,
                                    linkUrl = att.linkUrl,
                                    type = att.type.name,
                                    driveFileId = extractDriveFileId(att.linkUrl),
                                    thumbnailUrl = att.thumbnailUrl
                                )
                            )
                        }
                    }
                }

                if (annEntitiesToUpsert.isNotEmpty()) {
                    classroomDao.upsertAnnouncements(annEntitiesToUpsert)
                    classroomDao.upsertAttachments(attEntitiesToUpsert)
                    Log.d(TAG, "Upserted ${annEntitiesToUpsert.size} changed/new announcements into Room.")
                } else {
                    Log.d(TAG, "All ${remoteAnnouncements.size} announcements unchanged; skipped Room writes.")
                }

                val staleIds = localAnnouncements.keys.filter { it !in remoteIds }
                if (staleIds.isNotEmpty()) {
                    classroomDao.deleteStaleAnnouncements(courseId, userId, remoteAnnouncements.map { it.id })
                    Log.d(TAG, "Deleted ${staleIds.size} stale announcements from Room.")
                }

                ClassroomSyncManager.markSynced(syncKey)
                Result.success(remoteAnnouncements)
            } catch (e: ClassroomAuthException) {
                authManager.refreshAuthState()
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- 4. CourseWork ---

    fun observeCourseWork(courseId: String): Flow<List<ClassroomCourseWork>> {
        val userId = getUserId()
        return combine(
            classroomDao.observeCourseWork(courseId, userId),
            classroomDao.observeAttachmentsForCourse(courseId, userId)
        ) { cwEntities, attachmentsEntities ->
            val attachmentsByParent = attachmentsEntities.groupBy { it.parentId }
            cwEntities.map { cw ->
                val atts = attachmentsByParent[cw.courseWorkId].orEmpty().map { att ->
                    ClassroomAttachment(
                        title = att.title,
                        linkUrl = att.linkUrl,
                        type = try { AttachmentType.valueOf(att.type) } catch (e: Exception) { AttachmentType.UNKNOWN },
                        thumbnailUrl = att.thumbnailUrl
                    )
                }
                ClassroomCourseWork(
                    id = cw.courseWorkId,
                    title = cw.title,
                    description = cw.description,
                    dueFormatted = cw.dueFormatted,
                    creationTime = cw.creationTime,
                    alternateLink = cw.alternateLink,
                    attachments = atts
                )
            }
        }
    }

    suspend fun syncCourseWork(courseId: String, force: Boolean = false): Result<List<ClassroomCourseWork>> = withContext(Dispatchers.IO) {
        val userId = getUserId()
        val account = getClassroomAccount()
        val syncKey = "${userId}_${account}_coursework_${courseId}"

        if (!force && ClassroomSyncManager.isFresh(syncKey)) {
            Log.d(TAG, "CourseWork for course $courseId is fresh (< 3m). Skipping network fetch.")
            return@withContext Result.success(emptyList())
        }

        ClassroomSyncManager.runCoalesced(syncKey) {
            val token = authManager.getClassroomAccessToken() ?: return@runCoalesced Result.failure(
                ClassroomAuthException("No active Google Classroom authorization.")
            )

            try {
                val remoteCourseWork = classroomService.listCourseWork(courseId, token)
                val localCourseWork = classroomDao.getCourseWorkForCourse(courseId, userId).associateBy { it.courseWorkId }
                val localAttachments = classroomDao.getAttachmentsForCourse(courseId, userId).groupBy { it.parentId }

                val remoteIds = remoteCourseWork.map { it.id }.toSet()
                val cwEntitiesToUpsert = mutableListOf<ClassroomCourseWorkEntity>()
                val attEntitiesToUpsert = mutableListOf<ClassroomAttachmentEntity>()

                for (cw in remoteCourseWork) {
                    val local = localCourseWork[cw.id]
                    val existingAtts = localAttachments[cw.id].orEmpty()

                    val isContentChanged = local == null ||
                            local.title != cw.title ||
                            local.description != cw.description ||
                            local.dueFormatted != cw.dueFormatted ||
                            local.alternateLink != cw.alternateLink ||
                            areAttachmentsChanged(existingAtts, cw.attachments)

                    if (isContentChanged) {
                        cwEntitiesToUpsert.add(
                            ClassroomCourseWorkEntity(
                                id = "${userId}_${cw.id}",
                                courseWorkId = cw.id,
                                courseId = courseId,
                                userId = userId,
                                title = cw.title,
                                description = cw.description,
                                dueFormatted = cw.dueFormatted,
                                creationTime = cw.creationTime,
                                alternateLink = cw.alternateLink,
                                lastSyncedAt = System.currentTimeMillis()
                            )
                        )
                        cw.attachments.forEachIndexed { index, att ->
                            attEntitiesToUpsert.add(
                                ClassroomAttachmentEntity(
                                    id = "${userId}_${cw.id}_$index",
                                    parentId = cw.id,
                                    parentType = "COURSEWORK",
                                    courseId = courseId,
                                    userId = userId,
                                    title = att.title,
                                    linkUrl = att.linkUrl,
                                    type = att.type.name,
                                    driveFileId = extractDriveFileId(att.linkUrl),
                                    thumbnailUrl = att.thumbnailUrl
                                )
                            )
                        }
                    }
                }

                if (cwEntitiesToUpsert.isNotEmpty()) {
                    classroomDao.upsertCourseWork(cwEntitiesToUpsert)
                    classroomDao.upsertAttachments(attEntitiesToUpsert)
                    Log.d(TAG, "Upserted ${cwEntitiesToUpsert.size} changed/new coursework into Room.")
                } else {
                    Log.d(TAG, "All ${remoteCourseWork.size} coursework unchanged; skipped Room writes.")
                }

                val staleIds = localCourseWork.keys.filter { it !in remoteIds }
                if (staleIds.isNotEmpty()) {
                    classroomDao.deleteStaleCourseWork(courseId, userId, remoteCourseWork.map { it.id })
                    Log.d(TAG, "Deleted ${staleIds.size} stale coursework from Room.")
                }

                ClassroomSyncManager.markSynced(syncKey)
                Result.success(remoteCourseWork)
            } catch (e: ClassroomAuthException) {
                authManager.refreshAuthState()
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun areAttachmentsChanged(
        local: List<ClassroomAttachmentEntity>,
        remote: List<ClassroomAttachment>
    ): Boolean {
        if (local.size != remote.size) return true
        val localMap = local.associateBy { it.linkUrl }
        for (r in remote) {
            val l = localMap[r.linkUrl] ?: return true
            if (l.title != r.title || l.type != r.type.name || l.thumbnailUrl != r.thumbnailUrl) {
                return true
            }
        }
        return false
    }

    private fun extractDriveFileId(url: String): String? {
        val patterns = listOf(
            Regex("/d/([a-zA-Z0-9_-]+)"),
            Regex("id=([a-zA-Z0-9_-]+)"),
            Regex("/file/d/([a-zA-Z0-9_-]+)")
        )
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }
}
