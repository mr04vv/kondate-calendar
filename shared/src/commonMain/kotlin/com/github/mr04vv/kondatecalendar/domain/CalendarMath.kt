package com.github.mr04vv.kondatecalendar.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus

sealed interface ListRow {
    data class WeekHeader(val monday: LocalDate) : ListRow
    data class Day(val date: LocalDate) : ListRow
}

/** Monday of the week containing [date]. */
fun weekStart(date: LocalDate): LocalDate = date.minus(date.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber, DateTimeUnit.DAY)

/** Monday..Sunday of the week containing [date]. */
fun weekOf(date: LocalDate): List<LocalDate> = weekStart(date).let { monday -> (0L until DAYS_IN_WEEK).map { monday.plus(it, DateTimeUnit.DAY) } }

/** Every date shown in a Monday-start month grid: whole weeks covering [month] (4 to 6 rows). */
fun monthGrid(month: YearMonth): List<LocalDate> {
    val first = weekStart(month.firstDay)
    val last = weekStart(month.lastDay).plus(DAYS_IN_WEEK - 1, DateTimeUnit.DAY)
    return generateSequence(first) { it.plus(1, DateTimeUnit.DAY) }.takeWhile { it <= last }.toList()
}

/** Rows for the list mode: a header before each week, then its seven days, for whole weeks covering [from]..[to]. */
fun listRows(from: LocalDate, to: LocalDate): List<ListRow> =
    generateSequence(weekStart(from)) { it.plus(1, DateTimeUnit.WEEK) }
        .takeWhile { it <= to }
        .flatMap { monday -> sequenceOf(ListRow.WeekHeader(monday)) + weekOf(monday).map(ListRow::Day) }
        .toList()

/** Pager index of [month] in the swipeable grid: consecutive months get consecutive pages, starting at January of year 1. */
fun monthPage(month: YearMonth): Int = (month.year - FIRST_PAGE_YEAR) * MONTHS_IN_YEAR + month.month.number - 1

/** Inverse of [monthPage]. */
fun monthOfPage(page: Int): YearMonth = YearMonth(FIRST_PAGE_YEAR + page / MONTHS_IN_YEAR, page % MONTHS_IN_YEAR + 1)

const val DAYS_IN_WEEK = 7L
private const val MONTHS_IN_YEAR = 12
private const val FIRST_PAGE_YEAR = 1
private const val LAST_PAGE_YEAR = 9999

/** Number of pages in the month pager: every month from year [FIRST_PAGE_YEAR] to [LAST_PAGE_YEAR]. */
const val MONTH_PAGE_COUNT = (LAST_PAGE_YEAR - FIRST_PAGE_YEAR + 1) * MONTHS_IN_YEAR
