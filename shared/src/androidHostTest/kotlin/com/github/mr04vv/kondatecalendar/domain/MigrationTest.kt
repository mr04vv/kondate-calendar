package com.github.mr04vv.kondatecalendar.domain

import com.github.mr04vv.kondatecalendar.data.Dish
import com.github.mr04vv.kondatecalendar.data.Genre
import com.github.mr04vv.kondatecalendar.data.Ingredient
import com.github.mr04vv.kondatecalendar.data.LegacyDish
import com.github.mr04vv.kondatecalendar.data.LegacyMealSlot
import com.github.mr04vv.kondatecalendar.data.LegacyShoppingItem
import com.github.mr04vv.kondatecalendar.data.Meal
import com.github.mr04vv.kondatecalendar.data.MealSlot
import com.github.mr04vv.kondatecalendar.data.ShoppingItem
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationTest {
    private val day = LocalDate(2026, 10, 7)
    private val karaagePreset = Dish(
        id = "karaage",
        presetKey = "karaage",
        name = "唐揚げ",
        emoji = "🍗",
        genre = Genre.WASHOKU,
        types = emptySet(),
        minutes = 30,
        ingredients = listOf(Ingredient("鶏もも肉", "300g")),
        steps = "漬ける\n揚げる",
    )
    private val untouchedPreset = LegacyDish(
        id = 1,
        presetKey = "karaage",
        name = "唐揚げ",
        emoji = "🍗",
        genre = Genre.WASHOKU,
        types = emptySet(),
        minutes = 30,
        ingredients = listOf(Ingredient("鶏もも肉", "300g")),
        steps = "漬ける\n揚げる",
        photoPath = null,
    )
    private val ownDish = LegacyDish(
        id = 7,
        name = "自作の煮物",
        emoji = "🍲",
        genre = Genre.WASHOKU,
        types = emptySet(),
        ingredients = listOf(Ingredient("大根", "1/2本")),
        steps = "煮る",
        photoPath = "/data/photos/a.jpg",
        canCook = true,
    )

    private fun migrate(
        dishes: List<LegacyDish> = listOf(untouchedPreset, ownDish),
        slots: List<LegacyMealSlot> = emptyList(),
        items: List<LegacyShoppingItem> = emptyList(),
    ) = migrationData(dishes, slots, items, presets = listOf(karaagePreset))

    @Test
    fun untouchedPresetsStayBehindBecauseTheBundleAlreadyHasThem() {
        assertEquals(listOf("local-7"), migrate().dishes.map { it.id })
    }

    @Test
    fun anEditedPresetMovesUnderItsPresetKey() {
        val edited = untouchedPreset.copy(canCook = true)
        assertEquals(listOf(karaagePreset.copy(canCook = true)), migrate(dishes = listOf(edited)).dishes)
    }

    @Test
    fun anOwnDishMovesWithAStableIdAndWithoutItsPhoto() {
        assertEquals(
            Dish(
                id = "local-7",
                name = "自作の煮物",
                emoji = "🍲",
                genre = Genre.WASHOKU,
                types = emptySet(),
                ingredients = listOf(Ingredient("大根", "1/2本")),
                steps = "煮る",
                canCook = true,
            ),
            migrate().dishes.single(),
        )
    }

    @Test
    fun slotsPointAtTheMovedDishes() {
        val slots = listOf(LegacyMealSlot(day, Meal.DINNER, dishId = 1), LegacyMealSlot(day, Meal.LUNCH, dishId = 7))
        assertEquals(
            listOf(MealSlot(day, Meal.DINNER, dishId = "karaage"), MealSlot(day, Meal.LUNCH, dishId = "local-7")),
            migrate(slots = slots).slots,
        )
    }

    @Test
    fun aSlotWhoseDishIsGoneIsDropped() {
        assertTrue(migrate(slots = listOf(LegacyMealSlot(day, Meal.DINNER, dishId = 99))).slots.isEmpty())
    }

    @Test
    fun shoppingRowsKeepTheirOrderAndSource() {
        val items = listOf(
            LegacyShoppingItem(id = 3, name = "大根", amount = "1/2本", date = day, meal = Meal.LUNCH, dishId = 7, dishName = "自作の煮物"),
            LegacyShoppingItem(id = 4, name = "牛乳", amount = "", checked = true),
        )
        assertEquals(
            listOf(
                ShoppingItem(
                    id = "local-3",
                    name = "大根",
                    amount = "1/2本",
                    date = day,
                    meal = Meal.LUNCH,
                    dishId = "local-7",
                    dishName = "自作の煮物",
                    createdAt = 3,
                ),
                ShoppingItem(id = "local-4", name = "牛乳", amount = "", checked = true, createdAt = 4),
            ),
            migrate(items = items).shopping,
        )
    }

    @Test
    fun aShoppingRowWhoseDishIsGoneKeepsItsNameOnly() {
        val item = LegacyShoppingItem(id = 3, name = "大根", amount = "", dishId = 99, dishName = "消えた料理")
        assertEquals(null, migrate(items = listOf(item)).shopping.single().dishId)
    }

    @Test
    fun onlyUntouchedPresetsMeansThereIsNothingToMove() {
        assertTrue(migrate(dishes = listOf(untouchedPreset)).isEmpty())
        assertTrue(!migrate().isEmpty())
    }
}
