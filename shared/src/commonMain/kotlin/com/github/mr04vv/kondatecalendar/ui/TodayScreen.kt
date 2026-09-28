package com.github.mr04vv.kondatecalendar.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.mr04vv.kondatecalendar.data.Dish
import com.github.mr04vv.kondatecalendar.data.Ingredient
import com.github.mr04vv.kondatecalendar.data.Meal
import com.github.mr04vv.kondatecalendar.domain.ShoppingSource
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

private val CardShape = RoundedCornerShape(12.dp)
private const val PREVIEW_INGREDIENTS = 4
private const val CARD_TYPES = 2

@Composable
fun TodayScreen(
    plan: Map<SlotKey, Dish>,
    onOpenRecipe: (Dish) -> Unit,
    onPick: (SlotKey) -> Unit,
    onAddToShopping: (List<ShoppingSource>) -> Unit,
) {
    val today = today()
    var epochDay by rememberSaveable { mutableLongStateOf(today.toEpochDays()) }
    val date = LocalDate.fromEpochDays(epochDay)
    val sources = plan.sourcesOn(listOf(date))
    val ingredients = sources.flatMap { it.dish.ingredients }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { epochDay-- }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "前の日")
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (date == today) "今日" else date.weekdayLabel() + "曜日",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${date.month.number}月${date.day}日（${date.weekdayLabel()}）",
                    style = HeadingMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            IconButton(onClick = { epochDay++ }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "次の日")
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Meal.entries.forEach { meal ->
                val key = SlotKey(date, meal)
                val dish = plan[key]
                if (dish == null) {
                    EmptyMealCard(meal, onPick = { onPick(key) })
                } else {
                    MealCard(meal, dish, large = meal == Meal.DINNER, onOpen = { onOpenRecipe(dish) }, onSwap = { onPick(key) })
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Button(
            onClick = { onAddToShopping(sources) },
            enabled = ingredients.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(48.dp),
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null)
            Text(
                "${if (date == today) "今日" else "この日"}の材料 ${ingredients.size} 品を買い物リストへ",
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun cardModifier() = Modifier.fillMaxWidth().clip(CardShape)
    .background(MaterialTheme.colorScheme.surface)
    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CardShape)

@Composable
private fun MealLabel(meal: Meal) {
    Text(meal.label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun EmptyMealCard(meal: Meal, onPick: () -> Unit) {
    Row(cardModifier().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        MealLabel(meal)
        Text(
            "未定",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(start = 16.dp),
        )
        OutlinedButton(onClick = onPick) { Text("献立を決める") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DishInfo(dish: Dish) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        GenreTag(dish.genre)
        dish.types.take(CARD_TYPES).forEach { TypeTag(it) }
        Text(
            "材料 ${dish.ingredients.size} 品" + (dish.minutes?.let { " · $it 分" } ?: ""),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}

@Composable
private fun MealCard(meal: Meal, dish: Dish, large: Boolean, onOpen: () -> Unit, onSwap: () -> Unit) {
    if (!large) {
        Row(
            cardModifier().clickable(onClickLabel = "レシピを見る", onClick = onOpen).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DishThumb(dish, 60.dp)
            Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MealLabel(meal)
                Text(dish.name, style = Heading, fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurface)
                DishInfo(dish)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    Column(cardModifier().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MealLabel(meal)
        Row(verticalAlignment = Alignment.CenterVertically) {
            DishThumb(dish, 96.dp)
            Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(dish.name, style = HeadingMedium, fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurface)
                DishInfo(dish)
            }
        }
        if (dish.ingredients.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            dish.ingredients.take(PREVIEW_INGREDIENTS).forEach { IngredientLine(it) }
            val rest = dish.ingredients.size - PREVIEW_INGREDIENTS
            if (rest > 0) EmptyNote("ほか $rest 品")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f)) { Text("レシピを見る") }
            OutlinedButton(onClick = onSwap, modifier = Modifier.weight(1f)) { Text("入れ替える") }
        }
    }
}

@Composable
fun IngredientLine(ingredient: Ingredient) {
    Row(Modifier.fillMaxWidth()) {
        Text(ingredient.name, fontSize = 14.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
        Text(ingredient.amount, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
