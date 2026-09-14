package com.mehfil.app.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

/** Calendar helpers. "Today" is real time; the sample season is date-shifted to fit around it. */
object Cal {
    val today: LocalDateTime get() = LocalDateTime.now()
    val todayDate: LocalDate get() = LocalDate.now()
    fun newId(): String = java.util.UUID.randomUUID().toString().replace("-", "").take(20)
    /** Season window (Oct–Feb) containing `day`. */
    fun seasonStart(day: LocalDate): LocalDate = LocalDate.of(if (day.monthValue >= 10) day.year else day.year - 1, 10, 1)
    fun seasonLabel(day: LocalDate): String { val y = seasonStart(day).year; return "Season $y–${(y + 1).toString().takeLast(2)}" }

    fun d(y: Int, m: Int, day: Int): LocalDate = LocalDate.of(y, m, day)
    fun dt(y: Int, m: Int, day: Int, h: Int, min: Int = 0): LocalDateTime = LocalDateTime.of(y, m, day, h, min)
    fun daysBetween(a: LocalDate, b: LocalDate): Int = ChronoUnit.DAYS.between(a, b).toInt()
    fun hour(t: LocalDateTime): Double = t.hour + t.minute / 60.0

    /** Monday-to-Sunday week containing today (NOTES Phase 4). */
    val thisWeek: ClosedRange<LocalDate>
        get() {
            val monday = todayDate.minusDays(((todayDate.dayOfWeek.value + 6) % 7).toLong())
            return monday..monday.plusDays(6)
        }
    /** Next Saturday strictly after today (the wedding-season focal day). */
    val nextSaturday: LocalDate
        get() {
            var d = todayDate.plusDays(1)
            while (d.dayOfWeek != DayOfWeek.SATURDAY) d = d.plusDays(1)
            return d
        }
}

object Fmt {
    /** Indian digit grouping: ₹23,47,500 — never ₹2,347,500 (spec §6). */
    fun inr(amount: Int, symbol: Boolean = true): String {
        val s = abs(amount).toString()
        val grouped = if (s.length > 3) {
            val last3 = s.takeLast(3)
            var rest = s.dropLast(3)
            val parts = mutableListOf<String>()
            while (rest.length > 2) { parts.add(0, rest.takeLast(2)); rest = rest.dropLast(2) }
            if (rest.isNotEmpty()) parts.add(0, rest)
            parts.joinToString(",") + "," + last3
        } else s
        return (if (amount < 0) "−" else "") + (if (symbol) "₹" else "") + grouped
    }

    /** Lakh notation for narrow columns only: ₹22.1L */
    fun lakh(amount: Int): String {
        val l = amount / 100_000.0
        return "₹" + (if (l == l.toLong().toDouble()) l.toLong().toString() else String.format(Locale.UK, "%.1f", l)) + "L"
    }

    private fun f(p: String) = DateTimeFormatter.ofPattern(p, Locale.UK)
    private val time = f("HH:mm")
    private val dayMonth = f("d MMM")
    private val dayMonthLong = f("d MMMM")
    private val weekdayDayMonth = f("EEE d MMM")
    private val weekdayLong = f("EEEE d MMMM")
    private val weekdayLongYear = f("EEEE d MMMM yyyy")
    private val monthYear = f("MMMM yyyy")
    private val monthShort = f("MMM")
    private val weekdayShort = f("EEE")
    private val weekdayDayMonthTime = f("EEE d MMM, HH:mm")

    fun time(t: LocalDateTime): String = time.format(t)
    fun dayMonth(d: LocalDate): String = dayMonth.format(d)
    fun dayMonthLong(d: LocalDate): String = dayMonthLong.format(d)
    fun weekdayDayMonth(d: LocalDate): String = weekdayDayMonth.format(d)
    fun weekdayLong(d: LocalDate): String = weekdayLong.format(d)
    fun weekdayLongYear(d: LocalDate): String = weekdayLongYear.format(d)
    fun monthYear(d: LocalDate): String = monthYear.format(d)
    fun monthShort(d: LocalDate): String = monthShort.format(d)
    fun weekdayShort(d: LocalDate): String = weekdayShort.format(d)
    fun weekdayDayMonthTime(t: LocalDateTime): String = weekdayDayMonthTime.format(t)
    fun timeRange(a: LocalDateTime, b: LocalDateTime) = "${time(a)}–${time(b)}"

    fun relativeDays(d: LocalDate): String {
        val n = Cal.daysBetween(Cal.todayDate, d)
        return when {
            n == 0 -> "today"
            n == 1 -> "tomorrow"
            n < 0 -> "${-n} days late"
            else -> "in $n days"
        }
    }
    fun daysLate(d: LocalDate): Int = maxOf(0, Cal.daysBetween(d, Cal.todayDate))
    fun capitalized(s: String) = s.replaceFirstChar { it.uppercase() }
}
