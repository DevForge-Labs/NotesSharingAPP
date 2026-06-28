package com.pravor.notessharing.data

import com.pravor.notessharing.BuildConfig
import com.pravor.notessharing.ui.screens.search.SearchResultModel
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
        selectedDocumentTypes: Set<String> = emptySet()
    ): List<SearchResultModel> = withContext(Dispatchers.IO) {
        if (appId.isBlank() || apiKey.isBlank()) {
            return@withContext emptyList()
        }

        // Check the cache first (using normalized query internally in SearchCache)
        val cachedResults = searchCache.get(query)
        val rawResults = cachedResults ?: try {
            // Fetch raw, unfiltered results from Algolia on cache miss
            val networkResults = executeNetworkSearch(query, emptySet())
            // Cache successful raw results (including empty result lists)
            searchCache.put(query, networkResults)
            networkResults
        } catch (e: Exception) {
            throw e
        }

        // Apply selected resource type filters locally
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

    /**
     * Executes the actual network search using the Algolia client.
     * Extracted and made open for unit testing.
     */
    internal open suspend fun executeNetworkSearch(
        query: String,
        selectedDocumentTypes: Set<String>
    ): List<SearchResultModel> {
        val filterExpression = if (selectedDocumentTypes.isNotEmpty()) {
            selectedDocumentTypes.joinToString(separator = " OR ", prefix = "(", postfix = ")") {
                "documentType:$it"
            }
        } else {
            null
        }

        val params = SearchParamsObject(
            query = query,
            filters = filterExpression
        )

        val response = client.searchSingleIndex(
            indexName = indexName,
            searchParams = params
        )
        return response.hits.map { hit ->
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
    }
}
