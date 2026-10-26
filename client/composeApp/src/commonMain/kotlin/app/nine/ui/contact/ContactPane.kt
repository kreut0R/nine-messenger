package app.nine.ui.contact

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.nine.domain.BubbleColor
import app.nine.domain.Chat
import app.nine.domain.displayCode
import app.nine.ui.components.Avatar
import app.nine.ui.components.AvatarSize
import app.nine.ui.components.GlassPanel
import app.nine.ui.components.PanelShape
import app.nine.ui.components.SearchField
import app.nine.ui.components.pressable
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme
import app.nine.ui.theme.color
import app.nine.ui.theme.label

/** Ширина правой колонки с данными контакта. */
val ContactPaneWidth = 316.dp

/**
 * Правая колонка: кто это, поиск по переписке, цвет своих сообщений и перенос чата в архив.
 * Здесь нет вещей, которые пока не работают (общие фото, проверка ключей): они появятся вместе с функциями.
 */
@Composable
fun ContactPane(
    chat: Chat,
    bubbleColor: BubbleColor,
    messageSearch: String,
    onMessageSearchChange: (String) -> Unit,
    onBubbleColorChange: (BubbleColor) -> Unit,
    onToggleArchive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = NineTheme.typography
    GlassPanel(modifier.width(ContactPaneWidth)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Avatar(chat.title, seed = chat.id.value, size = AvatarSize.Profile)
                Spacer(Modifier.height(6.dp))
                BasicText(chat.title, style = type.profileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText(chat.id.displayCode, style = type.code)
            }
            SearchField(messageSearch, onMessageSearchChange, placeholder = "Search in chat")
            Section("Message color") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BubbleColor.entries.forEach { color ->
                        Swatch(color, selected = color == bubbleColor, onClick = { onBubbleColorChange(color) })
                    }
                }
            }
            ArchiveButton(archived = chat.archived, onClick = onToggleArchive)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BasicText(title, style = NineTheme.typography.section)
        content()
    }
}

/** Круглый образец цвета. У выбранного вокруг кружка рисуется светлое кольцо. */
@Composable
private fun Swatch(color: BubbleColor, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .pressable(onClick, CircleShape, hoverBackground = NineColors.ControlHover)
            .border(2.dp, if (selected) NineColors.TextPrimary else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
            .padding(4.dp)
            .semantics { contentDescription = color.label },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(color.color)
                .border(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.14f), CircleShape),
        )
    }
}

@Composable
private fun ArchiveButton(archived: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .pressable(onClick, PanelShape, hoverBackground = NineColors.Control)
            .border(1.dp, NineColors.Rim, PanelShape),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = if (archived) "Unarchive" else "Archive chat",
            style = NineTheme.typography.button.copy(color = NineColors.TextSecondary),
        )
    }
}
