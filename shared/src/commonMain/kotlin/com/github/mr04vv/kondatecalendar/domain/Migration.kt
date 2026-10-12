package com.github.mr04vv.kondatecalendar.domain

import com.github.mr04vv.kondatecalendar.data.Dish
import com.github.mr04vv.kondatecalendar.data.LegacyDish
import com.github.mr04vv.kondatecalendar.data.LegacyMealSlot
import com.github.mr04vv.kondatecalendar.data.LegacyShoppingItem
import com.github.mr04vv.kondatecalendar.data.MealSlot
import com.github.mr04vv.kondatecalendar.data.ShoppingItem

/** What the local database moves to the server. */
data class MigrationData(val dishes: List<Dish>, val slots: List<MealSlot>, val shopping: List<ShoppingItem>) {
    fun isEmpty() = dishes.isEmpty() && slots.isEmpty() && shopping.isEmpty()
}

/**
 * Converts the local database's rows for the server. Ids are derived from the local ones so a retried move
 * overwrites what an interrupted one wrote. Presets the user never changed stay behind because the bundle has them,
 * and photos are dropped.
 */
fun migrationData(
    dishes: List<LegacyDish>,
    slots: List<LegacyMealSlot>,
    items: List<LegacyShoppingItem>,
    presets: List<Dish>,
): MigrationData {
    val presetsById = presets.associateBy { it.id }
    val converted = dishes.associate { legacy ->
        legacy.id to Dish(
            id = legacy.presetKey ?: localId(legacy.id),
            presetKey = legacy.presetKey,
            name = legacy.name,
            emoji = legacy.emoji,
            genre = legacy.genre,
            types = legacy.types,
            minutes = legacy.minutes,
            ingredients = legacy.ingredients,
            steps = legacy.steps,
            canCook = legacy.canCook,
        )
    }
    return MigrationData(
        dishes = converted.values.filter { it.presetKey == null || it != presetsById[it.presetKey] },
        slots = slots.mapNotNull { slot -> converted[slot.dishId]?.let { MealSlot(slot.date, slot.meal, it.id) } },
        shopping = items.map { item ->
            ShoppingItem(
                id = localId(item.id),
                name = item.name,
                amount = item.amount,
                checked = item.checked,
                date = item.date,
                meal = item.meal,
                dishId = item.dishId?.let { converted[it]?.id },
                dishName = item.dishName,
                // Local ids grow with each insert, so they keep the old order ahead of rows added later.
                createdAt = item.id,
            )
        },
    )
}

private fun localId(id: Long) = "$LOCAL_ID_PREFIX$id"

private const val LOCAL_ID_PREFIX = "local-"
