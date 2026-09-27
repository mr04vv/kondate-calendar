package com.github.mr04vv.kondatecalendar.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

sealed interface ListRow {
    data class WeekHeader(val monday: LocalDate) : ListRow
    data class Day(val date: LocalDate) : ListRow
}

/** Monday of the week containing [date]. */
fun weekStart(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())

/** Monday..Sunday of the week containing [date]. */
fun weekOf(date: LocalDate): List<LocalDate> = weekStart(date).let { monday -> (0L until DAYS_IN_WEEK).map(monday::plusDays) }

/** Every date shown in a Monday-start month grid: whole weeks covering [month] (4 to 6 rows). */
fun monthGrid(month: YearMonth): List<LocalDate> {
    val first = weekStart(month.atDay(1))
    val last = weekStart(month.atEndOfMonth()).plusDays(DAYS_IN_WEEK - 1)
    return generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(last) }.toList()
}

/** Rows for the list mode: a header before each week, then its seven days, for whole weeks covering [from]..[to]. */
fun listRows(from: LocalDate, to: LocalDate): List<ListRow> =
    generateSequence(weekStart(from)) { it.plusWeeks(1) }
        .takeWhile { !it.isAfter(to) }
        .flatMap { monday -> sequenceOf(ListRow.WeekHeader(monday)) + weekOf(monday).map(ListRow::Day) }
        .toList()

const val DAYS_IN_WEEK = 7L
