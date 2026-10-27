package app.nine.ui.settings

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import app.nine.ui.components.NineIcons
import app.nine.ui.theme.NineTheme

// Sections that are not built yet. The pages show what will appear in them: this is the same list as in the settings draft.
// Every row is marked "Soon", so that nothing looks working ahead of time.

@Composable
fun NotificationsPage() = SettingsPage("Notifications") {
    Subsection("New messages") {
        SoonRow(NineIcons.Bell, "Show notifications", "Turn them off entirely or show them only while the window is minimized.")
        SoonRow(
            NineIcons.HiddenEye,
            "Hide the text in a notification",
            "A notification will say only \"New message\": no contact name and no text.",
        )
    }
}

@Composable
fun MessagesPage() = SettingsPage("Messages") {
    Subsection("History") {
        SoonRow(
            NineIcons.Clock,
            "Disappearing messages",
            "Off, an hour, a day or a week. The timer is stored inside the message and is the same for both contacts.",
        )
        SoonRow(NineIcons.Database, "History storage", "How long to keep conversations on the device.")
    }
    SettingsDivider()
    Subsection("Appearance") {
        SoonRow(NineIcons.Chat, "Color of own messages", "Chosen in the contact panel for now, a global choice will appear here.")
    }
}

@Composable
fun ContactsPage() = SettingsPage("Contacts") {
    Subsection("Verification") {
        SoonRow(
            NineIcons.Shield,
            "Safety number",
            "For verifying in person or with a QR code. The contact panel already has a fingerprint.",
        )
        SoonRow(NineIcons.Key, "Contact key change", "Warn if a contact's key has changed.")
    }
}

@Composable
fun NetworkPage() = SettingsPage("Network") {
    Subsection("Connection") {
        SoonRow(NineIcons.Globe, "Server address", "Where the app connects. Will appear together with the server.")
        SoonRow(NineIcons.Link, "Proxy or Tor", "Transport setup, does not change the app logic.")
    }
}

@Composable
fun AppearancePage() = SettingsPage("Appearance") {
    Subsection("Theme") {
        SoonRow(NineIcons.Moon, "Theme and colors", "The dark theme is already here. Other themes and colors will come later.")
        SoonRow(NineIcons.Keyboard, "Font size and language", "The text size and the interface language.")
    }
    SettingsDivider()
    Subsection("Chat list") {
        SoonRow(NineIcons.HiddenEye, "Anonymity mask", "Show the mask at the bottom of the chat list or hide it.")
    }
}

@Composable
fun DataPage() = SettingsPage("Data") {
    Subsection("Backups") {
        SoonRow(
            NineIcons.Download,
            "Chat backup",
            "Export an encrypted backup of the chats to a file and import it back.",
        )
        SoonRow(
            NineIcons.Trash,
            "Delete everything on this device",
            "With a confirmation. For now this can be done through \"Sign out\" in the \"Profile and sign-in\" section.",
        )
    }
}

@Composable
fun AboutPage() = SettingsPage("About") {
    Subsection("Build") {
        SoonRow(NineIcons.Info, "Version and license", "The version, the AGPL license and a link to the source code.")
        SoonRow(
            NineIcons.Shield,
            "Build verification",
            "The build hash and the release signature, so that it can be checked that it was built from exactly this code.",
        )
    }
    SettingsDivider()
    Subsection("What will not be here") {
        BasicText(
            "Analytics, access to contacts, payments and subscriptions, extra permissions.",
            style = NineTheme.typography.chatPreview,
        )
    }
}
