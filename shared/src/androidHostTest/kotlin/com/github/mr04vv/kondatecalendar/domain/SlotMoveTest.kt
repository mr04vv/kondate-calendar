package com.github.mr04vv.kondatecalendar.domain

import com.github.mr04vv.kondatecalendar.data.Meal
import com.github.mr04vv.kondatecalendar.data.MealSlot
import com.github.mr04vv.kondatecalendar.data.ShoppingItem
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SlotMoveTest {
    private val monday = LocalDate(2026, 10, 12)
    private val tuesday = LocalDate(2026, 10, 13)
    private val nikujaga = MealSlot(monday, Meal.DINNER, dishId = 1)

    private fun row(id: Long, dishId: Long?, date: LocalDate? = null, meal: Meal? = null, checked: Boolean = false) =
        ShoppingItem(id = id, name = "玉ねぎ", amount = "1個", checked = checked, date = date, meal = meal, dishId = dishId)

    @Test
    fun droppingOnAnEmptySlotMovesTheDishAndEmptiesWhereItWas() {
        val result = slotMove(nikujaga, toDate = tuesday, toMeal = Meal.LUNCH, toDishId = null, items = emptyList())
        assertEquals(listOf(MealSlot(tuesday, Meal.LUNCH, 1)), result.slots)
        assertEquals(nikujaga, result.vacated)
    }

    @Test
    fun droppingOnAPlannedSlotSwapsTheTwoDishes() {
        val result = slotMove(nikujaga, toDate = tuesday, toMeal = Meal.DINNER, toDishId = 2, items = emptyList())
        assertEquals(listOf(MealSlot(tuesday, Meal.DINNER, 1), MealSlot(monday, Meal.DINNER, 2)), result.slots)
        assertEquals(null, result.vacated)
    }

    @Test
    fun droppingOnTheSameSlotChangesNothing() {
        val items = listOf(row(10, dishId = 1, date = monday, meal = Meal.DINNER))
        assertEquals(
            SlotMove(emptyList(), null, emptyList()),
            slotMove(nikujaga, toDate = monday, toMeal = Meal.DINNER, toDishId = 1, items = items),
        )
    }

    @Test
    fun rowsEachDishAddedFromItsSlotFollowItCheckedOrNot() {
        val items = listOf(
            row(10, dishId = 1, date = monday, meal = Meal.DINNER),
            row(11, dishId = 1, date = monday, meal = Meal.DINNER, checked = true),
            row(12, dishId = 2, date = tuesday, meal = Meal.DINNER),
        )
        val result = slotMove(nikujaga, toDate = tuesday, toMeal = Meal.DINNER, toDishId = 2, items = items)
        assertEquals(
            listOf(
                row(10, dishId = 1, date = tuesday, meal = Meal.DINNER),
                row(11, dishId = 1, date = tuesday, meal = Meal.DINNER, checked = true),
                row(12, dishId = 2, date = monday, meal = Meal.DINNER),
            ),
            result.items,
        )
    }

    @Test
    fun rowsNotAddedFromEitherSlotsCurrentDishStay() {
        val items = listOf(
            row(10, dishId = null),
            row(11, dishId = 1),
            row(12, dishId = 1, date = monday, meal = Meal.LUNCH),
            row(13, dishId = 3, date = monday, meal = Meal.DINNER, checked = true),
            row(14, dishId = 3, date = tuesday, meal = Meal.DINNER, checked = true),
        )
        val result = slotMove(nikujaga, toDate = tuesday, toMeal = Meal.DINNER, toDishId = 2, items = items)
        assertEquals(emptyList<ShoppingItem>(), result.items)
    }
}
