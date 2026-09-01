package com.pravor.notessharing.ui.features.classroom.components

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Clearly visible, ambient drifting geometric background for the Classroom Subject Detail screen.
 * Dynamically inherits the subject's CourseCardTheme palette (e.g. pink, purple, cyan, orange),
 * rendering a rich mixture of floating polygons, hexagon outlines, triangles, irregular shapes,
 * and edge-cropped forms with perceptible asynchronous drift.
 */
@Composable
fun ClassroomSubjectAtmosphericBackground(
    theme: CourseCardTheme,
    courseId: String,
    courseName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val areAnimationsEnabled = remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale > 0f
        } catch (e: Exception) {
            true
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "classroom_subject_bg")
    val ambientPhase by if (areAnimationsEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 20000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ambientPhase"
        )
    } else {
        remember { mutableStateOf(0.95f) }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawSubjectAtmosphere(
            theme = theme,
            courseId = courseId,
            courseName = courseName,
            phase = ambientPhase
        )
    }
}

private fun DrawScope.drawSubjectAtmosphere(
    theme: CourseCardTheme,
    courseId: String,
    courseName: String,
    phase: Float
) {
    val seed = abs(courseId.hashCode() * 37 + courseName.hashCode())
    val strokeWidthPx = 1.6.dp.toPx()

    // 1. Visible Thematic Radial Ambient Glows
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                theme.accentColor.copy(alpha = 0.16f),
                theme.secondaryAccent.copy(alpha = 0.07f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.88f, size.height * 0.10f),
            radius = size.width * 0.85f
        ),
        center = Offset(size.width * 0.88f, size.height * 0.10f),
        radius = size.width * 0.85f
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                theme.secondaryAccent.copy(alpha = 0.12f),
                theme.accentColor.copy(alpha = 0.05f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.12f, size.height * 0.65f),
            radius = size.width * 0.90f
        ),
        center = Offset(size.width * 0.12f, size.height * 0.65f),
        radius = size.width * 0.90f
    )

    // 2. Structured Layout Positions across full screen height (including edge-cropped shapes)
    val shapeLayouts = listOf(
        // (xRatio, yRatio, typeHint, sizeDp)
        ShapeSpec(xRatio = 0.94f, yRatio = 0.07f, type = 4, sizeDp = 140f), // Top-Right Hexagon (partially cropped)
        ShapeSpec(xRatio = 0.08f, yRatio = 0.18f, type = 2, sizeDp = 110f), // Upper-Left Triangle
        ShapeSpec(xRatio = 0.82f, yRatio = 0.32f, type = 0, sizeDp = 130f), // Upper-Mid Right Rounded Polygon
        ShapeSpec(xRatio = -0.04f, yRatio = 0.44f, type = 4, sizeDp = 150f), // Mid-Left Hexagon (edge-cropped)
        ShapeSpec(xRatio = 0.54f, yRatio = 0.48f, type = 6, sizeDp = 120f), // Center-Mid Angular Diamond / Trapezoid
        ShapeSpec(xRatio = 0.92f, yRatio = 0.60f, type = 2, sizeDp = 135f), // Mid-Lower Right Triangle (edge-cropped)
        ShapeSpec(xRatio = 0.18f, yRatio = 0.72f, type = 3, sizeDp = 125f), // Lower-Left Pentagon
        ShapeSpec(xRatio = 0.80f, yRatio = 0.84f, type = 4, sizeDp = 145f), // Lower-Right Hexagon
        ShapeSpec(xRatio = 0.10f, yRatio = 0.93f, type = 5, sizeDp = 120f)  // Bottom-Left Pill / Rounded Shard
    )

    for (i in shapeLayouts.indices) {
        val spec = shapeLayouts[i]
        val shapeSeed = abs((seed + i * 53) * 31)

        // Asynchronous, perceptible drifting speeds and paths for each shape
        val speedX = 0.8f + ((shapeSeed % 5) * 0.15f)
        val speedY = 0.7f + (((shapeSeed / 3) % 4) * 0.18f)
        val phaseOffset = (i * 0.95f) + ((shapeSeed % 10) * 0.2f)

        val driftX = sin(phase * speedX + phaseOffset) * (18 + (shapeSeed % 14)).dp.toPx()
        val driftY = cos(phase * speedY + phaseOffset) * (16 + ((shapeSeed / 2) % 12)).dp.toPx()

        val centerX = (size.width * spec.xRatio) + driftX
        val centerY = (size.height * spec.yRatio) + driftY
        val shapeSize = (spec.sizeDp + ((shapeSeed % 20) - 10)).dp.toPx()

        // Perceptible subtle rotation
        val baseRotation = ((shapeSeed % 360) + (i * 40)).toFloat()
        val rotationSpeed = 0.6f + ((shapeSeed % 3) * 0.2f)
        val rotationAngle = baseRotation + (16f * sin(phase * rotationSpeed + phaseOffset))

        // Gentle breathing opacity
        val pulse = 0.88f + (0.12f * sin(phase * 0.8f + i * 1.1f))

        val isPrimary = (i % 3) != 1
        val color = if (isPrimary) theme.accentColor else theme.secondaryAccent

        // Noticeable, tasteful alpha levels
        val fillAlpha = (0.07f + ((shapeSeed % 4) * 0.015f)) * pulse
        val strokeAlpha = (0.22f + ((shapeSeed % 5) * 0.025f)) * pulse

        val fillBrush = Brush.linearGradient(
            colors = listOf(
                color.copy(alpha = fillAlpha),
                color.copy(alpha = fillAlpha * 0.25f),
                Color.Transparent
            ),
            start = Offset(centerX - shapeSize * 0.6f, centerY - shapeSize * 0.6f),
            end = Offset(centerX + shapeSize * 0.6f, centerY + shapeSize * 0.6f)
        )
        val strokeColor = color.copy(alpha = strokeAlpha)

        rotate(rotationAngle, pivot = Offset(centerX, centerY)) {
            when (spec.type) {
                // 0: Rounded Polygon / Square
                0 -> {
                    val corner = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                    val topLeft = Offset(centerX - shapeSize / 2, centerY - shapeSize / 2)
                    val rectSize = Size(shapeSize, shapeSize * 0.9f)
                    drawRoundRect(brush = fillBrush, topLeft = topLeft, size = rectSize, cornerRadius = corner)
                    drawRoundRect(color = strokeColor, topLeft = topLeft, size = rectSize, cornerRadius = corner, style = Stroke(strokeWidthPx))
                }
                // 2: Triangle
                2 -> {
                    val path = createRegularPolygon(center = Offset(centerX, centerY), radius = shapeSize * 0.55f, sides = 3)
                    drawPath(path = path, brush = fillBrush)
                    drawPath(path = path, color = strokeColor, style = Stroke(strokeWidthPx))
                }
                // 3: Pentagon
                3 -> {
                    val path = createRegularPolygon(center = Offset(centerX, centerY), radius = shapeSize * 0.52f, sides = 5)
                    drawPath(path = path, brush = fillBrush)
                    drawPath(path = path, color = strokeColor, style = Stroke(strokeWidthPx))
                }
                // 4: Hexagon Outline + Translucent Fill
                4 -> {
                    val path = createRegularPolygon(center = Offset(centerX, centerY), radius = shapeSize * 0.55f, sides = 6)
                    drawPath(path = path, brush = fillBrush)
                    drawPath(path = path, color = strokeColor, style = Stroke(strokeWidthPx))
                }
                // 5: Pill / Capsule
                5 -> {
                    val width = shapeSize * 1.35f
                    val height = shapeSize * 0.52f
                    val corner = CornerRadius(height / 2, height / 2)
                    val topLeft = Offset(centerX - width / 2, centerY - height / 2)
                    val rectSize = Size(width, height)
                    drawRoundRect(brush = fillBrush, topLeft = topLeft, size = rectSize, cornerRadius = corner)
                    drawRoundRect(color = strokeColor, topLeft = topLeft, size = rectSize, cornerRadius = corner, style = Stroke(strokeWidthPx))
                }
                // 6: Angular Diamond / Irregular Quad
                else -> {
                    val path = createDiamondPath(center = Offset(centerX, centerY), width = shapeSize * 0.9f, height = shapeSize * 1.2f)
                    drawPath(path = path, brush = fillBrush)
                    drawPath(path = path, color = strokeColor, style = Stroke(strokeWidthPx))
                }
            }
        }
    }
}

private data class ShapeSpec(
    val xRatio: Float,
    val yRatio: Float,
    val type: Int,
    val sizeDp: Float
)

private fun createRegularPolygon(center: Offset, radius: Float, sides: Int): Path {
    val path = Path()
    val angleStep = (2 * Math.PI / sides).toFloat()
    val initialAngle = -Math.PI.toFloat() / 2f

    for (i in 0 until sides) {
        val angle = initialAngle + i * angleStep
        val x = center.x + radius * cos(angle)
        val y = center.y + radius * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun createDiamondPath(center: Offset, width: Float, height: Float): Path {
    val path = Path()
    path.moveTo(center.x, center.y - height / 2)
    path.lineTo(center.x + width / 2, center.y)
    path.lineTo(center.x, center.y + height / 2)
    path.lineTo(center.x - width / 2, center.y)
    path.close()
    return path
}
