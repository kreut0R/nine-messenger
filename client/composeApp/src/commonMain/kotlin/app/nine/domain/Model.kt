package app.nine.domain

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

// Модели помечены @Serializable, чтобы хранить их в файле как есть, без отдельного слоя DTO.
// Для прототипа этого достаточно. Когда появится сервер, формат передачи по сети будет описан отдельно.

/** Идентификатор чата. Это случайная строка, а не имя собеседника: имя можно будет менять. */
@Serializable
@JvmInline
value class ChatId(val value: String)

/** Короткий код чата для показа человеку: первые 16 символов группами по 4, например «9F3A 41C2 B7D0 E615». */
val ChatId.displayCode: String
    get() = value.filter { it.isLetterOrDigit() }.uppercase().take(16).chunked(4).joinToString(" ")

enum class Author { Me, Peer }

/**
 * Что известно о доставке сообщения.
 * Пока сервера нет, все сообщения остаются [Local]. Галочки в интерфейсе рисуются только для [Sent] и [Read],
 * чтобы не обещать доставку, которой не было.
 */
enum class Delivery { Local, Sent, Read }

@Serializable
data class Message(
    val id: String,
    val author: Author,
    val text: String,
    /** Время отправки в миллисекундах от 1970 года (UTC). В местное время переводится только при показе. */
    val sentAtMillis: Long,
    val delivery: Delivery = Delivery.Local,
)

@Serializable
data class Chat(
    val id: ChatId,
    val title: String,
    val createdAtMillis: Long,
    val archived: Boolean = false,
    val unreadCount: Int = 0,
    val messages: List<Message> = emptyList(),
) {
    val lastMessage: Message? get() = messages.lastOrNull()

    /** По этому времени чаты сортируются в списке: у пустого чата это момент создания. */
    val lastActivityMillis: Long get() = lastMessage?.sentAtMillis ?: createdAtMillis
}

/** Цвет собственных сообщений. Сам цвет задаётся в слое интерфейса, здесь хранится только выбор. */
enum class BubbleColor { Forest, Emerald, Olive, Moss, Graphite }

/** Всё, что приложение хранит на устройстве. */
@Serializable
data class MessengerData(
    /** Версия формата файла. Понадобится, когда формат придётся менять и переносить старые данные. */
    val version: Int = 1,
    val chats: List<Chat> = emptyList(),
    val bubbleColor: BubbleColor = BubbleColor.Forest,
)
