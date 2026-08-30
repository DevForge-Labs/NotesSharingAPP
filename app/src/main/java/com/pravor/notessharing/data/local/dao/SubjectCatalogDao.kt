package com.pravor.notessharing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pravor.notessharing.data.local.entity.SubjectCatalogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectCatalogDao {

    @Query("""
        SELECT * FROM subject_catalog 
        WHERE LOWER(collegeId) = LOWER(:collegeId)
          AND LOWER(branchId) = LOWER(:branchId)
          AND (LOWER(semester) = LOWER(:semester) OR semester = :semesterNum)
        ORDER BY displayName ASC
    """)
    fun observeSubjectsForScope(
        collegeId: String,
        branchId: String,
        semester: String,
        semesterNum: String
    ): Flow<List<SubjectCatalogEntity>>

    @Query("""
        SELECT * FROM subject_catalog 
        WHERE LOWER(collegeId) = LOWER(:collegeId)
          AND LOWER(branchId) = LOWER(:branchId)
          AND (LOWER(semester) = LOWER(:semester) OR semester = :semesterNum)
        ORDER BY displayName ASC
    """)
    suspend fun getSubjectsForScope(
        collegeId: String,
        branchId: String,
        semester: String,
        semesterNum: String
    ): List<SubjectCatalogEntity>

    @Query("SELECT * FROM subject_catalog WHERE LOWER(subjectId) = LOWER(:subjectId) LIMIT 1")
    suspend fun findSubjectById(subjectId: String): SubjectCatalogEntity?

    @Query("SELECT * FROM subject_catalog WHERE LOWER(collegeId) = LOWER(:collegeId) AND LOWER(subjectId) = LOWER(:subjectId) LIMIT 1")
    suspend fun findSubjectByCollegeAndId(collegeId: String, subjectId: String): SubjectCatalogEntity?

    @Query("SELECT * FROM subject_catalog WHERE active = 1")
    fun observeAllActiveSubjects(): Flow<List<SubjectCatalogEntity>>

    @Query("SELECT * FROM subject_catalog")
    suspend fun getAllSubjects(): List<SubjectCatalogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubjects(subjects: List<SubjectCatalogEntity>): List<Long>

    @Query("DELETE FROM subject_catalog WHERE LOWER(collegeId) = LOWER(:collegeId)")
    suspend fun clearCollegeCatalog(collegeId: String): Int

    @Query("DELETE FROM subject_catalog")
    suspend fun clearAll(): Int
}
