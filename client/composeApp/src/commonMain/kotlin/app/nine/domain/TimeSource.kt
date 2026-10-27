package app.nine.domain

import kotlin.time.Clock

/** Source of the current time. Lets tests substitute a fixed time. */
fun interface TimeSource {
    fun nowMillis(): Long

    companion object {
        val System = TimeSource { Clock.System.now().toEpochMilliseconds() }
    }
}
