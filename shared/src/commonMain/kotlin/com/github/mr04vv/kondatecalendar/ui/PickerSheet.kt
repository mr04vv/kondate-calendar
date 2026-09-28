package com.github.mr04vv.kondatecalendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.mr04vv.kondatecalendar.data.Dish
import com.github.mr04vv.kondatecalendar.data.DishType
import com.github.mr04vv.kondatecalendar.data.Genre
import com.github.mr04vv.kondatecalendar.domain.DishFilter
import com.github.mr04vv.kondatecalendar.domain.recommend
import kotlin.random.Random

private const val SHEET_HEIGHT_FRACTION = 0.92f
private const val GRID_COLUMNS = 4
private val TileShape = RoundedCornerShape(8.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerSheet(
    slot: SlotKey,
    dishes: List<Dish>,
    onPick: (Dish) -> Unit,
    onNewDish: () -> Unit,
    canCookOnly: Boolean,
    onCanCookOnlyChange: (Boolean) -> Unit,
    onToggleCanCook: (Dish) -> Unit,
    onDismiss: () -> Unit,
) {
    var filter by remember { mutableStateOf(DishFilter(canCookOnly = canCookOnly)) }
    var recommended by remember { mutableStateOf<Dish?>(null) }
    var askedForRecommendation by remember { mutableStateOf(false) }
    val shown = remember(dishes, filter) { dishes.filter(filter::matches) }
    fun recommendAgain() {
        recommended = recommend(dishes, filter, Random.Default, excludeId = recommended?.id)
        askedForRecommendation = true
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(SHEET_HEIGHT_FRACTION).padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${slot.date.shortLabel()} ${slot.meal.label}の献立",
                    style = HeadingMedium,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onNewDish) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("新しい料理")
                }
            }
            OutlinedTextField(
                value = filter.query,
                onValueChange = { filter = filter.copy(query = it) },
                label = { Text("料理名で検索") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                item {
                    FilterChip(selected = filter.genre == null, onClick = { filter = filter.copy(genre = null) }, label = { Text("すべて") })
                }
                items(Genre.entries) { genre ->
                    FilterChip(
                        selected = filter.genre == genre,
                        onClick = { filter = filter.copy(genre = if (filter.genre == genre) null else genre) },
                        label = { Text(genre.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = genre.color,
                            selectedContainerColor = genre.color,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = filter.canCookOnly,
                        onClick = {
                            filter = filter.copy(canCookOnly = !filter.canCookOnly)
                            onCanCookOnlyChange(filter.canCookOnly)
                        },
                        label = { Text("作れる") },
                        leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    )
                }
                items(DishType.entries) { type ->
                    FilterChip(
                        selected = type in filter.types,
                        onClick = {
                            filter = filter.copy(types = if (type in filter.types) filter.types - type else filter.types + type)
                        },
                        label = { Text(type.label) },
                    )
                }
            }
            if (askedForRecommendation) {
                RecommendationCard(recommended, onPick = onPick, onAgain = ::recommendAgain)
            }
            EmptyNote("料理を長押しすると「作れる」に登録・解除できます", Modifier.padding(top = 4.dp))
            if (shown.isEmpty()) {
                EmptyNote(
                    if (filter.canCookOnly && dishes.none { it.canCook }) {
                        "「作れる」に登録した料理はまだありません。「作れる」を外して料理を長押しすると登録できます"
                    } else {
                        "条件に合う料理がありません"
                    },
                    Modifier.padding(vertical = 16.dp),
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(GRID_COLUMNS),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).padding(top = 8.dp),
            ) {
                items(shown, key = { it.id }) { dish ->
                    DishTile(dish, onClick = { onPick(dish) }, onLongClick = { onToggleCanCook(dish) })
                }
            }
            Button(
                onClick = ::recommendAgain,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(48.dp),
            ) {
                Icon(Icons.Default.Star, contentDescription = null)
                Text("この条件でおすすめを出す", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun RecommendationCard(dish: Dish?, onPick: (Dish) -> Unit, onAgain: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dish == null) {
            Text("条件に合う料理がありません", fontSize = 13.sp, modifier = Modifier.weight(1f))
            return@Row
        }
        DishThumb(dish, 44.dp)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text("おすすめ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(dish.name, style = Heading, fontSize = 17.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        TextButton(onClick = onAgain) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Text("ほかの")
        }
        Button(onClick = { onPick(dish) }) { Text("これにする") }
    }
}

@Composable
private fun DishTile(dish: Dish, onClick: () -> Unit, onLongClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Column(
        Modifier.fillMaxWidth().height(80.dp).clip(TileShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, TileShape)
            .combinedClickable(
                onClickLabel = "${dish.name}にする",
                onLongClickLabel = if (dish.canCook) "「作れる」から外す" else "「作れる」に登録",
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(dish.genre.color))
        Box(Modifier.fillMaxWidth()) {
            DishThumb(dish, 40.dp, Modifier.padding(top = 6.dp).align(Alignment.Center))
            if (dish.canCook) CanCookBadge(Modifier.align(Alignment.TopEnd).padding(4.dp))
        }
        Text(
            dish.name,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotMenuSheet(
    slot: SlotKey,
    dish: Dish,
    onOpenRecipe: () -> Unit,
    onSwap: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                DishThumb(dish, 52.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    EmptyNote("${slot.date.shortLabel()} ${slot.meal.label}")
                    Text(dish.name, style = HeadingMedium)
                }
            }
            val colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            ListItem(
                headlineContent = { Text("レシピを見る") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                colors = colors,
                modifier = Modifier.clickable(onClick = onOpenRecipe),
            )
            ListItem(
                headlineContent = { Text("入れ替える") },
                leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
                colors = colors,
                modifier = Modifier.clickable(onClick = onSwap),
            )
            ListItem(
                headlineContent = { Text("空にする") },
                leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                colors = colors,
                modifier = Modifier.clickable(onClick = onClear),
            )
        }
    }
}
