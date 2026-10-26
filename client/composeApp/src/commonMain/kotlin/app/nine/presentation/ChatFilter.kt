package app.nine.presentation

import app.nine.domain.Chat
import app.nine.domain.Message

enum class ChatTab(val title: String) {
    All("All"),
    Unread("Unread"),
    Archive("Archive"),
}

/**
 * Выбирает чаты для вкладки и поиска и сортирует их: сверху те, где недавно писали.
 * Архивные чаты видны только во вкладке «Архив», даже если в них есть непрочитанные.
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

/** Сообщения, содержащие запрос. Пустой запрос оставляет все сообщения. */
fun filterMessages(messages: List<Message>, query: String): List<Message> {
    val needle = query.trim()
    return if (needle.isEmpty()) messages else messages.filter { it.text.contains(needle, ignoreCase = true) }
}

/** Что написать в списке, когда показывать нечего. Текст зависит от того, почему список пуст. */
fun emptyListMessage(totalChats: Int, tab: ChatTab, query: String): String = when {
    totalChats == 0 -> "No chats yet.\nClick “+” to start your first one."
    query.isNotBlank() -> "Nothing found"
    tab == ChatTab.Unread -> "No unread chats"
    tab == ChatTab.Archive -> "Archive is empty"
    else -> "Nothing here yet"
}
