package com.pravor.notessharing.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "subject_catalog",
    primaryKeys = ["collegeId", "branchId", "semester", "subjectId"],
    indices = [
        Index(value = ["collegeId"]),
        Index(value = ["collegeId", "branchId", "semester"]),
        Index(value = ["subjectId"])
    ]
)
data class SubjectCatalogEntity(
    val collegeId: String,
    val branchId: String,
    val semester: String,
    val subjectId: String,
    val displayName: String,
    val shortName: String,
    val active: Boolean = true,
    val lastSyncedAtMs: Long = System.currentTimeMillis()
)
