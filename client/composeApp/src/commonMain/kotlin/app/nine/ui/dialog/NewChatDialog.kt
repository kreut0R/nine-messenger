package app.nine.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.nine.ui.components.ActionButton
import app.nine.ui.components.Modal
import app.nine.ui.components.PanelShape
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme

/**
 * Окно создания чата поверх всего приложения. Закрывается по Escape, по клику на затемнение и по «Отмена».
 * Создаёт чат только с непустым именем, поэтому [onCreate] всегда получает уже готовое имя.
 */
@Composable
fun NewChatDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    val type = NineTheme.typography
    var name by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val canCreate = name.isNotBlank()
    val submit = { if (canCreate) onCreate(name.trim()) }

    Modal(onDismiss, focusOnOpen = false) {
        Column(
            modifier = Modifier
                .width(380.dp)
                .clip(PanelShape)
                .background(NineColors.Panel)
                .border(1.dp, NineColors.Rim, PanelShape)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            BasicText("Новый чат", style = type.profileName)
            BasicText("Как назвать собеседника? Имя видно только вам.", style = type.chatPreview)
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = type.field,
                cursorBrush = SolidColor(NineColors.Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .onPreviewKeyEvent { event ->
                        val enter = event.key == Key.Enter && event.type == KeyEventType.KeyDown
                        if (enter) submit()
                        enter
                    },
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(PanelShape)
                            .background(NineColors.Control)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (name.isEmpty()) BasicText("Имя", style = type.hint)
                        inner()
                    }
                },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                ActionButton("Отмена", onClick = onDismiss)
                ActionButton("Создать", onClick = { submit() }, primary = true, enabled = canCreate)
            }
        }
    }
}
