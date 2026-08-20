package com.pravor.notessharing.ui.common.navigation

import com.pravor.notessharing.ui.common.navigation.model.WaterWaveState

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*


object WaterPhysics {
    fun step(
        state: WaterWaveState,
        width: Float,
        height: Float,
        centerX: Float,
        density: Float
    ) {
        if (width <= 0f || height <= 0f) return

        val numColumns = state.numColumns
        val baseline = height * (1f - state.fillLevel)
        val columnWidth = width / (numColumns - 1)

        // Convert DP configurations to physical pixels
        val depthPx = state.depressionDepth * density
        val widthPx = state.depressionWidth * density

        // 1. Calculate target heights with dynamic idle ripples
        for (i in 0 until numColumns) {
            val x = i * columnWidth
            val dist = x - centerX
            
            // Gaussian dip (depression) scaled properly in pixels
            val dip = depthPx * kotlin.math.exp(-(dist * dist) / (2f * widthPx * widthPx))
            
            // Add subtle secondary idle ripples to keep the water surface alive
            val idleRipple = if (!state.isReducedMotion) {
                3f * kotlin.math.sin(state.timeAccumulator * 3.2f + i * 0.18f) + 1.2f * kotlin.math.cos(state.timeAccumulator * 5.5f - i * 0.3f)
            } else {
                0f
            }
            
            state.targetHeights[i] = baseline + dip + idleRipple
        }

        if (state.isReducedMotion) {
            // Static fallback: heights are just the target heights
            for (i in 0 until numColumns) {
                state.heights[i] = state.targetHeights[i]
                state.velocities[i] = 0f
            }
            state.tickTrigger++
            return
        }

        // 2. Solve wave equation step (tension, spring, damping)
        for (i in 0 until numColumns) {
            val left = if (i > 0) state.heights[i - 1] else state.heights[i]
            val right = if (i < numColumns - 1) state.heights[i + 1] else state.heights[i]

            // Acceleration = tension * (left + right - 2 * height) + springConstant * (target - height)
            val acceleration = state.tension * (left + right - 2f * state.heights[i]) +
                    state.springConstant * (state.targetHeights[i] - state.heights[i])

            state.velocities[i] = (state.velocities[i] + acceleration) * state.damping
            state.heights[i] += state.velocities[i]
        }

        state.tickTrigger++
    }
}
