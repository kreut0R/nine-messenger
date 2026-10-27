package app.nine.presentation

/** How much longer to wait, as "m:ss". Rounds up so that "0:00" never appears on screen too early. */
fun formatPause(remainingMillis: Long): String {
    val seconds = ((remainingMillis.coerceAtLeast(0) + 999) / 1000)
    val minutes = seconds / 60
    return "$minutes:${(seconds % 60).toString().padStart(2, '0')}"
}
