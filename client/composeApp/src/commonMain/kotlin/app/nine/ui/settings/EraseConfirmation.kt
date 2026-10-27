package app.nine.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.nine.ui.components.ActionButton
import app.nine.ui.theme.NineTheme

/**
 * Confirmation of signing out with erasure. Signing out is irreversible: without a key backup there is no way back into the account,
 * so it always goes through this question, wherever the person signs out from.
 */
@Composable
fun EraseConfirmation(onErase: () -> Unit, onCancel: () -> Unit) {
    Column(Modifier.widthIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BasicText(
            "The key and all conversations will be erased from this device. Without a key backup there is no way back into the account.",
            style = NineTheme.typography.chatPreview,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
            ActionButton("Cancel", onClick = onCancel)
            ActionButton("Erase", onClick = onErase, danger = true)
        }
    }
}
