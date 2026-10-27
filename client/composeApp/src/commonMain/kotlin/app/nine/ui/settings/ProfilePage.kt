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

/** Страница «Профиль и вход»: код аккаунта, копия ключа и выход со стиранием. */
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
        ProfileStep.Backup -> SettingsPage("Копия ключа") {
            Backup(session, filePicker, onDone = { step = ProfileStep.Overview })
        }
        ProfileStep.ConfirmErase -> SettingsPage("Стереть данные?") {
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

    SettingsPage("Профиль и вход") {
        Subsection("Профиль") {
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
                    BasicText("Аватар", style = NineTheme.typography.featureTitle)
                    BasicText("Фото из файла. Пока вместо него первая буква имени.", style = NineTheme.typography.chatPreview)
                }
                SoonBadge()
            }
            Spacer(Modifier.height(10.dp))
            NameField(displayName, onDisplayNameChange)
            Spacer(Modifier.height(10.dp))
            ReadOnlyField(
                label = "Код аккаунта",
                helper = "Собеседник может сверить его с вами голосом, чтобы убедиться, что пишет именно вам.",
                value = code,
            ) {
                RowButton(if (copied) "Скопировано" else "Копировать", onClick = {
                    onCopy(code)
                    copied = true
                })
            }
        }
        SettingsDivider()
        Subsection("Ключ и вход") {
            FeatureRow(
                icon = NineIcons.Key,
                title = "Копия ключа",
                description = "Файл с вашим ключом, закрытый отдельным паролем. Без него аккаунт не вернуть, если устройство потеряно.",
                tone = Tone.Amber,
            ) { RowButton("Сохранить", onClick = onBackup) }
            FeatureRow(
                icon = NineIcons.SignOut,
                title = "Выйти с этого устройства",
                description = "Стирает ключ и всю переписку. Без копии ключа вернуться нельзя.",
                tone = Tone.Red,
            ) { RowButton("Выйти", onClick = onErase, danger = true) }
        }
    }
}

/** Поле имени с кнопкой «Сохранить»: имя меняется только по кнопке или Enter, а не на каждую набранную букву. */
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
        label = "Имя",
        helper = "Так вас увидят собеседники. Пока сервера нет, имя видно только вам.",
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FormField(
                value = draft,
                onValueChange = { draft = it.take(MaxDisplayNameLength) },
                placeholder = "Как вас называть",
                modifier = Modifier.weight(1f),
                filled = true,
                onSubmit = save,
            )
            RowButton(if (saved && !changed) "Сохранено" else "Сохранить", onClick = save, enabled = changed)
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
        !isAcceptableAccessCode(password) -> "Пароль короче $MinAccessCodeLength символов."
        password != repeat -> "Пароли не совпадают."
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
            "Файл с вашим ключом, зашифрованный отдельным паролем. Храните его вне этого устройства: " +
                "без него аккаунт не вернуть, если устройство потеряно.",
            style = NineTheme.typography.chatPreview,
        )
        FormField(password, { password = it }, "Пароль для копии", secret = true, filled = true)
        FormField(repeat, { repeat = it }, "Повторите пароль", secret = true, filled = true, onSubmit = submit)
        if (saved) {
            BasicText("Копия сохранена.", style = NineTheme.typography.chatPreview.copy(color = NineColors.Accent))
        } else if (password.isNotEmpty() && repeat.isNotEmpty() && problem != null) {
            BasicText(problem, style = NineTheme.typography.chatPreview.copy(color = NineColors.Danger))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
            ActionButton(if (saved) "Готово" else "Назад", onClick = onDone)
            ActionButton(
                if (busy) "Сохраняем…" else "Сохранить файл",
                onClick = submit,
                primary = true,
                enabled = problem == null && !busy,
            )
        }
    }
}
