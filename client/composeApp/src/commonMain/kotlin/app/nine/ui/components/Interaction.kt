package app.nine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role

/**
 * Делает область кнопкой: курсор-рука, свой фон в покое и другой при наведении мыши.
 * Цвета передаются явно, потому что у выбранного элемента списка фон особый и наведение его не меняет.
 */
@Composable
fun Modifier.pressable(
    onClick: () -> Unit,
    shape: Shape,
    hoverBackground: Color,
    background: Color = Color.Transparent,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    return this
        .clip(shape)
        .background(if (hovered) hoverBackground else background)
        .hoverable(source)
        .pointerHoverIcon(PointerIcon.Hand)
        .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
}
