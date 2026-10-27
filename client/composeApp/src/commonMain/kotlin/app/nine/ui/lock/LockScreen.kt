package app.nine.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.nine.domain.TimeSource
import app.nine.presentation.formatPause
import app.nine.security.MinAccessCodeLength
import app.nine.security.RestoreResult
import app.nine.security.Session
import app.nine.security.UnlockResult
import app.nine.security.Vault
import app.nine.security.isAcceptableAccessCode
import app.nine.ui.components.ActionButton
import app.nine.ui.components.AnonymityMask
import app.nine.ui.components.FormField
import app.nine.ui.components.GlassPanel
import app.nine.ui.platform.FilePicker
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Stage { Welcome, CreateCode, Unlock, Restore }

/**
 * Экран до входа в приложение: создание аккаунта, ввод кода доступа или восстановление из копии ключа.
 * Какой экран показать первым, решает наличие аккаунта на устройстве. Проверка кода идёт на фоновом потоке:
 * Argon2id намеренно медленный, и окно не должно зависать.
 */
@Composable
fun LockScreen(vault: Vault, filePicker: FilePicker, time: TimeSource, onOpened: (Session) -> Unit) {
    var stage by remember { mutableStateOf(if (vault.hasAccount()) Stage.Unlock else Stage.Welcome) }
    val type = NineTheme.typography

    Box(Modifier.fillMaxSize().background(NineColors.Background), contentAlignment = Alignment.Center) {
        GlassPanel(Modifier.width(440.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnonymityMask(Modifier.width(150.dp))
                BasicText("Nine", style = type.screenTitle)
                when (stage) {
                    Stage.Welcome -> Welcome(
                        onCreate = { stage = Stage.CreateCode },
                        onRestore = { stage = Stage.Restore },
                    )
                    Stage.CreateCode -> CreateCode(vault, onOpened, onBack = { stage = Stage.Welcome })
                    Stage.Unlock -> Unlock(vault, time, onOpened, onForgot = { stage = Stage.Restore })
                    Stage.Restore -> Restore(
                        vault, filePicker, onOpened,
                        onBack = { stage = if (vault.hasAccount()) Stage.Unlock else Stage.Welcome },
                    )
                }
            }
        }
    }
}

@Composable
private fun Welcome(onCreate: () -> Unit, onRestore: () -> Unit) {
    Hint("Аккаунт создаётся на этом устройстве. Телефон и почта не нужны.")
    ActionButton("Создать аккаунт", onClick = onCreate, primary = true)
    ActionButton("Восстановить из копии ключа", onClick = onRestore)
}

@Composable
private fun CreateCode(vault: Vault, onOpened: (Session) -> Unit, onBack: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val problem = codeProblem(code, repeat)
    val submit = {
        if (problem == null && !busy) {
            busy = true
            scope.launch {
                val session = withContext(Dispatchers.Default) { vault.createAccount(code) }
                onOpened(session)
            }
        }
    }

    Hint(
        "Придумайте код доступа. Он открывает приложение на этом устройстве. " +
            "Лучше фраза из нескольких слов. Забытый код восстановить нельзя, останется только копия ключа.",
    )
    FormField(code, { code = it }, "Код доступа", Modifier.focusRequester(focus), secret = true)
    FormField(repeat, { repeat = it }, "Повторите код", secret = true, onSubmit = submit)
    ProblemText(if (code.isEmpty() || repeat.isEmpty()) null else problem)
    ButtonsRow(onBack, if (busy) "Создаём…" else "Создать", enabled = problem == null && !busy, onClick = submit)
}

