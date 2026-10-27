package app.nine.security

/**
 * Код доступа открывает приложение. Это может быть длинный PIN или фраза.
 * Ключ из кода получают медленной функцией Argon2id, но если у злоумышленника есть копия файлов, короткий код
 * перебирается быстро: поэтому минимум восемь символов, а лучше фраза из нескольких слов.
 */
const val MinAccessCodeLength = 8

fun isAcceptableAccessCode(code: String): Boolean = code.length >= MinAccessCodeLength

/**
 * Защита от подбора кода в самом приложении: несколько попыток без пауз, дальше пауза растёт вдвое
 * после каждой неудачи, но не больше часа. Это останавливает человека, взявшего телефон на минуту.
 * От подбора по копии файлов защищает не она, а стоимость Argon2id и длина кода.
 */
object Lockout {
    const val FreeAttempts = 5
    private const val FirstPauseMillis = 30_000L
    private const val MaxPauseMillis = 60 * 60 * 1000L

    /** Сколько ждать после [failures] неудачных попыток подряд. */
    fun pauseAfter(failures: Int): Long {
        if (failures < FreeAttempts) return 0
        val doublings = (failures - FreeAttempts).coerceAtMost(20)
        return minOf(FirstPauseMillis shl doublings, MaxPauseMillis)
    }
}
