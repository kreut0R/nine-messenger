package app.nine.ui.platform

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SystemClipboardTest {
    private class MemoryClipboard : SystemClipboard {
        var current: String? = null

        override fun copy(text: String) {
            current = text.ifEmpty { null }
        }

        override fun text(): String? = current
    }

    @Test
    fun copiedTextIsRemovedAfterTheDelay() = runBlocking {
        val clipboard = MemoryClipboard()

        clipboard.copyTemporarily("account code", forgetAfterMillis = 5)

        assertNull(clipboard.text())
    }

    @Test
    fun somethingCopiedLaterIsLeftAlone() = runBlocking {
        val clipboard = MemoryClipboard()
        val other = object : SystemClipboard by clipboard {
            override fun text(): String? = "someone else's"
        }

        other.copyTemporarily("account code", forgetAfterMillis = 5)

        // Clearing must not erase what the person copied from somewhere other than Nine.
        assertEquals("account code", clipboard.current)
    }
}
