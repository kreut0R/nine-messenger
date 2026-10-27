package app.nine.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.nine.ui.components.Avatar
import app.nine.ui.components.IconAction
import app.nine.ui.components.NineIcon
import app.nine.ui.components.NineIcons
import app.nine.ui.components.PanelShape
import app.nine.ui.components.pressable
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme

/**
 * The profile button in the chat list header and the menu it opens.
 * @param displayName the person's name, empty if they have not set one
 * @param shortCode the start of the account code: it shows which account the person is signed in with
 */
@Composable
fun ProfileMenuButton(
    displayName: String,
    shortCode: String,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    onLock: () -> Unit,
    onSignOut: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val below = with(LocalDensity.current) { 44.dp.roundToPx() }
    Box {
        IconAction(NineIcons.Account, "Profile", onClick = { open = !open }, shape = CircleShape, size = 36.dp)
        if (open) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, below),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true),
            ) {
                MenuPanel(displayName, shortCode) { choice ->
                    // Close the menu before the action: it may open a window on top.
                    open = false
                    when (choice) {
                        MenuChoice.Profile -> onProfile()
                        MenuChoice.Settings -> onSettings()
                        MenuChoice.Lock -> onLock()
                        MenuChoice.SignOut -> onSignOut()
                    }
                }
            }
        }
    }
}

private enum class MenuChoice { Profile, Settings, Lock, SignOut }

@Composable
private fun MenuPanel(displayName: String, shortCode: String, onPick: (MenuChoice) -> Unit) {
    val type = NineTheme.typography
    Column(
        modifier = Modifier
            .width(250.dp)
            .clip(PanelShape)
            .background(NineColors.Menu)
            .border(1.dp, NineColors.Rim, PanelShape)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (displayName.isBlank()) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(NineColors.Control), contentAlignment = Alignment.Center) {
                    NineIcon(NineIcons.Person, tint = NineColors.TextSecondary, size = 20.dp)
                }
            } else {
                Avatar(displayName, seed = shortCode, size = 38.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(displayName.ifBlank { "My account" }, style = type.rowTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText(shortCode, style = type.code)
            }
        }
        Divider()
        MenuItem(NineIcons.PersonCircle, "Profile") { onPick(MenuChoice.Profile) }
        MenuItem(NineIcons.Sliders, "Settings") { onPick(MenuChoice.Settings) }
        Divider()
        MenuItem(NineIcons.Lock, "Lock") { onPick(MenuChoice.Lock) }
        MenuItem(NineIcons.SignOut, "Sign out", danger = true) { onPick(MenuChoice.SignOut) }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(NineColors.Hairline))
}

@Composable
private fun MenuItem(icon: ImageVector, text: String, danger: Boolean = false, onClick: () -> Unit) {
    val color = if (danger) NineColors.Danger else NineColors.TextPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .pressable(onClick, RoundedCornerShape(8.dp), hoverBackground = NineColors.ControlHover)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NineIcon(icon, tint = color, size = 19.dp)
        BasicText(text, style = NineTheme.typography.button.copy(color = color))
    }
}
