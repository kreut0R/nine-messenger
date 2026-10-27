package app.nine.data

import app.nine.domain.Author
import app.nine.domain.AutoLock
import app.nine.domain.BubbleColor
import app.nine.domain.Chat
import app.nine.domain.ChatId
import app.nine.domain.MaxDisplayNameLength
import app.nine.domain.MessengerData
import app.nine.domain.Message
import app.nine.domain.TimeSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The only place where chats and messages are changed.
 *
 * Everything lives on the device for now. Once there is a server, sending a message will go through here as well:
 * first a record with [app.nine.domain.Delivery.Local], then the send and a status change. The UI will not change.
 *
 * Call the methods from the UI thread: the state is not designed for concurrent writes from several threads.
 */
class MessengerRepository(
    private val store: MessengerStore,
    private val time: TimeSource = TimeSource.System,
    private val newId: () -> String = ::randomId,
) {
    private val state = MutableStateFlow(store.load())

    val data: StateFlow<MessengerData> = state.asStateFlow()

    /** Creates an empty chat and returns its id. Returns null if the name is blank. */
    fun createChat(title: String): ChatId? {
        val name = title.trim()
        if (name.isEmpty()) return null
        val chat = Chat(id = ChatId(newId()), title = name, createdAtMillis = time.nowMillis())
        change { it.copy(chats = it.chats + chat) }
        return chat.id
    }

    /**
     * Appends an outgoing message to the end of the chat.
     * Leading and trailing whitespace is trimmed. An empty message or an unknown chat is ignored and false is returned.
     */
    fun sendMessage(chatId: ChatId, text: String): Boolean {
        val body = text.trim()
        if (body.isEmpty()) return false
        val message = Message(id = newId(), author = Author.Me, text = body, sentAtMillis = time.nowMillis())
        return changeChat(chatId) { it.copy(messages = it.messages + message) }
    }

    fun setArchived(chatId: ChatId, archived: Boolean) {
        changeChat(chatId) { it.copy(archived = archived) }
    }

    /** Resets the unread counter. Called when the person has opened the chat. */
    fun markRead(chatId: ChatId) {
        changeChat(chatId) { if (it.unreadCount == 0) it else it.copy(unreadCount = 0) }
    }

    fun setBubbleColor(color: BubbleColor) {
        change { it.copy(bubbleColor = color) }
    }

    fun setAutoLock(autoLock: AutoLock) {
        change { it.copy(settings = it.settings.copy(autoLock = autoLock)) }
    }

    fun setClearClipboard(clear: Boolean) {
        change { it.copy(settings = it.settings.copy(clearClipboard = clear)) }
    }

    /** Leading and trailing whitespace is trimmed, an overlong name is shortened. A blank name means no name is set. */
    fun setDisplayName(name: String) {
        val clean = name.trim().take(MaxDisplayNameLength).trimEnd()
        change { it.copy(displayName = clean) }
    }

    private fun changeChat(chatId: ChatId, transform: (Chat) -> Chat): Boolean {
        if (state.value.chats.none { it.id == chatId }) return false
        change { data -> data.copy(chats = data.chats.map { if (it.id == chatId) transform(it) else it }) }
        return true
    }

    /**
     * Changes the state and saves it right away: the data is small, and losing what was typed in a crash is worse than writing once too often.
     * If nothing changed (for example, the chat is already read), the file is not touched.
     */
    private fun change(transform: (MessengerData) -> MessengerData) {
        val before = state.value
        state.update(transform)
        if (state.value != before) store.save(state.value)
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun randomId(): String = Uuid.random().toString()
