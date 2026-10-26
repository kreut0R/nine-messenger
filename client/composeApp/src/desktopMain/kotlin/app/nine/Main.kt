package app.nine

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.nine.data.FileMessengerStore
import app.nine.data.MessengerRepository
import app.nine.data.appDataDirectory
import java.awt.Dimension

/** Точка входа настольного приложения: собирает хранилище, репозиторий и открывает окно. */
fun main() = application {
    val repository = remember { MessengerRepository(FileMessengerStore(appDataDirectory())) }
    Window(
        onCloseRequest = ::exitApplication,
        title = "Nine",
        state = rememberWindowState(size = DpSize(1440.dp, 810.dp)),
    ) {
        // Ниже этих размеров три колонки уже не помещаются.
        window.minimumSize = Dimension(900, 600)
        NineApp(repository)
    }
}
