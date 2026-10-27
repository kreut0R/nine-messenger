package app.nine.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LockoutTest {
    @Test
    fun firstAttemptsHaveNoPause() {
        for (failures in 0 until Lockout.FreeAttempts) assertEquals(0, Lockout.pauseAfter(failures))
    }

    @Test
    fun pauseStartsAtHalfAMinuteAndDoublesEachFailure() {
        assertEquals(30_000, Lockout.pauseAfter(5))
        assertEquals(60_000, Lockout.pauseAfter(6))
        assertEquals(120_000, Lockout.pauseAfter(7))
    }

    @Test
    fun pauseNeverExceedsAnHourEvenAfterManyFailures() {
        assertEquals(3_600_000, Lockout.pauseAfter(30))
        assertEquals(3_600_000, Lockout.pauseAfter(Int.MAX_VALUE / 2))
    }

    @Test
    fun shortCodesAreRejected() {
        assertFalse(isAcceptableAccessCode("1234567"))
        assertTrue(isAcceptableAccessCode("12345678"))
        assertTrue(isAcceptableAccessCode("лес и река"))
    }
}
