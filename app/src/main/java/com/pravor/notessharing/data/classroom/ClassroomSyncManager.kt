package com.pravor.notessharing.data.classroom

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

object ClassroomSyncManager {

    private const val TAG = "ClassroomSyncManager"

    const val COURSES_TTL_MS = 5 * 60 * 1000L // 5 minutes
    const val COURSE_CONTENT_TTL_MS = 3 * 60 * 1000L // 3 minutes

    // Store timestamps keyed by "${userId}_${account}_${resourceKey}"
    private val lastSyncTimestamps = ConcurrentHashMap<String, Long>()

    // In-flight synchronization coalescing
    private val mutex = Mutex()
    private val inFlightSyncs = ConcurrentHashMap<String, CompletableDeferred<Result<Any?>>>()

    fun isFresh(key: String, ttlMs: Long = COURSE_CONTENT_TTL_MS): Boolean {
        val lastSynced = lastSyncTimestamps[key] ?: return false
        val elapsed = System.currentTimeMillis() - lastSynced
        return elapsed in 0 until ttlMs
    }

    fun markSynced(key: String) {
        lastSyncTimestamps[key] = System.currentTimeMillis()
        Log.d(TAG, "Marked fresh: $key")
    }

    fun clearForAccount(userId: String, account: String) {
        val prefix = "${userId}_${account}"
        val iterator = lastSyncTimestamps.keys().iterator()
        while (iterator.hasNext()) {
            val k = iterator.next()
            if (k.startsWith(prefix)) {
                lastSyncTimestamps.remove(k)
            }
        }
        Log.d(TAG, "Cleared freshness timestamps for: $prefix")
    }

    /**
     * Coalesces concurrent sync calls for the exact same key so that
     * simultaneous requests await the same in-flight network call rather
     * than firing duplicate requests.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> runCoalesced(key: String, block: suspend () -> Result<T>): Result<T> {
        val (deferred, isInitiator) = mutex.withLock {
            val existing = inFlightSyncs[key]
            if (existing != null) {
                Pair(existing, false)
            } else {
                val newDeferred = CompletableDeferred<Result<Any?>>()
                inFlightSyncs[key] = newDeferred
                Pair(newDeferred, true)
            }
        }

        if (isInitiator) {
            try {
                val result = block()
                deferred.complete(result as Result<Any?>)
                return result
            } catch (e: Throwable) {
                val failure = Result.failure<T>(e)
                deferred.complete(failure as Result<Any?>)
                return failure
            } finally {
                mutex.withLock {
                    inFlightSyncs.remove(key)
                }
            }
        } else {
            Log.d(TAG, "Coalescing onto in-flight sync for key: $key")
            return deferred.await() as Result<T>
        }
    }
}
