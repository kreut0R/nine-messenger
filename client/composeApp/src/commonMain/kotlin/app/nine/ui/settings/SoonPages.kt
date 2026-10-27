package app.nine.ui.settings

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import app.nine.ui.components.NineIcons
import app.nine.ui.theme.NineTheme

// Разделы, которые ещё не сделаны. Страницы показывают, что в них появится: это тот же список, что в черновике настроек.
// Каждая строка помечена «Скоро», чтобы ничто не выглядело рабочим раньше времени.

@Composable
fun MessagesPage() = SettingsPage("Сообщения") {
    Subsection("История") {
        SoonRow(
            NineIcons.Clock,
            "Исчезающие сообщения",
            "Выключено, час, день или неделя. Таймер хранится внутри сообщения, у обоих собеседников одинаково.",
        )
        SoonRow(NineIcons.Database, "Хранение истории", "Сколько хранить переписку на устройстве.")
    }
    SettingsDivider()
    Subsection("Оформление") {
        SoonRow(NineIcons.Chat, "Цвет своих сообщений", "Сейчас выбирается в панели контакта, здесь появится общий выбор.")
    }
}

@Composable
fun ContactsPage() = SettingsPage("Собеседники") {
    Subsection("Проверка") {
        SoonRow(
            NineIcons.Shield,
            "Номер безопасности",
            "Для проверки лично или по QR-коду. В панели контакта уже есть отпечаток.",
        )
        SoonRow(NineIcons.Key, "Смена ключа собеседника", "Предупреждать, если у собеседника сменился ключ.")
    }
}

@Composable
fun NetworkPage() = SettingsPage("Сеть") {
    Subsection("Подключение") {
        SoonRow(NineIcons.Globe, "Адрес сервера", "Куда подключается приложение. Появится вместе с сервером.")
        SoonRow(NineIcons.Link, "Прокси или Tor", "Настройка транспорта, логику приложения не меняет.")
    }
}

@Composable
fun AppearancePage() = SettingsPage("Внешний вид") {
    Subsection("Тема") {
        SoonRow(NineIcons.Moon, "Тема и цвета", "Тёмная тема уже есть. Другие темы и цвета появятся позже.")
        SoonRow(NineIcons.Keyboard, "Размер шрифта и язык", "Размер текста и язык интерфейса.")
    }
    SettingsDivider()
    Subsection("Список чатов") {
        SoonRow(NineIcons.EyeOff, "Маска анонимности", "Показывать маску внизу списка чатов или скрыть её.")
    }
}

@Composable
fun DataPage() = SettingsPage("Данные") {
    Subsection("Копии") {
        SoonRow(
            NineIcons.Download,
            "Копия чатов",
            "Экспорт зашифрованной копии чатов в файл и импорт обратно.",
        )
        SoonRow(
            NineIcons.Trash,
            "Удалить всё на этом устройстве",
            "С подтверждением. Пока это можно сделать через «Выйти» в разделе «Профиль и вход».",
        )
    }
}

@Composable
fun AboutPage() = SettingsPage("О приложении") {
    Subsection("Сборка") {
        SoonRow(NineIcons.Info, "Версия и лицензия", "Версия, лицензия AGPL и ссылка на исходный код.")
        SoonRow(
            NineIcons.Shield,
            "Проверка сборки",
            "Хэш сборки и подпись релиза, чтобы можно было проверить, что собрано именно из этого кода.",
        )
    }
    SettingsDivider()
    Subsection("Чего здесь не будет") {
        BasicText(
            "Аналитики, доступа к контактам, оплаты и подписки, лишних разрешений.",
            style = NineTheme.typography.chatPreview,
        )
    }
}
