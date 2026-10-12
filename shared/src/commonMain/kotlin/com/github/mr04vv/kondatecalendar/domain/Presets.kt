package com.github.mr04vv.kondatecalendar.domain

import com.github.mr04vv.kondatecalendar.data.Dish
import com.github.mr04vv.kondatecalendar.data.DishType
import com.github.mr04vv.kondatecalendar.data.Genre
import com.github.mr04vv.kondatecalendar.data.Ingredient
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PresetDish(
    val key: String,
    val name: String,
    val emoji: String,
    val genre: Genre,
    val types: List<DishType>,
    val minutes: Int,
    val ingredients: List<Ingredient>,
    val steps: List<String>,
) {
    fun toDish() = Dish(
        id = key,
        presetKey = key,
        name = name,
        emoji = emoji,
        genre = genre,
        types = types.toSet(),
        minutes = minutes,
        ingredients = ingredients,
        steps = steps.joinToString("\n"),
    )
}

fun parsePresets(json: String): List<PresetDish> = Json.decodeFromString(json)

/**
 * Every dish to show, by name: the bundled [presets] with the user's [stored] edits laid over them, plus the dishes
 * the user created. An edited preset stays even after it leaves the bundle.
 */
fun mergeDishes(presets: List<Dish>, stored: List<Dish>): List<Dish> {
    val storedById = stored.associateBy { it.id }
    val presetIds = presets.mapTo(HashSet()) { it.id }
    return (presets.map { storedById[it.id] ?: it } + stored.filter { it.id !in presetIds }).sortedBy { it.name }
}
