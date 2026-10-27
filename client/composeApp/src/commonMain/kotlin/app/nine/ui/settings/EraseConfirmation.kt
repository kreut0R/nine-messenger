package app.nine.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.nine.ui.components.ActionButton
import app.nine.ui.theme.NineTheme

/**
 * Подтверждение выхода со стиранием. Выход необратим: без копии ключа в аккаунт не вернуться,
 * поэтому он всегда идёт через этот вопрос, откуда бы человек ни вышел.
 */
@Composable
fun EraseConfirmation(onErase: () -> Unit, onCancel: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BasicText(
            "Ключ и вся переписка будут стёрты с этого устройства. Без копии ключа вернуться в аккаунт нельзя.",
            style = NineTheme.typography.chatPreview,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
            ActionButton("Отмена", onClick = onCancel)
            ActionButton("Стереть", onClick = onErase, danger = true)
        }
    }
}
