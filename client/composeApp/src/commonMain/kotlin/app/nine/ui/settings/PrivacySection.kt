package app.nine.ui.settings

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import app.nine.domain.AppSettings
import app.nine.ui.components.ToggleSwitch
import app.nine.ui.theme.NineTheme

/** Раздел «Приватность». Рабочая здесь только очистка буфера, остальное показано как «Скоро». */
@Composable
fun PrivacySection(settings: AppSettings, onClearClipboardChange: (Boolean) -> Unit) {
    SectionTitle("Приватность")
    SettingRow(
        "Превью ссылок",
        "Не загружаются. Приложение само не ходит на чужие сайты и не показывает им ваш IP-адрес.",
    ) {
        BasicText("Выключено", style = NineTheme.typography.chatPreview)
    }
    SettingRow(
        "Очищать буфер обмена",
        "Через 30 секунд после копирования из Nine скопированное убирается из буфера.",
    ) {
        ToggleSwitch(settings.clearClipboard, onClearClipboardChange)
    }
    SoonRow("«Печатает» и «прочитано»", "Выбор, показывать ли собеседнику эти статусы.")
    SoonRow("Чистка метаданных", "Убирает геотеги, модель камеры и время съёмки из фото и файлов перед отправкой.")
    SoonRow("Клавиатура без обучения", "Только на телефоне.")
}
