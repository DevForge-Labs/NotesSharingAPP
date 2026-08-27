package com.pravor.notessharing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pravor.notessharing.data.local.entity.ClassroomAnnouncementEntity
import com.pravor.notessharing.data.local.entity.ClassroomAttachmentEntity
import com.pravor.notessharing.data.local.entity.ClassroomCourseEntity
import com.pravor.notessharing.data.local.entity.ClassroomCourseWorkEntity
import com.pravor.notessharing.data.local.entity.ClassroomFileEntity
import com.pravor.notessharing.data.local.entity.ClassroomHiddenCourseEntity
import com.pravor.notessharing.data.local.entity.ClassroomMaterialEntity
import com.pravor.notessharing.data.local.entity.ClassroomSubmissionEntity
import com.pravor.notessharing.data.local.entity.ClassroomManualCompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassroomDao {

    // --- Courses ---
    @Query("SELECT * FROM classroom_courses WHERE userId = :userId AND classroomAccount = :classroomAccount AND state = 'ACTIVE' ORDER BY name ASC")
    fun observeCourses(userId: String, classroomAccount: String): Flow<List<ClassroomCourseEntity>>

    @Query("SELECT * FROM classroom_courses WHERE courseId = :courseId AND userId = :userId LIMIT 1")
    suspend fun getCourse(courseId: String, userId: String): ClassroomCourseEntity?

    @Query("SELECT * FROM classroom_courses WHERE userId = :userId AND classroomAccount = :classroomAccount")
    suspend fun getCoursesForAccount(userId: String, classroomAccount: String): List<ClassroomCourseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCourses(courses: List<ClassroomCourseEntity>): List<Long>

    @Query("DELETE FROM classroom_courses WHERE userId = :userId AND classroomAccount = :classroomAccount AND courseId NOT IN (:keepCourseIds)")
    suspend fun deleteStaleCourses(userId: String, classroomAccount: String, keepCourseIds: List<String>): Int

    @Query("DELETE FROM classroom_courses WHERE userId = :userId AND classroomAccount = :classroomAccount")
    suspend fun clearAccountCourses(userId: String, classroomAccount: String): Int

    // --- Hidden Courses ---
    @Query("SELECT courseId FROM classroom_hidden_courses WHERE userId = :userId AND classroomAccount = :classroomAccount")
    fun observeHiddenCourseIds(userId: String, classroomAccount: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hideCourses(hidden: List<ClassroomHiddenCourseEntity>): List<Long>

    @Query("DELETE FROM classroom_hidden_courses WHERE userId = :userId AND classroomAccount = :classroomAccount AND courseId IN (:unhideCourseIds)")
    suspend fun unhideCourses(userId: String, classroomAccount: String, unhideCourseIds: List<String>): Int

    @Query("DELETE FROM classroom_hidden_courses WHERE userId = :userId AND classroomAccount = :classroomAccount")
    suspend fun clearHiddenCoursesForAccount(userId: String, classroomAccount: String): Int

    // --- Materials ---
    @Query("SELECT * FROM classroom_materials WHERE courseId = :courseId AND userId = :userId ORDER BY creationTime DESC")
    fun observeMaterials(courseId: String, userId: String): Flow<List<ClassroomMaterialEntity>>

    @Query("SELECT * FROM classroom_materials WHERE courseId = :courseId AND userId = :userId")
    suspend fun getMaterialsForCourse(courseId: String, userId: String): List<ClassroomMaterialEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMaterials(materials: List<ClassroomMaterialEntity>): List<Long>

    @Query("DELETE FROM classroom_materials WHERE courseId = :courseId AND userId = :userId AND materialId NOT IN (:keepIds)")
    suspend fun deleteStaleMaterials(courseId: String, userId: String, keepIds: List<String>): Int

    // --- Announcements ---
    @Query("SELECT * FROM classroom_announcements WHERE courseId = :courseId AND userId = :userId ORDER BY creationTime DESC")
    fun observeAnnouncements(courseId: String, userId: String): Flow<List<ClassroomAnnouncementEntity>>

    @Query("SELECT * FROM classroom_announcements WHERE courseId = :courseId AND userId = :userId")
    suspend fun getAnnouncementsForCourse(courseId: String, userId: String): List<ClassroomAnnouncementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnnouncements(announcements: List<ClassroomAnnouncementEntity>): List<Long>

    @Query("DELETE FROM classroom_announcements WHERE courseId = :courseId AND userId = :userId AND announcementId NOT IN (:keepIds)")
    suspend fun deleteStaleAnnouncements(courseId: String, userId: String, keepIds: List<String>): Int

    // --- CourseWork ---
    @Query("SELECT * FROM classroom_coursework WHERE courseId = :courseId AND userId = :userId ORDER BY creationTime DESC")
    fun observeCourseWork(courseId: String, userId: String): Flow<List<ClassroomCourseWorkEntity>>

    @Query("SELECT * FROM classroom_coursework WHERE courseId = :courseId AND userId = :userId")
    suspend fun getCourseWorkForCourse(courseId: String, userId: String): List<ClassroomCourseWorkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCourseWork(courseWork: List<ClassroomCourseWorkEntity>): List<Long>

    @Query("DELETE FROM classroom_coursework WHERE courseId = :courseId AND userId = :userId AND courseWorkId NOT IN (:keepIds)")
    suspend fun deleteStaleCourseWork(courseId: String, userId: String, keepIds: List<String>): Int

    // --- Attachments ---
    @Query("SELECT * FROM classroom_attachments WHERE courseId = :courseId AND userId = :userId")
    fun observeAttachmentsForCourse(courseId: String, userId: String): Flow<List<ClassroomAttachmentEntity>>

    @Query("SELECT * FROM classroom_attachments WHERE courseId = :courseId AND userId = :userId")
    suspend fun getAttachmentsForCourse(courseId: String, userId: String): List<ClassroomAttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttachments(attachments: List<ClassroomAttachmentEntity>): List<Long>

    @Query("DELETE FROM classroom_attachments WHERE courseId = :courseId AND userId = :userId AND parentId NOT IN (:keepParentIds)")
    suspend fun deleteStaleAttachments(courseId: String, userId: String, keepParentIds: List<String>): Int

    // --- Cached Files ---
    @Query("SELECT * FROM classroom_cached_files WHERE driveFileId = :driveFileId AND userId = :userId LIMIT 1")
    suspend fun getCachedFile(driveFileId: String, userId: String): ClassroomFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedFile(file: ClassroomFileEntity): Long

    // --- Submissions ---
    @Query("SELECT * FROM classroom_submissions WHERE courseId = :courseId AND userId = :userId")
    fun observeSubmissions(courseId: String, userId: String): Flow<List<ClassroomSubmissionEntity>>

    @Query("SELECT * FROM classroom_submissions WHERE courseId = :courseId AND courseWorkId = :courseWorkId AND userId = :userId LIMIT 1")
    suspend fun getSubmission(courseId: String, courseWorkId: String, userId: String): ClassroomSubmissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubmissions(submissions: List<ClassroomSubmissionEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubmission(submission: ClassroomSubmissionEntity): Long

    @Query("DELETE FROM classroom_submissions WHERE courseId = :courseId AND userId = :userId AND courseWorkId NOT IN (:keepCourseWorkIds)")
    suspend fun deleteStaleSubmissions(courseId: String, userId: String, keepCourseWorkIds: List<String>): Int

    // --- Manual External Completions ---
    @Query("SELECT courseWorkId FROM classroom_manual_completions WHERE courseId = :courseId AND userId = :userId AND completed = 1")
    fun observeManualCompletions(courseId: String, userId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertManualCompletion(entity: ClassroomManualCompletionEntity): Long

    @Query("DELETE FROM classroom_manual_completions WHERE courseWorkId = :courseWorkId AND userId = :userId")
    suspend fun deleteManualCompletion(courseWorkId: String, userId: String): Int

    @Query("DELETE FROM classroom_manual_completions WHERE userId = :userId")
    suspend fun clearManualCompletionsForUser(userId: String): Int

    // --- Aggregated Queries for Upcoming Assignments ---
    @Query("SELECT * FROM classroom_coursework WHERE userId = :userId")
    fun observeAllCourseWork(userId: String): Flow<List<ClassroomCourseWorkEntity>>

    @Query("SELECT * FROM classroom_coursework WHERE userId = :userId")
    suspend fun getAllCourseWork(userId: String): List<ClassroomCourseWorkEntity>

    @Query("SELECT * FROM classroom_attachments WHERE userId = :userId")
    fun observeAllAttachments(userId: String): Flow<List<ClassroomAttachmentEntity>>

    @Query("SELECT * FROM classroom_attachments WHERE userId = :userId")
    suspend fun getAllAttachments(userId: String): List<ClassroomAttachmentEntity>

    @Query("SELECT * FROM classroom_submissions WHERE userId = :userId")
    fun observeAllSubmissions(userId: String): Flow<List<ClassroomSubmissionEntity>>

    @Query("SELECT * FROM classroom_submissions WHERE userId = :userId")
    suspend fun getAllSubmissions(userId: String): List<ClassroomSubmissionEntity>

    @Query("SELECT courseWorkId FROM classroom_manual_completions WHERE userId = :userId AND completed = 1")
    fun observeAllManualCompletions(userId: String): Flow<List<String>>

    @Query("SELECT courseWorkId FROM classroom_manual_completions WHERE userId = :userId AND completed = 1")
    suspend fun getAllManualCompletions(userId: String): List<String>

    @Query("SELECT courseId FROM classroom_hidden_courses WHERE userId = :userId AND classroomAccount = :classroomAccount")
    suspend fun getHiddenCourseIds(userId: String, classroomAccount: String): List<String>
}
