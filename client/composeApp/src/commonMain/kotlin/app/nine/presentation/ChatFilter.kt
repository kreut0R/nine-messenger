package app.nine.presentation

import app.nine.domain.Chat
import app.nine.domain.Message

enum class ChatTab(val title: String) {
    All("All"),
    Unread("Unread"),
    Archive("Archive"),
}

/**
 * Picks the chats for a tab and a search and sorts them: those with recent messages come first.
 * Archived chats are visible only on the "Archive" tab, even if they have unread messages.
 */
fun filterChats(chats: List<Chat>, tab: ChatTab, query: String): List<Chat> {
    val needle = query.trim()
    return chats
        .filter { chat ->
            when (tab) {
                ChatTab.All -> !chat.archived
                ChatTab.Unread -> !chat.archived && chat.unreadCount > 0
                ChatTab.Archive -> chat.archived
            }
        }
        .filter { needle.isEmpty() || it.title.contains(needle, ignoreCase = true) }
        .sortedByDescending { it.lastActivityMillis }
}

/** Messages containing the query. An empty query keeps all messages. */
fun filterMessages(messages: List<Message>, query: String): List<Message> {
    val needle = query.trim()
    return if (needle.isEmpty()) messages else messages.filter { it.text.contains(needle, ignoreCase = true) }
}

/** What to write in the list when there is nothing to show. The text depends on why the list is empty. */
fun emptyListMessage(totalChats: Int, tab: ChatTab, query: String): String = when {
    totalChats == 0 -> "No chats yet.\nPress + to create your first one."
    query.isNotBlank() -> "Nothing found"
    tab == ChatTab.Unread -> "No unread chats"
    tab == ChatTab.Archive -> "Archive is empty"
    else -> "Nothing here yet"
}
