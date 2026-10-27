package app.nine.ui.components

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PanicShortcutTest {
    private fun shortcut(
        key: Key = Key.L,
        down: Boolean = true,
        ctrl: Boolean = false,
        meta: Boolean = false,
        shift: Boolean = false,
    ) = isPanicShortcut(down, key, ctrl, meta, shift)

    @Test
    fun ctrlShiftLTriggersIt() {
        assertTrue(shortcut(ctrl = true, shift = true))
    }

    @Test
    fun cmdShiftLTriggersItOnAMac() {
        assertTrue(shortcut(meta = true, shift = true))
    }

    @Test
    fun plainTypingOfLDoesNot() {
        assertFalse(shortcut())
        assertFalse(shortcut(shift = true))
        assertFalse(shortcut(ctrl = true))
    }

    @Test
    fun otherKeysDoNot() {
        assertFalse(shortcut(key = Key.K, ctrl = true, shift = true))
    }

    @Test
    fun releasingTheKeyDoesNotTriggerItASecondTime() {
        assertFalse(shortcut(down = false, ctrl = true, shift = true))
    }
}
