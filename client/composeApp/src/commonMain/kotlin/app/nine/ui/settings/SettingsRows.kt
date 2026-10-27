package app.nine.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import app.nine.ui.components.pressable
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme

/** Заголовок раздела настроек. */
@Composable
fun SectionTitle(text: String) {
    BasicText(text, style = NineTheme.typography.profileName, modifier = Modifier.padding(bottom = 14.dp))
}

/** Тонкая линия под каждой строкой: она же отделяет первую строку от заголовка раздела. */
private fun Modifier.underline(): Modifier = drawBehind {
    drawLine(
        color = NineColors.Hairline,
        start = Offset(0f, size.height - 0.5f),
        end = Offset(size.width, size.height - 0.5f),
        strokeWidth = 1f,
    )
}

/** Строка настройки: название и пояснение слева, то, чем её меняют, справа. */
@Composable
fun SettingRow(title: String, description: String, trailing: @Composable () -> Unit) {
    val type = NineTheme.typography
    Row(
        modifier = Modifier.fillMaxWidth().underline().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicText(title, style = type.rowTitle)
            BasicText(description, style = type.chatPreview)
        }
        trailing()
    }
}

/** Строка, у которой элемент управления не умещается справа и идёт под текстом. */
@Composable
fun SettingBlock(title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    val type = NineTheme.typography
    Column(
        modifier = Modifier.fillMaxWidth().underline().padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicText(title, style = type.rowTitle)
            BasicText(description, style = type.chatPreview)
        }
        content()
    }
}

/** Настройка, которую мы ещё не сделали. Показываем её, чтобы было видно, что будет, но не притворяемся, что она работает. */
@Composable
fun SoonRow(title: String, description: String) {
    val type = NineTheme.typography
    SettingRow(title, description) {
        BasicText(
            "Скоро",
            style = type.stamp,
            modifier = Modifier
                .background(NineColors.Control, RoundedCornerShape(6.dp))
                .padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

/** Кнопка в правой части строки настроек. [danger] красит текст красным: так отмечают то, что нельзя отменить. */
@Composable
fun RowButton(text: String, onClick: () -> Unit, danger: Boolean = false) {
    BasicText(
        text,
        style = NineTheme.typography.button.copy(color = if (danger) NineColors.Danger else NineColors.TextPrimary),
        modifier = Modifier
            .pressable(onClick, RoundedCornerShape(8.dp), hoverBackground = NineColors.ControlHover, background = NineColors.Control)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}
