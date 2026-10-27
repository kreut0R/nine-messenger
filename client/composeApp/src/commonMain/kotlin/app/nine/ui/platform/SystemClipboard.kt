package app.nine.ui.platform

import kotlinx.coroutines.delay

/** Буфер обмена системы. Нужен только для копирования из Nine и для очистки после него. */
interface SystemClipboard {
    fun copy(text: String)

    /** Текст из буфера. Null, если там пусто или лежит что-то другое. */
    fun text(): String?
}

/** Как долго скопированное из Nine лежит в буфере, если очистка включена. */
const val ClipboardForgetMillis = 30_000L

/**
 * Копирует [text] и через [forgetAfterMillis] убирает его из буфера, но только если там всё ещё он:
 * то, что человек успел скопировать позже, не трогаем.
 */
suspend fun SystemClipboard.copyTemporarily(text: String, forgetAfterMillis: Long = ClipboardForgetMillis) {
    copy(text)
    delay(forgetAfterMillis)
    if (this.text() == text) copy("")
}
