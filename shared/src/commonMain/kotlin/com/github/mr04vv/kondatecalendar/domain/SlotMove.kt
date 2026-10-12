package com.github.mr04vv.kondatecalendar.domain

import com.github.mr04vv.kondatecalendar.data.Meal
import com.github.mr04vv.kondatecalendar.data.MealSlot
import com.github.mr04vv.kondatecalendar.data.ShoppingItem
import kotlinx.datetime.LocalDate

/** What dropping one planned slot onto another writes: [slots] to save, a slot left [vacated], and moved rows. */
data class SlotMove(val slots: List<MealSlot>, val vacated: MealSlot?, val items: List<ShoppingItem>)

/**
 * Drops the dish planned at [from] onto the slot at [toDate] [toMeal], whose dish [toDishId] goes the other way; when
 * that slot is empty the dish just moves and [from] is left empty. Shopping rows each dish added from its old slot,
 * checked or not, follow it, so clearing the new slot or adding it again still finds them. Rows left by an earlier
 * dish in either slot stay where they are.
 */
fun slotMove(from: MealSlot, toDate: LocalDate, toMeal: Meal, toDishId: Long?, items: List<ShoppingItem>): SlotMove {
    if (from.date == toDate && from.meal == toMeal) return SlotMove(emptyList(), null, emptyList())
    val to = toDishId?.let { MealSlot(toDate, toMeal, it) }
    val moved = items.mapNotNull { item ->
        when {
            item.cameFrom(from) -> item.copy(date = toDate, meal = toMeal)
            to != null && item.cameFrom(to) -> item.copy(date = from.date, meal = from.meal)
            else -> null
        }
    }
    return SlotMove(
        slots = listOfNotNull(MealSlot(toDate, toMeal, from.dishId), to?.copy(date = from.date, meal = from.meal)),
        vacated = from.takeIf { to == null },
        items = moved,
    )
}

private fun ShoppingItem.cameFrom(slot: MealSlot): Boolean =
    date == slot.date && meal == slot.meal && dishId == slot.dishId
