package app.nine.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.nine.domain.BubbleColor
import app.nine.ui.theme.color
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme

/**
 * Текстовая кнопка для окон. Главная действует зелёным цветом, остальные тише.
 * [danger] красит текст красным: так отмечают то, что нельзя отменить.
 */
@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    val base = if (primary) NineColors.TabThumb else Color.Transparent
    val textColor = when {
        danger -> NineColors.Danger
        primary && enabled -> NineColors.Accent
        else -> NineColors.TextSecondary
    }
    BasicText(
        text = text,
        style = NineTheme.typography.button.copy(color = textColor),
        modifier = modifier
            .height(40.dp)
            .pressable(
                onClick = { if (enabled) onClick() },
                shape = PanelShape,
                background = base,
                hoverBackground = if (enabled) NineColors.ControlHover else base,
            )
            .padding(horizontal = 18.dp, vertical = 11.dp),
    )
}

/** Кнопка на всю ширину формы. Главная залита цветом своих сообщений, вторая обведена. */
@Composable
fun WideButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(10.dp)
    val fill = if (primary) BubbleColor.Forest.color else Color.Transparent
    BasicText(
        text = text,
        style = NineTheme.typography.button.copy(
            color = if (enabled) NineColors.TextPrimary else NineColors.TextTertiary,
            textAlign = TextAlign.Center,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .pressable(
                onClick = { if (enabled) onClick() },
                shape = shape,
                background = fill,
                hoverBackground = if (!enabled) fill else if (primary) BubbleColor.Emerald.color else NineColors.ControlHover,
            )
            .then(if (primary) Modifier else Modifier.border(1.dp, NineColors.FieldBorder, shape))
            .padding(vertical = 13.dp),
    )
}
