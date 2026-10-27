package app.nine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.nine.data.MessengerRepository
import app.nine.domain.TimeSource
import app.nine.security.Session
import app.nine.security.Vault
import app.nine.ui.lock.LockScreen
import app.nine.ui.platform.FilePicker
import app.nine.ui.theme.NineTheme

/**
 * Самый верхний уровень: пока аккаунт не открыт, показывает экран входа, после входа открывает мессенджер.
 * Репозиторий создаётся заново для каждой сессии, поэтому после блокировки в памяти не остаётся чужой переписки.
 */
@Composable
fun NineRoot(vault: Vault, filePicker: FilePicker, time: TimeSource = TimeSource.System) {
    var session by remember { mutableStateOf<Session?>(null) }
    val open = session
    if (open == null) {
        NineTheme { LockScreen(vault, filePicker, time, onOpened = { session = it }) }
    } else {
        val repository = remember(open) { MessengerRepository(open.store, time) }
        NineApp(
            repository = repository,
            session = open,
            filePicker = filePicker,
            onLock = {
                open.lock()
                session = null
            },
            onErase = {
                open.lock()
                vault.wipe()
                session = null
            },
            time = time,
        )
    }
}
