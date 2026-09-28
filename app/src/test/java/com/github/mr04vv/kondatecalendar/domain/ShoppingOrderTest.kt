package com.github.mr04vv.kondatecalendar.domain

import com.github.mr04vv.kondatecalendar.data.ShoppingItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ShoppingOrderTest {

    private fun item(id: Long, checked: Boolean = false) = ShoppingItem(id = id, name = "材料$id", amount = "", checked = checked)

    private fun ids(rows: List<ShoppingItem>) = rows.map { it.id }

    @Test
    fun withoutAPreviousOrderTheCurrentOrderIsKept() {
        val current = listOf(item(1), item(2), item(3, checked = true))
        assertEquals(listOf(1L, 2L, 3L), ids(keepShoppingOrder(emptyList(), current)))
    }

    @Test
    fun checkingARowDoesNotMoveIt() {
        // The DB now sorts the checked row 1 last, but it stays at the top.
        val current = listOf(item(2), item(3), item(1, checked = true))
        val result = keepShoppingOrder(listOf(1, 2, 3), current)
        assertEquals(listOf(1L, 2L, 3L), ids(result))
        assertEquals(true, result.first().checked)
    }

    @Test
    fun uncheckingARowDoesNotMoveIt() {
        val current = listOf(item(1), item(3), item(2, checked = true))
        assertEquals(listOf(1L, 3L, 2L), ids(keepShoppingOrder(listOf(1, 3, 2), current)))
    }

    @Test
    fun removedRowsDropOut() {
        val current = listOf(item(1), item(3))
        assertEquals(listOf(1L, 3L), ids(keepShoppingOrder(listOf(1, 2, 3), current)))
    }

    @Test
    fun aNewRowGoesRightAfterTheRowBeforeItInTheCurrentOrder() {
        // Shown: 1 (checked), 2, 3 (checked). The DB places new row 4 after 2, before the checked rows.
        val current = listOf(item(2), item(4), item(1, checked = true), item(3, checked = true))
        assertEquals(listOf(1L, 2L, 4L, 3L), ids(keepShoppingOrder(listOf(1, 2, 3), current)))
    }

    @Test
    fun aNewRowWithNothingBeforeItGoesFirst() {
        val current = listOf(item(4), item(1, checked = true))
        assertEquals(listOf(4L, 1L), ids(keepShoppingOrder(listOf(1), current)))
    }

    @Test
    fun severalNewRowsKeepTheirCurrentOrder() {
        val current = listOf(item(2), item(4), item(5), item(1, checked = true))
        assertEquals(listOf(1L, 2L, 4L, 5L), ids(keepShoppingOrder(listOf(1, 2), current)))
    }

    @Test
    fun rowsCarryTheirLatestContent() {
        val updated = ShoppingItem(id = 1, name = "卵", amount = "2個、1個")
        assertEquals(listOf(updated), keepShoppingOrder(listOf(1), listOf(updated)))
    }
}
