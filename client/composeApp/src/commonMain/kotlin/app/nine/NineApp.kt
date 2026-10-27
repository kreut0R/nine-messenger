package app.nine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.nine.data.MessengerRepository
import app.nine.domain.displayCode
import app.nine.domain.TimeSource
import app.nine.presentation.MessengerUiState
import app.nine.presentation.filterChats
import app.nine.security.Session
import app.nine.ui.chatlist.ChatListPane
import app.nine.ui.contact.ContactPane
import app.nine.ui.conversation.ConversationPane
import app.nine.ui.dialog.NewChatDialog
import app.nine.ui.platform.FilePicker
import app.nine.ui.platform.SystemClipboard
import app.nine.ui.platform.copyTemporarily
import app.nine.ui.profile.ProfileMenuButton
import app.nine.ui.profile.SignOutDialog
import app.nine.ui.settings.SettingsDialog
import app.nine.ui.settings.SettingsSection
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme
import kotlinx.coroutines.launch

/** Сколько знаков кода аккаунта показывать в меню профиля: две группы по четыре и пробел между ними. */
private const val ShortCodeLength = 9

/** Если окно уже этого, правая колонка скрывается, чтобы переписке хватило места. */
private val ContactPaneMinWindowWidth = 1150.dp

/** Корень интерфейса: три колонки (чаты, переписка, контакт) и окно создания чата поверх них. */
@Composable
fun NineApp(
    repository: MessengerRepository,
    session: Session,
    filePicker: FilePicker,
    clipboard: SystemClipboard,
    onLock: () -> Unit,
    onErase: () -> Unit,
    time: TimeSource = TimeSource.System,
) {
    NineTheme {
        val data by repository.data.collectAsState()
        val ui = remember { MessengerUiState() }
        var settingsSection by remember { mutableStateOf<SettingsSection?>(null) }
        var signOutOpen by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val selected = data.chats.firstOrNull { it.id == ui.selectedChatId }

        // Открытый чат считается прочитанным: сбрасываем счётчик и когда выбрали чат, и когда в него пришло новое.
        LaunchedEffect(selected?.id, selected?.unreadCount) {
            selected?.let { repository.markRead(it.id) }
        }

        Box(Modifier.fillMaxSize().background(NineColors.Background)) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(14.dp)) {
                val showContactPane = selected != null && ui.contactPaneOpen && maxWidth >= ContactPaneMinWindowWidth
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ChatListPane(
                        chats = filterChats(data.chats, ui.tab, ui.chatSearch),
                        totalChats = data.chats.size,
                        ui = ui,
                        time = time,
                        onNewChat = { ui.newChatDialogOpen = true },
                        profileMenu = {
                            ProfileMenuButton(
                                shortCode = session.accountId.displayCode.take(ShortCodeLength),
                                onProfile = { settingsSection = SettingsSection.Profile },
                                onSettings = { settingsSection = SettingsSection.Security },
                                onLock = onLock,
                                onSignOut = { signOutOpen = true },
                            )
                        },
                        modifier = Modifier.fillMaxHeight(),
                    )
                    ConversationPane(
                        chat = selected,
                        bubbleColor = data.bubbleColor,
                        ui = ui,
                        time = time,
                        onSend = { text ->
                            val chatId = ui.selectedChatId ?: return@ConversationPane
                            if (repository.sendMessage(chatId, text)) ui.setDraft(chatId, "")
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    if (showContactPane && selected != null) {
                        ContactPane(
                            chat = selected,
                            bubbleColor = data.bubbleColor,
                            messageSearch = ui.messageSearch,
                            onMessageSearchChange = { ui.messageSearch = it },
                            onBubbleColorChange = repository::setBubbleColor,
                            onToggleArchive = {
                                repository.setArchived(selected.id, !selected.archived)
                                // Чат ушёл из текущего списка, поэтому закрываем его, а не оставляем «висеть» без строки.
                                ui.selectChat(null)
                            },
                            modifier = Modifier.fillMaxHeight(),
                        )
                    }
                }
            }
            settingsSection?.let { section ->
                SettingsDialog(
                    initial = section,
                    settings = data.settings,
                    session = session,
                    filePicker = filePicker,
                    time = time,
                    onAutoLockChange = repository::setAutoLock,
                    onClearClipboardChange = repository::setClearClipboard,
                    onCopy = { text ->
                        scope.launch {
                            if (data.settings.clearClipboard) clipboard.copyTemporarily(text) else clipboard.copy(text)
                        }
                    },
                    onErase = onErase,
                    onDismiss = { settingsSection = null },
                )
            }
            if (signOutOpen) {
                SignOutDialog(onErase = onErase, onDismiss = { signOutOpen = false })
            }
            if (ui.newChatDialogOpen) {
                NewChatDialog(
                    onCreate = { name ->
                        repository.createChat(name)?.let { id ->
                            ui.showAllChats()
                            ui.selectChat(id)
                        }
                        ui.newChatDialogOpen = false
                    },
                    onDismiss = { ui.newChatDialogOpen = false },
                )
            }
        }
    }
}
