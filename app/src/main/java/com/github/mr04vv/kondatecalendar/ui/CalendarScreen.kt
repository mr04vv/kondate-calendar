package com.github.mr04vv.kondatecalendar.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.mr04vv.kondatecalendar.data.Dish
import com.github.mr04vv.kondatecalendar.data.Genre
import com.github.mr04vv.kondatecalendar.data.Meal
import com.github.mr04vv.kondatecalendar.domain.ListRow
import com.github.mr04vv.kondatecalendar.domain.ShoppingSource
import com.github.mr04vv.kondatecalendar.domain.listRows
import com.github.mr04vv.kondatecalendar.domain.monthGrid
import com.github.mr04vv.kondatecalendar.domain.weekOf
import com.github.mr04vv.kondatecalendar.domain.weekStart
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private enum class CalendarMode(val label: String) { GRID("格子"), LIST("リスト") }

private const val LIST_WEEKS_BEFORE = 26L
private const val LIST_WEEKS_AFTER = 52L
private const val OUTSIDE_MONTH_ALPHA = 0.5f
private const val GRID_NAME_MAX_LINES = 3
private val CellShape = RoundedCornerShape(6.dp)
private val SlotShape = RoundedCornerShape(4.dp)
private val ListRowHeight = 48.dp
private val DateColumnWidth = 52.dp

@Composable
fun CalendarScreen(
    plan: Map<SlotKey, Dish>,
    onTapSlot: (SlotKey) -> Unit,
    onAddToShopping: (List<ShoppingSource>) -> Unit,
) {
    val today = remember { LocalDate.now() }
    var mode by rememberSaveable { mutableStateOf(CalendarMode.GRID) }
    var monthIndex by rememberSaveable { mutableIntStateOf(YearMonth.from(today).let { it.year * MONTHS + it.monthValue - 1 }) }
    val month = YearMonth.of(monthIndex / MONTHS, monthIndex % MONTHS + 1)
    var focus by rememberSaveable { mutableStateOf(Meal.DINNER) }

    val rows = remember(today) { listRows(today.minusWeeks(LIST_WEEKS_BEFORE), today.plusWeeks(LIST_WEEKS_AFTER)) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = rows.indexOf(ListRow.WeekHeader(weekStart(today))))
    val listMonth by remember {
        derivedStateOf {
            when (val row = rows[listState.firstVisibleItemIndex]) {
                is ListRow.WeekHeader -> YearMonth.from(row.monday)
                is ListRow.Day -> YearMonth.from(row.date)
            }
        }
    }
    val shownMonth = if (mode == CalendarMode.GRID) month else listMonth

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${shownMonth.monthValue}月", style = HeadingLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "${shownMonth.year}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            if (mode == CalendarMode.GRID) {
                IconButton(onClick = { monthIndex-- }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "前の月")
                }
                IconButton(onClick = { monthIndex++ }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "次の月")
                }
            }
            SingleChoiceSegmentedButtonRow(Modifier.width(150.dp)) {
                CalendarMode.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = { mode = m },
                        shape = SegmentedButtonDefaults.itemShape(i, CalendarMode.entries.size),
                        icon = {},
                    ) { Text(m.label, fontSize = 13.sp) }
                }
            }
        }
        when (mode) {
            CalendarMode.GRID -> MonthGrid(month, today, plan, focus, onFocus = { focus = it }, onTapSlot)
            CalendarMode.LIST -> PlanList(rows, listState, today, plan, onTapSlot, onAddToShopping)
        }
    }
}

