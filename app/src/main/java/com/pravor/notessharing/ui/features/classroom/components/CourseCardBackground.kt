package com.pravor.notessharing.ui.features.classroom.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedurally generates 2-4 randomized subtle geometric shapes (circle, square, triangle,
 * pentagon, hexagon, pill) across the card background with ultra-lightweight GPU ambient drifting.
 */
fun DrawScope.drawRandomizedCourseShapes(
    courseId: String,
    courseName: String,
    accentColor: Color,
    ambientPhase: Float
) {
    val seed = abs((courseId.hashCode() * 37) + courseName.hashCode())
    val strokeWidthPx = 1.dp.toPx()

    // 1. Ambient Radial Glow behind the icon area
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.16f),
                Color.Transparent
            ),
            center = Offset(42.dp.toPx(), 42.dp.toPx()),
            radius = 65.dp.toPx()
        ),
        center = Offset(42.dp.toPx(), 42.dp.toPx()),
        radius = 65.dp.toPx()
    )

    val shapeCount = 2 + (seed % 3) // 2 to 4 shapes

    for (i in 0 until shapeCount) {
        val shapeSeed = abs(seed * (i + 1) * 31)
        val shapeType = shapeSeed % 6 // 0: RoundRect, 1: Circle, 2: Triangle, 3: Pentagon, 4: Hexagon, 5: Pill

        // Dynamic Position (distributed across edges, corners, and offset zones)
        val posXRatio = when ((shapeSeed / 7) % 5) {
            0 -> 0.85f  // Top/Right
            1 -> 0.15f  // Top/Left
            2 -> 0.70f  // Bottom/Right
            3 -> 0.30f  // Bottom/Left
            else -> 0.50f // Center/Edge
        }
        val posYRatio = when ((shapeSeed / 11) % 4) {
            0 -> 0.10f
            1 -> 0.85f
            2 -> 0.45f
            else -> -0.05f
        }

        // Noticeable yet smooth ambient drift (6-9px float)
        val driftX = sin(ambientPhase + i * 1.5f) * 8.dp.toPx()
        val driftY = cos(ambientPhase + i * 1.2f) * 7.dp.toPx()

        val centerX = size.width * posXRatio + (((shapeSeed % 40) - 20).dp.toPx()) + driftX
        val centerY = size.height * posYRatio + ((((shapeSeed / 3) % 40) - 20).dp.toPx()) + driftY
        val shapeSize = (55 + (shapeSeed % 75)).dp.toPx()

        // Gentle rotation drift (±6.5 degrees)
        val rotationAngle = (shapeSeed % 360).toFloat() + (6.5f * sin(ambientPhase + i * 0.8f))

        val alphaFill = 0.035f + ((shapeSeed % 5) * 0.01f)
        val alphaStroke = 0.045f + ((shapeSeed % 4) * 0.01f)

        val fillBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = alphaFill),
                Color.White.copy(alpha = 0.005f)
            ),
            start = Offset(centerX - shapeSize / 2, centerY - shapeSize / 2),
            end = Offset(centerX + shapeSize / 2, centerY + shapeSize / 2)
        )
        val borderColor = Color.White.copy(alpha = alphaStroke)

        rotate(rotationAngle, pivot = Offset(centerX, centerY)) {
            when (shapeType) {
                // 0: Rounded Rectangle / Square
                0 -> {
                    val corner = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                    val topLeft = Offset(centerX - shapeSize / 2, centerY - shapeSize / 2)
                    val rectSize = Size(shapeSize, shapeSize * (0.8f + (shapeSeed % 5) * 0.1f))
                    drawRoundRect(brush = fillBrush, topLeft = topLeft, size = rectSize, cornerRadius = corner)
                    drawRoundRect(color = borderColor, topLeft = topLeft, size = rectSize, cornerRadius = corner, style = Stroke(strokeWidthPx))
                }
                // 1: Circle / Oval
                1 -> {
                    val radius = shapeSize / 2
                    drawCircle(brush = fillBrush, radius = radius, center = Offset(centerX, centerY))
                    drawCircle(color = borderColor, radius = radius, center = Offset(centerX, centerY), style = Stroke(strokeWidthPx))
                }
                // 2: Triangle (Path)
                2 -> {
                    val path = createPolygonPath(center = Offset(centerX, centerY), radius = shapeSize / 2, sides = 3)
                    drawPath(path = path, brush = fillBrush)
                    drawPath(path = path, color = borderColor, style = Stroke(strokeWidthPx))
                }
                // 3: Pentagon (5-sided polygon)
                3 -> {
                    val path = createPolygonPath(center = Offset(centerX, centerY), radius = shapeSize / 2, sides = 5)
                    drawPath(path = path, brush = fillBrush)
                    drawPath(path = path, color = borderColor, style = Stroke(strokeWidthPx))
                }
                // 4: Hexagon (6-sided polygon)
                4 -> {
                    val path = createPolygonPath(center = Offset(centerX, centerY), radius = shapeSize / 2, sides = 6)
                    drawPath(path = path, brush = fillBrush)
                    drawPath(path = path, color = borderColor, style = Stroke(strokeWidthPx))
                }
                // 5: Pill / Elongated Capsule
                else -> {
                    val width = shapeSize * 1.5f
                    val height = shapeSize * 0.55f
                    val corner = CornerRadius(height / 2, height / 2)
                    val topLeft = Offset(centerX - width / 2, centerY - height / 2)
                    val rectSize = Size(width, height)
                    drawRoundRect(brush = fillBrush, topLeft = topLeft, size = rectSize, cornerRadius = corner)
                    drawRoundRect(color = borderColor, topLeft = topLeft, size = rectSize, cornerRadius = corner, style = Stroke(strokeWidthPx))
                }
            }
        }
    }
}

/**
 * Creates a regular polygon path (Triangle, Pentagon, Hexagon, etc.) centered at a given point.
 */
private fun createPolygonPath(center: Offset, radius: Float, sides: Int): Path {
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
