package app.nine.presentation

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class TimeFormatTest {
    private val zone = TimeZone.UTC

    private fun millis(iso: String) = Instant.parse(iso).toEpochMilliseconds()

    @Test
    fun clockUsesTwoDigits() {
        assertEquals("09:05", formatClock(millis("2026-10-26T09:05:00Z"), zone))
        assertEquals("23:59", formatClock(millis("2026-10-26T23:59:00Z"), zone))
    }

    @Test
    fun listTimeShowsClockForTodayAndWordForYesterday() {
        val now = millis("2026-10-26T15:00:00Z")

        assertEquals("09:05", formatListTime(millis("2026-10-26T09:05:00Z"), now, zone))
        assertEquals("Yesterday", formatListTime(millis("2026-10-25T20:00:00Z"), now, zone))
    }

    @Test
    fun listTimeShowsDateForOlderMessages() {
        val now = millis("2026-10-26T15:00:00Z")

        assertEquals("Sep 3, 2026", formatListTime(millis("2026-09-03T10:00:00Z"), now, zone))
    }

    @Test
    fun dayHeaderNamesTodayAndYesterday() {
        val today = LocalDate(2026, 10, 26)

        assertEquals("Today", formatDayHeader(LocalDate(2026, 10, 26), today))
        assertEquals("Yesterday", formatDayHeader(LocalDate(2026, 10, 25), today))
    }

    @Test
    fun dayHeaderSpellsMonthAndAddsYearOnlyForOtherYears() {
        val today = LocalDate(2026, 10, 26)

        assertEquals("September 3", formatDayHeader(LocalDate(2026, 9, 3), today))
        assertEquals("December 31, 2025", formatDayHeader(LocalDate(2025, 12, 31), today))
    }
}
