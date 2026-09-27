package com.github.mr04vv.kondatecalendar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarMathTest {

    @Test
    fun weekStartIsMondayOfTheSameWeek() {
        assertEquals(LocalDate.of(2026, 10, 5), weekStart(LocalDate.of(2026, 10, 7)))
        assertEquals(LocalDate.of(2026, 10, 5), weekStart(LocalDate.of(2026, 10, 5)))
        assertEquals(LocalDate.of(2026, 10, 5), weekStart(LocalDate.of(2026, 10, 11)))
    }

    @Test
    fun weekOfReturnsMondayToSunday() {
        val week = weekOf(LocalDate.of(2026, 10, 1))
        assertEquals(7, week.size)
        assertEquals(LocalDate.of(2026, 9, 28), week.first())
        assertEquals(LocalDate.of(2026, 10, 4), week.last())
    }

    @Test
    fun monthGridWithFiveRows() {
        // October 2026 starts on Thursday and has 31 days.
        val grid = monthGrid(YearMonth.of(2026, 10))
        assertEquals(35, grid.size)
        assertEquals(LocalDate.of(2026, 9, 28), grid.first())
        assertEquals(LocalDate.of(2026, 11, 1), grid.last())
    }

    @Test
    fun monthGridWithSixRows() {
        // August 2026 starts on Saturday and has 31 days.
        val grid = monthGrid(YearMonth.of(2026, 8))
        assertEquals(42, grid.size)
        assertEquals(LocalDate.of(2026, 7, 27), grid.first())
        assertEquals(LocalDate.of(2026, 9, 6), grid.last())
    }

    @Test
    fun monthGridWithFourRows() {
        // February 2027 starts on Monday and has 28 days.
        val grid = monthGrid(YearMonth.of(2027, 2))
        assertEquals(28, grid.size)
        assertEquals(LocalDate.of(2027, 2, 1), grid.first())
        assertEquals(LocalDate.of(2027, 2, 28), grid.last())
    }

    @Test
    fun monthGridIsConsecutiveAndStartsOnMonday() {
        val grid = monthGrid(YearMonth.of(2026, 12))
        assertEquals(DayOfWeek.MONDAY, grid.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, grid.last().dayOfWeek)
        grid.zipWithNext().forEach { (a, b) -> assertEquals(a.plusDays(1), b) }
    }

    @Test
    fun listRowsPutAHeaderBeforeEveryWeek() {
        val rows = listRows(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 20))
        // Weeks of 10/5, 10/12 and 10/19.
        assertEquals(3 * 8, rows.size)
        assertEquals(ListRow.WeekHeader(LocalDate.of(2026, 10, 5)), rows[0])
        assertEquals(ListRow.Day(LocalDate.of(2026, 10, 5)), rows[1])
        assertEquals(ListRow.Day(LocalDate.of(2026, 10, 11)), rows[7])
        assertEquals(ListRow.WeekHeader(LocalDate.of(2026, 10, 12)), rows[8])
        assertEquals(ListRow.Day(LocalDate.of(2026, 10, 25)), rows.last())
    }

    @Test
    fun listRowsCrossMonthBoundaries() {
        val rows = listRows(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 10, 1))
        val days = rows.filterIsInstance<ListRow.Day>().map { it.date }
        assertEquals(weekOf(LocalDate.of(2026, 10, 1)), days)
        assertTrue(rows.first() is ListRow.WeekHeader)
    }
}
