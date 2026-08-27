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
        filterParts.add("college:${scope.canonicalCollegeId}")

        if (scope.hasBranch) {
            filterParts.add("(branch:${scope.canonicalBranchId} OR branch:common OR branch:all)")
        }

        if (scope.hasSemester) {
            val semDigits = scope.semester!!.filter { it.isDigit() }
            if (semDigits.isNotEmpty()) {
                filterParts.add("(semester:'${scope.semester}' OR semester:'$semDigits' OR semester:'Semester $semDigits')")
            } else {
                filterParts.add("semester:'${scope.semester}'")
            }
        }

        if (selectedDocumentTypes.isNotEmpty()) {
            val typeFilters = selectedDocumentTypes.joinToString(separator = " OR ", prefix = "(", postfix = ")") {
                "documentType:$it"
            }
            filterParts.add(typeFilters)
        }

        val filterExpression = filterParts.joinToString(" AND ")

        val params = SearchParamsObject(
            query = query,
            filters = filterExpression
        )

        val response = client.searchSingleIndex(
            indexName = indexName,
            searchParams = params
        )
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

            if (scope != null && scope.isCollegeValid) {
                val isPermitted = scope.isDocumentPermitted(
                    docCollege = college,
                    docBranch = branch,
                    docSemester = semester,
                    docSubjectId = null
                )
                if (!isPermitted) return@mapNotNull null
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
