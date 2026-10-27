package app.nine.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.nine.ui.components.Modal
import app.nine.ui.components.PanelShape
import app.nine.ui.settings.EraseConfirmation
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme

/** The "sign out and erase?" question for the "Sign out" menu item. Settings ask the same question. */
@Composable
fun SignOutDialog(onErase: () -> Unit, onDismiss: () -> Unit) {
    Modal(onDismiss) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .clip(PanelShape)
                .background(NineColors.Panel)
                .border(1.dp, NineColors.Rim, PanelShape)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BasicText("Sign out and erase data?", style = NineTheme.typography.profileName)
            EraseConfirmation(onErase, onCancel = onDismiss)
        }
    }
}
