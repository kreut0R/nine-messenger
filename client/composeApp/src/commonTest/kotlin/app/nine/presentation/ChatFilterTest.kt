package app.nine.presentation

import app.nine.domain.Author
import app.nine.domain.Chat
import app.nine.domain.ChatId
import app.nine.domain.Message
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatFilterTest {
    private fun chat(
        title: String,
        lastAt: Long,
        archived: Boolean = false,
        unread: Int = 0,
    ) = Chat(
        id = ChatId(title),
        title = title,
        createdAtMillis = 0,
        archived = archived,
        unreadCount = unread,
        messages = listOf(Message(id = "m-$title", author = Author.Peer, text = "text", sentAtMillis = lastAt)),
    )

    private val old = chat("Old", lastAt = 100)
    private val fresh = chat("Fresh", lastAt = 300)
    private val unread = chat("Unread", lastAt = 200, unread = 2)
    private val archived = chat("Archived", lastAt = 400, archived = true, unread = 5)
    private val all = listOf(old, fresh, unread, archived)

    @Test
    fun allTabHidesArchivedAndSortsByLastActivity() {
        val titles = filterChats(all, ChatTab.All, query = "").map { it.title }

        assertEquals(listOf("Fresh", "Unread", "Old"), titles)
    }

    @Test
    fun unreadTabShowsOnlyActiveChatsWithUnreadMessages() {
        val titles = filterChats(all, ChatTab.Unread, query = "").map { it.title }

        assertEquals(listOf("Unread"), titles)
    }

    @Test
    fun archiveTabShowsOnlyArchivedChats() {
        val titles = filterChats(all, ChatTab.Archive, query = "").map { it.title }

        assertEquals(listOf("Archived"), titles)
    }

    @Test
    fun queryMatchesTitleIgnoringCaseAndSurroundingSpaces() {
        val titles = filterChats(all, ChatTab.All, query = "  fre ").map { it.title }

        assertEquals(listOf("Fresh"), titles)
    }

    @Test
    fun emptyChatIsSortedByCreationTime() {
        val empty = Chat(id = ChatId("e"), title = "Empty", createdAtMillis = 250)

        val titles = filterChats(listOf(old, empty, fresh), ChatTab.All, query = "").map { it.title }

        assertEquals(listOf("Fresh", "Empty", "Old"), titles)
    }

    @Test
    fun filterMessagesKeepsEverythingForBlankQuery() {
        val messages = listOf(message("a", "Hello"), message("b", "Bye"))

        assertEquals(messages, filterMessages(messages, "  "))
    }

    @Test
    fun filterMessagesFindsTextIgnoringCase() {
        val messages = listOf(message("a", "Zürich trip"), message("b", "Bye"))

        assertEquals(listOf("a"), filterMessages(messages, "ZÜRICH").map { it.id })
    }

    @Test
    fun emptyMessageExplainsWhyListIsEmpty() {
        assertEquals("No chats yet.\nPress + to create your first one.", emptyListMessage(0, ChatTab.All, ""))
        assertEquals("Nothing found", emptyListMessage(3, ChatTab.All, "zzz"))
        assertEquals("No unread chats", emptyListMessage(3, ChatTab.Unread, ""))
        assertEquals("Archive is empty", emptyListMessage(3, ChatTab.Archive, ""))
    }

    private fun message(id: String, text: String) = Message(id, Author.Me, text, sentAtMillis = 0)
}
