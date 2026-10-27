package app.nine

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.nine.data.MessengerRepository
import app.nine.domain.TimeSource
import app.nine.security.Session
import app.nine.security.Vault
import app.nine.security.shouldAutoLock
import app.nine.ui.components.trackActivity
import app.nine.ui.lock.LockScreen
import app.nine.ui.platform.FilePicker
import app.nine.ui.platform.SystemClipboard
import app.nine.ui.theme.NineTheme
import kotlinx.coroutines.delay

/** Как часто проверяем, не пора ли заблокироваться. Достаточно часто, чтобы блокировка по сворачиванию была «сразу». */
private const val AutoLockCheckMillis = 500L

/**
 * Самый верхний уровень: пока аккаунт не открыт, показывает экран входа, после входа открывает мессенджер.
 * Репозиторий создаётся заново для каждой сессии, поэтому после блокировки в памяти не остаётся чужой переписки.
 *
 * @param windowHidden свёрнуто ли окно. Это функция, а не значение: у свёрнутого окна кадры могут не рисоваться,
 *   и пока ничего не перерисовывается, новое значение через параметр не дошло бы, а блокировка не сработала бы.
 */
@Composable
fun NineRoot(
    vault: Vault,
    filePicker: FilePicker,
    clipboard: SystemClipboard,
    time: TimeSource = TimeSource.System,
    windowHidden: () -> Boolean = { false },
) {
    var session by remember { mutableStateOf<Session?>(null) }
    val open = session
    if (open == null) {
        NineTheme { LockScreen(vault, filePicker, time, onOpened = { session = it }) }
        return
    }

    val repository = remember(open) { MessengerRepository(open.store, time) }
    val data by repository.data.collectAsState()
    var lastActivity by remember(open) { mutableLongStateOf(time.nowMillis()) }
    val lock = {
        open.lock()
        session = null
    }

    // Опрос по таймеру, а не по состоянию: блокировка по бездействию не связана ни с одним событием интерфейса.
    LaunchedEffect(open, data.settings.autoLock) {
        while (true) {
            if (shouldAutoLock(data.settings.autoLock, time.nowMillis() - lastActivity, windowHidden())) {
                lock()
                break
            }
            delay(AutoLockCheckMillis)
        }
    }

    Box(Modifier.fillMaxSize().trackActivity { lastActivity = time.nowMillis() }) {
        NineApp(
            repository = repository,
            session = open,
            filePicker = filePicker,
            clipboard = clipboard,
            onLock = lock,
            onErase = {
                open.lock()
                vault.wipe()
                session = null
            },
            time = time,
        )
    }
}
