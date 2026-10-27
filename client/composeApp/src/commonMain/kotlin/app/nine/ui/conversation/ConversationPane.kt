package app.nine.ui.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.nine.domain.Author
import app.nine.domain.BubbleColor
import app.nine.domain.Chat
import app.nine.domain.Delivery
import app.nine.domain.TimeSource
import app.nine.domain.Message
import app.nine.presentation.MessengerUiState
import app.nine.presentation.TimelineItem
import app.nine.presentation.buildTimeline
import app.nine.presentation.filterMessages
import app.nine.presentation.formatClock
import app.nine.presentation.formatDayHeader
import app.nine.ui.components.Avatar
import app.nine.ui.components.AvatarSize
import app.nine.ui.components.GlassPanel
import app.nine.ui.components.IconAction
import app.nine.ui.components.NineIcon
import app.nine.ui.components.NineIcons
import app.nine.ui.components.PanelShape
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme
import app.nine.ui.theme.color
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val HeaderHeight = 78.dp

// Messages scroll under the header and the input field, so the list has padding at the top and bottom equal to their height.
private val ListTopPadding = HeaderHeight + 26.dp
private val ListBottomPadding = 112.dp

/** The share of the conversation width a bubble may take. The rest is left empty so that lines do not stretch. */
private const val MAX_BUBBLE_FRACTION = 0.62f

/** The central column. While no chat is selected, it shows a hint. */
@Composable
fun ConversationPane(
    chat: Chat?,
    bubbleColor: BubbleColor,
    ui: MessengerUiState,
    time: TimeSource,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPanel(modifier) {
        if (chat == null) {
            CenteredHint("Pick a chat on the left\nor create a new one")
        } else {
            // key recreates everything inside when the chat changes: scroll and focus start anew.
            androidx.compose.runtime.key(chat.id) {
                OpenConversation(chat, bubbleColor, ui, time, onSend)
            }
        }
    }
}

@Composable
private fun OpenConversation(
    chat: Chat,
    bubbleColor: BubbleColor,
    ui: MessengerUiState,
    time: TimeSource,
    onSend: (String) -> Unit,
) {
    val zone = remember { TimeZone.currentSystemDefault() }
    val visible = remember(chat.messages, ui.messageSearch) { filterMessages(chat.messages, ui.messageSearch) }
    val timeline = remember(visible) { buildTimeline(visible, zone) }
    val draft = ui.draftFor(chat.id)

    Box(Modifier.fillMaxSize()) {
        MessageList(timeline, chat, bubbleColor.color, zone, time)
        if (timeline.isEmpty()) {
            CenteredHint(if (chat.messages.isEmpty()) "Write the first message" else "Nothing found")
        }
        ConversationHeader(
            chat = chat,
            contactPaneOpen = ui.contactPaneOpen,
            onToggleContactPane = { ui.contactPaneOpen = !ui.contactPaneOpen },
            modifier = Modifier.align(Alignment.TopCenter),
        )
        Composer(
            draft = draft,
            onDraftChange = { ui.setDraft(chat.id, it) },
            onSend = { onSend(draft) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun MessageList(timeline: List<TimelineItem>, chat: Chat, ownColor: Color, zone: TimeZone, time: TimeSource) {
    val listState = rememberLazyListState()
    // The feed is reversed (reverseLayout), so item 0 is the newest and sits at the bottom.
    // When a new message arrives, return to it even if the person was scrolling through history.
    LaunchedEffect(chat.lastMessage?.id) { listState.scrollToItem(0) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxBubbleWidth = maxWidth * MAX_BUBBLE_FRACTION
        // "Today" is recomputed on every new message: that is enough while the app does not live through midnight.
        val today = remember(chat.lastMessage?.id) {
            Instant.fromEpochMilliseconds(time.nowMillis()).toLocalDateTime(zone).date
        }
        // Selection is needed so that the message text can be copied with the mouse.
        SelectionContainer {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                contentPadding = PaddingValues(start = 40.dp, end = 40.dp, top = ListTopPadding, bottom = ListBottomPadding),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(timeline.asReversed(), key = { it.key }) { item ->
                    when (item) {
                        is TimelineItem.DayHeader -> DayHeader(formatDayHeader(item.date, today))
                        is TimelineItem.Group -> MessageGroup(item, chat, ownColor, maxBubbleWidth, zone)
                    }
                }
            }
        }
    }
}

/** A stable key for a feed item, so that Compose does not recreate rows when messages are added. */
private val TimelineItem.key: String
    get() = when (this) {
        is TimelineItem.DayHeader -> "day-$date"
        is TimelineItem.Group -> "group-${messages.first().id}"
    }

@Composable
private fun DayHeader(text: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BasicText(
            text = text,
            style = NineTheme.typography.dayHeader,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(NineColors.Control)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun MessageGroup(group: TimelineItem.Group, chat: Chat, ownColor: Color, maxBubbleWidth: Dp, zone: TimeZone) {
    val mine = group.author == Author.Me
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!mine) Avatar(chat.title, seed = chat.id.value, size = AvatarSize.Small)
        Column(
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            group.messages.forEach { Bubble(it.text, mine, ownColor, maxBubbleWidth) }
            Stamp(group.messages.last(), mine, zone)
        }
    }
}

@Composable
private fun Bubble(text: String, mine: Boolean, ownColor: Color, maxWidth: Dp) {
    Box(
        modifier = Modifier
            .widthIn(max = maxWidth)
            .clip(PanelShape)
            .background(if (mine) ownColor else NineColors.IncomingBubble)
            .border(1.dp, if (mine) NineColors.OwnBubbleBorder else NineColors.IncomingBubbleBorder, PanelShape)
            .padding(horizontal = 16.dp, vertical = 11.dp),
    ) {
        BasicText(
            text = text,
            style = NineTheme.typography.messageText.copy(color = if (mine) NineColors.OnOwnBubble else NineColors.TextPrimary),
        )
    }
}

/** The time under the last message of a group. The tick appears only on delivered own messages. */
@Composable
private fun Stamp(message: Message, mine: Boolean, zone: TimeZone) {
    Row(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        BasicText(formatClock(message.sentAtMillis, zone), style = NineTheme.typography.stamp)
        if (mine && message.delivery != Delivery.Local) {
            NineIcon(
                icon = if (message.delivery == Delivery.Read) NineIcons.DoubleCheck else NineIcons.Check,
                tint = NineColors.AccentSoft,
                size = 15.dp,
            )
        }
    }
}

@Composable
private fun ConversationHeader(
    chat: Chat,
    contactPaneOpen: Boolean,
    onToggleContactPane: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = NineTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(HeaderHeight)
            .background(NineColors.FloatingBar)
            .drawBehind {
                drawLine(NineColors.Rim, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), strokeWidth = 1f)
            }
            .padding(start = 24.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Avatar(chat.title, seed = chat.id.value, size = AvatarSize.Header)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(chat.title, style = type.chatName, maxLines = 1)
            // There is no "online" status yet: it will come with the server. For now we honestly say where the conversation is stored.
            BasicText("The conversation is stored only on this device", style = type.time, maxLines = 1)
        }
        IconAction(
            icon = NineIcons.SidePanel,
            description = if (contactPaneOpen) "Hide the contact panel" else "Show the contact panel",
            onClick = onToggleContactPane,
            tint = if (contactPaneOpen) NineColors.TextPrimary else NineColors.TextSecondary,
        )
    }
}

@Composable
private fun CenteredHint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(text, style = NineTheme.typography.chatPreview.copy(textAlign = TextAlign.Center))
    }
}
