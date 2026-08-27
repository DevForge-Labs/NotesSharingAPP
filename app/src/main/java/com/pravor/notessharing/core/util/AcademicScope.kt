package com.pravor.notessharing.core.util

import com.pravor.notessharing.data.repository.MetadataRepository
import com.pravor.notessharing.domain.model.Profile
import java.util.Locale

/**
 * Encapsulates the canonical academic identity of a user or query context.
 */
data class AcademicScope(
    val collegeId: String,
    val branchId: String? = null,
    val semester: String? = null,
    val subjectIds: List<String> = emptyList()
) {
    val canonicalCollegeId: String get() = LegacyAcademicCompatibilityResolver.resolveCollegeId(collegeId)
    val canonicalBranchId: String? get() = branchId?.takeIf { it.isNotBlank() }?.let { LegacyAcademicCompatibilityResolver.resolveBranchId(it) }
    val isCollegeValid: Boolean get() = canonicalCollegeId.isNotBlank()
    val hasSemester: Boolean get() = !semester.isNullOrBlank() && semester != "Not Set"
    val hasBranch: Boolean get() = !canonicalBranchId.isNullOrBlank()

    /**
     * Unique key for caching feeds and search results scoped to this exact academic context.
     */
    val scopeKey: String
        get() {
            if (!isCollegeValid) return "global"
            val parts = mutableListOf(canonicalCollegeId)
            if (hasBranch) parts.add(canonicalBranchId!!)
            if (hasSemester) parts.add(semester!!.filter { it.isDigit() }.ifEmpty { semester!!.trim().lowercase(Locale.ROOT).replace(" ", "_") })
            return parts.joinToString("_")
        }

    /**
     * Checks if a resource document is authorized / permitted within this academic scope.
     * Used for direct document retrieval, video viewing, and detailed authorization checks.
     */
    fun isDocumentPermitted(
        docCollege: String?,
        docBranch: String?,
        docSemester: String?,
        docSubjectId: String?
    ): Boolean {
        if (!isCollegeValid) return true

        // 1. College verification: Must match canonical college if specified
        if (!docCollege.isNullOrBlank()) {
            val docCanonicalCollege = LegacyAcademicCompatibilityResolver.resolveCollegeId(docCollege)
            if (docCanonicalCollege != canonicalCollegeId) {
                return false
            }
        }

        // 2. Direct Subject ID verification: If catalog subject matches, permit immediately
        if (!docSubjectId.isNullOrBlank() && subjectIds.isNotEmpty()) {
            if (subjectIds.any { it.equals(docSubjectId, ignoreCase = true) }) {
                return true
            }
        }

        // 3. Semester verification
        if (hasSemester && !docSemester.isNullOrBlank() && docSemester != "Not Set") {
            val userSemDigits = semester!!.filter { it.isDigit() }
            val docSemDigits = docSemester.filter { it.isDigit() }
            if (userSemDigits.isNotEmpty() && docSemDigits.isNotEmpty()) {
                if (userSemDigits != docSemDigits) {
                    return false
                }
            } else if (!docSemester.equals(semester, ignoreCase = true)) {
                return false
            }
        }

        // 4. Branch verification (for 2nd year onwards)
        val isFirstYear = semester?.let {
            val lower = it.lowercase(Locale.ROOT)
            lower.contains("sem 1") || lower.contains("sem 2") || lower.contains("semester 1") || lower.contains("semester 2") || it.filter { c -> c.isDigit() } in listOf("1", "2")
        } ?: false

        if (!isFirstYear && hasBranch && !docBranch.isNullOrBlank()) {
            val docCanonicalBranch = LegacyAcademicCompatibilityResolver.resolveBranchId(docBranch)
            if (docCanonicalBranch != canonicalBranchId && docCanonicalBranch != "common" && docCanonicalBranch != "all") {
                return false
            }
        }

        return true
    }
}

/**
 * Resolves full AcademicScope including dynamic subject catalog IDs for a given user profile.
 */
object AcademicScopeResolver {

    suspend fun resolve(
        profile: Profile?,
        metadataRepository: MetadataRepository = MetadataRepository()
    ): AcademicScope {
        if (profile == null) {
            return AcademicScope(collegeId = "")
        }

        val rawCollege = profile.college.takeIf { it.isNotBlank() } ?: return AcademicScope(collegeId = "")
        val canonicalCollegeId = LegacyAcademicCompatibilityResolver.resolveCollegeId(rawCollege)
        val rawBranch = profile.branch.takeIf { it.isNotBlank() }
        val rawSemester = profile.semester.takeIf { it.isNotBlank() && it != "Not Set" }

        val hasSemester = !rawSemester.isNullOrBlank()
        val hasBranch = !rawBranch.isNullOrBlank()

        val subjectIds = if (hasSemester && hasBranch) {
            try {
                val branchId = LegacyAcademicCompatibilityResolver.resolveBranchId(rawBranch!!)
                val catalog = metadataRepository.getSubjectCatalog()
                val collegeCatalog = catalog[canonicalCollegeId.lowercase(Locale.ROOT)] as? Map<*, *>

                val semLower = rawSemester!!.trim().lowercase(Locale.ROOT)
                val isFirstYear = semLower.contains("semester 1") || semLower.contains("sem 1") || semLower == "1" || semLower.startsWith("1st") ||
                        semLower.contains("semester 2") || semLower.contains("sem 2") || semLower == "2" || semLower.startsWith("2nd")

                val semesterData = if (isFirstYear) {
                    val isGroupA = semLower.contains("semester 1") || semLower.contains("sem 1") || semLower == "1" || semLower.startsWith("1st")
                    val groupKey = if (isGroupA) "GROUP_A" else "GROUP_B"
                    collegeCatalog?.get(groupKey)
                } else {
                    val branchCatalog = collegeCatalog?.get(branchId) as? Map<*, *>
                    val semNum = rawSemester.filter { it.isDigit() }
                    branchCatalog?.get(rawSemester) ?: (if (semNum.isNotEmpty()) branchCatalog?.get(semNum) else null)
                }

                when (semesterData) {
                    is List<*> -> semesterData.mapNotNull { item ->
                        when (item) {
                            is Map<*, *> -> (item["id"] ?: item["subjectId"])?.toString()
                            is String -> item
                            else -> null
                        }
                    }
                    is Map<*, *> -> semesterData.keys.mapNotNull { it?.toString() }
                    else -> emptyList()
                }.filter { it.isNotBlank() }
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        return AcademicScope(
            collegeId = canonicalCollegeId,
            branchId = rawBranch,
            semester = rawSemester,
            subjectIds = subjectIds
        )
    }
}
