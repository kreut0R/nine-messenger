package app.nine.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
