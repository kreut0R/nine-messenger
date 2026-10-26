package app.nine.ui.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.nine.domain.Author
import app.nine.domain.Chat
import app.nine.domain.Delivery
import app.nine.domain.TimeSource
import app.nine.presentation.formatListTime
import app.nine.ui.components.Avatar
import app.nine.ui.components.AvatarSize
import app.nine.ui.components.NineIcon
import app.nine.ui.components.NineIcons
import app.nine.ui.components.pressable
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme
import kotlinx.datetime.TimeZone

// Разделитель начинается там, где начинается текст, то есть после аватарки и отступов.
private val DividerStart = 74.dp

/**
 * Строка списка: аватарка, имя со временем и превью последнего сообщения со счётчиком.
 * Выбранная строка залита зелёным, при наведении мыши другие строки слабо подсвечиваются.
 * Минимальное скругление (6 dp) нужно, чтобы подсветка не упиралась в края панели.
 */
@Composable
fun ChatRow(chat: Chat, selected: Boolean, time: TimeSource, onClick: () -> Unit) {
    val type = NineTheme.typography
    val zone = remember { TimeZone.currentSystemDefault() }
    val last = chat.lastMessage

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 77.dp)
                .pressable(
                    onClick = onClick,
                    shape = RoundedCornerShape(6.dp),
                    background = if (selected) NineColors.SelectedRow else Color.Transparent,
                    hoverBackground = if (selected) NineColors.SelectedRow else NineColors.HoverRow,
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Avatar(chat.title, seed = chat.id.value, size = AvatarSize.Row)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BasicText(chat.title, style = type.chatName, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    // Времени нет у пустого чата: показывать момент его создания как «последнее сообщение» было бы неверно.
                    if (last != null) {
                        BasicText(formatListTime(last.sentAtMillis, time.nowMillis(), zone), style = type.time, maxLines = 1)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (last != null && last.author == Author.Me && last.delivery != Delivery.Local) {
                            NineIcon(
                                icon = if (last.delivery == Delivery.Read) NineIcons.DoubleCheck else NineIcons.Check,
                                tint = NineColors.Accent,
                                size = 17.dp,
                            )
                        }
                        BasicText(
                            text = last?.text?.replace('\n', ' ') ?: "Нет сообщений",
                            style = if (last == null) type.time else type.chatPreview,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (chat.unreadCount > 0) UnreadBadge(chat.unreadCount)
                }
            }
        }
        if (!selected) {
            Box(Modifier.padding(start = DividerStart, end = 12.dp).fillMaxWidth().height(1.dp).background(NineColors.Hairline))
        }
    }
}

@Composable
private fun UnreadBadge(count: Int) {
    Box(
        modifier = Modifier
            .widthIn(min = 20.dp)
            .height(20.dp)
            .clip(CircleShape)
            .background(NineColors.Badge)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(count.toString(), style = NineTheme.typography.badge, maxLines = 1)
    }
}
