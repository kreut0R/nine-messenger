package app.nine.presentation

import app.nine.domain.Author
import app.nine.domain.Message
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Элемент ленты переписки: либо заголовок дня, либо подряд идущие сообщения одного автора. */
sealed interface TimelineItem {
    data class DayHeader(val date: LocalDate) : TimelineItem

    data class Group(val author: Author, val messages: List<Message>) : TimelineItem
}

/**
 * Превращает плоский список сообщений в ленту для показа.
 * Сообщения одного автора подряд склеиваются в группу, а новый день всегда начинается с заголовка и новой группы.
 * Ожидается, что сообщения уже отсортированы по времени, как они и хранятся.
 */
fun buildTimeline(messages: List<Message>, zone: TimeZone): List<TimelineItem> {
    val items = mutableListOf<TimelineItem>()
    var currentDay: LocalDate? = null
    var currentGroup: MutableList<Message>? = null
    var currentAuthor: Author? = null

    fun closeGroup() {
        val group = currentGroup ?: return
        items += TimelineItem.Group(requireNotNull(currentAuthor), group)
        currentGroup = null
    }

    for (message in messages) {
        val day = localDate(message.sentAtMillis, zone)
        if (day != currentDay) {
            closeGroup()
            items += TimelineItem.DayHeader(day)
            currentDay = day
        }
        if (message.author != currentAuthor) closeGroup()
        val group = currentGroup ?: mutableListOf<Message>().also { currentGroup = it; currentAuthor = message.author }
        group += message
    }
    closeGroup()
    return items
}

private fun localDate(millis: Long, zone: TimeZone): LocalDate =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone).date
