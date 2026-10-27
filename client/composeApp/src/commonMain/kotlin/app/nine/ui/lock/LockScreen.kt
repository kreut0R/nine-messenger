package app.nine.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nine.domain.TimeSource
import app.nine.presentation.formatPause
import app.nine.security.MinAccessCodeLength
import app.nine.security.RestoreResult
import app.nine.security.Session
import app.nine.security.UnlockResult
import app.nine.security.Vault
import app.nine.security.isAcceptableAccessCode
import app.nine.ui.components.FormField
import app.nine.ui.components.GlassPanel
import app.nine.ui.components.NineLogo
import app.nine.ui.components.WideButton
import app.nine.ui.platform.FilePicker
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Stage { Create, Unlock, Restore }

/**
 * The screen before entering the app: creating an account, entering the access code or restoring from a key backup.
 * Which screen to show first is decided by whether the device has an account. The code check runs on a background thread:
 * Argon2id is deliberately slow, and the window must not freeze.
 */
@Composable
fun LockScreen(vault: Vault, filePicker: FilePicker, time: TimeSource, onOpened: (Session) -> Unit) {
    var stage by remember { mutableStateOf(if (vault.hasAccount()) Stage.Unlock else Stage.Create) }
    var notice by remember { mutableStateOf<String?>(null) }
    val backStage = if (vault.hasAccount()) Stage.Unlock else Stage.Create

    Box(Modifier.fillMaxSize().background(NineColors.Background), contentAlignment = Alignment.Center) {
        GlassPanel(Modifier.fillMaxWidth(0.88f).widthIn(max = 1040.dp).fillMaxHeight(0.88f).heightIn(min = 520.dp, max = 680.dp)) {
            Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AuthArt(
                    eyebrow = "No phone, no email",
                    headline = "Conversations only you can read",
                    modifier = Modifier.weight(0.9f).fillMaxHeight(),
                )
                Box(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.widthIn(max = 340.dp).fillMaxWidth().padding(horizontal = 12.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        when (stage) {
                            Stage.Create -> CreateCode(vault, notice, onOpened, onRestore = { stage = Stage.Restore })
                            Stage.Unlock -> Unlock(
                                vault, time, onOpened,
                                onForgot = { stage = Stage.Restore },
                                onWiped = {
                                    notice = "Everything on this device was erased after too many wrong codes."
                                    stage = Stage.Create
                                },
                            )
                            Stage.Restore -> Restore(vault, filePicker, onOpened, onBack = { stage = backStage })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Heading(title: String, subtitle: String) {
    val type = NineTheme.typography
    NineLogo(NineColors.TextPrimary, size = 26.dp)
    BasicText(title, style = type.screenTitle.copy(fontSize = 28.sp, lineHeight = 34.sp))
    BasicText(subtitle, style = type.chatPreview)
}

@Composable
private fun CreateCode(vault: Vault, notice: String?, onOpened: (Session) -> Unit, onRestore: () -> Unit) {
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

    Heading(
        "Create an account",
        "It is created on this device, no phone number or email needed. The access code opens the app. " +
            "A phrase of several words is better: a forgotten code cannot be recovered.",
    )
    ProblemText(notice)
    FormField(code, { code = it }, "Choose a code", Modifier.focusRequester(focus), label = "Access code", secret = true)
    FormField(repeat, { repeat = it }, "Repeat the code", label = "Repeat the code", secret = true, onSubmit = submit)
    ProblemText(if (code.isEmpty() || repeat.isEmpty()) null else problem)
    WideButton(if (busy) "Creating…" else "Get started", onClick = submit, enabled = problem == null && !busy)
    OrDivider()
    WideButton("Restore from a key backup", onClick = onRestore, primary = false)
}

@Composable
private fun Unlock(vault: Vault, time: TimeSource, onOpened: (Session) -> Unit, onForgot: () -> Unit, onWiped: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var wipeNote by remember { mutableStateOf<String?>(null) }
    var pausedUntil by remember { mutableStateOf(0L) }
    var now by remember { mutableStateOf(time.nowMillis()) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    // While a pause lasts, the countdown ticks on screen, and when it ends the message disappears.
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
                    is UnlockResult.WrongCode -> {
                        message = "Wrong code. Attempts before a pause: ${result.attemptsBeforePause}."
                        wipeNote = wipeWarning(result.attemptsBeforeWipe)
                    }
                    is UnlockResult.Paused -> {
                        message = null
                        pausedUntil = result.untilMillis
                        wipeNote = wipeWarning(result.attemptsBeforeWipe)
                    }
                    UnlockResult.Wiped -> onWiped()
                    UnlockResult.Damaged -> message = "The key file is damaged. The account can still be restored from a key backup."
                }
            }
        }
    }

    Heading("Welcome back", "Enter the access code to open your conversations.")
    FormField(code, { code = it }, "Access code", Modifier.focusRequester(focus), label = "Access code", secret = true, onSubmit = submit)
    ProblemText(if (paused) "Too many attempts. Wait ${formatPause(pausedUntil - now)}." else message)
    ProblemText(wipeNote)
    WideButton(if (busy) "Checking…" else "Sign in", onClick = submit, enabled = code.isNotEmpty() && !busy && !paused)
    OrDivider()
    WideButton("Restore from a key backup", onClick = onForgot, primary = false)
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
                    RestoreResult.WrongBackupPassword -> message = "The backup password is wrong."
                    RestoreResult.NotABackup -> message = "This file does not look like a key backup."
                }
            }
        }
    }

    Heading(
        "Restore",
        if (vault.hasAccount()) {
            "The account will come back from the key backup, and you set a new code on this device. The conversations currently on this device will be erased."
        } else {
            "Choose the key backup file, enter its password and set an access code for this device."
        },
    )
    WideButton(
        if (backup == null) "Choose backup file" else "File chosen, choose another",
        onClick = { filePicker.open()?.let { backup = it; message = null } },
        primary = false,
    )
    FormField(backupPassword, { backupPassword = it }, "Backup password", label = "Backup password", secret = true)
    FormField(code, { code = it }, "New code", label = "New access code", secret = true)
    FormField(repeat, { repeat = it }, "Repeat the code", label = "Repeat the code", secret = true, onSubmit = submit)
    ProblemText(message ?: if (code.isEmpty() || repeat.isEmpty()) null else problem)
    WideButton(if (busy) "Restoring…" else "Restore", onClick = submit, enabled = ready)
    WideButton("Back", onClick = onBack, primary = false)
}

/** The warning under a wrong code when erasing after failures is on, or null when it is off. */
private fun wipeWarning(attemptsLeft: Int?): String? = attemptsLeft?.let {
    "After $it more wrong ${if (it == 1) "code" else "codes"} everything on this device will be erased."
}

/** What is wrong with the entered code, or null if all is well. */
private fun codeProblem(code: String, repeat: String): String? = when {
    !isAcceptableAccessCode(code) -> "The code is shorter than $MinAccessCodeLength characters."
    code != repeat -> "The codes do not match."
    else -> null
}

@Composable
private fun ProblemText(text: String?) {
    if (text != null) {
        BasicText(text, style = NineTheme.typography.chatPreview.copy(color = NineColors.Danger))
    }
}

/** A thin line with the word "or" in the middle: separates the main action from the fallback. */
@Composable
private fun OrDivider() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).height(1.dp).background(NineColors.Hairline))
        BasicText("or", style = NineTheme.typography.time)
        Box(Modifier.weight(1f).height(1.dp).background(NineColors.Hairline))
    }
}