private const val MONTHS = 12

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    plan: Map<SlotKey, Dish>,
    focus: Meal,
    onFocus: (Meal) -> Unit,
    onTapSlot: (SlotKey) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("名前を出す:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(Meal.DINNER, Meal.LUNCH, Meal.BREAKFAST).forEach { meal ->
                FilterChip(selected = focus == meal, onClick = { onFocus(meal) }, label = { Text(meal.label) })
            }
            Spacer(Modifier.weight(1f))
            Text("丸はほかの時間帯", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            DayOfWeek.entries.forEach { day ->
                Text(
                    day.label(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    color = weekdayColor(day, MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            monthGrid(month).chunked(DayOfWeek.entries.size).forEach { week ->
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    week.forEach { date ->
                        GridCell(
                            date = date,
                            inMonth = YearMonth.from(date) == month,
                            isToday = date == today,
                            plan = plan,
                            focus = focus,
                            onTap = { onTapSlot(SlotKey(date, focus)) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
        FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Genre.entries.forEach { genre ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(genre.color))
                    Text(genre.label, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun GridCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    plan: Map<SlotKey, Dish>,
    focus: Meal,
    onTap: () -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dish = plan[SlotKey(date, focus)]
    Column(
        modifier
            .alpha(if (inMonth) 1f else OUTSIDE_MONTH_ALPHA)
            .clip(CellShape)
            .background(colors.surface)
            .border(1.dp, colors.outlineVariant, CellShape)
            .clickable(onClickLabel = "${date.shortLabel()}${focus.label}の献立") { onTap() }
            .padding(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            DateBadge(date, isToday)
            Spacer(Modifier.weight(1f))
            (Meal.entries - focus).forEach { meal ->
                Box(
                    Modifier.padding(start = 2.dp).size(7.dp).clip(CircleShape)
                        .background(plan[SlotKey(date, meal)]?.genre?.color ?: colors.outlineVariant),
                )
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().clip(SlotShape)
                .background(dish?.genre?.color?.copy(alpha = GENRE_TINT_ALPHA) ?: Color.Transparent),
        ) {
            Box(Modifier.fillMaxWidth().height(3.dp).background(dish?.genre?.color ?: colors.outlineVariant))
            Box(Modifier.weight(1f).fillMaxWidth().padding(2.dp), contentAlignment = Alignment.Center) {
                Text(
                    dish?.name ?: "未定",
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center,
                    maxLines = GRID_NAME_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                    color = if (dish == null) colors.onSurfaceVariant.copy(alpha = OUTSIDE_MONTH_ALPHA) else colors.onSurface,
                )
            }
        }
    }
}

@Composable
private fun DateBadge(date: LocalDate, isToday: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.size(18.dp).clip(CircleShape).background(if (isToday) colors.onBackground else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${date.dayOfMonth}",
            fontSize = 11.sp,
            color = if (isToday) colors.background else weekdayColor(date.dayOfWeek, colors.onSurface),
        )
    }
}

@Composable
private fun PlanList(
    rows: List<ListRow>,
    state: androidx.compose.foundation.lazy.LazyListState,
    today: LocalDate,
    plan: Map<SlotKey, Dish>,
    onTapSlot: (SlotKey) -> Unit,
    onAddToShopping: (List<ShoppingSource>) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Spacer(Modifier.width(DateColumnWidth))
            Meal.entries.forEach { meal ->
                Text(
                    meal.label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
            items(rows, key = { row ->
                when (row) {
                    is ListRow.WeekHeader -> "w${row.monday}"
                    is ListRow.Day -> "d${row.date}"
                }
            }) { row ->
                when (row) {
                    is ListRow.WeekHeader -> WeekHeader(row.monday, plan, onAddToShopping)
                    is ListRow.Day -> DayRow(row.date, row.date == today, plan, onTapSlot)
                }
            }
        }
    }
}

@Composable
private fun WeekHeader(monday: LocalDate, plan: Map<SlotKey, Dish>, onAddToShopping: (List<ShoppingSource>) -> Unit) {
    val week = weekOf(monday)
    val sources = plan.sourcesOn(week)
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${monday.monthValue}/${monday.dayOfMonth} – ${week.last().monthValue}/${week.last().dayOfMonth}",
            style = Heading,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onAddToShopping(sources) }, enabled = sources.any { it.dish.ingredients.isNotEmpty() }) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("この週の材料を買い物リストへ", fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun DayRow(date: LocalDate, isToday: Boolean, plan: Map<SlotKey, Dish>, onTapSlot: (SlotKey) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().height(ListRowHeight).padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.width(DateColumnWidth).fillMaxHeight().clip(SlotShape)
                .background(if (isToday) colors.onBackground else Color.Transparent),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val main = if (isToday) colors.background else colors.onBackground
            Text(
                if (date.dayOfMonth == 1) "${date.monthValue}/1" else "${date.dayOfMonth}",
                style = Heading,
                fontSize = 16.sp,
                color = main,
            )
            Text(
                date.weekdayLabel(),
                fontSize = 10.sp,
                color = if (isToday) main else weekdayColor(date.dayOfWeek, colors.onSurfaceVariant),
                modifier = Modifier.padding(start = 3.dp),
            )
        }
        Meal.entries.forEach { meal ->
            val key = SlotKey(date, meal)
            val dish = plan[key]
            Row(
                Modifier.weight(1f).fillMaxHeight().heightIn(min = 44.dp).clip(SlotShape)
                    .background(dish?.genre?.color?.copy(alpha = GENRE_TINT_ALPHA) ?: Color.Transparent)
                    .border(1.dp, if (dish == null) colors.outlineVariant else Color.Transparent, SlotShape)
                    .clickable(onClickLabel = "${date.shortLabel()}${meal.label}の献立") { onTapSlot(key) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(3.dp).fillMaxHeight().background(dish?.genre?.color ?: Color.Transparent))
                Text(
                    dish?.name ?: "",
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.onSurface,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}
