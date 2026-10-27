package app.nine.presentation

/** Сколько ещё ждать, в виде «м:сс». Округляет вверх, чтобы на экране не появлялось «0:00» раньше времени. */
fun formatPause(remainingMillis: Long): String {
    val seconds = ((remainingMillis.coerceAtLeast(0) + 999) / 1000)
    val minutes = seconds / 60
    return "$minutes:${(seconds % 60).toString().padStart(2, '0')}"
}
