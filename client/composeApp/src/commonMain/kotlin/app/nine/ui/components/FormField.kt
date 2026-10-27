package app.nine.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme

private val FieldShape = RoundedCornerShape(10.dp)

/**
 * Однострочное поле с рамкой и подписью сверху. [secret] прячет набираемое точками, а значок глаза справа
 * показывает набранное до повторного нажатия. Enter вызывает [onSubmit].
 */
@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    secret: Boolean = false,
    onSubmit: () -> Unit = {},
) {
    val type = NineTheme.typography
    var revealed by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (label != null) BasicText(label, style = type.section)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(FieldShape)
                .border(1.dp, NineColors.FieldBorder, FieldShape)
                .padding(start = 14.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = type.field,
                cursorBrush = SolidColor(NineColors.Accent),
                visualTransformation = if (secret && !revealed) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { event ->
                        val enter = event.key == Key.Enter && event.type == KeyEventType.KeyDown
                        if (enter) onSubmit()
                        enter
                    },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) BasicText(placeholder, style = type.hint)
                        inner()
                    }
                },
            )
            if (secret) {
                IconAction(
                    icon = if (revealed) NineIcons.EyeOff else NineIcons.Eye,
                    description = if (revealed) "Скрыть" else "Показать",
                    onClick = { revealed = !revealed },
                    tint = NineColors.TextTertiary,
                    size = 30.dp,
                )
            }
        }
    }
}
