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

/** How often to check whether it is time to lock. Often enough for lock-on-minimize to feel "instant". */
private const val AutoLockCheckMillis = 500L

/**
 * The top level: while no account is open it shows the sign-in screen, after sign-in it opens the messenger.
 * The repository is created anew for each session, so after a lock no conversations remain in memory.
 *
 * @param windowHidden whether the window is minimized. It is a function, not a value: a minimized window may not draw frames,
 *   and while nothing is redrawn, a new value passed as a parameter would not arrive and the lock would not fire.
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

    // Polling on a timer, not on state: the idle lock is not tied to any UI event.
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
