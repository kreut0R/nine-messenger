package app.nine.security

/**
 * The access code opens the app. It can be a long PIN or a phrase.
 * The key is derived from the code with the slow Argon2id function, but if an attacker has a copy of the files, a short code
 * is brute-forced quickly: hence the eight-character minimum, and a phrase of several words is better.
 */
const val MinAccessCodeLength = 8

fun isAcceptableAccessCode(code: String): Boolean = code.length >= MinAccessCodeLength

/**
 * Protection against guessing the code inside the app itself: a few attempts without pauses, then the pause doubles
 * after every failure, up to one hour. This stops a person who got hold of the phone for a minute.
 * Guessing against a copy of the files is stopped not by this but by the cost of Argon2id and the length of the code.
 */
object Lockout {
    const val FreeAttempts = 5
    private const val FirstPauseMillis = 30_000L
    private const val MaxPauseMillis = 60 * 60 * 1000L

    /** How long to wait after [failures] failed attempts in a row. */
    fun pauseAfter(failures: Int): Long {
        if (failures < FreeAttempts) return 0
        val doublings = (failures - FreeAttempts).coerceAtMost(20)
        return minOf(FirstPauseMillis shl doublings, MaxPauseMillis)
    }
}
