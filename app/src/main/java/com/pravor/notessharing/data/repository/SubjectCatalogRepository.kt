package com.pravor.notessharing.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.pravor.notessharing.NotesSharingApplication
import com.pravor.notessharing.core.util.LegacyAcademicCompatibilityResolver
import com.pravor.notessharing.data.local.db.AppDatabase
import com.pravor.notessharing.data.local.dao.SubjectCatalogDao
import com.pravor.notessharing.data.local.entity.SubjectCatalogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class SubjectMetadata(
    val id: String,
    val name: String,
    val shortName: String,
    val active: Boolean = true
)

class SubjectCatalogRepository(
    context: Context = NotesSharingApplication.appContext,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    private val subjectDao: SubjectCatalogDao = database.subjectCatalogDao(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "SubjectCatalogRepo"

        @Volatile
        private var instance: SubjectCatalogRepository? = null

        fun getInstance(context: Context = NotesSharingApplication.appContext): SubjectCatalogRepository {
            return instance ?: synchronized(this) {
                instance ?: SubjectCatalogRepository(context.applicationContext).also { instance = it }
            }
        }

        // Fast global memory cache mapping canonical subjectId (lowercase) -> SubjectMetadata
        private val inMemorySubjectCache = ConcurrentHashMap<String, SubjectMetadata>()
        val catalogVersionFlow = kotlinx.coroutines.flow.MutableStateFlow<Long>(1L)
    }

    val catalogVersionFlow get() = SubjectCatalogRepository.catalogVersionFlow

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // 1. Prime in-memory cache from Room on startup
        repositoryScope.launch {
            try {
                val cached = subjectDao.getAllSubjects()
                cached.forEach { entity ->
                    inMemorySubjectCache[entity.subjectId.lowercase()] = SubjectMetadata(
                        id = entity.subjectId,
                        name = entity.displayName,
                        shortName = entity.shortName,
                        active = entity.active
                    )
                }
                if (cached.isNotEmpty()) {
                    catalogVersionFlow.value = System.currentTimeMillis()
                    Log.d(TAG, "Primed ${cached.size} subjects from Room into memory cache")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error priming subject cache from Room", e)
            }
        }

        // 2. Attach real-time snapshot listener for instant live updates from Firestore
        try {
            firestore.collection("app_config")
                .document("subject_catalog")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Subject catalog snapshot listener error", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists() && snapshot.data != null) {
                        repositoryScope.launch {
                            try {
                                parseAndSaveCatalog(snapshot.data!!)
                            } catch (e: Exception) {
                                Log.w(TAG, "Error processing real-time catalog update", e)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to attach real-time catalog listener", e)
        }
    }

    /**
     * Resolves the compact badge / short name for a subject (e.g. "daa" -> "DAA", "se" -> "hard").
     * Uses in-memory cache first (scoped + global), then searches catalog entries, then falls back to fallbackName.
     */
    fun resolveShortName(
        subjectId: String?,
        fallbackName: String? = null,
        branchId: String? = null,
        semester: String? = null,
        collegeId: String? = null
    ): String {
        if (subjectId.isNullOrBlank() && fallbackName.isNullOrBlank()) return ""
        val cleanId = subjectId?.trim()?.lowercase() ?: ""
        val cleanFallback = fallbackName?.trim() ?: ""
        val targetLookupKey = cleanId.ifEmpty { cleanFallback.lowercase() }
        val cleanBranch = branchId?.trim()?.lowercase() ?: ""
        val semNum = semester?.filter { it.isDigit() }?.ifEmpty { semester?.trim() } ?: ""

        // 1. Try precise branch + semester scoped lookup
        if (cleanBranch.isNotEmpty() && semNum.isNotEmpty()) {
            val key = "${cleanBranch}::${semNum}::${targetLookupKey}"
            inMemorySubjectCache[key]?.let { return it.shortName }
        }
        if (cleanBranch.isNotEmpty()) {
            val key = "${cleanBranch}::${targetLookupKey}"
            inMemorySubjectCache[key]?.let { return it.shortName }
        }

        // 2. Try direct subjectId in cache
        if (cleanId.isNotEmpty()) {
            inMemorySubjectCache[cleanId]?.let { return it.shortName }
        }

        // 3. Try fallbackName in cache
        if (cleanFallback.isNotEmpty()) {
            val fallbackKey = cleanFallback.lowercase()
            inMemorySubjectCache[fallbackKey]?.let { return it.shortName }

            // Search by full display name, short name, or id in catalog
            val match = inMemorySubjectCache.values.firstOrNull {
                it.name.equals(cleanFallback, ignoreCase = true) ||
                it.shortName.equals(cleanFallback, ignoreCase = true) ||
                it.id.equals(cleanFallback, ignoreCase = true)
            }
            if (match != null) return match.shortName
            return cleanFallback
        }
        return cleanId.uppercase()
    }

    /**
     * Resolves the full human-readable display name for a subject (e.g. "daa" -> "Design and Analysis of Algorithms", "se" -> "Soft").
     */
    fun resolveDisplayName(
        subjectId: String?,
        fallbackName: String? = null,
        branchId: String? = null,
        semester: String? = null,
        collegeId: String? = null
    ): String {
        if (subjectId.isNullOrBlank() && fallbackName.isNullOrBlank()) return ""
        val cleanId = subjectId?.trim()?.lowercase() ?: ""
        val cleanFallback = fallbackName?.trim() ?: ""
        val targetLookupKey = cleanId.ifEmpty { cleanFallback.lowercase() }
        val cleanBranch = branchId?.trim()?.lowercase() ?: ""
        val semNum = semester?.filter { it.isDigit() }?.ifEmpty { semester?.trim() } ?: ""

        // 1. Try precise branch + semester scoped lookup
        if (cleanBranch.isNotEmpty() && semNum.isNotEmpty()) {
            val key = "${cleanBranch}::${semNum}::${targetLookupKey}"
            inMemorySubjectCache[key]?.let { return it.name }
        }
        if (cleanBranch.isNotEmpty()) {
            val key = "${cleanBranch}::${targetLookupKey}"
            inMemorySubjectCache[key]?.let { return it.name }
        }

        // 2. Try direct subjectId in cache
        if (cleanId.isNotEmpty()) {
            inMemorySubjectCache[cleanId]?.let { return it.name }
        }

        // 3. Try fallbackName in cache
        if (cleanFallback.isNotEmpty()) {
            val fallbackKey = cleanFallback.lowercase()
            inMemorySubjectCache[fallbackKey]?.let { return it.name }

            val match = inMemorySubjectCache.values.firstOrNull {
                it.name.equals(cleanFallback, ignoreCase = true) ||
                it.shortName.equals(cleanFallback, ignoreCase = true) ||
                it.id.equals(cleanFallback, ignoreCase = true)
            }
            if (match != null) return match.name
            return cleanFallback
        }
        return cleanId.uppercase()
    }

    /**
     * Observes subjects for a specific academic scope from local Room database.
     */
    fun observeSubjectsForScope(
        collegeId: String,
        branchId: String,
        semester: String
    ): Flow<List<SubjectMetadata>> {
        val canonicalCollegeId = LegacyAcademicCompatibilityResolver.resolveCollegeId(collegeId)
        val canonicalBranchId = LegacyAcademicCompatibilityResolver.resolveBranchId(branchId)
        val digits = semester.filter { it.isDigit() }
        val semesterNum = digits.ifEmpty { "1" }

        val branchQueryKey = if (semesterNum == "1" || semesterNum == "2") {
            // For first year, include both GROUP_A and GROUP_B or requested group
            if (branchId.startsWith("GROUP_", ignoreCase = true)) branchId.uppercase() else "GROUP_A"
        } else {
            canonicalBranchId
        }

        return subjectDao.observeSubjectsForScope(
            collegeId = canonicalCollegeId,
            branchId = branchQueryKey,
            semester = semester,
            semesterNum = semesterNum
        ).map { entities ->
            entities.map {
                SubjectMetadata(
                    id = it.subjectId,
                    name = it.displayName,
                    shortName = it.shortName,
                    active = it.active
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Synchronizes the full subject catalog from Firestore app_config/subject_catalog into local Room SQLite.
     * Offline-friendly: retains existing Room catalog on failure.
     */
    suspend fun syncCatalog(force: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("app_config")
                .document("subject_catalog")
                .get()
                .await()

            if (!snapshot.exists() || snapshot.data == null) {
                return@withContext Result.failure(Exception("subject_catalog document does not exist in Firestore"))
            }

            parseAndSaveCatalog(snapshot.data!!)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync subject catalog from Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun parseAndSaveCatalog(rawCatalog: Map<String, Any>) = withContext(Dispatchers.IO) {
        val entitiesToPersist = mutableListOf<SubjectCatalogEntity>()
        val newCacheMap = mutableMapOf<String, SubjectMetadata>()
        val now = System.currentTimeMillis()

        for ((collegeKey, collegeValue) in rawCatalog) {
            val collegeId = collegeKey.lowercase().trim()
            val collegeMap = collegeValue as? Map<*, *> ?: continue

            for ((branchKey, branchValue) in collegeMap) {
                val branchStr = branchKey.toString().trim()

                // Handle first-year groups: GROUP_A, GROUP_B
                if (branchStr.startsWith("GROUP_", ignoreCase = true)) {
                    val groupKey = branchStr.uppercase()
                    val subjectsList = branchValue as? List<*> ?: continue
                    for (item in subjectsList) {
                        val subjectMap = item as? Map<*, *> ?: continue
                        val subId = subjectMap["id"]?.toString()?.trim()?.lowercase() ?: continue
                        val subName = subjectMap["name"]?.toString()?.trim() ?: subId
                        val subShortName = subjectMap["shortName"]?.toString()?.trim() ?: subName
                        val isActive = subjectMap["active"] as? Boolean ?: true

                        val entity = SubjectCatalogEntity(
                            collegeId = collegeId,
                            branchId = groupKey,
                            semester = if (groupKey == "GROUP_A") "Semester 1" else "Semester 2",
                            subjectId = subId,
                            displayName = subName,
                            shortName = subShortName,
                            active = isActive,
                            lastSyncedAtMs = now
                        )
                        entitiesToPersist.add(entity)

                        val metadata = SubjectMetadata(subId, subName, subShortName, isActive)
                        val semNum = if (groupKey == "GROUP_A") "1" else "2"
                        newCacheMap["${collegeId}::${groupKey.lowercase()}::${semNum}::${subId}"] = metadata
                        newCacheMap["${groupKey.lowercase()}::${semNum}::${subId}"] = metadata
                        newCacheMap["${groupKey.lowercase()}::${subId}"] = metadata
                        if (subName.isNotBlank() && !subName.equals(subId, ignoreCase = true)) {
                            newCacheMap[subName.lowercase()] = metadata
                        }
                        if (subShortName.isNotBlank() && !subShortName.equals(subId, ignoreCase = true)) {
                            newCacheMap[subShortName.lowercase()] = metadata
                        }

                        val existing = newCacheMap[subId]
                        if (existing == null || (!subShortName.equals(subId, ignoreCase = true) && !subName.equals(subId, ignoreCase = true))) {
                            newCacheMap[subId] = metadata
                        }
                    }
                } else {
                    // Branch level: cse, it, etc.
                    val branchId = branchStr.lowercase()
                    val semestersMap = branchValue as? Map<*, *> ?: continue

                    for ((semKey, semValue) in semestersMap) {
                        val semDigits = semKey.toString().filter { it.isDigit() }
                        val semNumber = semDigits.ifEmpty { semKey.toString() }
                        val semName = "Semester $semNumber"

                        val subjectsList = semValue as? List<*> ?: continue
                        for (item in subjectsList) {
                            val subjectMap = item as? Map<*, *> ?: continue
                            val subId = subjectMap["id"]?.toString()?.trim()?.lowercase() ?: continue
                            val subName = subjectMap["name"]?.toString()?.trim() ?: subId
                            val subShortName = subjectMap["shortName"]?.toString()?.trim() ?: subName
                            val isActive = subjectMap["active"] as? Boolean ?: true

                            val entity = SubjectCatalogEntity(
                                collegeId = collegeId,
                                branchId = branchId,
                                semester = semName,
                                subjectId = subId,
                                displayName = subName,
                                shortName = subShortName,
                                active = isActive,
                                lastSyncedAtMs = now
                            )
                            entitiesToPersist.add(entity)

                            val metadata = SubjectMetadata(subId, subName, subShortName, isActive)
                            newCacheMap["${collegeId}::${branchId}::${semNumber}::${subId}"] = metadata
                            newCacheMap["${branchId}::${semNumber}::${subId}"] = metadata
                            newCacheMap["${branchId}::${subId}"] = metadata
                            if (subName.isNotBlank() && !subName.equals(subId, ignoreCase = true)) {
                                newCacheMap[subName.lowercase()] = metadata
                            }
                            if (subShortName.isNotBlank() && !subShortName.equals(subId, ignoreCase = true)) {
                                newCacheMap[subShortName.lowercase()] = metadata
                            }

                            val existing = newCacheMap[subId]
                            // Do not let a default/unedited subject override an explicitly customized shortName/name
                            val isCustomized = !subShortName.equals(subId, ignoreCase = true) || !subName.equals(subId, ignoreCase = true)
                            if (existing == null || isCustomized) {
                                newCacheMap[subId] = metadata
                            }
                        }
                    }
                }
            }
        }

        if (entitiesToPersist.isNotEmpty()) {
            subjectDao.clearAll()
            subjectDao.upsertSubjects(entitiesToPersist)
            inMemorySubjectCache.clear()
            inMemorySubjectCache.putAll(newCacheMap)
            MetadataRepository.updateSubjectCatalogCache(rawCatalog)
            catalogVersionFlow.value = System.currentTimeMillis()
            Log.d(TAG, "Successfully synchronized ${entitiesToPersist.size} catalog subjects into Room and cache. Version=${catalogVersionFlow.value}. Sample 'se' -> ${inMemorySubjectCache["se"]}")
        }
    }
}
