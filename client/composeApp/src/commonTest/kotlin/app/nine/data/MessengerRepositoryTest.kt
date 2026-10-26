package app.nine.data

import app.nine.domain.Author
import app.nine.domain.BubbleColor
import app.nine.domain.ChatId
import app.nine.domain.Delivery
import app.nine.support.FakeTime
import app.nine.support.InMemoryStore
import app.nine.support.sequentialIds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessengerRepositoryTest {
    private val time = FakeTime(now = 1_000)
    private val store = InMemoryStore()
    private val repository = MessengerRepository(store, time, sequentialIds())

    @Test
    fun startsWithoutChats() {
        assertTrue(repository.data.value.chats.isEmpty())
    }

    @Test
    fun createChatTrimsTitleAndRemembersCreationTime() {
        val id = assertNotNull(repository.createChat("  Лис  "))

        val chat = repository.data.value.chats.single()
        assertEquals(id, chat.id)
        assertEquals("Лис", chat.title)
        assertEquals(1_000, chat.createdAtMillis)
    }

    @Test
    fun createChatRejectsBlankTitle() {
        assertNull(repository.createChat("   "))
        assertTrue(repository.data.value.chats.isEmpty())
    }

    @Test
    fun sendMessageAppendsOutgoingMessageInOrder() {
        val id = assertNotNull(repository.createChat("Лис"))

        time.now = 2_000
        assertTrue(repository.sendMessage(id, "Привет"))
        time.now = 3_000
        assertTrue(repository.sendMessage(id, "Как дела?"))

        val messages = repository.data.value.chats.single().messages
        assertEquals(listOf("Привет", "Как дела?"), messages.map { it.text })
        assertEquals(listOf(2_000L, 3_000L), messages.map { it.sentAtMillis })
        assertTrue(messages.all { it.author == Author.Me && it.delivery == Delivery.Local })
    }

    @Test
    fun sendMessageTrimsEdgesButKeepsInnerLineBreaks() {
        val id = assertNotNull(repository.createChat("Лис"))

        repository.sendMessage(id, "\n  строка 1\nстрока 2  \n")

        assertEquals("строка 1\nстрока 2", repository.data.value.chats.single().messages.single().text)
    }

    @Test
    fun sendMessageIgnoresBlankTextAndUnknownChat() {
        val id = assertNotNull(repository.createChat("Лис"))

        assertFalse(repository.sendMessage(id, " \n "))
        assertFalse(repository.sendMessage(ChatId("нет-такого"), "Привет"))

        assertTrue(repository.data.value.chats.single().messages.isEmpty())
    }

    @Test
    fun everyChangeIsSavedToStore() {
        val id = assertNotNull(repository.createChat("Лис"))
        repository.sendMessage(id, "Привет")
        repository.setBubbleColor(BubbleColor.Moss)

        assertEquals(3, store.saves)
        assertEquals(repository.data.value, store.stored)
    }

    @Test
    fun newRepositoryRestoresDataFromStore() {
        val id = assertNotNull(repository.createChat("Лис"))
        repository.sendMessage(id, "Привет")

        val restored = MessengerRepository(store, time, sequentialIds())

        assertEquals(repository.data.value, restored.data.value)
    }

    @Test
    fun archiveFlagCanBeSetAndCleared() {
        val id = assertNotNull(repository.createChat("Лис"))

        repository.setArchived(id, true)
        assertTrue(repository.data.value.chats.single().archived)

        repository.setArchived(id, false)
        assertFalse(repository.data.value.chats.single().archived)
    }

    @Test
    fun markReadResetsUnreadCounter() {
        val id = assertNotNull(repository.createChat("Лис"))
        store.stored = repository.data.value.let { data ->
            data.copy(chats = data.chats.map { it.copy(unreadCount = 3) })
        }
        val withUnread = MessengerRepository(store, time, sequentialIds())

        withUnread.markRead(id)

        assertEquals(0, withUnread.data.value.chats.single().unreadCount)
    }

    @Test
    fun unchangedStateIsNotWrittenAgain() {
        val id = assertNotNull(repository.createChat("Лис"))
        val savesBefore = store.saves

        repository.markRead(id)
        repository.setBubbleColor(repository.data.value.bubbleColor)

        assertEquals(savesBefore, store.saves)
    }

    @Test
    fun bubbleColorIsRemembered() {
        repository.setBubbleColor(BubbleColor.Graphite)

        assertEquals(BubbleColor.Graphite, repository.data.value.bubbleColor)
    }
}
