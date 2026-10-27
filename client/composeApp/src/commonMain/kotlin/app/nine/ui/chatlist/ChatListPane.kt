package app.nine.ui.chatlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.nine.domain.Chat
import app.nine.domain.ChatId
import app.nine.domain.TimeSource
import app.nine.presentation.ChatTab
import app.nine.presentation.MessengerUiState
import app.nine.presentation.emptyListMessage
import app.nine.ui.components.AnonymityMask
import app.nine.ui.components.GlassPanel
import app.nine.ui.components.IconAction
import app.nine.ui.components.NineIcons
import app.nine.ui.components.SearchField
import app.nine.ui.components.SegmentedTabs
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme

/** Width of the left column with the chat list. */
val ChatListWidth = 360.dp

/**
 * The left column: title, search, tabs and the chat list.
 * @param chats chats already filtered and sorted for the current tab
 * @param totalChats how many chats there are in total, needed only for the empty-list text
 * @param profileMenu the profile button with its menu: what is inside it, the chat list does not know
 */
@Composable
fun ChatListPane(
    chats: List<Chat>,
    totalChats: Int,
    ui: MessengerUiState,
    time: TimeSource,
    onNewChat: () -> Unit,
    profileMenu: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = NineTheme.typography
    GlassPanel(modifier.width(ChatListWidth)) {
        // The mask lies under the list at the bottom center and is visible while there are few chats.
        AnonymityMask(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .width(220.dp),
        )
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 18.dp, top = 28.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BasicText("Chats", style = type.screenTitle, modifier = Modifier.weight(1f))
                profileMenu()
                IconAction(NineIcons.Search, "Search chats", ui::toggleChatSearch, shape = CircleShape, size = 36.dp)
                IconAction(
                    icon = NineIcons.PlusCircle,
                    description = "New chat",
                    onClick = onNewChat,
                    tint = NineColors.TextPrimary,
                    shape = CircleShape,
                    size = 36.dp,
                )
            }
            if (ui.chatSearchOpen) {
                SearchField(
                    value = ui.chatSearch,
                    onValueChange = { ui.chatSearch = it },
                    placeholder = "Search",
                    modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 14.dp),
                )
            }
            SegmentedTabs(
                titles = ChatTab.entries.map { it.title },
                selectedIndex = ui.tab.ordinal,
                onSelect = { ui.tab = ChatTab.entries[it] },
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 18.dp),
            )
            if (chats.isEmpty()) {
                EmptyList(emptyListMessage(totalChats, ui.tab, ui.chatSearch))
            } else {
                ChatList(chats, ui.selectedChatId, time, onSelect = ui::selectChat)
            }
        }
    }
}

@Composable
private fun ChatList(chats: List<Chat>, selectedId: ChatId?, time: TimeSource, onSelect: (ChatId) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
        items(chats, key = { it.id.value }) { chat ->
            ChatRow(chat, selected = chat.id == selectedId, time = time, onClick = { onSelect(chat.id) })
        }
    }
}

@Composable
private fun EmptyList(message: String) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp), contentAlignment = Alignment.TopCenter) {
        BasicText(message, style = NineTheme.typography.chatPreview.copy(textAlign = TextAlign.Center))
    }
}
