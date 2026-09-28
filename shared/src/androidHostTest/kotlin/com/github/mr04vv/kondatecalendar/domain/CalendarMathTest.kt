package com.github.mr04vv.kondatecalendar.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.plus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarMathTest {

    @Test
    fun weekStartIsMondayOfTheSameWeek() {
        assertEquals(LocalDate(2026, 10, 5), weekStart(LocalDate(2026, 10, 7)))
        assertEquals(LocalDate(2026, 10, 5), weekStart(LocalDate(2026, 10, 5)))
        assertEquals(LocalDate(2026, 10, 5), weekStart(LocalDate(2026, 10, 11)))
    }

    @Test
    fun weekOfReturnsMondayToSunday() {
        val week = weekOf(LocalDate(2026, 10, 1))
        assertEquals(7, week.size)
        assertEquals(LocalDate(2026, 9, 28), week.first())
        assertEquals(LocalDate(2026, 10, 4), week.last())
    }

    @Test
    fun monthGridWithFiveRows() {
        // October 2026 starts on Thursday and has 31 days.
        val grid = monthGrid(YearMonth(2026, 10))
        assertEquals(35, grid.size)
        assertEquals(LocalDate(2026, 9, 28), grid.first())
        assertEquals(LocalDate(2026, 11, 1), grid.last())
    }

    @Test
    fun monthGridWithSixRows() {
        // August 2026 starts on Saturday and has 31 days.
        val grid = monthGrid(YearMonth(2026, 8))
        assertEquals(42, grid.size)
        assertEquals(LocalDate(2026, 7, 27), grid.first())
        assertEquals(LocalDate(2026, 9, 6), grid.last())
    }

    @Test
    fun monthGridWithFourRows() {
        // February 2027 starts on Monday and has 28 days.
        val grid = monthGrid(YearMonth(2027, 2))
        assertEquals(28, grid.size)
        assertEquals(LocalDate(2027, 2, 1), grid.first())
        assertEquals(LocalDate(2027, 2, 28), grid.last())
    }

    @Test
    fun monthGridIsConsecutiveAndStartsOnMonday() {
        val grid = monthGrid(YearMonth(2026, 12))
        assertEquals(DayOfWeek.MONDAY, grid.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, grid.last().dayOfWeek)
        grid.zipWithNext().forEach { (a, b) -> assertEquals(a.plus(1, DateTimeUnit.DAY), b) }
    }

    @Test
    fun listRowsPutAHeaderBeforeEveryWeek() {
        val rows = listRows(LocalDate(2026, 10, 7), LocalDate(2026, 10, 20))
        // Weeks of 10/5, 10/12 and 10/19.
        assertEquals(3 * 8, rows.size)
        assertEquals(ListRow.WeekHeader(LocalDate(2026, 10, 5)), rows[0])
        assertEquals(ListRow.Day(LocalDate(2026, 10, 5)), rows[1])
        assertEquals(ListRow.Day(LocalDate(2026, 10, 11)), rows[7])
        assertEquals(ListRow.WeekHeader(LocalDate(2026, 10, 12)), rows[8])
        assertEquals(ListRow.Day(LocalDate(2026, 10, 25)), rows.last())
    }

    @Test
    fun listRowsCrossMonthBoundaries() {
        val rows = listRows(LocalDate(2026, 9, 30), LocalDate(2026, 10, 1))
        val days = rows.filterIsInstance<ListRow.Day>().map { it.date }
        assertEquals(weekOf(LocalDate(2026, 10, 1)), days)
        assertTrue(rows.first() is ListRow.WeekHeader)
    }

    @Test
    fun monthPageIsConsecutiveAcrossYears() {
        assertEquals(monthPage(YearMonth(2026, 12)) + 1, monthPage(YearMonth(2027, 1)))
        assertEquals(monthPage(YearMonth(2026, 1)) - 1, monthPage(YearMonth(2025, 12)))
    }

    @Test
    fun monthOfPageRoundTrips() {
        listOf(YearMonth(2026, 1), YearMonth(2026, 10), YearMonth(2026, 12), YearMonth(1, 1), YearMonth(9999, 12))
            .forEach { assertEquals(it, monthOfPage(monthPage(it))) }
    }

    @Test
    fun monthPagesCoverYearOneToYear9999() {
        assertEquals(0, monthPage(YearMonth(1, 1)))
        assertEquals(MONTH_PAGE_COUNT - 1, monthPage(YearMonth(9999, 12)))
    }
}
