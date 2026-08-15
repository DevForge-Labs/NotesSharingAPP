package com.pravor.notessharing.data.mapper

import com.pravor.notessharing.data.local.entity.UserProfileEntity
import com.pravor.notessharing.model.Profile

fun UserProfileEntity.toDomainModel(): Profile {
    return Profile(
        uid = uid,
        name = name,
        email = email,
        semester = semester,
        college = college,
        section = section,
        profileImageUrl = profileImageUrl,
        role = role,
        accountStatus = accountStatus,
        totalUploads = totalUploads,
        bookmarks = bookmarks,
        upvotes = upvotes,
        contributorLevel = contributorLevel,
        branch = branch,
        createdAt = createdAt,
        pyqUploads = pyqUploads,
        notesUploads = notesUploads,
        assignmentUploads = assignmentUploads,
        cheatSheetUploads = cheatSheetUploads,
        youtubeResourceUploads = youtubeResourceUploads
    )
}

fun Profile.toEntity(updatedAtMs: Long = System.currentTimeMillis()): UserProfileEntity {
    return UserProfileEntity(
        uid = uid,
        name = name,
        email = email,
        semester = semester,
        college = college,
        section = section,
        profileImageUrl = profileImageUrl,
        role = role,
        accountStatus = accountStatus,
        totalUploads = totalUploads,
        bookmarks = bookmarks,
        upvotes = upvotes,
        contributorLevel = contributorLevel,
        branch = branch,
        createdAt = createdAt,
        pyqUploads = pyqUploads,
        notesUploads = notesUploads,
        assignmentUploads = assignmentUploads,
        cheatSheetUploads = cheatSheetUploads,
        youtubeResourceUploads = youtubeResourceUploads,
        lastUpdatedMs = updatedAtMs
    )
}
