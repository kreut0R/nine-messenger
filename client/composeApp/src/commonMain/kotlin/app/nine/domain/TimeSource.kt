package app.nine.domain

import kotlin.time.Clock

/** Источник текущего времени. Нужен, чтобы в тестах подставлять фиксированное время. */
fun interface TimeSource {
    fun nowMillis(): Long

    companion object {
        val System = TimeSource { Clock.System.now().toEpochMilliseconds() }
    }
}
