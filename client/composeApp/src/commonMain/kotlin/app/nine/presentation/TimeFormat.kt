package app.nine.presentation

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val monthsGenitive = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

/** Время суток для подписи под сообщением, например «09:05». */
fun formatClock(millis: Long, zone: TimeZone): String {
    val time = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone)
    return "${time.hour.twoDigits()}:${time.minute.twoDigits()}"
}

/** Время последнего сообщения в списке чатов: сегодня часы, вчера слово «Вчера», раньше дата. */
fun formatListTime(millis: Long, nowMillis: Long, zone: TimeZone): String {
    val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone).date
    val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(zone).date
    return when (date) {
        today -> formatClock(millis, zone)
        today.minus(1, DateTimeUnit.DAY) -> "Вчера"
        else -> "${date.day.twoDigits()}.${date.month.number.twoDigits()}.${date.year}"
    }
}

/** Заголовок дня в переписке: «Сегодня», «Вчера» или «26 октября» (с годом, если он не текущий). */
fun formatDayHeader(date: LocalDate, today: LocalDate): String = when {
    date == today -> "Сегодня"
    date == today.minus(1, DateTimeUnit.DAY) -> "Вчера"
    else -> {
        val dayAndMonth = "${date.day} ${monthsGenitive[date.month.number - 1]}"
        if (date.year == today.year) dayAndMonth else "$dayAndMonth ${date.year}"
    }
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')
