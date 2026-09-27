package com.github.mr04vv.kondatecalendar.domain

import com.github.mr04vv.kondatecalendar.data.Ingredient
import com.github.mr04vv.kondatecalendar.data.ShoppingItem

/**
 * Returns the rows to upsert when [adding] is added to a list that currently holds [existing].
 * An ingredient joins an unchecked row with exactly the same name; otherwise it becomes a new row (id 0).
 */
fun mergeIntoShoppingList(existing: List<ShoppingItem>, adding: List<Ingredient>): List<ShoppingItem> {
    val open = existing.filterNot { it.checked }.associateByTo(LinkedHashMap()) { it.name }
    val touched = LinkedHashSet<String>()
    for (ingredient in adding) {
        val name = ingredient.name.trim()
        val amount = ingredient.amount.trim()
        val current = open[name]
        open[name] = when {
            current == null -> ShoppingItem(name = name, amount = amount)
            amount.isEmpty() -> current
            current.amount.isEmpty() -> current.copy(amount = amount)
            else -> current.copy(amount = current.amount + AMOUNT_SEPARATOR + amount)
        }
        touched += name
    }
    return touched.map { open.getValue(it) }.filter { it !in existing }
}

private const val AMOUNT_SEPARATOR = "、"
