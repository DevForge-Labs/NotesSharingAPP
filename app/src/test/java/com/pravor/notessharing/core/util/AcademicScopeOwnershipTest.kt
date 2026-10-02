package com.pravor.notessharing.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AcademicScopeOwnershipTest {

    private val userCollege = "kiit"
    private val otherCollege = "iter"
    private val currentUserId = "student_user_123"
    private val otherUserId = "other_student_456"

    @Test
    fun `user in 5th semester can access own 4th semester note`() {
        val userScope = AcademicScope(
            collegeId = userCollege,
            branchId = "cse",
            semester = "Semester 5",
            subjectIds = listOf("sub_sem5_algo"),
            userId = currentUserId
        )

        val isPermitted = userScope.isDocumentPermitted(
            docCollege = userCollege,
            docBranch = "cse",
            docSemester = "Semester 4",
            docSubjectId = "sub_sem4_os",
            docSubjectName = "Operating Systems",
            docUploaderId = currentUserId
        )

        assertTrue("User must be permitted to access their own cross-semester note", isPermitted)
    }

    @Test
    fun `user in 5th semester can access own 3rd semester video`() {
        val userScope = AcademicScope(
            collegeId = userCollege,
            branchId = "cse",
            semester = "Semester 5",
            subjectIds = listOf("sub_sem5_algo"),
            userId = currentUserId
        )

        val isPermitted = userScope.isDocumentPermitted(
            docCollege = userCollege,
            docBranch = "cse",
            docSemester = "Semester 3",
            docSubjectId = "sub_sem3_dsa",
            docSubjectName = "Data Structures",
            docUploaderId = currentUserId
        )

        assertTrue("User must be permitted to access their own cross-semester video", isPermitted)
    }

    @Test
    fun `user in 5th semester can access 5th semester note uploaded by another student`() {
        val userScope = AcademicScope(
            collegeId = userCollege,
            branchId = "cse",
            semester = "Semester 5",
            subjectIds = listOf("sub_sem5_algo"),
            userId = currentUserId
        )

        val isPermitted = userScope.isDocumentPermitted(
            docCollege = userCollege,
            docBranch = "cse",
            docSemester = "Semester 5",
            docSubjectId = "sub_sem5_algo",
            docSubjectName = "Algorithms",
            docUploaderId = otherUserId
        )

        assertTrue("User must be permitted to access peer's note from their own semester", isPermitted)
    }

    @Test
    fun `user in 5th semester cannot access another student 4th semester note`() {
        val userScope = AcademicScope(
            collegeId = userCollege,
            branchId = "cse",
            semester = "Semester 5",
            subjectIds = listOf("sub_sem5_algo"),
            userId = currentUserId
        )

        val isPermitted = userScope.isDocumentPermitted(
            docCollege = userCollege,
            docBranch = "cse",
            docSemester = "Semester 4",
            docSubjectId = "sub_sem4_os",
            docSubjectName = "Operating Systems",
            docUploaderId = otherUserId
        )

        assertFalse("User must NOT be permitted to access peer's note from another semester", isPermitted)
    }

    @Test
    fun `user in 3rd semester cannot access another student 5th semester resource`() {
        val userScope = AcademicScope(
            collegeId = userCollege,
            branchId = "cse",
            semester = "Semester 3",
            subjectIds = listOf("sub_sem3_dsa"),
            userId = currentUserId
        )

        val isPermitted = userScope.isDocumentPermitted(
            docCollege = userCollege,
            docBranch = "cse",
            docSemester = "Semester 5",
            docSubjectId = "sub_sem5_algo",
            docSubjectName = "Algorithms",
            docUploaderId = otherUserId
        )

        assertFalse("User in 3rd sem must NOT be permitted to access peer's 5th sem resource", isPermitted)
    }

    @Test
    fun `user cannot access resource from another college even if they are the uploader`() {
        val userScope = AcademicScope(
            collegeId = userCollege,
            branchId = "cse",
            semester = "Semester 5",
            subjectIds = listOf("sub_sem5_algo"),
            userId = currentUserId
        )

        val isPermitted = userScope.isDocumentPermitted(
            docCollege = otherCollege,
            docBranch = "cse",
            docSemester = "Semester 4",
            docSubjectId = "sub_sem4_os",
            docSubjectName = "Operating Systems",
            docUploaderId = currentUserId
        )

        assertFalse("College boundary must be strictly enforced even for own uploads", isPermitted)
    }

    @Test
    fun `user with null or blank userId cannot access cross-semester resource`() {
        val userScope = AcademicScope(
            collegeId = userCollege,
            branchId = "cse",
            semester = "Semester 5",
            subjectIds = listOf("sub_sem5_algo"),
            userId = null
        )

        val isPermitted = userScope.isDocumentPermitted(
            docCollege = userCollege,
            docBranch = "cse",
            docSemester = "Semester 4",
            docSubjectId = "sub_sem4_os",
            docSubjectName = "Operating Systems",
            docUploaderId = currentUserId
        )

        assertFalse("Missing requesting userId must not grant ownership exception", isPermitted)
    }

    @Test
    fun `resource with missing, blank, or dummy uploaderId cannot be granted ownership access`() {
        val userScope = AcademicScope(
            collegeId = userCollege,
            branchId = "cse",
            semester = "Semester 5",
            subjectIds = listOf("sub_sem5_algo"),
            userId = "dummy-uid"
        )

        val isPermittedWithDummy = userScope.isDocumentPermitted(
            docCollege = userCollege,
            docBranch = "cse",
            docSemester = "Semester 4",
            docSubjectId = "sub_sem4_os",
            docSubjectName = "Operating Systems",
            docUploaderId = "dummy-uid"
        )
        assertFalse("Placeholder dummy-uid must never grant ownership access", isPermittedWithDummy)

        val validUserScope = AcademicScope(
            collegeId = userCollege,
            branchId = "cse",
            semester = "Semester 5",
            subjectIds = listOf("sub_sem5_algo"),
            userId = currentUserId
        )

        val isPermittedWithBlank = validUserScope.isDocumentPermitted(
            docCollege = userCollege,
            docBranch = "cse",
            docSemester = "Semester 4",
            docSubjectId = "sub_sem4_os",
            docSubjectName = "Operating Systems",
            docUploaderId = ""
        )
        assertFalse("Blank uploaderId must never grant ownership access", isPermittedWithBlank)

        val isPermittedWithNull = validUserScope.isDocumentPermitted(
            docCollege = userCollege,
            docBranch = "cse",
            docSemester = "Semester 4",
            docSubjectId = "sub_sem4_os",
            docSubjectName = "Operating Systems",
            docUploaderId = null
        )
        assertFalse("Null uploaderId must never grant ownership access", isPermittedWithNull)
    }

    @Test
    fun `peer resource in different branch for 2nd year onwards is denied`() {
        val userScope = AcademicScope(
            collegeId = userCollege,
            branchId = "cse",
            semester = "Semester 5",
            subjectIds = emptyList(), // no catalog match
            userId = currentUserId
        )

        val isPermitted = userScope.isDocumentPermitted(
            docCollege = userCollege,
            docBranch = "mechanical",
            docSemester = "Semester 5",
            docSubjectId = null,
            docSubjectName = "Thermodynamics",
            docUploaderId = otherUserId
        )

        assertFalse("Different branch resource for peer must be denied", isPermitted)
    }
}
