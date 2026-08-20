package com.pravor.notessharing.ui.common.navigation.model

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class WaterWaveState(val numColumns: Int = 80) {
    // Height and velocity arrays for the columns
    val heights = FloatArray(numColumns)
    val velocities = FloatArray(numColumns)
    val targetHeights = FloatArray(numColumns)

    // Physics parameters (exposed for debugging/tuning)
    var tension by mutableStateOf(0.018f)
    var springConstant by mutableStateOf(0.010f)
    var damping by mutableStateOf(0.96f) // Higher damping for slightly heavier fluid feel

    // Wave configuration
    var fillLevel by mutableStateOf(0.78f) // 78% filled
    var depressionDepth by mutableStateOf(38f) // Deepen the selection depression
    var depressionWidth by mutableStateOf(65f) // Smooth gradual entry/exit slope

    // Fallback support
    var isReducedMotion by mutableStateOf(false)

    // State tick trigger to notify Compose of updates
    var tickTrigger by mutableStateOf(0)

    // Monotonic high-precision time accumulator
    var timeAccumulator by mutableStateOf(0f)
}
