package app.nine.presentation

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val months = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

/** Time of day for the caption under a message, for example "09:05". */
fun formatClock(millis: Long, zone: TimeZone): String {
    val time = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone)
    return "${time.hour.twoDigits()}:${time.minute.twoDigits()}"
}

/** Time of the last message in the chat list: the clock for today, the word "Yesterday" for yesterday, the date for earlier. */
fun formatListTime(millis: Long, nowMillis: Long, zone: TimeZone): String {
    val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone).date
    val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(zone).date
    return when (date) {
        today -> formatClock(millis, zone)
        today.minus(1, DateTimeUnit.DAY) -> "Yesterday"
        else -> "${months[date.month.number - 1].take(3)} ${date.day}, ${date.year}"
    }
}

/** Day header in a conversation: "Today", "Yesterday" or "October 26" (with the year if it is not the current one). */
fun formatDayHeader(date: LocalDate, today: LocalDate): String = when {
    date == today -> "Today"
    date == today.minus(1, DateTimeUnit.DAY) -> "Yesterday"
    else -> {
        val monthAndDay = "${months[date.month.number - 1]} ${date.day}"
        if (date.year == today.year) monthAndDay else "$monthAndDay, ${date.year}"
    }
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')
