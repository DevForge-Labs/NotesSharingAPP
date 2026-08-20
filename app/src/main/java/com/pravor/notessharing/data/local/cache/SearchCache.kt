package com.pravor.notessharing.data.local.cache

import com.pravor.notessharing.ui.screens.search.SearchResultModel

/**
 * A lightweight, thread-safe, session-based in-memory search cache.
 * Uses a Least Recently Used (LRU) eviction policy with a maximum size of 100 entries.
 * Entries remain valid for up to 6 hours and are automatically evicted on expiry when accessed.
 */
class SearchCache(
    private val maxLimit: Int = 100,
    private val ttlMillis: Long = 6 * 60 * 60 * 1000L, // 6 hours
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    private class CacheEntry(
        val results: List<SearchResultModel>,
        val timestamp: Long
    )

    // LinkedHashMap with accessOrder = true for LRU eviction
    private val cache = object : LinkedHashMap<String, CacheEntry>(maxLimit, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean {
            return size > maxLimit
        }
    }

    /**
     * Normalizes the query by trimming whitespace and converting to lowercase.
     */
    private fun normalize(query: String): String {
        return query.trim().lowercase()
    }

    /**
     * Looks up cached search results for the given query.
     * Returns null if the entry does not exist or has expired (expired entries are removed immediately).
     */
    fun get(query: String): List<SearchResultModel>? {
        val key = normalize(query)
        return synchronized(cache) {
            val entry = cache[key] ?: return@synchronized null
            if (timeProvider() - entry.timestamp > ttlMillis) {
                cache.remove(key)
                null
            } else {
                entry.results
            }
        }
    }

    /**
     * Caches the given list of results for the query.
     */
    fun put(query: String, results: List<SearchResultModel>) {
        val key = normalize(query)
        val entry = CacheEntry(results, timeProvider())
        synchronized(cache) {
            cache[key] = entry
        }
    }

    /**
     * Clears all entries from the cache.
     */
    fun clear() {
        synchronized(cache) {
            cache.clear()
        }
    }
}
