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

/** Время суток для подписи под сообщением, например «09:05». */
fun formatClock(millis: Long, zone: TimeZone): String {
    val time = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone)
    return "${time.hour.twoDigits()}:${time.minute.twoDigits()}"
}

/** Время последнего сообщения в списке чатов: сегодня часы, вчера слово «Yesterday», раньше дата. */
fun formatListTime(millis: Long, nowMillis: Long, zone: TimeZone): String {
    val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone).date
    val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(zone).date
    return when (date) {
        today -> formatClock(millis, zone)
        today.minus(1, DateTimeUnit.DAY) -> "Yesterday"
        else -> "${date.day.twoDigits()}.${date.month.number.twoDigits()}.${date.year}"
    }
}

/** Заголовок дня в переписке: «Today», «Yesterday» или «October 26» (с годом, если он не текущий). */
fun formatDayHeader(date: LocalDate, today: LocalDate): String = when {
    date == today -> "Today"
    date == today.minus(1, DateTimeUnit.DAY) -> "Yesterday"
    else -> {
        val dayAndMonth = "${months[date.month.number - 1]} ${date.day}"
        if (date.year == today.year) dayAndMonth else "$dayAndMonth, ${date.year}"
    }
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')
