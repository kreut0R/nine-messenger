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

        clipboard.copyTemporarily("код аккаунта", forgetAfterMillis = 5)

        assertNull(clipboard.text())
    }

    @Test
    fun somethingCopiedLaterIsLeftAlone() = runBlocking {
        val clipboard = MemoryClipboard()
        val other = object : SystemClipboard by clipboard {
            override fun text(): String? = "чужое"
        }

        other.copyTemporarily("код аккаунта", forgetAfterMillis = 5)

        // Очистка не должна стирать то, что человек скопировал уже не из Nine.
        assertEquals("код аккаунта", clipboard.current)
    }
}
