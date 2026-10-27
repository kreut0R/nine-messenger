package app.nine.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.nine.domain.AppSettings
import app.nine.domain.AutoLock
import app.nine.domain.TimeSource
import app.nine.presentation.formatPause
import app.nine.security.ChangeCodeResult
import app.nine.security.MinAccessCodeLength
import app.nine.security.Session
import app.nine.security.isAcceptableAccessCode
import app.nine.ui.components.ActionButton
import app.nine.ui.components.FormField
import app.nine.ui.components.NineIcons
import app.nine.ui.components.SegmentedTabs
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SecurityStep { Overview, ChangeCode }

/** Страница «Безопасность»: автоблокировка и код доступа. Остальное из списка показано как «Скоро». */
@Composable
fun SecurityPage(
    settings: AppSettings,
    session: Session,
    time: TimeSource,
    onAutoLockChange: (AutoLock) -> Unit,
) {
    var step by remember { mutableStateOf(SecurityStep.Overview) }
    when (step) {
        SecurityStep.Overview -> SettingsPage("Безопасность") {
            Subsection("Вход") {
                FeatureRow(
                    icon = NineIcons.Clock,
                    title = "Автоблокировка",
                    description = settings.autoLock.description,
                    tone = Tone.Blue,
                    below = {
                        Box(Modifier.widthIn(max = 460.dp)) {
                            SegmentedTabs(
                                titles = AutoLock.entries.map { it.title },
                                selectedIndex = settings.autoLock.ordinal,
                                onSelect = { onAutoLockChange(AutoLock.entries[it]) },
                            )
                        }
                    },
                )
                FeatureRow(
                    icon = NineIcons.Key,
                    title = "Код доступа",
                    description = "Открывает приложение. Минимум $MinAccessCodeLength символов, лучше фраза из нескольких слов.",
                    tone = Tone.Green,
                ) { RowButton("Сменить", onClick = { step = SecurityStep.ChangeCode }) }
            }
            SettingsDivider()
            Subsection("Если вас заставляют открыть приложение") {
                SoonRow(NineIcons.Warning, "Тревожный код", "Открывает подставной аккаунт, а настоящий остаётся скрытым.")
                SoonRow(
                    NineIcons.Trash,
                    "Стирание после неудачных попыток",
                    "Сотрёт данные после заданного числа неверных кодов. Будет выключено по умолчанию.",
                )
                SoonRow(NineIcons.Alert, "Кнопка паники", "Мгновенно блокирует приложение и прячет его окно.")
            }
            SettingsDivider()
            Subsection("Экран") {
                SoonRow(NineIcons.NoScreen, "Запрет скриншотов", "Только на телефоне.")
            }
        }
        SecurityStep.ChangeCode -> SettingsPage("Новый код доступа") {
            ChangeCodeForm(session, time, onDone = { step = SecurityStep.Overview })
        }
    }
}

private val AutoLock.title: String
    get() = when (this) {
        AutoLock.OnMinimize -> "Сразу"
        AutoLock.OneMinute -> "1 мин"
        AutoLock.FiveMinutes -> "5 мин"
        AutoLock.FifteenMinutes -> "15 мин"
        AutoLock.Never -> "Никогда"
    }

private val AutoLock.description: String
    get() = when (this) {
        AutoLock.OnMinimize -> "Приложение блокируется, как только окно свернули."
        AutoLock.OneMinute -> "Приложение блокируется, если минуту ничего не делать."
        AutoLock.FiveMinutes -> "Приложение блокируется, если 5 минут ничего не делать."
        AutoLock.FifteenMinutes -> "Приложение блокируется, если 15 минут ничего не делать."
        AutoLock.Never -> "Само не блокируется: закрывать доступ придётся вручную из меню профиля."
    }

@Composable
private fun ChangeCodeForm(session: Session, time: TimeSource, onDone: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var newCode by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var changed by remember { mutableStateOf(false) }
    var refusal by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val problem = when {
        !isAcceptableAccessCode(newCode) -> "Новый код короче $MinAccessCodeLength символов."
        newCode != repeat -> "Коды не совпадают."
        newCode == current -> "Новый код совпадает с текущим."
        else -> null
    }
    val canSubmit = current.isNotEmpty() && problem == null && !busy && !changed
    val submit = {
        if (canSubmit) {
            busy = true
            refusal = null
            scope.launch {
                // Argon2id намеренно медленный и вызывается дважды, поэтому не на потоке интерфейса.
                val result = withContext(Dispatchers.Default) { session.changeAccessCode(current, newCode) }
                busy = false
                when (result) {
                    ChangeCodeResult.Changed -> {
                        changed = true
                        current = ""
                        newCode = ""
                        repeat = ""
                    }
                    is ChangeCodeResult.WrongCode -> refusal = "Текущий код неверный. Попыток до паузы: ${result.attemptsBeforePause}."
                    is ChangeCodeResult.Paused ->
                        refusal = "Слишком много неверных попыток. Подождите ${formatPause(result.untilMillis - time.nowMillis())}."
                    ChangeCodeResult.Damaged -> refusal = "Файл с ключами не читается."
                }
            }
        }
    }

    val type = NineTheme.typography
    Column(Modifier.widthIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BasicText("Старый код перестанет открывать приложение. Забытый код восстановить нельзя.", style = type.chatPreview)
        FormField(current, { current = it }, "Текущий код", secret = true, filled = true)
        FormField(newCode, { newCode = it }, "Новый код", secret = true, filled = true)
        FormField(repeat, { repeat = it }, "Повторите новый код", secret = true, filled = true, onSubmit = submit)
        when {
            changed -> BasicText("Код изменён.", style = type.chatPreview.copy(color = NineColors.Accent))
            refusal != null -> BasicText(refusal.orEmpty(), style = type.chatPreview.copy(color = NineColors.Danger))
            newCode.isNotEmpty() && repeat.isNotEmpty() && problem != null ->
                BasicText(problem, style = type.chatPreview.copy(color = NineColors.Danger))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
            ActionButton(if (changed) "Готово" else "Назад", onClick = onDone)
            ActionButton(if (busy) "Меняем…" else "Сменить код", onClick = submit, primary = true, enabled = canSubmit)
        }
    }
}
