package com.github.mr04vv.kondatecalendar.domain

import com.github.mr04vv.kondatecalendar.data.Dish
import com.github.mr04vv.kondatecalendar.data.Genre
import com.github.mr04vv.kondatecalendar.data.Ingredient
import com.github.mr04vv.kondatecalendar.data.Meal
import com.github.mr04vv.kondatecalendar.data.ShoppingItem
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.junit.Assert.assertEquals
import org.junit.Test

class ShoppingListTest {
    private val day = LocalDate(2026, 10, 7)
    private val nikujaga = dish(1, "肉じゃが", Ingredient("じゃがいも", "3個"), Ingredient("玉ねぎ", "1個"))
    private val curry = dish(2, "カレー", Ingredient("玉ねぎ", "1/2個"))

    private fun dish(id: Long, name: String, vararg ingredients: Ingredient) =
        Dish(
            id = id,
            name = name,
            emoji = "🍲",
            genre = Genre.WASHOKU,
            types = emptySet(),
            ingredients = ingredients.toList(),
            steps = "",
        )

    private fun slotRow(id: Long, dish: Dish, name: String, amount: String, date: LocalDate = day, checked: Boolean = false) =
        ShoppingItem(
            id = id,
            name = name,
            amount = amount,
            checked = checked,
            date = date,
            meal = Meal.DINNER,
            dishId = dish.id,
            dishName = dish.name,
        )

    // rowsToAdd

    @Test
    fun aSlotAddsOneRowPerIngredientRecordingWhereItCameFrom() {
        val result = shoppingRowsToAdd(emptyList(), listOf(ShoppingSource.Slot(day, Meal.DINNER, nikujaga)), today = day)
        assertEquals(
            listOf(
                ShoppingItem(name = "じゃがいも", amount = "3個", date = day, meal = Meal.DINNER, dishId = 1, dishName = "肉じゃが"),
                ShoppingItem(name = "玉ねぎ", amount = "1個", date = day, meal = Meal.DINNER, dishId = 1, dishName = "肉じゃが"),
            ),
            result,
        )
    }

    @Test
    fun aRecipeAddsRowsWithoutASlot() {
        val result = shoppingRowsToAdd(emptyList(), listOf(ShoppingSource.Recipe(curry)), today = day)
        assertEquals(listOf(ShoppingItem(name = "玉ねぎ", amount = "1/2個", dishId = 2, dishName = "カレー")), result)
    }

    @Test
    fun namesAndAmountsAreTrimmed() {
        val result = shoppingRowsToAdd(emptyList(), listOf(ShoppingSource.Recipe(dish(3, "卵焼き", Ingredient(" 卵 ", " 2個 ")))), today = day)
        assertEquals(listOf(ShoppingItem(name = "卵", amount = "2個", dishId = 3, dishName = "卵焼き")), result)
    }

    @Test
    fun aSlotAlreadyInTheListIsNotAddedAgain() {
        val existing = listOf(slotRow(10, nikujaga, "じゃがいも", "3個"), slotRow(11, nikujaga, "玉ねぎ", "1個"))
        val result = shoppingRowsToAdd(
            existing,
            listOf(ShoppingSource.Slot(day, Meal.DINNER, nikujaga), ShoppingSource.Slot(day, Meal.LUNCH, curry)),
            today = day,
        )
        assertEquals(
            listOf(ShoppingItem(name = "玉ねぎ", amount = "1/2個", date = day, meal = Meal.LUNCH, dishId = 2, dishName = "カレー")),
            result,
        )
    }

    @Test
    fun aSlotWhoseRowsAreCheckedCountsAsAlreadyAdded() {
        val existing = listOf(slotRow(10, nikujaga, "じゃがいも", "3個", checked = true))
        assertEquals(emptyList<ShoppingItem>(), shoppingRowsToAdd(existing, listOf(ShoppingSource.Slot(day, Meal.DINNER, nikujaga)), today = day))
    }

    @Test
    fun theSameSlotWithAnotherDishIsAdded() {
        val existing = listOf(slotRow(10, nikujaga, "じゃがいも", "3個", checked = true))
        val result = shoppingRowsToAdd(existing, listOf(ShoppingSource.Slot(day, Meal.DINNER, curry)), today = day)
        assertEquals(1, result.size)
    }

    @Test
    fun aRecipeAlreadyInTheListIsNotAddedAgainButItsSlotRowsDoNotCount() {
        val fromRecipe = ShoppingItem(id = 5, name = "玉ねぎ", amount = "1/2個", dishId = 2, dishName = "カレー")
        assertEquals(emptyList<ShoppingItem>(), shoppingRowsToAdd(listOf(fromRecipe), listOf(ShoppingSource.Recipe(curry)), today = day))
        val fromSlot = slotRow(6, curry, "玉ねぎ", "1/2個")
        assertEquals(1, shoppingRowsToAdd(listOf(fromSlot), listOf(ShoppingSource.Recipe(curry)), today = day).size)
    }

