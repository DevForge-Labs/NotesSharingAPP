package com.pravor.notessharing.core.util


import java.util.Locale

object LegacyAcademicCompatibilityResolver {
    
    /**
     * Resolves a potentially legacy college value to its canonical college ID (e.g. "kiit").
     */
    fun resolveCollegeId(storedCollege: String): String {
        val clean = storedCollege.trim().lowercase(Locale.ROOT)
        if (clean == "kiit" || clean.contains("kalinga")) return "kiit"
        return clean
    }

    /**
     * Resolves a potentially legacy branch display name or code to its canonical catalog branch ID (e.g. "cse").
     */
    fun resolveBranchId(storedBranch: String): String {
        val clean = storedBranch.trim().lowercase(Locale.ROOT)
        if (clean.contains("computer science") || clean == "cs" || clean == "cse") return "cse"
        if (clean.contains("information technology") || clean == "it") return "it"
        if (clean.contains("electronics") || clean == "ece") return "ece"
        if (clean.contains("electrical") || clean == "eee") return "eee"
        if (clean.contains("mechanical") || clean == "mech") return "mechanical"
        if (clean.contains("civil")) return "civil"
        if (clean.contains("biotech")) return "biotechnology"
        return clean
    }
}
