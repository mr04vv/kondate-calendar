package com.github.mr04vv.kondatecalendar.domain

import com.github.mr04vv.kondatecalendar.data.ShoppingItem

/**
 * Returns [current] arranged so rows keep the positions they had in [previous] (a list of ids),
 * even when checking a row moves it in [current]'s order. Rows missing from [current] drop out.
 * A new row goes right after the row that precedes it in [current], or first if nothing does.
 */
fun keepShoppingOrder(previous: List<Long>, current: List<ShoppingItem>): List<ShoppingItem> {
    val byId = current.associateBy { it.id }
    val order = previous.filterTo(ArrayList()) { it in byId }
    val placed = order.toHashSet()
    current.forEachIndexed { index, item ->
        if (placed.add(item.id)) {
            val at = if (index == 0) 0 else order.indexOf(current[index - 1].id) + 1
            order.add(at, item.id)
        }
    }
    return order.map { byId.getValue(it) }
}
