package app.nine.ui.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import app.nine.ui.components.NineIcon
import app.nine.ui.components.NineIcons
import app.nine.ui.components.PanelShape
import app.nine.ui.components.pressable
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme

private val SendButtonColor = Color(0xFFE9EFEC)
private val OnSendButton = Color(0xFF0A100D)

/**
 * Поле ввода сообщения. Enter отправляет, Shift+Enter переносит строку.
 * Фокус ставится сразу при открытии чата, чтобы можно было печатать без клика.
 */
@Composable
fun Composer(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = NineTheme.typography
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val canSend = draft.isNotBlank()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, bottom = 20.dp)
            .clip(PanelShape)
            .background(NineColors.FloatingBar)
            .border(1.dp, NineColors.Rim, PanelShape)
            .padding(start = 18.dp, end = 9.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BasicTextField(
            value = draft,
            onValueChange = onDraftChange,
            maxLines = 6,
            textStyle = type.field.copy(fontSize = type.messageText.fontSize, lineHeight = type.messageText.lineHeight),
            cursorBrush = SolidColor(NineColors.Accent),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focus)
                .onPreviewKeyEvent { event ->
                    val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter
                    val isPlainEnter = isEnter && !event.isShiftPressed
                    if (isPlainEnter && event.type == KeyEventType.KeyDown) {
                        if (canSend) onSend()
                    }
                    // Поглощаем и отпускание клавиши, иначе Enter всё равно успеет вставить перенос строки.
                    isPlainEnter
                },
            decorationBox = { inner ->
                Box(Modifier.heightIn(min = 40.dp), contentAlignment = Alignment.CenterStart) {
                    if (draft.isEmpty()) BasicText("Сообщение", style = type.hint.copy(fontSize = type.messageText.fontSize))
                    inner()
                }
            },
        )
        SendButton(enabled = canSend, onClick = onSend)
    }
}

@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit) {
    val color = if (enabled) SendButtonColor else SendButtonColor.copy(alpha = 0.35f)
    Box(
        modifier = Modifier
            .size(40.dp)
            .pressable(
                onClick = { if (enabled) onClick() },
                shape = CircleShape,
                background = color,
                hoverBackground = if (enabled) Color.White else color,
            ),
        contentAlignment = Alignment.Center,
    ) {
        NineIcon(NineIcons.Send, tint = OnSendButton, contentDescription = "Отправить")
    }
}
