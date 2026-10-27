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
 * Единственное место, где меняются чаты и сообщения.
 *
 * Сейчас всё живёт на устройстве. Когда появится сервер, отправка сообщения будет проходить здесь же:
 * сначала запись с [app.nine.domain.Delivery.Local], затем отправка и смена статуса. Интерфейс при этом не изменится.
 *
 * Вызывать методы нужно с потока интерфейса: состояние не рассчитано на одновременную запись из нескольких потоков.
 */
class MessengerRepository(
    private val store: MessengerStore,
    private val time: TimeSource = TimeSource.System,
    private val newId: () -> String = ::randomId,
) {
    private val state = MutableStateFlow(store.load())

    val data: StateFlow<MessengerData> = state.asStateFlow()

    /** Создаёт пустой чат и возвращает его id. Возвращает null, если имя пустое. */
    fun createChat(title: String): ChatId? {
        val name = title.trim()
        if (name.isEmpty()) return null
        val chat = Chat(id = ChatId(newId()), title = name, createdAtMillis = time.nowMillis())
        change { it.copy(chats = it.chats + chat) }
        return chat.id
    }

    /**
     * Добавляет исходящее сообщение в конец чата.
     * Пробелы по краям обрезаются. Пустое сообщение и неизвестный чат игнорируются, тогда возвращается false.
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

    /** Сбрасывает счётчик непрочитанных. Вызывается, когда человек открыл чат. */
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

    /** Пробелы по краям обрезаются, слишком длинное имя укорачивается. Пустое имя значит, что имя не задано. */
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
     * Меняет состояние и сразу сохраняет его: данные небольшие, а потерять набранное при сбое хуже, чем записать лишний раз.
     * Если ничего не изменилось (например, чат уже прочитан), файл не трогаем.
     */
    private fun change(transform: (MessengerData) -> MessengerData) {
        val before = state.value
        state.update(transform)
        if (state.value != before) store.save(state.value)
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun randomId(): String = Uuid.random().toString()
