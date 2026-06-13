package com.pravor.notessharing.core.config

import com.pravor.notessharing.BuildConfig

object DeveloperConfig {
    private const val FORCE_SHOW_ONBOARDING_RAW = false
    
    val FORCE_SHOW_ONBOARDING: Boolean
        get() = BuildConfig.DEBUG && FORCE_SHOW_ONBOARDING_RAW
}
