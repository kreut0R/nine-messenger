package app.nine.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.nine.domain.AppSettings
import app.nine.domain.AutoLock
import app.nine.domain.TimeSource
import app.nine.presentation.formatPause
import app.nine.security.ChangeCodeResult
import app.nine.security.MinAccessCodeLength
import app.nine.security.Session
import app.nine.security.WipePolicy
import app.nine.security.isAcceptableAccessCode
import app.nine.ui.components.ActionButton
import app.nine.ui.components.FormField
import app.nine.ui.components.NineIcons
import app.nine.ui.components.SegmentedTabs
import app.nine.ui.components.ToggleSwitch
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SecurityStep { Overview, ChangeCode }

/** The "Security" page: autolock, the access code, erasing after wrong codes and the panic button. The rest of the list is shown as "Soon". */
@Composable
fun SecurityPage(
    settings: AppSettings,
    session: Session,
    time: TimeSource,
    onAutoLockChange: (AutoLock) -> Unit,
    onPanicButtonChange: (Boolean) -> Unit,
    onWiped: () -> Unit,
) {
    var step by remember { mutableStateOf(SecurityStep.Overview) }
    when (step) {
        SecurityStep.Overview -> SettingsPage("Security") {
            Subsection("Sign-in") {
                FeatureRow(
                    icon = NineIcons.Clock,
                    title = "Autolock",
                    description = settings.autoLock.description,
                    tone = Tone.Blue,
                    below = {
                        Box(Modifier.widthIn(max = 460.dp)) {
                            SegmentedTabs(
                                titles = AutoLock.entries.map { it.title },
                                selectedIndex = settings.autoLock.ordinal,
                                onSelect = { onAutoLockChange(AutoLock.entries[it]) },
                            )
                        }
                    },
                )
                FeatureRow(
                    icon = NineIcons.Key,
                    title = "Access code",
                    description = "Opens the app. At least $MinAccessCodeLength characters, a phrase of several words is better.",
                    tone = Tone.Green,
                ) { RowButton("Change", onClick = { step = SecurityStep.ChangeCode }) }
            }
            SettingsDivider()
            Subsection("If you are forced to open the app") {
                SoonRow(NineIcons.Warning, "Duress code", "Opens a decoy account, while the real one stays hidden.")
                WipeAfterFailuresRow(session)
                FeatureRow(
                    icon = NineIcons.Alert,
                    title = "Panic button",
                    description = "Locks the app at once and minimizes the window. A red button appears in the chat list, " +
                        "and Ctrl+Shift+L does the same from anywhere in the app. Nothing is erased.",
                    tone = Tone.Amber,
                ) { ToggleSwitch(settings.panicButton, onPanicButtonChange) }
            }
            SettingsDivider()
            Subsection("Screen") {
                SoonRow(NineIcons.NoScreen, "Block screenshots", "Phone only.")
            }
        }
        SecurityStep.ChangeCode -> SettingsPage("New access code") {
            ChangeCodeForm(session, time, onDone = { step = SecurityStep.Overview }, onWiped = onWiped)
        }
    }
}

/**
 * The switch and the choice of the limit. The limit lives in the vault, not in the encrypted settings: the sign-in screen
 * has to know it before any code is entered.
 */
@Composable
private fun WipeAfterFailuresRow(session: Session) {
    var limit by remember(session) { mutableStateOf(session.wipeAfterFailures) }
    var problem by remember(session) { mutableStateOf<String?>(null) }
    val choose = { new: Int ->
        try {
            session.setWipeAfterFailures(new)
            limit = new
            problem = null
        } catch (error: IllegalStateException) {
            problem = "Could not save: the key file cannot be read."
        }
    }
    val on = limit != WipePolicy.Off
    FeatureRow(
        icon = NineIcons.Trash,
        title = "Wipe after failed attempts",
        description = if (on) {
            "Everything on this device is erased after $limit wrong codes in a row, at sign-in or when changing the code. " +
                "A key backup brings the account back, but not the conversations."
        } else {
            "Erases everything on this device after a set number of wrong codes in a row. Off."
        },
        tone = Tone.Red,
        below = if (on || problem != null) {
            {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (on) {
                        Box(Modifier.widthIn(max = 460.dp)) {
                            SegmentedTabs(
                                titles = WipePolicy.Options.map { "After $it" },
                                selectedIndex = WipePolicy.Options.indexOf(limit).coerceAtLeast(0),
                                onSelect = { choose(WipePolicy.Options[it]) },
                            )
                        }
                    }
                    problem?.let { BasicText(it, style = NineTheme.typography.chatPreview.copy(color = NineColors.Danger)) }
                }
            }
        } else {
            null
        },
    ) { ToggleSwitch(on, { turnedOn -> choose(if (turnedOn) WipePolicy.Default else WipePolicy.Off) }) }
}

