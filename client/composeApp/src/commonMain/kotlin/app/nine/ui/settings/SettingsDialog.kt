package app.nine.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.nine.domain.AppSettings
import app.nine.domain.AutoLock
import app.nine.domain.TimeSource
import app.nine.security.Session
import app.nine.ui.components.GlassPanel
import app.nine.ui.components.IconAction
import app.nine.ui.components.Modal
import app.nine.ui.components.NineIcon
import app.nine.ui.components.NineIcons
import app.nine.ui.components.pressable
import app.nine.ui.platform.FilePicker
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme

/** Разделы настроек в порядке показа. */
enum class SettingsSection(val title: String, val icon: ImageVector) {
    Profile("Профиль и вход", NineIcons.Person),
    Security("Безопасность", NineIcons.Shield),
    Privacy("Приватность", NineIcons.EyeOff),
}

/**
 * Окно настроек: слева разделы, справа выбранный. Открывается сразу на нужном разделе,
 * потому что в него ведут и «Профиль», и «Настройки» из меню профиля.
 */
@Composable
fun SettingsDialog(
    initial: SettingsSection,
    settings: AppSettings,
    session: Session,
    filePicker: FilePicker,
    time: TimeSource,
    onAutoLockChange: (AutoLock) -> Unit,
    onClearClipboardChange: (Boolean) -> Unit,
    onCopy: (String) -> Unit,
    onErase: () -> Unit,
    onDismiss: () -> Unit,
) {
    var section by remember { mutableStateOf(initial) }
    Modal(onDismiss) {
        // Сначала ограничение, потом доля: так окно не растягивается на огромных экранах.
        GlassPanel(Modifier.widthIn(max = 880.dp).fillMaxWidth(0.94f).heightIn(max = 700.dp).fillMaxHeight(0.92f)) {
            Row(Modifier.fillMaxSize()) {
                Rail(section, onSelect = { section = it }, Modifier.width(230.dp).fillMaxHeight())
                Box(Modifier.width(1.dp).fillMaxHeight().background(NineColors.Hairline))
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 28.dp),
                    ) {
                        when (section) {
                            SettingsSection.Profile -> ProfileSection(session, filePicker, onCopy, onErase)
                            SettingsSection.Security -> SecuritySection(settings, session, time, onAutoLockChange)
                            SettingsSection.Privacy -> PrivacySection(settings, onClearClipboardChange)
                        }
                    }
                    IconAction(
                        icon = NineIcons.Close,
                        description = "Закрыть",
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                        shape = CircleShape,
                        size = 36.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun Rail(selected: SettingsSection, onSelect: (SettingsSection) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 14.dp, vertical = 26.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(
            "Настройки",
            style = NineTheme.typography.profileName,
            modifier = Modifier.padding(start = 12.dp, bottom = 18.dp),
        )
        SettingsSection.entries.forEach { item ->
            val isSelected = item == selected
            val background = if (isSelected) NineColors.SelectedRow else androidx.compose.ui.graphics.Color.Transparent
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    // У выбранного пункта фон особый, и наведение его не меняет.
                    .pressable({ onSelect(item) }, RoundedCornerShape(6.dp), hoverBackground = if (isSelected) background else NineColors.HoverRow, background = background)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NineIcon(item.icon, tint = if (isSelected) NineColors.TextPrimary else NineColors.TextSecondary, size = 18.dp)
                BasicText(
                    item.title,
                    style = NineTheme.typography.rowTitle.copy(color = if (isSelected) NineColors.TextPrimary else NineColors.TextSecondary),
                )
            }
        }
    }
}
