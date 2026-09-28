package com.github.mr04vv.kondatecalendar.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.github.mr04vv.kondatecalendar.data.Genre
import kotlinx.datetime.DayOfWeek

val Genre.color: Color
    get() = when (this) {
        Genre.WASHOKU -> Color(0xFF3F7A4F)
        Genre.CHUKA -> Color(0xFFB8412E)
        Genre.YOSHOKU -> Color(0xFF2F5F9E)
        Genre.KANKOKU -> Color(0xFFC8701A)
        Genre.ETHNIC -> Color(0xFF7B4FA3)
        Genre.OTHER -> Color(0xFF6B665E)
    }

/** Background tint behind a dish name in its genre color. */
const val GENRE_TINT_ALPHA = 0.1f

private val Saturday = Color(0xFF2F5F9E)
private val Sunday = Color(0xFFB8412E)

private val lightScheme = lightColorScheme(
    primary = Color(0xFF8A5A2B),
    onPrimary = Color(0xFFFFFDF8),
    primaryContainer = Color(0xFFEBDCC8),
    onPrimaryContainer = Color(0xFF3D2810),
    background = Color(0xFFF6F1E7),
    onBackground = Color(0xFF2B2622),
    surface = Color(0xFFFFFDF8),
    onSurface = Color(0xFF2B2622),
    surfaceVariant = Color(0xFFE9E1D2),
    onSurfaceVariant = Color(0xFF6B6256),
    surfaceContainerLow = Color(0xFFFFFDF8),
    surfaceContainer = Color(0xFFF6F1E7),
    surfaceContainerHigh = Color(0xFFF1EBDF),
    outline = Color(0xFFD9CFBC),
    outlineVariant = Color(0xFFE2D9C8),
    secondaryContainer = Color(0xFFE9E1D2),
    onSecondaryContainer = Color(0xFF2B2622),
)

// ponytail: the platform serif stands in for Shippori Mincho; bundle the font if a device lacks a Japanese serif.
val Heading = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)

val HeadingLarge = Heading.copy(fontSize = 30.sp, lineHeight = 36.sp)
val HeadingMedium = Heading.copy(fontSize = 22.sp, lineHeight = 28.sp)

fun weekdayColor(day: DayOfWeek, default: Color): Color = when (day) {
    DayOfWeek.SATURDAY -> Saturday
    DayOfWeek.SUNDAY -> Sunday
    else -> default
}

/** Always the light 案1 palette, whatever the system dark mode is. */
@Composable
fun KondateTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightScheme, content = content)
}
