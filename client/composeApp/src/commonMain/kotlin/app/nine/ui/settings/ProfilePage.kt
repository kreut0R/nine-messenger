package app.nine.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.nine.domain.MaxDisplayNameLength
import app.nine.domain.displayCode
import app.nine.security.MinAccessCodeLength
import app.nine.security.Session
import app.nine.security.isAcceptableAccessCode
import app.nine.ui.components.ActionButton
import app.nine.ui.components.Avatar
import app.nine.ui.components.FormField
import app.nine.ui.components.NineIcon
import app.nine.ui.components.NineIcons
import app.nine.ui.platform.FilePicker
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ProfileStep { Overview, Backup, ConfirmErase }

/** The "Profile and sign-in" page: the name, the account code, the key backup and signing out with erasure. */
@Composable
fun ProfilePage(
    displayName: String,
    session: Session,
    filePicker: FilePicker,
    onDisplayNameChange: (String) -> Unit,
    onCopy: (String) -> Unit,
    onErase: () -> Unit,
) {
    var step by remember { mutableStateOf(ProfileStep.Overview) }
    when (step) {
        ProfileStep.Overview -> Overview(
            displayName,
            session,
            onDisplayNameChange,
            onCopy,
            onBackup = { step = ProfileStep.Backup },
            onErase = { step = ProfileStep.ConfirmErase },
        )
        ProfileStep.Backup -> SettingsPage("Key backup") {
            Backup(session, filePicker, onDone = { step = ProfileStep.Overview })
        }
        ProfileStep.ConfirmErase -> SettingsPage("Erase data?") {
            EraseConfirmation(onErase, onCancel = { step = ProfileStep.Overview })
        }
    }
}

@Composable
private fun Overview(
    displayName: String,
    session: Session,
    onDisplayNameChange: (String) -> Unit,
    onCopy: (String) -> Unit,
    onBackup: () -> Unit,
    onErase: () -> Unit,
) {
    val code = session.accountId.displayCode
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(2_000)
            copied = false
        }
    }

    SettingsPage("Profile and sign-in") {
        Subsection("Profile") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (displayName.isBlank()) {
                    Box(Modifier.size(72.dp).clip(CircleShape).background(NineColors.Control), contentAlignment = Alignment.Center) {
                        NineIcon(NineIcons.Person, tint = NineColors.TextSecondary, size = 32.dp)
                    }
                } else {
                    Avatar(displayName, seed = code, size = 72.dp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BasicText("Avatar", style = NineTheme.typography.featureTitle)
                    BasicText("A photo from a file. Until then, the first letter of the name is used.", style = NineTheme.typography.chatPreview)
                }
                SoonBadge()
            }
            Spacer(Modifier.height(10.dp))
            NameField(displayName, onDisplayNameChange)
            Spacer(Modifier.height(10.dp))
            ReadOnlyField(
                label = "Account code",
                helper = "A contact can compare it with you by voice to make sure they are writing to you.",
                value = code,
            ) {
                RowButton(if (copied) "Copied" else "Copy", onClick = {
                    onCopy(code)
                    copied = true
                })
            }
        }
        SettingsDivider()
        Subsection("Key and sign-in") {
            FeatureRow(
                icon = NineIcons.Key,
                title = "Key backup",
                description = "A file with your key, locked with a separate password. Without it the account cannot be restored if the device is lost.",
                tone = Tone.Amber,
            ) { RowButton("Save", onClick = onBackup) }
            FeatureRow(
                icon = NineIcons.SignOut,
                title = "Sign out of this device",
                description = "Erases the key and all conversations. Without a key backup there is no way back.",
                tone = Tone.Red,
            ) { RowButton("Sign out", onClick = onErase, danger = true) }
        }
    }
}

/** The name field with a "Save" button: the name changes only on the button or Enter, not on every typed letter. */
@Composable
private fun NameField(displayName: String, onChange: (String) -> Unit) {
    var draft by remember(displayName) { mutableStateOf(displayName) }
    var saved by remember { mutableStateOf(false) }
    LaunchedEffect(saved) {
        if (saved) {
            delay(2_000)
            saved = false
        }
    }
    val changed = draft.trim() != displayName
    val save = {
        if (changed) {
            onChange(draft)
            saved = true
        }
    }

    FieldBlock(
        label = "Name",
        helper = "This is how your contacts will see you. While there is no server, only you see the name.",
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FormField(
                value = draft,
                onValueChange = { draft = it.take(MaxDisplayNameLength) },
                placeholder = "What should we call you",
                modifier = Modifier.weight(1f),
                filled = true,
                onSubmit = save,
            )
            RowButton(if (saved && !changed) "Saved" else "Save", onClick = save, enabled = changed)
        }
    }
}

@Composable
private fun Backup(session: Session, filePicker: FilePicker, onDone: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val problem = when {
        !isAcceptableAccessCode(password) -> "The password is shorter than $MinAccessCodeLength characters."
        password != repeat -> "The passwords do not match."
        else -> null
    }
    val submit = {
        if (problem == null && !busy) {
            busy = true
            scope.launch {
                val backup = withContext(Dispatchers.Default) { session.exportKeyBackup(password) }
                saved = filePicker.save("nine-key-backup.json", backup)
                busy = false
            }
        }
    }

    Column(Modifier.widthIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BasicText(
            "A file with your key, encrypted with a separate password. Keep it away from this device: " +
                "without it the account cannot be restored if the device is lost.",
            style = NineTheme.typography.chatPreview,
        )
        FormField(password, { password = it }, "Backup password", secret = true, filled = true)
        FormField(repeat, { repeat = it }, "Repeat the password", secret = true, filled = true, onSubmit = submit)
        if (saved) {
            BasicText("Backup saved.", style = NineTheme.typography.chatPreview.copy(color = NineColors.Accent))
        } else if (password.isNotEmpty() && repeat.isNotEmpty() && problem != null) {
            BasicText(problem, style = NineTheme.typography.chatPreview.copy(color = NineColors.Danger))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
            ActionButton(if (saved) "Done" else "Back", onClick = onDone)
            ActionButton(
                if (busy) "Saving…" else "Save file",
                onClick = submit,
                primary = true,
                enabled = problem == null && !busy,
            )
        }
    }
}
