package com.pravor.notessharing.data

import com.google.firebase.firestore.DocumentSnapshot

object ExploreRankingUtils {
    val documentSnapshotComparator = compareByDescending<DocumentSnapshot> { doc ->
        (doc.data?.get("trendingScore") as? Number)?.toDouble() ?: 0.0
    }.thenByDescending { doc ->
        doc.getLong("uploadedAt") ?: 0L
    }

    fun <T> sortWithTieBreak(list: List<T>, comparator: Comparator<T>): List<T> {
        if (list.isEmpty()) return list
        val sorted = list.sortedWith(comparator)
        val result = mutableListOf<T>()
        var i = 0
        while (i < sorted.size) {
            val currentGroup = mutableListOf<T>()
            currentGroup.add(sorted[i])
            var j = i + 1
            while (j < sorted.size && comparator.compare(sorted[i], sorted[j]) == 0) {
                currentGroup.add(sorted[j])
                j++
            }
            currentGroup.shuffle()
            result.addAll(currentGroup)
            i = j
        }
        return result
    }
}