@Composable
private fun Unlock(vault: Vault, time: TimeSource, onOpened: (Session) -> Unit, onForgot: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pausedUntil by remember { mutableStateOf(0L) }
    var now by remember { mutableStateOf(time.nowMillis()) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    // Пока идёт пауза, обратный отсчёт тикает на экране, а по её окончании сообщение пропадает.
    LaunchedEffect(pausedUntil) {
        while (time.nowMillis() < pausedUntil) {
            now = time.nowMillis()
            delay(250)
        }
        if (pausedUntil > 0) message = null
        pausedUntil = 0
    }

    val paused = pausedUntil > now
    val submit = {
        if (code.isNotEmpty() && !busy && !paused) {
            busy = true
            scope.launch {
                val result = withContext(Dispatchers.Default) { vault.unlock(code) }
                busy = false
                code = ""
                when (result) {
                    is UnlockResult.Unlocked -> onOpened(result.session)
                    is UnlockResult.WrongCode -> message = "Код неверный. Попыток до паузы: ${result.attemptsBeforePause}."
                    is UnlockResult.Paused -> {
                        message = null
                        pausedUntil = result.untilMillis
                    }
                    UnlockResult.Damaged -> message = "Файл с ключами повреждён. Остаётся восстановить аккаунт из копии ключа."
                }
            }
        }
    }

    Hint("Введите код доступа.")
    FormField(code, { code = it }, "Код доступа", Modifier.focusRequester(focus), secret = true, onSubmit = submit)
    ProblemText(if (paused) "Слишком много попыток. Подождите ${formatPause(pausedUntil - now)}." else message)
    ActionButton("Открыть", onClick = submit, primary = true, enabled = code.isNotEmpty() && !busy && !paused)
    ActionButton("Забыли код? Восстановить из копии ключа", onClick = onForgot)
}

@Composable
private fun Restore(vault: Vault, filePicker: FilePicker, onOpened: (Session) -> Unit, onBack: () -> Unit) {
    var backup by remember { mutableStateOf<ByteArray?>(null) }
    var backupPassword by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val problem = codeProblem(code, repeat)
    val ready = backup != null && backupPassword.isNotEmpty() && problem == null && !busy
    val submit = {
        val file = backup
        if (ready && file != null) {
            busy = true
            message = null
            scope.launch {
                val result = withContext(Dispatchers.Default) { vault.restoreAccount(file, backupPassword, code) }
                busy = false
                when (result) {
                    is RestoreResult.Restored -> onOpened(result.session)
                    RestoreResult.WrongBackupPassword -> message = "Пароль копии неверный."
                    RestoreResult.NotABackup -> message = "Этот файл не похож на копию ключа."
                }
            }
        }
    }

    Hint(
        if (vault.hasAccount()) {
            "Аккаунт вернётся из копии ключа, а на этом устройстве задайте новый код. Переписка, которая сейчас на устройстве, будет стёрта."
        } else {
            "Выберите файл копии ключа, введите его пароль и задайте код доступа для этого устройства."
        },
    )
    ActionButton(
        if (backup == null) "Выбрать файл копии" else "Файл выбран, выбрать другой",
        onClick = { filePicker.open()?.let { backup = it; message = null } },
    )
    FormField(backupPassword, { backupPassword = it }, "Пароль копии", secret = true)
    FormField(code, { code = it }, "Новый код доступа", secret = true)
    FormField(repeat, { repeat = it }, "Повторите код", secret = true, onSubmit = submit)
    ProblemText(message ?: if (code.isEmpty() || repeat.isEmpty()) null else problem)
    ButtonsRow(onBack, if (busy) "Восстанавливаем…" else "Восстановить", enabled = ready, onClick = submit)
}

/** Что не так с введённым кодом, или null, если всё хорошо. */
private fun codeProblem(code: String, repeat: String): String? = when {
    !isAcceptableAccessCode(code) -> "Код короче $MinAccessCodeLength символов."
    code != repeat -> "Коды не совпадают."
    else -> null
}

@Composable
private fun Hint(text: String) {
    BasicText(text, style = NineTheme.typography.chatPreview.copy(textAlign = TextAlign.Center))
}

@Composable
private fun ProblemText(text: String?) {
    if (text != null) {
        BasicText(text, style = NineTheme.typography.chatPreview.copy(color = NineColors.Danger, textAlign = TextAlign.Center))
    }
}

@Composable
private fun ButtonsRow(onBack: () -> Unit, primaryText: String, enabled: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
        ActionButton("Назад", onClick = onBack)
        ActionButton(primaryText, onClick = onClick, primary = true, enabled = enabled)
    }
}
