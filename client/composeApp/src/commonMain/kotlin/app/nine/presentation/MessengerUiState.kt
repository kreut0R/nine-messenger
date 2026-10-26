package app.nine.presentation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.nine.domain.ChatId

/**
 * Состояние экрана, которое не нужно хранить на диске: что выбрано, что набрано и что открыто.
 * Сами чаты и сообщения живут в [app.nine.data.MessengerRepository].
 */
@Stable
class MessengerUiState {
    var selectedChatId by mutableStateOf<ChatId?>(null)
        private set

    var tab by mutableStateOf(ChatTab.All)
    var chatSearchOpen by mutableStateOf(false)
    var chatSearch by mutableStateOf("")

    /** Поиск внутри открытой переписки. Сбрасывается при смене чата. */
    var messageSearch by mutableStateOf("")

    var contactPaneOpen by mutableStateOf(true)
    var newChatDialogOpen by mutableStateOf(false)

    // Черновики хранятся по чатам, чтобы набранное не терялось при переходе в другой чат.
    private val drafts = mutableStateMapOf<ChatId, String>()

    fun selectChat(id: ChatId?) {
        selectedChatId = id
        messageSearch = ""
    }

    fun draftFor(id: ChatId): String = drafts[id].orEmpty()

    fun setDraft(id: ChatId, text: String) {
        if (text.isEmpty()) drafts.remove(id) else drafts[id] = text
    }

    /** Возвращает список в обычный вид: без поиска и на вкладке «Все». Нужно, чтобы новый чат не оказался скрыт фильтром. */
    fun showAllChats() {
        tab = ChatTab.All
        chatSearch = ""
    }

    fun toggleChatSearch() {
        chatSearchOpen = !chatSearchOpen
        if (!chatSearchOpen) chatSearch = ""
    }
}
