package com.pravor.notessharing.updates.data

import com.pravor.notessharing.updates.model.UpdatePageModel

object OnboardingPages {
    val pages = listOf(
        UpdatePageModel(
            pageIndex = 0,
            headline = "Everything your semester needs.",
            supportingText = "Notes, assignments, PYQs, cheat sheets, and learning resources — all organized in one place.",
            layoutType = UpdatePageModel.LayoutType.CONVERGING_RESOURCES
        ),
        UpdatePageModel(
            pageIndex = 1,
            headline = "Find it in seconds.",
            supportingText = "Search less and study more. Access study materials organized by subject and semester instantly.",
            layoutType = UpdatePageModel.LayoutType.STAGGERED_CARDS
        ),
        UpdatePageModel(
            pageIndex = 2,
            headline = "Built by Students. For Students.",
            supportingText = "A single note you share helps dozens of your peers prepare, learn, and succeed. Together, we build a collective brain.",
            layoutType = UpdatePageModel.LayoutType.RIPPLE_COMMUNITY
        ),
        UpdatePageModel(
            pageIndex = 3,
            headline = "Learn. Share. Grow.",
            supportingText = "Join a community where students help students succeed.",
            layoutType = UpdatePageModel.LayoutType.WELCOME_INSPIRING
        )
    )
}
