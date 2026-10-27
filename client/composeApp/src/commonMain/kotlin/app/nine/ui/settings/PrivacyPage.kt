package app.nine.ui.settings

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import app.nine.domain.AppSettings
import app.nine.ui.components.NineIcons
import app.nine.ui.components.ToggleSwitch
import app.nine.ui.theme.NineTheme

/** The "Privacy" page. Only clipboard clearing works here, the rest is shown as "Soon". */
@Composable
fun PrivacyPage(settings: AppSettings, onClearClipboardChange: (Boolean) -> Unit) {
    SettingsPage("Privacy") {
        Subsection("Leaks") {
            FeatureRow(
                icon = NineIcons.Link,
                title = "Link previews",
                description = "Not loaded. The app never visits other sites by itself and never shows them your IP address.",
                tone = Tone.Green,
            ) { BasicText("Off", style = NineTheme.typography.chatPreview) }
            FeatureRow(
                icon = NineIcons.Clipboard,
                title = "Clear the clipboard",
                description = "30 seconds after copying from Nine, what was copied is removed from the clipboard.",
                tone = Tone.Blue,
            ) { ToggleSwitch(settings.clearClipboard, onClearClipboardChange) }
        }
        SettingsDivider()
        Subsection("What contacts see") {
            SoonRow(NineIcons.Chat, "Typing and read receipts", "A choice of whether to show these statuses to a contact.")
            SoonRow(
                NineIcons.Photo,
                "Metadata stripping",
                "Removes geotags, the camera model and the shooting time from photos and files before sending.",
            )
        }
        SettingsDivider()
        Subsection("Input") {
            SoonRow(NineIcons.Keyboard, "Keyboard without learning", "Phone only.")
        }
    }
}
