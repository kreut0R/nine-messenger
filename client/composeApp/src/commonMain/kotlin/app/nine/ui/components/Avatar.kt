package app.nine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nine.ui.theme.NineTheme

// Приглушённые тона, чтобы аватарки не спорили с зелёным акцентом.
private val avatarColors = listOf(
    Color(0xFF3F6B4D), Color(0xFF47607A), Color(0xFF6B6A48), Color(0xFF5F6668),
    Color(0xFF5A5070), Color(0xFF4E5560), Color(0xFF7A5F43), Color(0xFF6B4F5C),
)

/**
 * Круглая аватарка с первой буквой имени. Цвет выбирается по [seed] (id чата), а не по имени,
 * поэтому не меняется, если чат переименуют.
 */
@Composable
fun Avatar(name: String, seed: String, size: Dp, modifier: Modifier = Modifier) {
    val letterSize: TextUnit = (size.value * 0.36f).sp
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarColors[seed.hashCode().mod(avatarColors.size)]),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = name.trim().take(1).uppercase().ifEmpty { "?" },
            style = NineTheme.typography.avatarLetter.copy(fontSize = letterSize),
        )
    }
}

/** Размеры аватарок, повторяющиеся в разных частях экрана. */
object AvatarSize {
    val Small = 32.dp
    val Header = 42.dp
    val Row = 48.dp
    val Profile = 92.dp
}
