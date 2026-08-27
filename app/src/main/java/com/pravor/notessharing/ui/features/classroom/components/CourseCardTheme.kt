package com.pravor.notessharing.ui.features.classroom.components

import androidx.compose.ui.graphics.Color
import com.pravor.notessharing.domain.model.classroom.ClassroomCourse
import kotlin.math.abs

/**
 * Visual theme definition for dynamic subject card differentiation.
 */
data class CourseCardTheme(
    val paletteIndex: Int,
    val accentColor: Color,
    val secondaryAccent: Color,
    val gradientColors: List<Color>
)

// 12 Sophisticated, Dark-Theme Tuned Color Palettes
val CoursePalettes = listOf(
    // 0. Violet / Amethyst
    CourseCardTheme(
        paletteIndex = 0,
        accentColor = Color(0xFFC084FC),
        secondaryAccent = Color(0xFF9333EA),
        gradientColors = listOf(Color(0xFF1E152E), Color(0xFF11101E), Color(0xFF090C14))
    ),
    // 1. Emerald / Mint
    CourseCardTheme(
        paletteIndex = 1,
        accentColor = Color(0xFF34D399),
        secondaryAccent = Color(0xFF059669),
        gradientColors = listOf(Color(0xFF0E261F), Color(0xFF0E1A1A), Color(0xFF090C14))
    ),
    // 2. Cyan / Ocean
    CourseCardTheme(
        paletteIndex = 2,
        accentColor = Color(0xFF22D3EE),
        secondaryAccent = Color(0xFF0284C7),
        gradientColors = listOf(Color(0xFF0E232E), Color(0xFF0D1A25), Color(0xFF090C14))
    ),
    // 3. Rose / Ruby
    CourseCardTheme(
        paletteIndex = 3,
        accentColor = Color(0xFFFB7185),
        secondaryAccent = Color(0xFFE11D48),
        gradientColors = listOf(Color(0xFF2B131E), Color(0xFF17101B), Color(0xFF090C14))
    ),
    // 4. Amber / Gold
    CourseCardTheme(
        paletteIndex = 4,
        accentColor = Color(0xFFFBBF24),
        secondaryAccent = Color(0xFFD97706),
        gradientColors = listOf(Color(0xFF271D0F), Color(0xFF171415), Color(0xFF090C14))
    ),
    // 5. Indigo / Electric
    CourseCardTheme(
        paletteIndex = 5,
        accentColor = Color(0xFF818CF8),
        secondaryAccent = Color(0xFF4F46E5),
        gradientColors = listOf(Color(0xFF151933), Color(0xFF0F1222), Color(0xFF090C14))
    ),
    // 6. Teal / Seafoam
    CourseCardTheme(
        paletteIndex = 6,
        accentColor = Color(0xFF2DD4BF),
        secondaryAccent = Color(0xFF0D9488),
        gradientColors = listOf(Color(0xFF0D2625), Color(0xFF0D1B1E), Color(0xFF090C14))
    ),
    // 7. Blue / Cobalt
    CourseCardTheme(
        paletteIndex = 7,
        accentColor = Color(0xFF60A5FA),
        secondaryAccent = Color(0xFF2563EB),
        gradientColors = listOf(Color(0xFF111F33), Color(0xFF0D1522), Color(0xFF090C14))
    ),
    // 8. Orange / Sunset
    CourseCardTheme(
        paletteIndex = 8,
        accentColor = Color(0xFFFB923C),
        secondaryAccent = Color(0xFFEA580C),
        gradientColors = listOf(Color(0xFF2A180E), Color(0xFF181215), Color(0xFF090C14))
    ),
    // 9. Magenta / Fuchsia
    CourseCardTheme(
        paletteIndex = 9,
        accentColor = Color(0xFFE879F9),
        secondaryAccent = Color(0xFFC026D3),
        gradientColors = listOf(Color(0xFF28122A), Color(0xFF16101F), Color(0xFF090C14))
    ),
    // 10. Lime / Neon Sage
    CourseCardTheme(
        paletteIndex = 10,
        accentColor = Color(0xFFA3E635),
        secondaryAccent = Color(0xFF65A30D),
        gradientColors = listOf(Color(0xFF1C2610), Color(0xFF131B12), Color(0xFF090C14))
    ),
    // 11. Pink / Blush
    CourseCardTheme(
        paletteIndex = 11,
        accentColor = Color(0xFFF472B6),
        secondaryAccent = Color(0xFFDB2777),
        gradientColors = listOf(Color(0xFF2A1324), Color(0xFF17101C), Color(0xFF090C14))
    )
)

/**
 * Dynamically resolves a course theme across the 12 color families.
 * Uses golden-ratio index stepping to guarantee non-repeating, diverse colors on adjacent cards.
 */
fun getCourseTheme(course: ClassroomCourse, index: Int = 0): CourseCardTheme {
    val hash = abs(course.id.hashCode() * 31 + course.name.hashCode())
    val paletteIndex = if (index >= 0) {
        (index * 7 + (hash % 3)) % CoursePalettes.size
    } else {
        hash % CoursePalettes.size
    }
    return CoursePalettes[paletteIndex]
}
