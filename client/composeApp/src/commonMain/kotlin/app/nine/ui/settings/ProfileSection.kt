package app.nine.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.nine.domain.displayCode
import app.nine.security.MinAccessCodeLength
import app.nine.security.Session
import app.nine.security.isAcceptableAccessCode
import app.nine.ui.components.ActionButton
import app.nine.ui.components.FormField
import app.nine.ui.platform.FilePicker
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ProfileStep { Overview, Backup, ConfirmErase }

/** Раздел «Профиль и вход»: код аккаунта, копия ключа и выход со стиранием. */
@Composable
fun ProfileSection(session: Session, filePicker: FilePicker, onCopy: (String) -> Unit, onErase: () -> Unit) {
    var step by remember { mutableStateOf(ProfileStep.Overview) }
    when (step) {
        ProfileStep.Overview -> Overview(
            session,
            onCopy,
            onBackup = { step = ProfileStep.Backup },
            onErase = { step = ProfileStep.ConfirmErase },
        )
        ProfileStep.Backup -> {
            SectionTitle("Копия ключа")
            Backup(session, filePicker, onDone = { step = ProfileStep.Overview })
        }
        ProfileStep.ConfirmErase -> {
            SectionTitle("Стереть данные?")
            EraseConfirmation(onErase, onCancel = { step = ProfileStep.Overview })
        }
    }
}

@Composable
private fun Overview(session: Session, onCopy: (String) -> Unit, onBackup: () -> Unit, onErase: () -> Unit) {
    val code = session.accountId.displayCode
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(2_000)
            copied = false
        }
    }

    SectionTitle("Профиль и вход")
    SettingBlock("Код аккаунта", "Собеседник может сверить его с вами голосом, чтобы убедиться, что пишет именно вам.") {
        SelectionContainer {
            BasicText(code, style = NineTheme.typography.code.copy(color = NineColors.TextPrimary))
        }
        Row { RowButton(if (copied) "Скопировано" else "Копировать", onClick = {
            onCopy(code)
            copied = true
        }) }
    }
    SoonRow("Имя и аватар", "Хранятся у вас и у собеседников, на сервер не уходят.")
    SettingRow(
        "Копия ключа",
        "Файл с вашим ключом, закрытый отдельным паролем. Без него аккаунт не вернуть, если устройство потеряно.",
    ) {
        RowButton("Сохранить", onClick = onBackup)
    }
    SettingRow("Выйти с этого устройства", "Стирает ключ и всю переписку. Без копии ключа вернуться нельзя.") {
        RowButton("Выйти", onClick = onErase, danger = true)
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

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BasicText(
            "Файл с вашим ключом, зашифрованный отдельным паролем. Храните его вне этого устройства: " +
                "без него аккаунт не вернуть, если устройство потеряно.",
            style = NineTheme.typography.chatPreview,
        )
        FormField(password, { password = it }, "Пароль для копии", secret = true)
        FormField(repeat, { repeat = it }, "Повторите пароль", secret = true, onSubmit = submit)
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
