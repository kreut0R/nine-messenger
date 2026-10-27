package app.nine.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.nine.domain.AppSettings
import app.nine.domain.AutoLock
import app.nine.domain.TimeSource
import app.nine.security.Session
import app.nine.ui.components.NineIcon
import app.nine.ui.components.NineIcons
import app.nine.ui.components.pressable
import app.nine.ui.platform.FilePicker
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme

/** Width of the sidebar. */
private val SidebarWidth = 290.dp

/** The widest page column: beyond it rows on a big screen would stretch too long. */
private val PageMaxWidth = 960.dp

/**
 * Settings across the whole screen: a grouped menu on the left, the page of the chosen section on the right.
 * Opens straight on the needed section, because both "Profile" and "Settings" in the profile menu lead into it.
 * Closed by the "Chats" arrow in the corner and by Escape.
 */
@Composable
fun SettingsScreen(
    initial: SettingsSection,
    settings: AppSettings,
    displayName: String,
    session: Session,
    filePicker: FilePicker,
    time: TimeSource,
    onAutoLockChange: (AutoLock) -> Unit,
    onClearClipboardChange: (Boolean) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onCopy: (String) -> Unit,
    onErase: () -> Unit,
    onDismiss: () -> Unit,
) {
    var section by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(NineColors.Panel)
            // The screen lies over the chats and catches all touches, otherwise they would reach the panels under it.
            .pointerInput(Unit) { detectTapGestures { } }
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                val escape = event.key == Key.Escape && event.type == KeyEventType.KeyDown
                if (escape) onDismiss()
                escape
            },
    ) {
        Sidebar(section, onSelect = { section = it }, onBack = onDismiss, Modifier.width(SidebarWidth).fillMaxHeight())
        Box(Modifier.width(1.dp).fillMaxHeight().background(NineColors.Hairline))
        Box(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
            Column(Modifier.widthIn(max = PageMaxWidth).fillMaxWidth().padding(horizontal = 56.dp, vertical = 44.dp)) {
                // The page is recreated when the section changes: each has its own forms, and unfinished input must not move over.
                key(section) {
                    when (section) {
                        SettingsSection.Profile -> ProfilePage(displayName, session, filePicker, onDisplayNameChange, onCopy, onErase)
                        SettingsSection.Security -> SecurityPage(settings, session, time, onAutoLockChange)
                        SettingsSection.Privacy -> PrivacyPage(settings, onClearClipboardChange)
                        SettingsSection.Notifications -> NotificationsPage()
                        SettingsSection.Messages -> MessagesPage()
                        SettingsSection.Contacts -> ContactsPage()
                        SettingsSection.Network -> NetworkPage()
                        SettingsSection.Appearance -> AppearancePage()
                        SettingsSection.Data -> DataPage()
                        SettingsSection.About -> AboutPage()
                    }
                }
            }
        }
    }
}

@Composable
private fun Sidebar(selected: SettingsSection, onSelect: (SettingsSection) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val type = NineTheme.typography
    Column(modifier.background(NineColors.Sidebar).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp)) {
        BackButton(onBack)
        Spacer(Modifier.height(18.dp))
        SettingsGroup.entries.forEachIndexed { index, group ->
            if (index > 0) {
                Box(Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp).height(1.dp).background(NineColors.Hairline))
            }
            BasicText(group.title, style = type.groupLabel, modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SettingsSection.entries.filter { it.group == group }.forEach { item ->
                    NavItem(item, selected = item == selected, onClick = { onSelect(item) })
                }
            }
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .pressable(onClick, RoundedCornerShape(8.dp), hoverBackground = NineColors.ControlHover)
            .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        NineIcon(NineIcons.ChevronLeft, tint = NineColors.TextSecondary, size = 20.dp)
        BasicText("Chats", style = NineTheme.typography.navItem.copy(color = NineColors.TextSecondary))
    }
}

@Composable
private fun NavItem(item: SettingsSection, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) NineColors.ControlHover else Color.Transparent
    val color = if (selected) NineColors.TextPrimary else NineColors.TextSecondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            // The selected item has a special background, and hover does not change it.
            .pressable(onClick, RoundedCornerShape(10.dp), hoverBackground = if (selected) background else NineColors.Control, background = background)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NineIcon(item.icon, tint = color, size = 19.dp)
        BasicText(item.title, style = NineTheme.typography.navItem.copy(color = color))
    }
}
