package app.nine.data

import app.nine.domain.Author
import app.nine.domain.AutoLock
import app.nine.domain.BubbleColor
import app.nine.domain.ChatId
import app.nine.domain.Delivery
import app.nine.domain.MaxDisplayNameLength
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
        val id = assertNotNull(repository.createChat("  Fox  "))

        val chat = repository.data.value.chats.single()
        assertEquals(id, chat.id)
        assertEquals("Fox", chat.title)
        assertEquals(1_000, chat.createdAtMillis)
    }

    @Test
    fun createChatRejectsBlankTitle() {
        assertNull(repository.createChat("   "))
        assertTrue(repository.data.value.chats.isEmpty())
    }

    @Test
    fun sendMessageAppendsOutgoingMessageInOrder() {
        val id = assertNotNull(repository.createChat("Fox"))

        time.now = 2_000
        assertTrue(repository.sendMessage(id, "Hello"))
        time.now = 3_000
        assertTrue(repository.sendMessage(id, "How are you?"))

        val messages = repository.data.value.chats.single().messages
        assertEquals(listOf("Hello", "How are you?"), messages.map { it.text })
        assertEquals(listOf(2_000L, 3_000L), messages.map { it.sentAtMillis })
        assertTrue(messages.all { it.author == Author.Me && it.delivery == Delivery.Local })
    }

    @Test
    fun sendMessageTrimsEdgesButKeepsInnerLineBreaks() {
        val id = assertNotNull(repository.createChat("Fox"))

        repository.sendMessage(id, "\n  line 1\nline 2  \n")

        assertEquals("line 1\nline 2", repository.data.value.chats.single().messages.single().text)
    }

    @Test
    fun sendMessageIgnoresBlankTextAndUnknownChat() {
        val id = assertNotNull(repository.createChat("Fox"))

        assertFalse(repository.sendMessage(id, " \n "))
        assertFalse(repository.sendMessage(ChatId("no-such-chat"), "Hello"))

        assertTrue(repository.data.value.chats.single().messages.isEmpty())
    }

    @Test
    fun everyChangeIsSavedToStore() {
        val id = assertNotNull(repository.createChat("Fox"))
        repository.sendMessage(id, "Hello")
        repository.setBubbleColor(BubbleColor.Moss)

        assertEquals(3, store.saves)
        assertEquals(repository.data.value, store.stored)
    }

    @Test
    fun newRepositoryRestoresDataFromStore() {
        val id = assertNotNull(repository.createChat("Fox"))
        repository.sendMessage(id, "Hello")

        val restored = MessengerRepository(store, time, sequentialIds())

        assertEquals(repository.data.value, restored.data.value)
    }

    @Test
    fun archiveFlagCanBeSetAndCleared() {
        val id = assertNotNull(repository.createChat("Fox"))

        repository.setArchived(id, true)
        assertTrue(repository.data.value.chats.single().archived)

        repository.setArchived(id, false)
        assertFalse(repository.data.value.chats.single().archived)
    }

    @Test
    fun markReadResetsUnreadCounter() {
        val id = assertNotNull(repository.createChat("Fox"))
        store.stored = repository.data.value.let { data ->
            data.copy(chats = data.chats.map { it.copy(unreadCount = 3) })
        }
        val withUnread = MessengerRepository(store, time, sequentialIds())

        withUnread.markRead(id)

        assertEquals(0, withUnread.data.value.chats.single().unreadCount)
    }

    @Test
    fun unchangedStateIsNotWrittenAgain() {
        val id = assertNotNull(repository.createChat("Fox"))
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

    @Test
    fun settingsStartWithSafeDefaults() {
        val settings = repository.data.value.settings

        assertEquals(AutoLock.FiveMinutes, settings.autoLock)
        assertTrue(settings.clearClipboard)
        // A visible red button is something the person asks for, it does not appear on its own.
        assertFalse(settings.panicButton)
    }

    @Test
    fun panicButtonSettingIsSaved() {
        val saves = store.saves

        repository.setPanicButton(true)

        assertEquals(saves + 1, store.saves)
        assertTrue(store.stored.settings.panicButton)
    }

    @Test
    fun settingsChangesAreSavedAndKeepTheChats() {
        repository.createChat("Fox")
        val saves = store.saves

        repository.setAutoLock(AutoLock.OnMinimize)
        repository.setClearClipboard(false)

        assertEquals(saves + 2, store.saves)
        assertEquals(AutoLock.OnMinimize, store.stored.settings.autoLock)
        assertFalse(store.stored.settings.clearClipboard)
        assertEquals("Fox", store.stored.chats.single().title)
    }

    @Test
    fun displayNameIsEmptyUntilSetAndIsSavedTrimmed() {
        assertEquals("", repository.data.value.displayName)

        repository.setDisplayName("  Fox  ")

        assertEquals("Fox", repository.data.value.displayName)
        assertEquals("Fox", store.stored.displayName)
    }

    @Test
    fun displayNameIsCutToTheLimitWithoutTrailingSpace() {
        repository.setDisplayName("a".repeat(MaxDisplayNameLength - 1) + " " + "b".repeat(10))

        val name = repository.data.value.displayName
        assertEquals(MaxDisplayNameLength - 1, name.length)
        assertFalse(name.endsWith(" "))
    }

    @Test
    fun blankDisplayNameClearsIt() {
        repository.setDisplayName("Fox")

        repository.setDisplayName("   ")

        assertEquals("", repository.data.value.displayName)
    }

    @Test
    fun repeatingTheSameSettingDoesNotWriteAgain() {
        repository.setAutoLock(AutoLock.Never)
        val saves = store.saves

        repository.setAutoLock(AutoLock.Never)

        assertEquals(saves, store.saves)
    }
}
