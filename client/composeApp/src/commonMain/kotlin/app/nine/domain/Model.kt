package app.nine.domain

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

// Models are marked @Serializable so they can be stored in a file as they are, without a separate DTO layer.
// That is enough for the prototype. Once there is a server, the wire format will be described separately.

/** Chat identifier. A random string, not the peer's name: the name can change. */
@Serializable
@JvmInline
value class ChatId(val value: String)

/** Short chat code for showing to a person: the first 16 characters in groups of 4, for example "9F3A 41C2 B7D0 E615". */
val ChatId.displayCode: String
    get() = value.filter { it.isLetterOrDigit() }.uppercase().take(16).chunked(4).joinToString(" ")

enum class Author { Me, Peer }

/**
 * What is known about a message's delivery.
 * While there is no server, every message stays [Local]. Ticks in the UI are drawn only for [Sent] and [Read],
 * so that the app never promises a delivery that did not happen.
 */
enum class Delivery { Local, Sent, Read }

@Serializable
data class Message(
    val id: String,
    val author: Author,
    val text: String,
    /** Send time in milliseconds since 1970 (UTC). Converted to local time only when displayed. */
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

    /** Chats are sorted in the list by this time; for an empty chat it is the creation moment. */
    val lastActivityMillis: Long get() = lastMessage?.sentAtMillis ?: createdAtMillis
}

/** Color of own messages. The color itself is defined in the UI layer; only the choice is stored here. */
enum class BubbleColor { Forest, Emerald, Olive, Moss, Graphite }

/** When the app locks itself. The rules are in `security/AutoLockPolicy.kt`. */
enum class AutoLock { OnMinimize, OneMinute, FiveMinutes, FifteenMinutes, Never }

/** Settings the person changes. Stored in the same encrypted file as the conversations. */
@Serializable
data class AppSettings(
    val autoLock: AutoLock = AutoLock.FiveMinutes,
    /** Remove what was copied from Nine from the clipboard after half a minute. */
    val clearClipboard: Boolean = true,
)

/** Everything the app stores on the device. */
@Serializable
data class MessengerData(
    /** File format version. Needed when the format has to change and old data has to be migrated. */
    val version: Int = 1,
    val chats: List<Chat> = emptyList(),
    val bubbleColor: BubbleColor = BubbleColor.Forest,
    val settings: AppSettings = AppSettings(),
    /** The name peers will see the person under. Empty until they set one. */
    val displayName: String = "",
)

/** The longest name: anything longer does not fit in a list row or the header. */
const val MaxDisplayNameLength = 40
