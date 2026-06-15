package com.pravor.notessharing.core.config

import com.pravor.notessharing.BuildConfig

object DeveloperConfig {
    private const val FORCE_SHOW_ONBOARDING_RAW = false //true for looping onboarding
    
    val FORCE_SHOW_ONBOARDING: Boolean
        get() = BuildConfig.DEBUG && FORCE_SHOW_ONBOARDING_RAW

    // Experimental Water Physics Bottom Navigation Toggle
    const val USE_WATER_NAV = false //false for normal/default customBar
}
