package com.pravor.notessharing.ui.common.utils

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

fun getDocumentTypeFromTitle(title: String): String {
    val t = title.lowercase(java.util.Locale.ROOT)
    return when {
        t.contains("pyq") || t.contains("solved") || t.contains("exam") || t.contains("paper") -> "PYQ"
        t.contains("cheat") || t.contains("formula") || t.contains("quick") || t.contains("sheet") -> "Cheat Sheet"
        t.contains("lab") || t.contains("assignment") || t.contains("manual") || t.contains("practice") -> "Assignment"
        else -> "Notes"
    }
}
