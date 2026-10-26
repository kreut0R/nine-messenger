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
        messages = listOf(Message(id = "m-$title", author = Author.Peer, text = "текст", sentAtMillis = lastAt)),
    )

    private val old = chat("Старый", lastAt = 100)
    private val fresh = chat("Свежий", lastAt = 300)
    private val unread = chat("Непрочитанный", lastAt = 200, unread = 2)
    private val archived = chat("Архивный", lastAt = 400, archived = true, unread = 5)
    private val all = listOf(old, fresh, unread, archived)

    @Test
    fun allTabHidesArchivedAndSortsByLastActivity() {
        val titles = filterChats(all, ChatTab.All, query = "").map { it.title }

        assertEquals(listOf("Свежий", "Непрочитанный", "Старый"), titles)
    }

    @Test
    fun unreadTabShowsOnlyActiveChatsWithUnreadMessages() {
        val titles = filterChats(all, ChatTab.Unread, query = "").map { it.title }

        assertEquals(listOf("Непрочитанный"), titles)
    }

    @Test
    fun archiveTabShowsOnlyArchivedChats() {
        val titles = filterChats(all, ChatTab.Archive, query = "").map { it.title }

        assertEquals(listOf("Архивный"), titles)
    }

    @Test
    fun queryMatchesTitleIgnoringCaseAndSurroundingSpaces() {
        val titles = filterChats(all, ChatTab.All, query = "  свеж ").map { it.title }

        assertEquals(listOf("Свежий"), titles)
    }

    @Test
    fun emptyChatIsSortedByCreationTime() {
        val empty = Chat(id = ChatId("e"), title = "Пустой", createdAtMillis = 250)

        val titles = filterChats(listOf(old, empty, fresh), ChatTab.All, query = "").map { it.title }

        assertEquals(listOf("Свежий", "Пустой", "Старый"), titles)
    }

    @Test
    fun filterMessagesKeepsEverythingForBlankQuery() {
        val messages = listOf(message("a", "Привет"), message("b", "Пока"))

        assertEquals(messages, filterMessages(messages, "  "))
    }

    @Test
    fun filterMessagesFindsTextIgnoringCase() {
        val messages = listOf(message("a", "Привет, мир"), message("b", "Пока"))

        assertEquals(listOf("a"), filterMessages(messages, "ПРИВЕТ").map { it.id })
    }

    @Test
    fun emptyMessageExplainsWhyListIsEmpty() {
        assertEquals("No chats yet.\nClick “+” to start your first one.", emptyListMessage(0, ChatTab.All, ""))
        assertEquals("Nothing found", emptyListMessage(3, ChatTab.All, "яяя"))
        assertEquals("No unread chats", emptyListMessage(3, ChatTab.Unread, ""))
        assertEquals("Archive is empty", emptyListMessage(3, ChatTab.Archive, ""))
    }

    private fun message(id: String, text: String) = Message(id, Author.Me, text, sentAtMillis = 0)
}
