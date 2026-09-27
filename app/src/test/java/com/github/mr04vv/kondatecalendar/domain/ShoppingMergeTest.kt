package com.github.mr04vv.kondatecalendar.domain

import com.github.mr04vv.kondatecalendar.data.Ingredient
import com.github.mr04vv.kondatecalendar.data.ShoppingItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ShoppingMergeTest {

    @Test
    fun newIngredientsBecomeNewRows() {
        val result = mergeIntoShoppingList(
            existing = emptyList(),
            adding = listOf(Ingredient("じゃがいも", "3個"), Ingredient("玉ねぎ", "1個")),
        )
        assertEquals(
            listOf(ShoppingItem(name = "じゃがいも", amount = "3個"), ShoppingItem(name = "玉ねぎ", amount = "1個")),
            result,
        )
    }

    @Test
    fun sameNameJoinsTheExistingRowAndConcatenatesAmounts() {
        val existing = listOf(ShoppingItem(id = 7, name = "じゃがいも", amount = "3個"))
        val result = mergeIntoShoppingList(existing, listOf(Ingredient("じゃがいも", "200g")))
        assertEquals(listOf(ShoppingItem(id = 7, name = "じゃがいも", amount = "3個、200g")), result)
    }

    @Test
    fun duplicatesInsideOneBatchAreMergedToo() {
        val result = mergeIntoShoppingList(
            existing = emptyList(),
            adding = listOf(Ingredient("卵", "2個"), Ingredient("卵", "1個"), Ingredient("卵", "3個")),
        )
        assertEquals(listOf(ShoppingItem(name = "卵", amount = "2個、1個、3個")), result)
    }

    @Test
    fun matchingIsExactOnly() {
        val existing = listOf(ShoppingItem(id = 1, name = "じゃがいも", amount = "3個"))
        val result = mergeIntoShoppingList(existing, listOf(Ingredient("ジャガイモ", "1個")))
        assertEquals(listOf(ShoppingItem(name = "ジャガイモ", amount = "1個")), result)
    }

    @Test
    fun surroundingWhitespaceIsIgnoredForMatching() {
        val existing = listOf(ShoppingItem(id = 1, name = "卵", amount = "2個"))
        val result = mergeIntoShoppingList(existing, listOf(Ingredient(" 卵 ", " 1個 ")))
        assertEquals(listOf(ShoppingItem(id = 1, name = "卵", amount = "2個、1個")), result)
    }

    @Test
    fun blankAmountsAreNotConcatenated() {
        val existing = listOf(ShoppingItem(id = 1, name = "塩", amount = ""))
        val result = mergeIntoShoppingList(existing, listOf(Ingredient("塩", "少々"), Ingredient("塩", "")))
        assertEquals(listOf(ShoppingItem(id = 1, name = "塩", amount = "少々")), result)
    }

    @Test
    fun checkedRowsAreNotMergedInto() {
        val existing = listOf(ShoppingItem(id = 1, name = "牛乳", amount = "1本", checked = true))
        val result = mergeIntoShoppingList(existing, listOf(Ingredient("牛乳", "200ml")))
        assertEquals(listOf(ShoppingItem(name = "牛乳", amount = "200ml")), result)
    }

    @Test
    fun untouchedRowsAreNotReturned() {
        val existing = listOf(
            ShoppingItem(id = 1, name = "牛乳", amount = "1本"),
            ShoppingItem(id = 2, name = "卵", amount = "2個"),
        )
        val result = mergeIntoShoppingList(existing, listOf(Ingredient("卵", "1個")))
        assertEquals(listOf(ShoppingItem(id = 2, name = "卵", amount = "2個、1個")), result)
    }
}
