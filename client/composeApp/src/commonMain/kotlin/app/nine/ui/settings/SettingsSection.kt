package app.nine.ui.settings

import androidx.compose.ui.graphics.vector.ImageVector
import app.nine.ui.components.NineIcons

/** The groups of the settings sidebar. */
enum class SettingsGroup(val title: String) {
    Personal("PERSONAL SETTINGS"),
    Chats("CONVERSATIONS"),
    App("APP"),
}

/** The settings sections in display order. The first three work, the rest only show what is coming. */
enum class SettingsSection(val title: String, val icon: ImageVector, val group: SettingsGroup) {
    Profile("Profile and sign-in", NineIcons.PersonCircle, SettingsGroup.Personal),
    Security("Security", NineIcons.Shield, SettingsGroup.Personal),
    Privacy("Privacy", NineIcons.HiddenEye, SettingsGroup.Personal),
    Notifications("Notifications", NineIcons.Bell, SettingsGroup.Personal),
    Messages("Messages", NineIcons.Chat, SettingsGroup.Chats),
    Contacts("Contacts", NineIcons.Users, SettingsGroup.Chats),
    Network("Network", NineIcons.Globe, SettingsGroup.App),
    Appearance("Appearance", NineIcons.Palette, SettingsGroup.App),
    Data("Data", NineIcons.Database, SettingsGroup.App),
    About("About", NineIcons.Info, SettingsGroup.App),
}
