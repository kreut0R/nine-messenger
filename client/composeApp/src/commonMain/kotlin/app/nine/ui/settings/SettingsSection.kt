package app.nine.ui.settings

import androidx.compose.ui.graphics.vector.ImageVector
import app.nine.ui.components.NineIcons

/** Группы бокового меню настроек. */
enum class SettingsGroup(val title: String) {
    Personal("ЛИЧНЫЕ НАСТРОЙКИ"),
    Chats("ПЕРЕПИСКА"),
    App("ПРИЛОЖЕНИЕ"),
}

/** Разделы настроек в порядке показа. Первые три рабочие, остальные пока показывают, что будет. */
enum class SettingsSection(val title: String, val icon: ImageVector, val group: SettingsGroup) {
    Profile("Профиль и вход", NineIcons.Person, SettingsGroup.Personal),
    Security("Безопасность", NineIcons.Shield, SettingsGroup.Personal),
    Privacy("Приватность", NineIcons.EyeOff, SettingsGroup.Personal),
    Messages("Сообщения", NineIcons.Chat, SettingsGroup.Chats),
    Contacts("Собеседники", NineIcons.Users, SettingsGroup.Chats),
    Network("Сеть", NineIcons.Globe, SettingsGroup.App),
    Appearance("Внешний вид", NineIcons.Moon, SettingsGroup.App),
    Data("Данные", NineIcons.Database, SettingsGroup.App),
    About("О приложении", NineIcons.Info, SettingsGroup.App),
}
