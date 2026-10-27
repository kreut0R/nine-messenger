package app.nine.security

import app.nine.domain.AutoLock

/** How long the app may sit idle before locking. Null if time does not matter: lock on minimize, or no lock. */
val AutoLock.idleLimitMillis: Long?
    get() = when (this) {
        AutoLock.OnMinimize, AutoLock.Never -> null
        AutoLock.OneMinute -> 60_000L
        AutoLock.FiveMinutes -> 5 * 60_000L
        AutoLock.FifteenMinutes -> 15 * 60_000L
    }

/**
 * Whether it is time to lock the app.
 * @param idleMillis how long since the last mouse or keyboard action
 * @param hidden the window is minimized or the app went to the background
 */
fun shouldAutoLock(setting: AutoLock, idleMillis: Long, hidden: Boolean): Boolean {
    if (setting == AutoLock.OnMinimize) return hidden
    val limit = setting.idleLimitMillis ?: return false
    return idleMillis >= limit
}
