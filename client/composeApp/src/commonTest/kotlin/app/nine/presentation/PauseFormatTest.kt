package app.nine.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class PauseFormatTest {
    @Test
    fun showsMinutesAndPaddedSeconds() {
        assertEquals("0:30", formatPause(30_000))
        assertEquals("1:05", formatPause(65_000))
        assertEquals("60:00", formatPause(3_600_000))
    }

    @Test
    fun roundsUpSoZeroAppearsOnlyWhenTheWaitIsOver() {
        assertEquals("0:01", formatPause(1))
        assertEquals("0:00", formatPause(0))
        assertEquals("0:00", formatPause(-500))
    }
}
