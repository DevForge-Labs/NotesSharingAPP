package com.pravor.notessharing.util

import java.util.Locale

object NormalizationUtil {
    fun normalizeCollege(college: String): String {
        return college.trim().lowercase(Locale.ROOT)
    }

    fun normalizeBranch(branch: String): String {
        return branch.trim().lowercase(Locale.ROOT)
    }

    fun normalizeSection(section: String): String {
        var clean = section.trim().lowercase(Locale.ROOT)
        // Replace underscores with spaces
        clean = clean.replace('_', ' ')
        // Collapse multiple spaces
        clean = clean.replace(Regex("\\s+"), " ")

        // Pattern matching for transitions like "cse 52", "cse52", "cse-52", "it 7", "ece_3", "a1"
        val regex = Regex("^([a-z]+)[\\s-]*([0-9]+)$")
        val match = regex.matchEntire(clean)
        if (match != null) {
            val letters = match.groupValues[1]
            val digits = match.groupValues[2]
            return if (letters.length > 1) {
                "$letters-$digits"
            } else {
                "$letters$digits"
            }
        }
        
        return clean.replace(' ', '-')
    }
}
