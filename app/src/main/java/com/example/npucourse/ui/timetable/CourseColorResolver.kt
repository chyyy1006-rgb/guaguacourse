package com.example.npucourse.ui.timetable

import androidx.compose.ui.graphics.Color
import com.example.npucourse.model.DemoCourse
import kotlin.math.abs

data class JellyCourseColors(
    val background: Color,
    val accent: Color,
    val title: Color,
    val secondary: Color
)

private data class PaletteEntry(
    val lightBackground: Long,
    val lightAccent: Long,
    val lightText: Long,
    val darkBackground: Long,
    val darkAccent: Long,
    val darkText: Long,
    val hue: Float
)

private val palette = listOf(
    PaletteEntry(0xFFDCEAFF, 0xFF7EA9F8, 0xFF285FB8, 0xFF19345B, 0xFF77A7FF, 0xFFC7DAFF, 216f),
    PaletteEntry(0xFFD9F5E9, 0xFF69CEA7, 0xFF1C7756, 0xFF173E32, 0xFF63D6AA, 0xFFB9F3DB, 155f),
    PaletteEntry(0xFFFFE0E8, 0xFFF58CA9, 0xFFB93661, 0xFF512333, 0xFFFF8CAD, 0xFFFFC4D4, 342f),
    PaletteEntry(0xFFFFE8D8, 0xFFF3A474, 0xFFA94F22, 0xFF51301F, 0xFFFFA873, 0xFFFFD0B2, 24f),
    PaletteEntry(0xFFFFF3C9, 0xFFF0C85E, 0xFF8A6200, 0xFF4B401C, 0xFFF3CD5E, 0xFFFFE89D, 47f),
    PaletteEntry(0xFFE9E2FF, 0xFFA99AF4, 0xFF5D46AD, 0xFF312A55, 0xFFB2A0FF, 0xFFD8CEFF, 258f),
    PaletteEntry(0xFFD9F3F5, 0xFF6CC5CF, 0xFF18717A, 0xFF183E43, 0xFF66CED8, 0xFFB9EEF2, 185f),
    PaletteEntry(0xFFEEDFFF, 0xFFBC8AE9, 0xFF7440A5, 0xFF402653, 0xFFC58EF1, 0xFFE5C5FF, 282f),
    PaletteEntry(0xFFE5F5D5, 0xFF93C96E, 0xFF4C7728, 0xFF2B411D, 0xFF94D56B, 0xFFD0EFB5, 94f),
    PaletteEntry(0xFFFFE1DA, 0xFFF09786, 0xFFA84335, 0xFF522A25, 0xFFFF9583, 0xFFFFC8BE, 9f)
)

/** Stable hashing followed by deterministic, local greedy conflict resolution. */
fun resolveJellyCourseColors(
    courses: List<DemoCourse>,
    darkTheme: Boolean
): Map<Long, JellyCourseColors> {
    if (courses.isEmpty()) return emptyMap()
    val eventsByKey = courses.groupBy(::stableCourseKey)
    val keys = eventsByKey.keys.sortedWith(compareBy({ stableHash(it) }, { it }))
    val neighbours = keys.associateWith { linkedSetOf<String>() }.toMutableMap()

    for (i in courses.indices) {
        for (j in i + 1 until courses.size) {
            val a = courses[i]
            val b = courses[j]
            val aKey = stableCourseKey(a)
            val bKey = stableCourseKey(b)
            if (aKey != bKey && areVisuallyAdjacent(a, b)) {
                neighbours.getValue(aKey).add(bKey)
                neighbours.getValue(bKey).add(aKey)
            }
        }
    }

    val assigned = mutableMapOf<String, Int>()
    keys.sortedWith(compareByDescending<String> { neighbours.getValue(it).size }.thenBy { stableHash(it) })
        .forEach { key ->
            val preferred = floorMod(stableHash(key), palette.size)
            val used = neighbours.getValue(key).mapNotNull(assigned::get)
            val preferredSafe = used.none { hueDistance(palette[preferred].hue, palette[it].hue) < 54f }
            val chosen = if (preferredSafe) {
                preferred
            } else {
                palette.indices.maxWithOrNull(
                    compareBy<Int> { candidate ->
                        used.minOfOrNull { hueDistance(palette[candidate].hue, palette[it].hue) } ?: 180f
                    }.thenBy { candidate ->
                        -circularIndexDistance(candidate, preferred, palette.size)
                    }.thenBy { candidate ->
                        -floorMod(stableHash("$key#$candidate"), 997)
                    }
                ) ?: preferred
            }
            assigned[key] = chosen
        }

    return courses.associate { course ->
        val entry = palette[assigned.getValue(stableCourseKey(course))]
        val background = Color(if (darkTheme) entry.darkBackground else entry.lightBackground)
        val accent = Color(if (darkTheme) entry.darkAccent else entry.lightAccent)
        val text = Color(if (darkTheme) entry.darkText else entry.lightText)
        course.id to JellyCourseColors(
            background = background,
            accent = accent,
            title = text,
            secondary = text.copy(alpha = if (darkTheme) 0.82f else 0.76f)
        )
    }
}

private fun stableCourseKey(course: DemoCourse): String =
    course.name.trim().lowercase() + "|" + course.teacher.trim().lowercase()

private fun areVisuallyAdjacent(a: DemoCourse, b: DemoCourse): Boolean {
    val overlapsVertically = a.startSection <= b.endSection && b.startSection <= a.endSection
    return when {
        a.day == b.day -> a.startSection <= b.endSection + 1 && b.startSection <= a.endSection + 1
        abs(a.day - b.day) == 1 -> overlapsVertically
        else -> false
    }
}

private fun hueDistance(a: Float, b: Float): Float {
    val raw = abs(a - b) % 360f
    return minOf(raw, 360f - raw)
}

private fun circularIndexDistance(a: Int, b: Int, size: Int): Int {
    val raw = abs(a - b)
    return minOf(raw, size - raw)
}

private fun stableHash(value: String): Int {
    var hash = 0x811C9DC5u
    value.forEach { char ->
        hash = (hash xor char.code.toUInt()) * 0x01000193u
    }
    return hash.toInt()
}

private fun floorMod(value: Int, divisor: Int): Int =
    ((value % divisor) + divisor) % divisor
