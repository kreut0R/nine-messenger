package app.nine.ui.components

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/** The keys that trigger the panic button: Ctrl+Shift+L (Cmd+Shift+L on a Mac). Holding the keys down does not repeat it. */
fun KeyEvent.isPanicShortcut(): Boolean =
    isPanicShortcut(type == KeyEventType.KeyDown, key, isCtrlPressed, isMetaPressed, isShiftPressed)

/** The rule itself, apart from the event, so that it can be tested without a window. */
internal fun isPanicShortcut(keyDown: Boolean, key: Key, ctrl: Boolean, meta: Boolean, shift: Boolean): Boolean =
    keyDown && key == Key.L && shift && (ctrl || meta)
