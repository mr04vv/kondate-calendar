package com.github.mr04vv.kondatecalendar.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.mr04vv.kondatecalendar.data.Dish
import com.github.mr04vv.kondatecalendar.data.DishType
import com.github.mr04vv.kondatecalendar.data.Genre
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlin.time.Clock

private val TagShape = RoundedCornerShape(4.dp)
private const val TAG_FONT_SIZE = 11
private const val EMOJI_SIZE_RATIO = 0.55f

@Composable
fun GenreTag(genre: Genre) {
    Text(
        genre.label,
        color = Color.White,
        fontSize = TAG_FONT_SIZE.sp,
        modifier = Modifier.clip(TagShape).background(genre.color).padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

@Composable
fun TypeTag(type: DishType) {
    Text(
        type.label,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = TAG_FONT_SIZE.sp,
        modifier = Modifier.clip(TagShape).background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** The dish photo when it has one, otherwise its emoji on a tinted square. */
@Composable
fun DishThumb(dish: Dish, size: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size / 6)
    val photo = dish.photoPath?.let { rememberPhoto(it) }
    Box(
        modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(photo, contentDescription = dish.name, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Text(dish.emoji, fontSize = (size.value * EMOJI_SIZE_RATIO).sp)
        }
    }
}

@Composable
fun rememberPhoto(path: String): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) { loadPhoto(path) }
    }
    return bitmap
}

fun DayOfWeek.label(): String = when (this) {
    DayOfWeek.MONDAY -> "月"
    DayOfWeek.TUESDAY -> "火"
    DayOfWeek.WEDNESDAY -> "水"
    DayOfWeek.THURSDAY -> "木"
    DayOfWeek.FRIDAY -> "金"
    DayOfWeek.SATURDAY -> "土"
    DayOfWeek.SUNDAY -> "日"
}

fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

fun LocalDate.weekdayLabel(): String = dayOfWeek.label()

/** e.g. 10/7（水） */
fun LocalDate.shortLabel(): String = "${month.number}/$day（${weekdayLabel()}）"

@Composable
fun EmptyNote(text: String, modifier: Modifier = Modifier) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = modifier)
}

/** A small check mark on dishes the user can cook. */
@Composable
fun CanCookBadge(modifier: Modifier = Modifier) {
    Box(
        modifier.size(16.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = "作れる",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(12.dp),
        )
    }
}

/** Toggle for the "can cook" mark, shown on the recipe and edit screens. */
@Composable
fun CanCookChip(selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(if (selected) "作れる料理" else "作れる料理に登録") },
        leadingIcon = {
            Icon(if (selected) Icons.Default.Check else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        },
    )
}
