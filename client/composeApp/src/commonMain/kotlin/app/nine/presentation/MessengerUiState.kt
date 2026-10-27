package app.nine.presentation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.nine.domain.ChatId

/**
 * Screen state that does not need to be stored on disk: what is selected, what is typed and what is open.
 * The chats and messages themselves live in [app.nine.data.MessengerRepository].
 */
@Stable
class MessengerUiState {
    var selectedChatId by mutableStateOf<ChatId?>(null)
        private set

    var tab by mutableStateOf(ChatTab.All)
    var chatSearchOpen by mutableStateOf(false)
    var chatSearch by mutableStateOf("")

    /** Search inside the open conversation. Reset when the chat changes. */
    var messageSearch by mutableStateOf("")

    var contactPaneOpen by mutableStateOf(true)
    var newChatDialogOpen by mutableStateOf(false)

    // Drafts are kept per chat so that what was typed is not lost when switching to another chat.
    private val drafts = mutableStateMapOf<ChatId, String>()

    fun selectChat(id: ChatId?) {
        selectedChatId = id
        messageSearch = ""
    }

    fun draftFor(id: ChatId): String = drafts[id].orEmpty()

    fun setDraft(id: ChatId, text: String) {
        if (text.isEmpty()) drafts.remove(id) else drafts[id] = text
    }

    /** Returns the list to its normal view: no search and on the "All" tab. Needed so that a new chat is not hidden by a filter. */
    fun showAllChats() {
        tab = ChatTab.All
        chatSearch = ""
    }

    fun toggleChatSearch() {
        chatSearchOpen = !chatSearchOpen
        if (!chatSearchOpen) chatSearch = ""
    }
}
