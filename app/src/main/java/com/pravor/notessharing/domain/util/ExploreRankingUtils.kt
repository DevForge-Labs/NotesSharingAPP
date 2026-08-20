package com.pravor.notessharing.domain.util


import com.google.firebase.firestore.DocumentSnapshot
import com.pravor.notessharing.domain.model.ResourceType
import com.pravor.notessharing.domain.model.TrendingNote

object ExploreRankingUtils {
    val trendingNoteComparator = compareByDescending<TrendingNote> { it.trendingScore }
        .thenByDescending { it.uploadedAt }

    val documentSnapshotComparator = compareByDescending<DocumentSnapshot> { doc ->
        (doc.data?.get("trendingScore") as? Number)?.toDouble() ?: 0.0
    }.thenByDescending { doc ->
        doc.getLong("uploadedAt") ?: 0L
    }

    fun sortResources(list: List<TrendingNote>): List<TrendingNote> {
        return list.sortedWith(trendingNoteComparator)
    }

    fun filterNotes(list: List<TrendingNote>): List<TrendingNote> {
        return list.filter { it.resourceType == ResourceType.NOTE }
    }

    fun filterExamPrep(list: List<TrendingNote>): List<TrendingNote> {
        return list.filter { it.resourceType == ResourceType.PYQ || it.resourceType == ResourceType.CHEAT_SHEET }
    }

    fun filterAssignments(list: List<TrendingNote>): List<TrendingNote> {
        return list.filter { it.resourceType == ResourceType.ASSIGNMENT }
    }

    fun filterVideos(list: List<TrendingNote>): List<TrendingNote> {
        return list.filter { it.resourceType == ResourceType.VIDEO || it.resourceType == ResourceType.PLAYLIST }
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
