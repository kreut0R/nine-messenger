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
import app.nine.security.isAcceptableAccessCode
import app.nine.ui.components.ActionButton
import app.nine.ui.components.FormField
import app.nine.ui.components.NineIcons
import app.nine.ui.components.SegmentedTabs
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SecurityStep { Overview, ChangeCode }

/** The "Security" page: autolock and the access code. The rest of the list is shown as "Soon". */
@Composable
fun SecurityPage(
    settings: AppSettings,
    session: Session,
    time: TimeSource,
    onAutoLockChange: (AutoLock) -> Unit,
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
                SoonRow(
                    NineIcons.Trash,
                    "Wipe after failed attempts",
                    "Erases the data after a set number of wrong codes. Will be off by default.",
                )
                SoonRow(NineIcons.Alert, "Panic button", "Instantly locks the app and hides its window.")
            }
            SettingsDivider()
            Subsection("Screen") {
                SoonRow(NineIcons.NoScreen, "Block screenshots", "Phone only.")
            }
        }
        SecurityStep.ChangeCode -> SettingsPage("New access code") {
            ChangeCodeForm(session, time, onDone = { step = SecurityStep.Overview })
        }
    }
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

@Composable
private fun ChangeCodeForm(session: Session, time: TimeSource, onDone: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var newCode by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var changed by remember { mutableStateOf(false) }
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
                    is ChangeCodeResult.WrongCode -> refusal = "The current code is wrong. Attempts before a pause: ${result.attemptsBeforePause}."
                    is ChangeCodeResult.Paused ->
                        refusal = "Too many wrong attempts. Wait ${formatPause(result.untilMillis - time.nowMillis())}."
                    ChangeCodeResult.Damaged -> refusal = "The key file cannot be read."
                }
            }
        }
    }

    val type = NineTheme.typography
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
