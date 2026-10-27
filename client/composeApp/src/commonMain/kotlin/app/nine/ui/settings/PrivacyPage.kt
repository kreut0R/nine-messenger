package app.nine.ui.settings

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import app.nine.domain.AppSettings
import app.nine.ui.components.NineIcons
import app.nine.ui.components.ToggleSwitch
import app.nine.ui.theme.NineTheme

/** Страница «Приватность». Рабочая здесь только очистка буфера, остальное показано как «Скоро». */
@Composable
fun PrivacyPage(settings: AppSettings, onClearClipboardChange: (Boolean) -> Unit) {
    SettingsPage("Приватность") {
        Subsection("Утечки") {
            FeatureRow(
                icon = NineIcons.Link,
                title = "Превью ссылок",
                description = "Не загружаются. Приложение само не ходит на чужие сайты и не показывает им ваш IP-адрес.",
                tone = Tone.Green,
            ) { BasicText("Выключено", style = NineTheme.typography.chatPreview) }
            FeatureRow(
                icon = NineIcons.Clipboard,
                title = "Очищать буфер обмена",
                description = "Через 30 секунд после копирования из Nine скопированное убирается из буфера.",
                tone = Tone.Blue,
            ) { ToggleSwitch(settings.clearClipboard, onClearClipboardChange) }
        }
        SettingsDivider()
        Subsection("Что видят собеседники") {
            SoonRow(NineIcons.Chat, "«Печатает» и «прочитано»", "Выбор, показывать ли собеседнику эти статусы.")
            SoonRow(
                NineIcons.Photo,
                "Чистка метаданных",
                "Убирает геотеги, модель камеры и время съёмки из фото и файлов перед отправкой.",
            )
        }
        SettingsDivider()
        Subsection("Ввод") {
            SoonRow(NineIcons.Keyboard, "Клавиатура без обучения", "Только на телефоне.")
        }
    }
}
