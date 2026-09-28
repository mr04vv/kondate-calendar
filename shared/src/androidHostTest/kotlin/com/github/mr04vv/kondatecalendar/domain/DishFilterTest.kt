package com.github.mr04vv.kondatecalendar.domain

import com.github.mr04vv.kondatecalendar.data.Dish
import com.github.mr04vv.kondatecalendar.data.DishType
import com.github.mr04vv.kondatecalendar.data.Genre
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DishFilterTest {

    private fun dish(
        id: Long,
        name: String,
        genre: Genre,
        vararg types: DishType,
        preset: Boolean = true,
        canCook: Boolean = false,
    ) = Dish(
        id = id,
        presetKey = if (preset) "k$id" else null,
        canCook = canCook,
        name = name,
        emoji = "🍲",
        genre = genre,
        types = types.toSet(),
        ingredients = emptyList(),
        steps = "",
    )

    private val nikujaga = dish(1, "肉じゃが", Genre.WASHOKU, DishType.MEAT, DishType.VEGETABLE, canCook = true)
    private val mabo = dish(2, "麻婆豆腐", Genre.CHUKA, DishType.MEAT)
    private val oyako = dish(3, "親子丼", Genre.WASHOKU, DishType.DON, DishType.MEAT, DishType.EGG)
    private val mine = dish(4, "うちの肉じゃが", Genre.WASHOKU, DishType.MEAT, preset = false, canCook = true)
    private val all = listOf(nikujaga, mabo, oyako, mine)

    @Test
    fun emptyFilterMatchesEverything() {
        assertTrue(all.all { DishFilter().matches(it) })
    }

    @Test
    fun queryIsAPartialNameMatch() {
        val filter = DishFilter(query = " じゃが ")
        assertEquals(listOf(nikujaga, mine), all.filter(filter::matches))
    }

    @Test
    fun genreNarrowsToOneGenre() {
        assertEquals(listOf(mabo), all.filter(DishFilter(genre = Genre.CHUKA)::matches))
    }

    @Test
    fun selectedTypesMustAllBePresent() {
        val filter = DishFilter(types = setOf(DishType.MEAT, DishType.EGG))
        assertEquals(listOf(oyako), all.filter(filter::matches))
        assertFalse(filter.matches(nikujaga))
    }

    @Test
    fun recommendPicksOnlyPresetsMatchingTheFilter() {
        val filter = DishFilter(genre = Genre.WASHOKU, types = setOf(DishType.MEAT))
        repeat(50) { seed ->
            val picked = recommend(all, filter, Random(seed))
            assertTrue(picked == nikujaga || picked == oyako)
        }
    }

    @Test
    fun recommendReturnsNullWhenNothingMatches() {
        assertNull(recommend(all, DishFilter(genre = Genre.ETHNIC), Random(0)))
    }

    @Test
    fun recommendAvoidsTheExcludedDishWhenPossible() {
        val filter = DishFilter(genre = Genre.WASHOKU)
        repeat(50) { seed ->
            assertNotEquals(nikujaga, recommend(all, filter, Random(seed), excludeId = nikujaga.id))
        }
    }

    @Test
    fun recommendKeepsTheOnlyCandidateEvenIfExcluded() {
        assertEquals(mabo, recommend(all, DishFilter(genre = Genre.CHUKA), Random(0), excludeId = mabo.id))
    }

    @Test
    fun canCookOnlyKeepsDishesMarkedAsCookable() {
        assertEquals(listOf(nikujaga, mine), all.filter(DishFilter(canCookOnly = true)::matches))
    }

    @Test
    fun canCookOnlyCombinesWithOtherConditions() {
        val filter = DishFilter(canCookOnly = true, query = "うちの")
        assertEquals(listOf(mine), all.filter(filter::matches))
    }

    @Test
    fun recommendIncludesOwnDishesWhenLimitedToCookable() {
        val picked = (0 until 200).mapNotNull { recommend(all, DishFilter(canCookOnly = true), Random(it)) }.toSet()
        assertEquals(setOf(nikujaga, mine), picked)
    }
}
