package app.nine.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nine.ui.components.NineIcon
import app.nine.ui.components.pressable
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme

/** Как окрашена плитка с иконкой: зелёная для того, что работает, серая для остального, красная для опасного. */
enum class Tone { Active, Neutral, Danger }

/** Страница настроек: большой заголовок и блоки под ним. Блоки отделяются друг от друга через [SettingsDivider]. */
@Composable
fun SettingsPage(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        BasicText(title, style = NineTheme.typography.pageTitle)
        Spacer(Modifier.height(20.dp))
        content()
    }
}

/** Блок страницы: жирный подзаголовок и строки под ним. */
@Composable
fun Subsection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(title, style = NineTheme.typography.subsectionTitle)
        content()
    }
}

@Composable
fun SettingsDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(NineColors.Hairline))
}

/**
 * Строка настройки: плитка с иконкой, название и пояснение, справа то, чем её меняют.
 * @param below элемент под пояснением, если справа он не умещается (например, выбор из пяти вариантов)
 */
@Composable
fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
    tone: Tone = Tone.Neutral,
    below: (@Composable () -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    val type = NineTheme.typography
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconTile(icon, tone)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicText(title, style = type.featureTitle)
            BasicText(description, style = type.chatPreview)
            if (below != null) {
                Spacer(Modifier.height(6.dp))
                below()
            }
        }
        Box(Modifier.heightIn(min = 40.dp), contentAlignment = Alignment.Center) { trailing() }
    }
}

@Composable
private fun IconTile(icon: ImageVector, tone: Tone) {
    val (fill, tint) = when (tone) {
        Tone.Active -> NineColors.Accent.copy(alpha = 0.16f) to NineColors.Accent
        Tone.Neutral -> NineColors.Control to NineColors.TextSecondary
        Tone.Danger -> NineColors.Danger.copy(alpha = 0.14f) to NineColors.Danger
    }
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(fill), contentAlignment = Alignment.Center) {
        NineIcon(icon, tint = tint, size = 20.dp)
    }
}

/** Пометка у настройки, которой ещё нет. Её показывают, чтобы было видно, что будет, но не выдают за рабочую. */
@Composable
fun SoonBadge() {
    BasicText(
        "Скоро",
        style = NineTheme.typography.stamp,
        modifier = Modifier
            .background(NineColors.Control, RoundedCornerShape(6.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
    )
}

/** Строка с пометкой «Скоро». */
@Composable
fun SoonRow(icon: ImageVector, title: String, description: String) {
    FeatureRow(icon, title, description, trailing = { SoonBadge() })
}

/** Кнопка в правой части строки. [danger] красит текст красным: так отмечают то, что нельзя отменить. */
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

/** Поле только для чтения с подписью и пояснением. Текст можно выделить и скопировать. */
@Composable
fun ReadOnlyField(label: String, helper: String, value: String, action: @Composable () -> Unit = {}) {
    val type = NineTheme.typography
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(label, style = type.featureTitle)
        BasicText(helper, style = type.chatPreview)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NineColors.Control)
                .padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
                SelectionContainer { BasicText(value, style = type.code.copy(color = NineColors.TextPrimary, fontSize = 14.sp)) }
            }
            action()
        }
    }
}
