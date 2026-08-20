package com.pravor.notessharing.domain.model

import java.util.Locale

object AcademicCatalog {
    val branches = emptyList<String>()

    val colleges = emptyList<String>()

    val semesters = listOf(
        "Semester 1",
        "Semester 2",
        "Semester 3",
        "Semester 4",
        "Semester 5",
        "Semester 6",
        "Semester 7",
        "Semester 8"
    )

    fun getDisplayBranch(normalized: String): String {
        val clean = normalized.trim().lowercase(Locale.ROOT)
        if (clean.contains("computer science") || clean == "cs" || clean == "cse") return "CS"
        if (clean.contains("information technology") || clean == "it") return "IT"
        if (clean.contains("electronics") || clean == "ece") return "ECE"
        if (clean.contains("electrical")) return "Electrical"
        if (clean.contains("mechanical")) return "Mechanical"
        if (clean.contains("civil")) return "Civil"
        return branches.firstOrNull { it.lowercase(Locale.ROOT) == clean } ?: normalized
    }
}
