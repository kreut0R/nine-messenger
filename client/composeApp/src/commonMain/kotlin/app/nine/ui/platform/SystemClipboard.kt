package app.nine.ui.platform

import kotlinx.coroutines.delay

/** The system clipboard. Needed only for copying from Nine and for clearing afterwards. */
interface SystemClipboard {
    fun copy(text: String)

    /** The text from the clipboard. Null if it is empty or holds something else. */
    fun text(): String?
}

/** How long what was copied from Nine stays in the clipboard, if clearing is on. */
const val ClipboardForgetMillis = 30_000L

/**
 * Copies [text] and removes it from the clipboard after [forgetAfterMillis], but only if it is still there:
 * what the person managed to copy later is left alone.
 */
suspend fun SystemClipboard.copyTemporarily(text: String, forgetAfterMillis: Long = ClipboardForgetMillis) {
    copy(text)
    delay(forgetAfterMillis)
    if (this.text() == text) copy("")
}
