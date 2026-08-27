package com.pravor.notessharing.data.classroom.reminder

enum class ClassroomReminderType(
    val offsetMillis: Long,
    val typeKey: String,
    val defaultLabel: String
) {
    DUE_24_HOURS(
        offsetMillis = 24 * 60 * 60 * 1000L,
        typeKey = "24H",
        defaultLabel = "Due Tomorrow"
    ),
    DUE_3_HOURS(
        offsetMillis = 3 * 60 * 60 * 1000L,
        typeKey = "3H",
        defaultLabel = "Due in 3 Hours"
    )
}
