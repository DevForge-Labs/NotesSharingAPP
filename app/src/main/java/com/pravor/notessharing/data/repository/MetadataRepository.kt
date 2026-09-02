package com.pravor.notessharing.data.repository

import com.pravor.notessharing.core.util.*

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.core.util.LegacyAcademicCompatibilityResolver
import kotlinx.coroutines.tasks.await
import java.util.Locale

class MetadataRepository {
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        // Shared in-memory caches to maintain a single source of truth and optimize network usage
        @Volatile
        private var collegesCache: List<CollegeMetadata>? = null

        @Volatile
        private var subjectCatalogCache: Map<String, Any>? = null

        fun updateSubjectCatalogCache(data: Map<String, Any>) {
            subjectCatalogCache = data
        }
    }

    /**
     * Fetches the dynamic list of supported colleges from Firestore app_config/colleges.
     */
    suspend fun getColleges(): List<CollegeMetadata> {
        collegesCache?.let { return it }
        return try {
            val snapshot = firestore.collection("app_config")
                .document("colleges")
                .get()
                .await()
            if (snapshot.exists()) {
                val rawList = snapshot.get("colleges") as? List<*>
                val colleges = rawList?.mapNotNull { item ->
                    val map = item as? Map<*, *>
                    val id = map?.get("id")?.toString() ?: ""
                    val name = map?.get("name")?.toString() ?: ""
                    if (id.isNotEmpty()) CollegeMetadata(id, name) else null
                } ?: emptyList()
                collegesCache = colleges
                colleges
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("MetadataRepository", "Error loading colleges from Firestore", e)
            emptyList()
        }
    }

    /**
     * Fetches the dynamic subject catalog from Firestore app_config/subject_catalog.
     */
    suspend fun getSubjectCatalog(): Map<String, Any> {
        subjectCatalogCache?.let { return it }
        return try {
            val snapshot = firestore.collection("app_config")
                .document("subject_catalog")
                .get()
                .await()
            val data = snapshot.data ?: emptyMap()
            subjectCatalogCache = data
            data
        } catch (e: Exception) {
            Log.e("MetadataRepository", "Error loading subject catalog from Firestore", e)
            emptyMap()
        }
    }

    /**
     * Resolves and returns the branches for a specific college.
     */
    suspend fun getBranchesForCollege(collegeId: String): List<BranchMetadata> {
        val canonicalCollegeId = LegacyAcademicCompatibilityResolver.resolveCollegeId(collegeId)
        val catalog = getSubjectCatalog()
        val collegeCatalog = catalog[canonicalCollegeId] as? Map<*, *> ?: return emptyList()
        return collegeCatalog.keys
            .map { it.toString() }
            .filter { !it.startsWith("GROUP_", ignoreCase = true) }
            .map { branchId ->
                val branchData = collegeCatalog[branchId] as? Map<*, *>
                val displayName = branchData?.get("name")?.toString() ?: branchId.uppercase(Locale.ROOT)
                BranchMetadata(id = branchId, name = displayName)
            }
    }

    /**
     * Resolves a college ID to its display name.
     */
    suspend fun resolveCollegeName(collegeId: String): String {
        val canonicalCollegeId = LegacyAcademicCompatibilityResolver.resolveCollegeId(collegeId)
        val colleges = getColleges()
        return colleges.firstOrNull { it.id.equals(canonicalCollegeId, ignoreCase = true) }?.name
            ?: collegeId.uppercase(Locale.ROOT)
    }

    /**
     * Resolves a branch ID or legacy branch value (e.g. "Computer Science") to its display name.
     */
    suspend fun resolveBranchName(collegeId: String, branchIdOrName: String): String {
        val canonicalCollegeId = LegacyAcademicCompatibilityResolver.resolveCollegeId(collegeId)
        val canonicalBranchId = LegacyAcademicCompatibilityResolver.resolveBranchId(branchIdOrName)
        val branches = getBranchesForCollege(canonicalCollegeId)
        
        // 1. Try match by ID
        branches.firstOrNull { it.id.equals(canonicalBranchId, ignoreCase = true) }?.let { return it.name }
        
        // 2. Try match by display name
        branches.firstOrNull { it.name.equals(branchIdOrName, ignoreCase = true) }?.let { return it.name }

        // Fallback to capitalizing the identifier
        return branchIdOrName.uppercase(Locale.ROOT)
    }
}

data class CollegeMetadata(val id: String, val name: String)
data class BranchMetadata(val id: String, val name: String)
