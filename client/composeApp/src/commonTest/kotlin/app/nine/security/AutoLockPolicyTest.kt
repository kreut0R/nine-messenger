package app.nine.security

import app.nine.domain.AutoLock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutoLockPolicyTest {
    @Test
    fun lockingOnMinimizeIgnoresIdleTime() {
        assertTrue(shouldAutoLock(AutoLock.OnMinimize, idleMillis = 0, hidden = true))
        assertFalse(shouldAutoLock(AutoLock.OnMinimize, idleMillis = 24 * 60 * 60_000L, hidden = false))
    }

    @Test
    fun timedSettingsLockExactlyWhenTheLimitIsReached() {
        assertFalse(shouldAutoLock(AutoLock.OneMinute, idleMillis = 59_999, hidden = false))
        assertTrue(shouldAutoLock(AutoLock.OneMinute, idleMillis = 60_000, hidden = false))
        assertFalse(shouldAutoLock(AutoLock.FiveMinutes, idleMillis = 299_999, hidden = false))
        assertTrue(shouldAutoLock(AutoLock.FiveMinutes, idleMillis = 300_000, hidden = false))
        assertFalse(shouldAutoLock(AutoLock.FifteenMinutes, idleMillis = 899_999, hidden = false))
        assertTrue(shouldAutoLock(AutoLock.FifteenMinutes, idleMillis = 900_000, hidden = false))
    }

    @Test
    fun hidingTheWindowDoesNotLockWhenOnlyIdleTimeIsConfigured() {
        assertFalse(shouldAutoLock(AutoLock.FiveMinutes, idleMillis = 1_000, hidden = true))
    }

    @Test
    fun neverMeansNever() {
        assertFalse(shouldAutoLock(AutoLock.Never, idleMillis = Long.MAX_VALUE, hidden = true))
        assertNull(AutoLock.Never.idleLimitMillis)
        assertEquals(60_000L, AutoLock.OneMinute.idleLimitMillis)
    }
}
