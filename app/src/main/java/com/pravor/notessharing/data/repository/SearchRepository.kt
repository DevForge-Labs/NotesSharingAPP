package com.pravor.notessharing.data.repository

import com.pravor.notessharing.data.local.cache.SearchCache
import com.pravor.notessharing.domain.model.*

import com.pravor.notessharing.core.util.*

import com.pravor.notessharing.BuildConfig
import com.pravor.notessharing.ui.features.search.SearchResultModel
import com.algolia.client.api.SearchClient
import com.algolia.client.model.search.SearchParamsObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Repository in charge of executing text search queries using the official Algolia Kotlin Client SDK.
 * Exposes a provider-agnostic method to return standard SearchResultModel records to presentation layers.
 */
open class SearchRepository(
    private val searchCache: SearchCache = SearchCache(),
    private val appId: String = BuildConfig.ALGOLIA_APP_ID,
    private val apiKey: String = BuildConfig.ALGOLIA_SEARCH_KEY,
    private val indexName: String = "resources"
) {
    private val client by lazy {
        SearchClient(
            appId = appId,
            apiKey = apiKey
        )
    }

    /**
     * Executes a text query against the search index.
     * @param query The search text input.
     * @param selectedDocumentTypes Set of document types to filter results by using Algolia facet filters.
     * @return List of mapped SearchResultModel items.
     */
    suspend fun search(
        query: String,
        scope: AcademicScope,
        selectedDocumentTypes: Set<String> = emptySet()
    ): List<SearchResultModel> = withContext(Dispatchers.IO) {
        if (appId.isBlank() || apiKey.isBlank() || !scope.isCollegeValid) {
            return@withContext emptyList()
        }

        val cacheKey = "${scope.scopeKey}_$query"
        val cachedResults = searchCache.get(cacheKey)
        val rawResults = cachedResults ?: try {
            val networkResults = executeNetworkSearch(query, scope, emptySet())
            searchCache.put(cacheKey, networkResults)
            networkResults
        } catch (e: Exception) {
            throw e
        }

        if (selectedDocumentTypes.isNotEmpty()) {
            rawResults.filter { result ->
                val type = if (result.documentType.isNotBlank()) result.documentType else result.type
                val normalizedType = type.lowercase(java.util.Locale.ROOT).trim()
                selectedDocumentTypes.any { filterType ->
                    val rawFilter = filterType.lowercase(java.util.Locale.ROOT).trim()
                    when {
                        rawFilter.contains("pyq") -> normalizedType.contains("pyq")
                        rawFilter.contains("assignment") -> normalizedType.contains("assignment")
                        rawFilter.contains("cheat") || rawFilter.contains("formula") -> normalizedType.contains("cheat") || normalizedType.contains("formula")
                        rawFilter.contains("notes") || normalizedType.contains("note") -> normalizedType.contains("notes") || normalizedType.contains("note")
                        rawFilter.contains("playlist") -> normalizedType.contains("playlist")
                        rawFilter.contains("video") || rawFilter.contains("youtube") -> normalizedType.contains("video") || normalizedType.contains("youtube")
                        else -> normalizedType.contains(rawFilter)
                    }
                }
            }
        } else {
            rawResults
        }
    }

    suspend fun search(
        query: String,
        userCollegeId: String?,
        selectedDocumentTypes: Set<String> = emptySet()
    ): List<SearchResultModel> {
        val scope = AcademicScope(collegeId = userCollegeId ?: "")
        return search(query, scope, selectedDocumentTypes)
    }

    internal open suspend fun executeNetworkSearch(
        query: String,
        scope: AcademicScope,
        selectedDocumentTypes: Set<String>
    ): List<SearchResultModel> {
        val filterParts = mutableListOf<String>()

        // 1. College Scoping (Mandatory if college is valid)
        if (scope.isCollegeValid) {
            filterParts.add("college:${scope.canonicalCollegeId}")
        }

        // 2. Semester Scoping (if user has semester configured)
        if (scope.hasSemester) {
            val semDigits = scope.semester!!.filter { it.isDigit() }
            val semVariants = mutableSetOf<String>()
            val trimmedSem = scope.semester.trim()
            if (trimmedSem.isNotBlank()) {
                semVariants.add(trimmedSem)
            }
            if (semDigits.isNotBlank()) {
                semVariants.add("Semester $semDigits")
                semVariants.add("Sem $semDigits")
                semVariants.add(semDigits)
            }
            semVariants.add("common")
            semVariants.add("all")

            val semFilterExpr = semVariants.joinToString(separator = " OR ", prefix = "(", postfix = ")") { sem ->
                "semester:'${sem.replace("'", "\\'")}'"
            }
            filterParts.add(semFilterExpr)
        }

        // 3. Branch Scoping (for 2nd year onwards, if user has branch configured)
        if (!scope.isFirstYear && scope.hasBranch) {
            val canonicalBranch = scope.canonicalBranchId!!
            val branchVariants = mutableSetOf<String>()
            branchVariants.add(canonicalBranch)
            branchVariants.add(canonicalBranch.uppercase(java.util.Locale.ROOT))

            val rawBranch = scope.branchId?.trim() ?: ""
            if (rawBranch.isNotBlank()) {
                branchVariants.add(rawBranch)
            }

            when (canonicalBranch.lowercase(java.util.Locale.ROOT)) {
                "cse" -> {
                    branchVariants.add("Computer Science")
                    branchVariants.add("CS")
                    branchVariants.add("CSE")
                }
                "it" -> {
                    branchVariants.add("Information Technology")
                    branchVariants.add("IT")
                }
                "ece" -> {
                    branchVariants.add("Electronics")
                    branchVariants.add("ECE")
                    branchVariants.add("ETC")
                }
                "eee" -> {
                    branchVariants.add("Electrical")
                    branchVariants.add("EEE")
                    branchVariants.add("EE")
                }
                "mechanical" -> {
                    branchVariants.add("Mechanical")
                    branchVariants.add("Mech")
                    branchVariants.add("ME")
                }
                "civil" -> {
                    branchVariants.add("Civil")
                    branchVariants.add("CE")
                }
                "biotechnology" -> {
                    branchVariants.add("Biotech")
                    branchVariants.add("BT")
                }
            }
            branchVariants.add("common")
            branchVariants.add("all")

            val branchFilterExpr = branchVariants.joinToString(separator = " OR ", prefix = "(", postfix = ")") { br ->
                "branch:'${br.replace("'", "\\'")}'"
            }
            filterParts.add(branchFilterExpr)
        }

        // 4. Document-Type Filtering (Preserve existing OR grouping)
        if (selectedDocumentTypes.isNotEmpty()) {
            val typeFilters = selectedDocumentTypes.joinToString(separator = " OR ", prefix = "(", postfix = ")") {
                "documentType:'${it.replace("'", "\\'")}'"
            }
            filterParts.add(typeFilters)
        }

        val filterExpression = filterParts.joinToString(" AND ")

        val params = SearchParamsObject(
            query = query,
            filters = filterExpression.ifBlank { null }
        )

        val response = try {
            val primaryResponse = client.searchSingleIndex(
                indexName = indexName,
                searchParams = params
            )
            // If primary restricted query returned results or had no additional filters, use it
            if (primaryResponse.hits.isNotEmpty() || filterParts.size <= 1) {
                primaryResponse
            } else {
                // If primary query returned 0 hits (e.g. index contains un-faceted or null-branch legacy records),
                // query by college and documentType, and allow client-side scope filtering to strictly prune
                val fallbackFilterParts = mutableListOf<String>()
                if (scope.isCollegeValid) {
                    fallbackFilterParts.add("college:${scope.canonicalCollegeId}")
                }
                if (selectedDocumentTypes.isNotEmpty()) {
                    val typeFilters = selectedDocumentTypes.joinToString(separator = " OR ", prefix = "(", postfix = ")") {
                        "documentType:'${it.replace("'", "\\'")}'"
                    }
                    fallbackFilterParts.add(typeFilters)
                }
                val fallbackParams = SearchParamsObject(
                    query = query,
                    filters = fallbackFilterParts.joinToString(" AND ").ifBlank { null }
                )
                client.searchSingleIndex(
                    indexName = indexName,
                    searchParams = fallbackParams
                )
            }
        } catch (e: Exception) {
            // If Algolia throws a syntax or facet error, gracefully fall back to college-level query
            val fallbackFilterParts = mutableListOf<String>()
            if (scope.isCollegeValid) {
                fallbackFilterParts.add("college:${scope.canonicalCollegeId}")
            }
            if (selectedDocumentTypes.isNotEmpty()) {
                val typeFilters = selectedDocumentTypes.joinToString(separator = " OR ", prefix = "(", postfix = ")") {
                    "documentType:'${it.replace("'", "\\'")}'"
                }
                fallbackFilterParts.add(typeFilters)
            }
            val fallbackParams = SearchParamsObject(
                query = query,
                filters = fallbackFilterParts.joinToString(" AND ").ifBlank { null }
            )
            client.searchSingleIndex(
                indexName = indexName,
                searchParams = fallbackParams
            )
        }
        val mapped = response.hits.mapNotNull { hit ->
            val id = hit.objectID
            val title = hit.additionalProperties?.get("title")?.jsonPrimitive?.content ?: ""
            val displaySubject = (hit.additionalProperties?.get("displaySubject") ?: hit.additionalProperties?.get("subject"))?.jsonPrimitive?.content ?: ""
            val documentType = hit.additionalProperties?.get("documentType")?.jsonPrimitive?.content ?: ""
            val thumbnailUrl = hit.additionalProperties?.get("thumbnailUrl")?.jsonPrimitive?.content ?: ""
            
            val sectionDisplay = hit.additionalProperties?.get("sectionDisplay")?.jsonPrimitive?.content ?: ""
            val examYear = hit.additionalProperties?.get("examYear")?.jsonPrimitive?.content ?: ""
            val examType = hit.additionalProperties?.get("examType")?.jsonPrimitive?.content ?: ""
            val branch = hit.additionalProperties?.get("branch")?.jsonPrimitive?.content ?: ""
            val semester = hit.additionalProperties?.get("semester")?.jsonPrimitive?.content ?: ""
            val college = hit.additionalProperties?.get("college")?.jsonPrimitive?.content ?: ""
            val channelName = hit.additionalProperties?.get("channelName")?.jsonPrimitive?.content ?: ""
            val playlistTitle = hit.additionalProperties?.get("playlistTitle")?.jsonPrimitive?.content ?: ""

            // Client-Side Defensive Layer: Verify hit against academic scope
            if (scope.isCollegeValid) {
                val isPermitted = scope.isDocumentPermitted(
                    docCollege = college,
                    docBranch = branch,
                    docSemester = semester,
                    docSubjectId = null
                )
                if (!isPermitted) {
                    return@mapNotNull null
                }
            }

            SearchResultModel(
                id = id,
                title = title,
                subtitle = displaySubject,
                type = documentType,
                additionalInfo = documentType,
                thumbnailUrl = thumbnailUrl,
                sectionDisplay = sectionDisplay,
                examYear = examYear,
                examType = examType,
                branch = branch,
                semester = semester,
                college = college,
                channelName = channelName,
                playlistTitle = playlistTitle,
                subject = displaySubject,
                documentType = documentType
            )
        }
        return mapped
    }
}
