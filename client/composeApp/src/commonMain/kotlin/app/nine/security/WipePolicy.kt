package app.nine.security

/**
 * Erasing the data after a set number of wrong codes in a row. It is off unless the person turns it on.
 *
 * The limit is kept in the plaintext `vault.json` next to the failure counter, because the sign-in screen has to know it
 * before any code is entered. So it protects against someone using the app itself, not against someone with a copy of the files:
 * they simply do not run the app (see "Security" in the README).
 */
object WipePolicy {
    /** The stored value that means the feature is off. */
    const val Off = 0

    /** What the choices are: after this many wrong codes in a row everything is erased. */
    val Options = listOf(5, 10, 20)

    /** What turning the feature on selects first. */
    const val Default = 10

    fun isValid(limit: Int): Boolean = limit == Off || limit in Options

    /** How many wrong attempts are left before the erase, or null if the feature is off. */
    fun attemptsLeft(limit: Int, failures: Int): Int? = if (limit == Off) null else (limit - failures).coerceAtLeast(0)
}
