package com.pravor.notessharing.updates.model

data class UpdatePageModel(
    val pageIndex: Int,
    val headline: String,
    val supportingText: String,
    val layoutType: LayoutType
) {
    enum class LayoutType {
        CONVERGING_RESOURCES,
        STAGGERED_CARDS,
        RIPPLE_COMMUNITY,
        WELCOME_INSPIRING
    }
}
