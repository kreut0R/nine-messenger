package app.nine.data

import java.io.File

/**
 * The folder where the app keeps its data, following the rules of the current system:
 * Windows: %APPDATA%\Nine, macOS: ~/Library/Application Support/Nine, others: $XDG_DATA_HOME/nine or ~/.local/share/nine.
 */
fun appDataDirectory(): File {
    val home = System.getProperty("user.home")
    val os = System.getProperty("os.name").lowercase()
    return when {
        "win" in os -> File(System.getenv("APPDATA") ?: "$home\\AppData\\Roaming", "Nine")
        "mac" in os -> File(home, "Library/Application Support/Nine")
        else -> File(System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() } ?: "$home/.local/share", "nine")
    }
}
