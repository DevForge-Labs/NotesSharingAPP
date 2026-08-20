package com.pravor.notessharing.data.local.cache

import java.util.concurrent.ConcurrentHashMap

/**
 * A generic, thread-safe, in-memory cache that expires entries after a specified time-to-live (TTL).
 */
class TimedMemoryCache<Key : Any, Value : Any>(private val ttlMillis: Long) {
    private class CacheEntry<Value>(val value: Value, val timestamp: Long)
    private val cache = ConcurrentHashMap<Key, CacheEntry<Value>>()

    fun get(key: Key): Value? {
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() - entry.timestamp < ttlMillis) {
            return entry.value
        }
        // Entry is expired
        cache.remove(key)
        return null
    }

    fun getExpiredButAvailable(key: Key): Value? {
        return cache[key]?.value
    }

    fun isExpired(key: Key): Boolean {
        val entry = cache[key] ?: return true
        return System.currentTimeMillis() - entry.timestamp >= ttlMillis
    }

    fun put(key: Key, value: Value) {
        cache[key] = CacheEntry(value, System.currentTimeMillis())
    }

    fun clear() {
        cache.clear()
    }
}

/**
 * A thread-safe, in-memory cache for a single value that expires after a specified time-to-live (TTL).
 */
class TimedValueCache<T : Any>(private val ttlMillis: Long) {
    private var cachedValue: T? = null
    private var lastUpdatedTime: Long = 0L

    @Synchronized
    fun get(): T? {
        val now = System.currentTimeMillis()
        if (cachedValue != null && (now - lastUpdatedTime) < ttlMillis) {
            return cachedValue
        }
        return null
    }

    @Synchronized
    fun getExpiredButAvailable(): T? {
        return cachedValue
    }

    @Synchronized
    fun isExpired(): Boolean {
        if (cachedValue == null) return true
        val now = System.currentTimeMillis()
        return (now - lastUpdatedTime) >= ttlMillis
    }

    @Synchronized
    fun put(value: T) {
        cachedValue = value
        lastUpdatedTime = System.currentTimeMillis()
    }

    @Synchronized
    fun putExpired(value: T) {
        cachedValue = value
        lastUpdatedTime = 0L
    }

    @Synchronized
    fun clear() {
        cachedValue = null
        lastUpdatedTime = 0L
    }
}
