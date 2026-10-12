package com.github.mr04vv.kondatecalendar.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

enum class Genre(val label: String) {
    WASHOKU("和食"),
    CHUKA("中華"),
    YOSHOKU("洋食"),
    KANKOKU("韓国"),
    ETHNIC("エスニック"),
    OTHER("その他"),
}

enum class DishType(val label: String) {
    DON("丼"),
    NOODLE("麺"),
    MEAT("肉"),
    FISH("魚"),
    EGG("卵"),
    VEGETABLE("野菜"),
    SOUP("汁物"),
    BREAD("パン"),
    RICE("ご飯もの"),
}

enum class Meal(val label: String) {
    BREAKFAST("朝"),
    LUNCH("昼"),
    DINNER("夜"),
}

@Serializable
data class Ingredient(val name: String, val amount: String)

/**
 * A dish. Bundled presets have [id] equal to [presetKey] and are stored only once the user edits them;
 * a dish the user created has a generated [id] and a null [presetKey].
 */
@Serializable
data class Dish(
    val id: String = "",
    val presetKey: String? = null,
    val name: String,
    val emoji: String,
    val genre: Genre,
    val types: Set<DishType>,
    val minutes: Int? = null,
    val ingredients: List<Ingredient>,
    /** One step per line. */
    val steps: String,
    /** The user marked this as a dish they can cook. */
    val canCook: Boolean = false,
)

/** One dish planned for a date and meal; an empty slot has no record. */
@Serializable
data class MealSlot(
    val date: LocalDate,
    val meal: Meal,
    val dishId: String,
)

/**
 * One ingredient from one source. [dishId] is null for a row typed in by hand; [date] and [meal] are null unless the
 * row came from a planned slot, so clearing that slot can take its unchecked rows back out.
 */
@Serializable
data class ShoppingItem(
    val id: String = "",
    val name: String,
    val amount: String,
    val checked: Boolean = false,
    val date: LocalDate? = null,
    val meal: Meal? = null,
    val dishId: String? = null,
    /** The dish name at the time the row was added, shown as its source. */
    val dishName: String? = null,
    /** Epoch milliseconds when the row was added; the list shows rows in this order. */
    val createdAt: Long = 0,
)
