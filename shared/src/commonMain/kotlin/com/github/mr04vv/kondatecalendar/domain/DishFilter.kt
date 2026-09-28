package com.github.mr04vv.kondatecalendar.domain

import com.github.mr04vv.kondatecalendar.data.Dish
import com.github.mr04vv.kondatecalendar.data.DishType
import com.github.mr04vv.kondatecalendar.data.Genre
import kotlin.random.Random

data class DishFilter(
    val query: String = "",
    val genre: Genre? = null,
    val types: Set<DishType> = emptySet(),
    val canCookOnly: Boolean = false,
) {
    fun matches(dish: Dish): Boolean =
        (query.isBlank() || dish.name.contains(query.trim(), ignoreCase = true)) &&
            (genre == null || dish.genre == genre) &&
            dish.types.containsAll(types) &&
            (!canCookOnly || dish.canCook)
}

/**
 * A random dish matching [filter], avoiding [excludeId] when another candidate exists.
 * Draws from presets, or from every cookable dish (own dishes included) when [DishFilter.canCookOnly] is set.
 */
fun recommend(dishes: List<Dish>, filter: DishFilter, random: Random, excludeId: Long? = null): Dish? {
    val candidates = dishes.filter { (filter.canCookOnly || it.presetKey != null) && filter.matches(it) }
    val fresh = candidates.filter { it.id != excludeId }
    return fresh.ifEmpty { candidates }.randomOrNull(random)
}
