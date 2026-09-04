package com.pravor.notessharing.domain.model

import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

enum class InteractiveHubType {
    ANNOUNCEMENT,
    PROMOTION,
    SURVEY;

    companion object {
        fun fromString(value: String?): InteractiveHubType {
            return when (value?.uppercase()?.trim()) {
                "SURVEY" -> SURVEY
                "PROMOTION" -> PROMOTION
                else -> ANNOUNCEMENT
            }
        }
    }
}

enum class InteractiveHubStatus {
    DRAFT,
    SCHEDULED,
    ACTIVE,
    COMPLETED,
    EXPIRED,
    ARCHIVED;

    companion object {
        fun fromString(value: String?): InteractiveHubStatus {
            return when (value?.uppercase()?.trim()) {
                "ACTIVE" -> ACTIVE
                "SCHEDULED" -> SCHEDULED
                "COMPLETED" -> COMPLETED
                "EXPIRED" -> EXPIRED
                "ARCHIVED" -> ARCHIVED
                else -> DRAFT
            }
        }
    }
}

@Immutable
@IgnoreExtraProperties
data class InteractiveHubSession(
    @get:PropertyName("sessionId") @set:PropertyName("sessionId")
    var sessionId: String = "",

    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("body") @set:PropertyName("body")
    var body: String = "",

    @get:PropertyName("type") @set:PropertyName("type")
    var type: String = "ANNOUNCEMENT",

    @get:PropertyName("ctaText") @set:PropertyName("ctaText")
    var ctaText: String? = null,

    @get:PropertyName("targetDestination") @set:PropertyName("targetDestination")
    var targetDestination: String? = null,

    @get:PropertyName("surveyOptions") @set:PropertyName("surveyOptions")
    var surveyOptions: List<String> = emptyList(),

    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "DRAFT",

    @get:PropertyName("startMode") @set:PropertyName("startMode")
    var startMode: String = "MANUAL",

    @get:PropertyName("startTime") @set:PropertyName("startTime")
    var startTime: Long? = null,

    @get:PropertyName("endTime") @set:PropertyName("endTime")
    var endTime: Long? = null,

    @get:PropertyName("repeatable") @set:PropertyName("repeatable")
    var repeatable: Boolean = true,

    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = 0L,

    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Long = 0L,

    @get:PropertyName("createdBy") @set:PropertyName("createdBy")
    var createdBy: String = ""
) {
    @get:Exclude
    val hubType: InteractiveHubType
        get() = InteractiveHubType.fromString(type)

    @get:Exclude
    val hubStatus: InteractiveHubStatus
        get() = InteractiveHubStatus.fromString(status)

    @get:Exclude
    val rawType: String
        get() = type

    @get:Exclude
    val rawStatus: String
        get() = status

    @Exclude
    fun isCurrentlyActive(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (hubStatus != InteractiveHubStatus.ACTIVE) return false
        if (startTime != null && nowMillis < startTime!!) return false
        if (endTime != null && nowMillis > endTime!!) return false
        return true
    }
}

@Immutable
@IgnoreExtraProperties
data class ActiveInteractiveHubConfig(
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = false,

    @get:PropertyName("activeSessionId") @set:PropertyName("activeSessionId")
    var activeSessionId: String? = null,

    @get:PropertyName("session") @set:PropertyName("session")
    var session: InteractiveHubSession? = null,

    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Long = 0L
)

@Immutable
@IgnoreExtraProperties
data class InteractiveHubResponse(
    @get:PropertyName("responseId") @set:PropertyName("responseId")
    var responseId: String = "",

    @get:PropertyName("sessionId") @set:PropertyName("sessionId")
    var sessionId: String = "",

    @get:PropertyName("userId") @set:PropertyName("userId")
    var userId: String = "",

    @get:PropertyName("response") @set:PropertyName("response")
    var response: String = "",

    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = 0L
)
