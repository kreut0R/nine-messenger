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

/** The color of the icon tile. Working settings are colored, red for irreversible ones, gray for those that do not exist yet. */
enum class Tone { Green, Blue, Amber, Red, Neutral }

/** A settings page: a big title and blocks under it. Blocks are separated from each other with [SettingsDivider]. */
@Composable
fun SettingsPage(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        BasicText(title, style = NineTheme.typography.pageTitle)
        Spacer(Modifier.height(20.dp))
        content()
    }
}

/** A page block: a bold subtitle and rows under it. */
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
 * A settings row: an icon tile, a name and an explanation, and on the right what changes it.
 * @param below an element under the explanation, if it does not fit on the right (for example, a choice of five options)
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
        Tone.Green -> NineColors.TileGreen to NineColors.OnTile
        Tone.Blue -> NineColors.TileBlue to NineColors.OnTile
        Tone.Amber -> NineColors.TileAmber to NineColors.OnTile
        Tone.Red -> NineColors.TileRed to NineColors.OnTile
        Tone.Neutral -> NineColors.Control to NineColors.TextSecondary
    }
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)).background(fill), contentAlignment = Alignment.Center) {
        NineIcon(icon, tint = tint, size = 22.dp)
    }
}

/** A mark on a setting that does not exist yet. It is shown so that it is visible what is coming, but is not passed off as working. */
@Composable
fun SoonBadge() {
    BasicText(
        "Soon",
        style = NineTheme.typography.stamp,
        modifier = Modifier
            .background(NineColors.Control, RoundedCornerShape(6.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
    )
}

/** A row with the "Soon" mark. */
@Composable
fun SoonRow(icon: ImageVector, title: String, description: String) {
    FeatureRow(icon, title, description, trailing = { SoonBadge() })
}

/**
 * A button on the right side of a row. [danger] colors the text red: that marks what cannot be undone.
 * A disabled button is dim and cannot be pressed.
 */
@Composable
fun RowButton(text: String, onClick: () -> Unit, danger: Boolean = false, enabled: Boolean = true) {
    val color = when {
        !enabled -> NineColors.TextTertiary
        danger -> NineColors.Danger
        else -> NineColors.TextPrimary
    }
    BasicText(
        text,
        style = NineTheme.typography.button.copy(color = color),
        modifier = Modifier
            .pressable(
                onClick = { if (enabled) onClick() },
                shape = RoundedCornerShape(8.dp),
                hoverBackground = if (enabled) NineColors.ControlHover else NineColors.Control,
                background = NineColors.Control,
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}

/** A label and an explanation above a field: this is how both the read-only field and the name field look. */
@Composable
fun FieldBlock(label: String, helper: String, content: @Composable () -> Unit) {
    val type = NineTheme.typography
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(label, style = type.featureTitle)
        BasicText(helper, style = type.chatPreview)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

/** A read-only field with a label and an explanation. The text can be selected and copied. */
@Composable
fun ReadOnlyField(label: String, helper: String, value: String, action: @Composable () -> Unit = {}) {
    val type = NineTheme.typography
    FieldBlock(label, helper) {
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
