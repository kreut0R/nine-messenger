package app.nine

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.nine.data.appDataDirectory
import app.nine.security.FileVault
import java.awt.Dimension

/** Entry point of the desktop app: builds the key storage and opens the window. */
fun main() = application {
    val vault = remember { FileVault(appDataDirectory()) }
    val filePicker = remember { AwtFilePicker() }
    val clipboard = remember { AwtSystemClipboard() }
    val windowState = rememberWindowState(size = DpSize(1440.dp, 810.dp))
    Window(
        onCloseRequest = ::exitApplication,
        title = "Nine",
        state = windowState,
    ) {
        // Below these sizes the three columns no longer fit.
        window.minimumSize = Dimension(900, 600)
        NineRoot(vault, filePicker, clipboard, windowHidden = { windowState.isMinimized })
    }
}