    @Test
    fun manualRowsNeverBlockAnAdd() {
        val manual = ShoppingItem(id = 1, name = "玉ねぎ", amount = "1個")
        assertEquals(1, shoppingRowsToAdd(listOf(manual), listOf(ShoppingSource.Recipe(curry)), today = day).size)
    }

    @Test
    fun theSameSourceTwiceInOneCallIsAddedOnce() {
        val source = ShoppingSource.Recipe(curry)
        assertEquals(1, shoppingRowsToAdd(emptyList(), listOf(source, source), today = day).size)
    }

    @Test
    fun slotsOnDaysThatHavePassedAreNotAdded() {
        val yesterday = day.minus(1, DateTimeUnit.DAY)
        val result = shoppingRowsToAdd(
            emptyList(),
            listOf(ShoppingSource.Slot(yesterday, Meal.DINNER, curry), ShoppingSource.Slot(day, Meal.DINNER, curry)),
            today = day,
        )
        assertEquals(listOf(day), result.map { it.date })
    }

    // expired

    @Test
    fun lastWeeksOpenSlotRowsExpire() {
        val lastWeek = slotRow(1, nikujaga, "じゃがいも", "3個", date = day.minus(7, DateTimeUnit.DAY))
        val today = slotRow(2, curry, "玉ねぎ", "1/2個")
        assertEquals(listOf(lastWeek), expiredShoppingRows(listOf(lastWeek, today), today = day))
    }

    @Test
    fun checkedManualAndRecipeRowsDoNotExpire() {
        val rows = listOf(
            slotRow(1, nikujaga, "じゃがいも", "3個", date = day.minus(1, DateTimeUnit.DAY), checked = true),
            ShoppingItem(id = 2, name = "牛乳", amount = "1本"),
            ShoppingItem(id = 3, name = "玉ねぎ", amount = "1/2個", dishId = 2, dishName = "カレー"),
        )
        assertEquals(emptyList<ShoppingItem>(), expiredShoppingRows(rows, today = day))
    }

    // group

    @Test
    fun rowsWithTheSameNameAreShownAsOneLineWithAmountsJoined() {
        val rows = listOf(
            slotRow(1, nikujaga, "玉ねぎ", "1個"),
            slotRow(2, nikujaga, "じゃがいも", "3個"),
            slotRow(3, curry, "玉ねぎ", "1/2個", date = day.plus(2, DateTimeUnit.DAY)),
        )
        val groups = groupShoppingList(rows)
        assertEquals(listOf("玉ねぎ", "じゃがいも"), groups.map { it.name })
        assertEquals("1個、1/2個", groups[0].amount)
        assertEquals(listOf(rows[0], rows[2]), groups[0].items)
    }

    @Test
    fun matchingIsExactOnly() {
        val rows = listOf(ShoppingItem(id = 1, name = "じゃがいも", amount = "3個"), ShoppingItem(id = 2, name = "ジャガイモ", amount = "1個"))
        assertEquals(2, groupShoppingList(rows).size)
    }

    @Test
    fun blankAmountsAreNotJoined() {
        val rows = listOf(
            ShoppingItem(id = 1, name = "塩", amount = ""),
            ShoppingItem(id = 2, name = "塩", amount = "少々"),
            ShoppingItem(id = 3, name = "塩", amount = ""),
        )
        assertEquals("少々", groupShoppingList(rows).single().amount)
    }

    @Test
    fun checkedAndUncheckedRowsAreSeparateLines() {
        val rows = listOf(
            ShoppingItem(id = 1, name = "牛乳", amount = "200ml"),
            ShoppingItem(id = 2, name = "牛乳", amount = "1本", checked = true),
        )
        assertEquals(listOf(false to "200ml", true to "1本"), groupShoppingList(rows).map { it.checked to it.amount })
    }

    @Test
    fun sourcesAreListedOncePerSlotOrRecipeAndManualRowsHaveNone() {
        val rows = listOf(
            slotRow(1, nikujaga, "玉ねぎ", "1個"),
            slotRow(2, nikujaga, "玉ねぎ", "少々"),
            ShoppingItem(id = 3, name = "玉ねぎ", amount = "1/2個", dishId = 2, dishName = "カレー"),
            ShoppingItem(id = 4, name = "玉ねぎ", amount = "1個"),
        )
        assertEquals(listOf("肉じゃが（10/7 夜）", "カレー（レシピから）"), groupShoppingList(rows).single().sources)
    }

    @Test
    fun sharedTextListsOpenItemsWithAmountsThenCheckedOnes() {
        val rows = listOf(
            ShoppingItem(id = 1, name = "しょうゆ", amount = "大さじ2", checked = true),
            ShoppingItem(id = 2, name = "玉ねぎ", amount = "1個"),
            ShoppingItem(id = 3, name = "玉ねぎ", amount = "1/2個"),
            ShoppingItem(id = 4, name = "牛乳", amount = ""),
        )
        assertEquals(
            "買い物リスト\n・玉ねぎ 1個、1/2個\n・牛乳\n・しょうゆ 大さじ2（済）",
            shoppingListText(groupShoppingList(rows)),
        )
    }
}