private val AutoLock.title: String
    get() = when (this) {
        AutoLock.OnMinimize -> "Instantly"
        AutoLock.OneMinute -> "1 min"
        AutoLock.FiveMinutes -> "5 min"
        AutoLock.FifteenMinutes -> "15 min"
        AutoLock.Never -> "Never"
    }

private val AutoLock.description: String
    get() = when (this) {
        AutoLock.OnMinimize -> "The app locks as soon as the window is minimized."
        AutoLock.OneMinute -> "The app locks if nothing is done for a minute."
        AutoLock.FiveMinutes -> "The app locks if nothing is done for 5 minutes."
        AutoLock.FifteenMinutes -> "The app locks if nothing is done for 15 minutes."
        AutoLock.Never -> "Never locks by itself: access has to be closed manually from the profile menu."
    }

/** The warning added to a refusal when erasing after failures is on, or nothing when it is off. */
private fun wipeWarning(attemptsLeft: Int?): String =
    if (attemptsLeft == null) "" else " After $attemptsLeft more wrong ${if (attemptsLeft == 1) "code" else "codes"} everything on this device will be erased."

@Composable
private fun ChangeCodeForm(session: Session, time: TimeSource, onDone: () -> Unit, onWiped: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var newCode by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var changed by remember { mutableStateOf(false) }
    var wiped by remember { mutableStateOf(false) }
    var refusal by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val problem = when {
        !isAcceptableAccessCode(newCode) -> "The new code is shorter than $MinAccessCodeLength characters."
        newCode != repeat -> "The codes do not match."
        newCode == current -> "The new code is the same as the current one."
        else -> null
    }
    val canSubmit = current.isNotEmpty() && problem == null && !busy && !changed
    val submit = {
        if (canSubmit) {
            busy = true
            refusal = null
            scope.launch {
                // Argon2id is deliberately slow and is called twice, so not on the UI thread.
                val result = withContext(Dispatchers.Default) { session.changeAccessCode(current, newCode) }
                busy = false
                when (result) {
                    ChangeCodeResult.Changed -> {
                        changed = true
                        current = ""
                        newCode = ""
                        repeat = ""
                    }
                    is ChangeCodeResult.WrongCode -> refusal = "The current code is wrong. Attempts before a pause: ${result.attemptsBeforePause}." +
                        wipeWarning(result.attemptsBeforeWipe)
                    is ChangeCodeResult.Paused -> refusal = "Too many wrong attempts. Wait ${formatPause(result.untilMillis - time.nowMillis())}." +
                        wipeWarning(result.attemptsBeforeWipe)
                    ChangeCodeResult.Wiped -> wiped = true
                    ChangeCodeResult.Damaged -> refusal = "The key file cannot be read."
                }
            }
        }
    }

    val type = NineTheme.typography
    if (wiped) {
        Column(Modifier.widthIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BasicText(
                "Too many wrong codes. Everything on this device has been erased. " +
                    "A key backup brings the account back, but not the conversations.",
                style = type.chatPreview.copy(color = NineColors.Danger),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { ActionButton("OK", onClick = onWiped, primary = true) }
        }
        return
    }
    Column(Modifier.widthIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BasicText("The old code will stop opening the app. A forgotten code cannot be recovered.", style = type.chatPreview)
        FormField(current, { current = it }, "Current code", secret = true, filled = true)
        FormField(newCode, { newCode = it }, "New code", secret = true, filled = true)
        FormField(repeat, { repeat = it }, "Repeat the new code", secret = true, filled = true, onSubmit = submit)
        when {
            changed -> BasicText("Code changed.", style = type.chatPreview.copy(color = NineColors.Accent))
            refusal != null -> BasicText(refusal.orEmpty(), style = type.chatPreview.copy(color = NineColors.Danger))
            newCode.isNotEmpty() && repeat.isNotEmpty() && problem != null ->
                BasicText(problem, style = type.chatPreview.copy(color = NineColors.Danger))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
            ActionButton(if (changed) "Done" else "Back", onClick = onDone)
            ActionButton(if (busy) "Changing…" else "Change code", onClick = submit, primary = true, enabled = canSubmit)
        }
    }
}
