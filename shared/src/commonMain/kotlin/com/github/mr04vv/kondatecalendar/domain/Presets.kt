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

/** Presets not yet in the database, so seeding on every launch never duplicates. */
fun presetsToInsert(presets: List<PresetDish>, existingKeys: Set<String>): List<PresetDish> =
    presets.filter { it.key !in existingKeys }
