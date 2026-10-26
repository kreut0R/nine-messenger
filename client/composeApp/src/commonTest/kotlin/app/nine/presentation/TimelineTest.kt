package app.nine.presentation

import app.nine.domain.Author
import app.nine.domain.Message
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class TimelineTest {
    private val zone = TimeZone.UTC

    private fun message(id: String, author: Author, iso: String) =
        Message(id, author, text = id, sentAtMillis = Instant.parse(iso).toEpochMilliseconds())

    @Test
    fun emptyListGivesEmptyTimeline() {
        assertTrue(buildTimeline(emptyList(), zone).isEmpty())
    }

    @Test
    fun consecutiveMessagesOfOneAuthorFormOneGroupUnderOneDayHeader() {
        val items = buildTimeline(
            listOf(
                message("a", Author.Me, "2026-10-26T10:00:00Z"),
                message("b", Author.Me, "2026-10-26T10:05:00Z"),
            ),
            zone,
        )

        assertEquals(2, items.size)
        assertEquals(TimelineItem.DayHeader(LocalDate(2026, 10, 26)), items[0])
        assertEquals(listOf("a", "b"), (items[1] as TimelineItem.Group).messages.map { it.id })
    }

    @Test
    fun changeOfAuthorStartsNewGroup() {
        val items = buildTimeline(
            listOf(
                message("a", Author.Me, "2026-10-26T10:00:00Z"),
                message("b", Author.Peer, "2026-10-26T10:01:00Z"),
                message("c", Author.Me, "2026-10-26T10:02:00Z"),
            ),
            zone,
        )

        val groups = items.filterIsInstance<TimelineItem.Group>()
        assertEquals(listOf(Author.Me, Author.Peer, Author.Me), groups.map { it.author })
    }

    @Test
    fun newDayStartsNewGroupEvenForSameAuthor() {
        val items = buildTimeline(
            listOf(
                message("a", Author.Me, "2026-10-25T23:59:00Z"),
                message("b", Author.Me, "2026-10-26T00:01:00Z"),
            ),
            zone,
        )

        assertEquals(
            listOf("DayHeader", "Group", "DayHeader", "Group"),
            items.map { it::class.simpleName },
        )
    }

    @Test
    fun dayBoundaryFollowsGivenTimeZone() {
        // 22:30 UTC — это уже следующий день в Москве (UTC+3).
        val items = buildTimeline(listOf(message("a", Author.Me, "2026-10-25T22:30:00Z")), TimeZone.of("Europe/Moscow"))

        assertEquals(TimelineItem.DayHeader(LocalDate(2026, 10, 26)), items.first())
    }
}
