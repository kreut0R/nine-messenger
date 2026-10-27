package app.nine.security

import app.nine.domain.AutoLock

/** Сколько можно бездействовать до блокировки. Null, если время не при чём: блокировка по сворачиванию или её нет. */
val AutoLock.idleLimitMillis: Long?
    get() = when (this) {
        AutoLock.OnMinimize, AutoLock.Never -> null
        AutoLock.OneMinute -> 60_000L
        AutoLock.FiveMinutes -> 5 * 60_000L
        AutoLock.FifteenMinutes -> 15 * 60_000L
    }

/**
 * Пора ли блокировать приложение.
 * @param idleMillis сколько прошло с последнего действия мышью или клавиатурой
 * @param hidden окно свёрнуто или приложение ушло в фон
 */
fun shouldAutoLock(setting: AutoLock, idleMillis: Long, hidden: Boolean): Boolean {
    if (setting == AutoLock.OnMinimize) return hidden
    val limit = setting.idleLimitMillis ?: return false
    return idleMillis >= limit
}
