package app.nine.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.nine.domain.displayCode
import app.nine.security.MinAccessCodeLength
import app.nine.security.Session
import app.nine.security.isAcceptableAccessCode
import app.nine.ui.components.ActionButton
import app.nine.ui.components.FormField
import app.nine.ui.components.PanelShape
import app.nine.ui.platform.FilePicker
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Step { Overview, Backup, ConfirmErase }

/**
 * Окно профиля: код аккаунта, копия ключа, блокировка и выход со стиранием.
 * Выход необратим, поэтому он идёт через отдельное подтверждение.
 */
@Composable
fun ProfileDialog(
    session: Session,
    filePicker: FilePicker,
    onLock: () -> Unit,
    onErase: () -> Unit,
    onDismiss: () -> Unit,
) {
    val type = NineTheme.typography
    var step by remember { mutableStateOf(Step.Overview) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NineColors.Scrim)
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
            .onPreviewKeyEvent { event ->
                val escape = event.key == Key.Escape && event.type == KeyEventType.KeyDown
                if (escape) onDismiss()
                escape
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .clip(PanelShape)
                .background(NineColors.Panel)
                .border(1.dp, NineColors.Rim, PanelShape)
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BasicText(
                when (step) {
                    Step.Overview -> "Профиль"
                    Step.Backup -> "Копия ключа"
                    Step.ConfirmErase -> "Стереть данные?"
                },
                style = type.profileName,
            )
            when (step) {
                Step.Overview -> Overview(
                    session,
                    onBackup = { step = Step.Backup },
                    onLock = onLock,
                    onErase = { step = Step.ConfirmErase },
                    onClose = onDismiss,
                )
                Step.Backup -> Backup(session, filePicker, onDone = { step = Step.Overview })
                Step.ConfirmErase -> ConfirmErase(onErase, onCancel = { step = Step.Overview })
            }
        }
    }
}

@Composable
private fun Overview(session: Session, onBackup: () -> Unit, onLock: () -> Unit, onErase: () -> Unit, onClose: () -> Unit) {
    val type = NineTheme.typography
    BasicText("Код вашего аккаунта. Собеседник может сверить его с вами голосом.", style = type.chatPreview)
    SelectionContainer {
        BasicText(session.accountId.displayCode, style = type.code.copy(color = NineColors.TextPrimary))
    }
    ActionButton("Сохранить копию ключа", onClick = onBackup)
    ActionButton("Заблокировать", onClick = onLock)
    ActionButton("Выйти и стереть данные", onClick = onErase, danger = true)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        ActionButton("Закрыть", onClick = onClose)
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

@Composable
private fun ConfirmErase(onErase: () -> Unit, onCancel: () -> Unit) {
    BasicText(
        "Ключ и вся переписка будут стёрты с этого устройства. Без копии ключа вернуться в аккаунт нельзя.",
        style = NineTheme.typography.chatPreview,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
        ActionButton("Отмена", onClick = onCancel)
        ActionButton("Стереть", onClick = onErase, danger = true)
    }
}
