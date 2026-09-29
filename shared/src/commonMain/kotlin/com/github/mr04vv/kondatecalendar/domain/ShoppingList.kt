package com.github.mr04vv.kondatecalendar.domain

import com.github.mr04vv.kondatecalendar.data.Dish
import com.github.mr04vv.kondatecalendar.data.Meal
import com.github.mr04vv.kondatecalendar.data.ShoppingItem
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

/** Where a dish's ingredients are added from: a planned slot, or a recipe opened on its own. */
sealed interface ShoppingSource {
    val dish: Dish

    data class Slot(val date: LocalDate, val meal: Meal, override val dish: Dish) : ShoppingSource

    data class Recipe(override val dish: Dish) : ShoppingSource
}

/**
 * Returns the rows to insert when [sources] are added to a list that currently holds [existing]: one row per
 * ingredient. A source that already has a row, checked or not, is skipped so pressing the button twice adds nothing.
 */
fun shoppingRowsToAdd(existing: List<ShoppingItem>, sources: List<ShoppingSource>): List<ShoppingItem> {
    val present = existing.mapNotNullTo(HashSet()) { it.sourceKey() }
    return sources.filter { present.add(it.key()) }.flatMap { source ->
        source.dish.ingredients.map { ingredient ->
            ShoppingItem(
                name = ingredient.name.trim(),
                amount = ingredient.amount.trim(),
                date = (source as? ShoppingSource.Slot)?.date,
                meal = (source as? ShoppingSource.Slot)?.meal,
                dishId = source.dish.id,
                dishName = source.dish.name,
            )
        }
    }
}

/** One line of the shopping list: every row with the same name and checked state. */
data class ShoppingGroup(
    val name: String,
    val checked: Boolean,
    val items: List<ShoppingItem>,
) {
    /** Non-blank amounts joined in row order. */
    val amount: String = items.map { it.amount }.filter { it.isNotEmpty() }.joinToString(AMOUNT_SEPARATOR)

    /** Where the rows came from, e.g. 「肉じゃが（10/7 夜）」; manual rows are not listed. */
    val sources: List<String> = items.mapNotNull { it.sourceLabel() }.distinct()
}

/** Groups rows by exact name and checked state, keeping the order in which each group first appears. */
fun groupShoppingList(items: List<ShoppingItem>): List<ShoppingGroup> =
    items.groupBy { it.name to it.checked }.map { (key, rows) -> ShoppingGroup(key.first, key.second, rows) }

/** Plain text for sharing the list elsewhere: open items first, then checked ones marked as bought. */
fun shoppingListText(groups: List<ShoppingGroup>): String =
    (listOf(SHARE_TITLE) + groups.sortedBy { it.checked }.map { group ->
        val amount = if (group.amount.isEmpty()) "" else " ${group.amount}"
        "・${group.name}$amount${if (group.checked) CHECKED_MARK else ""}"
    }).joinToString("\n")

private data class SourceKey(val date: LocalDate?, val meal: Meal?, val dishId: Long)

private fun ShoppingItem.sourceKey(): SourceKey? = dishId?.let { SourceKey(date, meal, it) }

private fun ShoppingSource.key(): SourceKey = when (this) {
    is ShoppingSource.Slot -> SourceKey(date, meal, dish.id)
    is ShoppingSource.Recipe -> SourceKey(null, null, dish.id)
}

private fun ShoppingItem.sourceLabel(): String? {
    val dish = dishName ?: return null
    return if (date == null || meal == null) "$dish（レシピから）" else "$dish（${date.month.number}/${date.day} ${meal.label}）"
}

private const val AMOUNT_SEPARATOR = "、"
private const val SHARE_TITLE = "買い物リスト"
private const val CHECKED_MARK = "（済）"
